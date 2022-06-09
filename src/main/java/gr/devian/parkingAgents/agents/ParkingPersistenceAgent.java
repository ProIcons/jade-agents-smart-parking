package gr.devian.parkingAgents.agents;

import gr.devian.parkingAgents.agents.infra.ManagedAgent;
import gr.devian.parkingAgents.models.*;
import gr.devian.parkingAgents.models.requests.*;
import gr.devian.parkingAgents.models.requests.enumerations.CarRegistrationState;
import gr.devian.parkingAgents.models.responses.*;
import jade.lang.acl.ACLMessage;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;

import static gr.devian.parkingAgents.utils.TimeUtils.TIME_SCALING;

@Slf4j
public class ParkingPersistenceAgent extends ManagedAgent {
    public final static Integer PARKING_SPACES = 500;
    public final static Integer PARKING_GROUPS = 10;

    public final static double COST_PER_MINUTE = 0.083d;
    public final static double WASHING_COST = 15d;
    public final static double REFUELING_COST_PER_MONAD = 3.242d;
    public final static double RECHARGING_COST_PER_MONAD_COST = 2.123d;

    private final Map<Integer, ParkingSession> parkSessionMap = new HashMap<>();

    private final Map<UUID, Client> clientDatabase = new HashMap<>();
    private final Map<String, Car> carDatabase = new HashMap<>();

    public Integer getFreeParkingSpace() {
        return IntStream.range(0, PARKING_SPACES).filter(space -> !parkSessionMap.containsKey(space)).findFirst().getAsInt();
    }

    public boolean hasFreeParkingSpace() {
        return parkSessionMap.size() < PARKING_SPACES;
    }

    @Override
    protected void setupInternal() {
        addCyclicBehavior(
                Handle(RegisterNewClientRequest.class, this::handleRegisterNewClientRequest),
                Handle(HasFreeParkingSpacesRequest.class, this::handleHasFreeParkingSpaceRequest),
                Handle(GetFreeParkingSpaceRequest.class, this::handleGetFreeParkingSpaceRequest),
                Handle(ReleaseParkingSpaceRequest.class, this::handleReleaseParkingSpaceRequest),
                Handle(UpdateParkingSpaceRequest.class, this::handleUpdateParkingSpaceRequest),
                Handle(UpdateTaskStatusRequest.class, this::handleUpdateTaskStatusRequest),
                Handle(RegisterCarIfNotExistsRequest.class, this::handleRegisterCarIfNotExistsRequest),
                Handle(GetCarFromRegistryRequest.class, this::handleGetCarFromRegistryRequest),
                Handle(GetParkingSessionRequest.class, this::handleGetParkedCarByIdRequest),
                Handle(GetClientFromRegistryRequest.class, this::handleGetClientFromRegistryRequest),
                Handle(FinalizeParkingExpensesRequest.class, this::handleFinalizeParkingExpensesRequest)
        );
    }

    private void handleHasFreeParkingSpaceRequest(final ACLMessage receivedMessage, final HasFreeParkingSpacesRequest request) {
        sendResponseToCoordinator(
                receivedMessage,
                HasFreeParkingSpacesResponse
                        .builder()
                        .state(hasFreeParkingSpace())
                        .build());
    }

    private void handleGetFreeParkingSpaceRequest(final ACLMessage receivedMessage, final GetFreeParkingSpaceRequest request) {
        final int parkingSpace = getFreeParkingSpace();
        parkSessionMap.put(parkingSpace, null);

        sendResponseToCoordinator(
                receivedMessage,
                GetFreeParkingSpaceResponse
                        .builder()
                        .parkingSpace(parkingSpace)
                        .build());
    }

    private void handleReleaseParkingSpaceRequest(final ACLMessage receivedMessage, final ReleaseParkingSpaceRequest request) {
        parkSessionMap.remove(request.getParkingSpace());

        sendResponseToCoordinator(
                receivedMessage,
                ReleaseParkingSpaceResponse
                        .builder()
                        .build());
    }

    private void handleUpdateParkingSpaceRequest(final ACLMessage receivedMessage, final UpdateParkingSpaceRequest request) {
        parkSessionMap.put(request.getParkingSpace(), request.getSession());

        sendResponseToCoordinator(
                receivedMessage,
                UpdateParkingSpaceResponse
                        .builder()
                        .build());
    }

    private void handleRegisterCarIfNotExistsRequest(final ACLMessage receivedMessage, final RegisterCarIfNotExistsRequest request) {
        final Optional<Client> clientOptional = getClient(request.getClient());

        if (clientOptional.isEmpty()) {
            throw new IllegalStateException();
        }

        final Client client = clientOptional.get();

        final Optional<Car> car = io.vavr.collection.List.ofAll(client.getCars())
                .find(targetCar -> targetCar.getLicensePlate().equals(request.getCar().getLicensePlate()))
                .toJavaOptional();

        if (car.isPresent()) {
            sendResponseToCoordinator(
                    receivedMessage,
                    RegisterCarIfNotExistsResponse
                            .builder()
                            .carRegistrationState(CarRegistrationState.EXISTED)
                            .car(car.get())
                            .build());
        } else {
            client.getCars()
                    .add(request.getCar());
            carDatabase.put(request.getCar().getLicensePlate(), request.getCar());

            sendResponseToCoordinator(
                    receivedMessage,
                    RegisterCarIfNotExistsResponse
                            .builder()
                            .carRegistrationState(CarRegistrationState.REGISTERED)
                            .car(request.getCar())
                            .build());
        }
    }

    private void handleGetCarFromRegistryRequest(final ACLMessage receivedMessage, final GetCarFromRegistryRequest request) {
        final Optional<Client> clientOptional = getClient(request.getClient());

        if (clientOptional.isEmpty()) {
            throw new IllegalStateException();
        }

        final Client client = clientOptional.get();

        final Optional<Car> car = io.vavr.collection.List.ofAll(client.getCars())
                .find(targetCar -> targetCar.getLicensePlate().equals(request.getLicensePlate()))
                .toJavaOptional();

        if (car.isPresent()) {
            sendResponseToCoordinator(
                    receivedMessage,
                    GetCarFromRegistryResponse
                            .builder()
                            .car(car.get())
                            .exists(true)
                            .build()
            );
        } else {
            final Optional<Car> carOptional = getCarByLicensePlate(request.getLicensePlate());
            if (carOptional.isPresent()) {
                final Car targetCar = carOptional.get();

                client.getCars()
                        .add(targetCar);

                sendResponseToCoordinator(
                        receivedMessage,
                        GetCarFromRegistryResponse
                                .builder()
                                .exists(true)
                                .car(targetCar)
                                .build()
                );
            } else {
                sendResponseToCoordinator(
                        receivedMessage,
                        GetCarFromRegistryResponse
                                .builder()
                                .exists(false)
                                .build()
                );

            }
        }
    }

    private void handleGetParkedCarByIdRequest(final ACLMessage message, final GetParkingSessionRequest request) {
        final Optional<ParkingSession> parkingSessionOptional = getParkingSession(request.getId());

        if (parkingSessionOptional.isPresent()) {
            sendResponseToCoordinator(message, GetParkingSessionResponse
                    .builder()
                    .session(parkingSessionOptional.get())
                    .exists(true)
                    .build());
        } else {
            sendResponseToCoordinator(message, GetParkingSessionResponse
                    .builder()
                    .exists(false)
                    .build());
        }
    }

    private void handleGetClientFromRegistryRequest(final ACLMessage message, final GetClientFromRegistryRequest request) {
        if (clientDatabase.containsKey(request.getId())) {
            sendResponseToCoordinator(
                    message,
                    GetClientFromRegistryResponse
                            .builder()
                            .exists(true)
                            .client(clientDatabase.get(request.getId()))
                            .build()
            );
        } else {
            sendResponseToCoordinator(
                    message,
                    GetClientFromRegistryResponse
                            .builder()
                            .exists(false)
                            .build()
            );
        }
    }

    private void handleFinalizeParkingExpensesRequest(final ACLMessage message, final FinalizeParkingExpensesRequest request) {
        final Optional<ParkingSession> sessionOptional = getParkingSession(request.getSession());

        if (sessionOptional.isEmpty()) {
            throw new IllegalStateException();
        }

        final ParkingSession session = sessionOptional.get();

        final List<Expense> expenses = new LinkedList<>();
        final Car car = session.getCar();

        if (session.getRefuelingOrRecharging() == TaskStatus.COMPLETED || session.getRefuelingOrRecharging() == TaskStatus.ONGOING) {
            final double amount;
            if (car.getType() == CarType.ELECTRIC) {
                amount = car.getRefuelingOrRechargingRemaining() * RECHARGING_COST_PER_MONAD_COST;
            } else if (car.getType() == CarType.GAS) {
                amount = car.getRefuelingOrRechargingRemaining() * REFUELING_COST_PER_MONAD;
            } else {
                amount = 0;
            }

            expenses.add(Expense
                    .builder()
                    .type(ExpenseType.RECHARGING_OR_REFUELING)
                    .amount(amount)
                    .build());
        }

        if (session.getWashing() == TaskStatus.COMPLETED || session.getWashing() == TaskStatus.ONGOING) {
            expenses.add(Expense
                    .builder()
                    .type(ExpenseType.WASHING)
                    .amount(WASHING_COST)
                    .build());
        }

        final double amount = ((Duration.between(session.getParkedSince(), LocalDateTime.now()).toSeconds() / TIME_SCALING) / 60) * COST_PER_MINUTE;
        expenses.add(Expense
                .builder()
                .type(ExpenseType.PARKING)
                .amount(amount)
                .build());

        session.setExpenses(expenses);

        sendResponseToCoordinator(
                message,
                FinalizeParkingExpensesResponse
                        .builder()
                        .parkingSession(session)
                        .build()
        );
    }

    private void handleUpdateTaskStatusRequest(final ACLMessage message, final UpdateTaskStatusRequest request) {
        final Optional<ParkingSession> session = getParkingSession(request.getSession());

        if (session.isPresent()) {
            session.get()
                    .setWashing(request.getSession().getWashing());
            session.get()
                    .setRefuelingOrRecharging(request.getSession().getRefuelingOrRecharging());

            sendResponseToCoordinator(message, UpdateTaskStatusResponse
                    .builder()
                    .parkingSession(session.get())
                    .build());
        }
    }

    private void handleRegisterNewClientRequest(final ACLMessage message, final RegisterNewClientRequest request) {
        final Client client = request.getClient()
                .toBuilder()
                .id(UUID.randomUUID())
                .cars(new LinkedList<>())
                .build();
        clientDatabase.put(client.getId(), client);

        sendResponseToCoordinator(message, RegisterNewClientResponse
                .builder()
                .identifiedClient(client)
                .build());
    }


    private Optional<ParkingSession> getParkingSession(final ParkingSession session) {
        return getParkingSession(session.getId());
    }

    private Optional<ParkingSession> getParkingSession(final UUID session) {
        return parkSessionMap
                .values()
                .stream()
                .filter(Objects::nonNull)
                .filter(parkingSession -> parkingSession.getId().equals(session))
                .findFirst();
    }

    private Optional<Car> getCarByLicensePlate(final String licensePlate) {
        return io.vavr.collection.HashMap.ofAll(carDatabase)
                .get(licensePlate)
                .toJavaOptional();
    }


    private Optional<Client> getClient(final Client client) {
        return getClient(client.getId());
    }

    private Optional<Client> getClient(final UUID id) {
        return io.vavr.collection.HashMap.ofAll(clientDatabase)
                .get(id)
                .toJavaOptional();
    }
}

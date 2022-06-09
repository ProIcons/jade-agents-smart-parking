package gr.devian.parkingAgents.agents;

import gr.devian.parkingAgents.agents.infra.ManagerAgent;
import gr.devian.parkingAgents.models.*;
import gr.devian.parkingAgents.models.events.ParkingAgentOnlineEvent;
import gr.devian.parkingAgents.models.events.RefuelingAndRechargingAgentOnlineEvent;
import gr.devian.parkingAgents.models.events.WashingAgentOnlineEvent;
import gr.devian.parkingAgents.models.requests.*;
import gr.devian.parkingAgents.models.responses.*;
import gr.devian.parkingAgents.models.responses.enumerations.CarEnteringResponseState;
import gr.devian.parkingAgents.models.responses.enumerations.CarExitingResponseState;
import gr.devian.parkingAgents.utils.ParkingUtils;
import io.vavr.collection.List;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import static gr.devian.parkingAgents.agents.ParkingPersistenceAgent.PARKING_GROUPS;
import static gr.devian.parkingAgents.agents.ParkingPersistenceAgent.PARKING_SPACES;
import static gr.devian.parkingAgents.utils.ConsoleUtils.*;

@Slf4j
public class CoordinatorAgent extends ManagerAgent {
    private final HashMap<String, TrackedEntity> trackedCarHashMap = new HashMap<>();
    private final LinkedBlockingQueue<TrackedEntity> trackedEntityParkingAgentQueue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<TrackedEntity> trackedEntityRefuelingAgentQueue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<TrackedEntity> trackedEntityWashingAgentQueue = new LinkedBlockingQueue<>();
    private final HashMap<String, TrackedEntity> ignoreRefuelingMap = new HashMap<>();
    private final HashMap<String, TrackedEntity> ignoreWashingMap = new HashMap<>();
    private final HashMap<AID, Boolean> parkingAgentAvailabilityMap = new HashMap<>();
    private final HashMap<AID, Boolean> washingAgentAvailabilityMap = new HashMap<>();
    private final HashMap<AID, Boolean> refuelingAndRechargingAgentAvailabilityMap = new HashMap<>();

    @Override
    protected void setupInternal() {
        addCyclicBehavior(
            Handle(ParkingAgentOnlineEvent.class, this::handleParkingAgentAnnouncements),
            Handle(WashingAgentOnlineEvent.class, this::handleWashingAgentAnnouncements),
            Handle(RefuelingAndRechargingAgentOnlineEvent.class, this::handleRefuelingAndRechargingAgentAnnouncements),

            // Handle Car Entering Coordination
            Handle(CarEnteringRequest.class, this::handleCarEnteringRequest),

            Handle(GetClientFromRegistryResponse.class, this::handleGetClientFromRegistryResponse),
            Handle(RegisterNewClientResponse.class, this::handleRegisterNewClientResponse),
            Handle(HasFreeParkingSpacesResponse.class, this::handleHasFreeParkingSpacesResponse),
            Handle(EntrancePhotoResponse.class, this::handleEntrancePhotoResponse),
            Handle(CarDetailsResponse.class, this::handleCarDetailsResponse),
            Handle(FuelDetectionResponse.class, this::handleModelAndBrandDetectionResponse),
            Handle(GetFreeParkingSpaceResponse.class, this::handleGetFreeParkingSpaceResponse),
            Handle(RegisterCarIfNotExistsResponse.class, this::handleRegisterCarIfNotExistsResponse),
            Handle(ParkCarResponse.class, this::handleParkCarResponse),
            Handle(RefuelingOrRechargingResponse.class, this::handleRefuelingOrRechargingResponse),
            Handle(WashingResponse.class, this::handleWashingResponse),

            // Handle Car Exiting Coordination
            Handle(CarExitingRequest.class, this::handleCarExitingRequest),

            Handle(GetParkingSessionResponse.class, this::handleGetParkingSessionResponse),
            Handle(FinalizeParkingExpensesResponse.class, this::handleFinalizeParkingExpensesResponse),
            Handle(CheckoutResponse.class, this::handleCheckoutResponse),
            Handle(UnparkCarResponse.class, this::handleUnparkCarResponse)
        );

        addRecurringBehavior(Duration.ofSeconds(1), this::handleRefuelingAndRechargingOrchestration);
        addRecurringBehavior(Duration.ofSeconds(1), this::handleWashingOrchestration);
        addRecurringBehavior(Duration.ofSeconds(1), this::handleParkingAndUnparkingOrchestration);

    }

    // Handle Car Parking Orchestration
    private void handleCarEnteringRequest(final ACLMessage message, final CarEnteringRequest request) {
        final ParkingSession.ParkingSessionBuilder parkingSessionBuilder = ParkingSession.builder()
                                                                                         .id(UUID.randomUUID())
                                                                                         .refuelingOrRecharging(
                                                                                             request.isRequestedRechargingOrRefueling() ? TaskStatus.REQUESTED : TaskStatus.NOT_REQUESTED)
                                                                                         .washing(
                                                                                             request.isRequestedWashing() ? TaskStatus.REQUESTED : TaskStatus.NOT_REQUESTED);

        track(message,
            TrackedEntityIntention.ENTER,
            builder -> builder
                .carId(request.getCarId())
                .session(parkingSessionBuilder.build()));

        if (request.isNewClient()) {
            log.info(GREEN + ">>>>> [{}]" + RESET + " Car Requested [W:{}|R:{}] Entry for Unregistered Client. Registering Client...",
                message.getConversationId(),
                request.isRequestedWashing(),
                request.isRequestedRechargingOrRefueling());


            sendRequest(RegisterNewClientRequest.builder()
                                                .client(request.getClient())
                                                .build(),
                message,
                ParkingPersistenceAgent.class);

        } else {
            log.info(GREEN + ">>>>> [{}]" + RESET + " Car Requested [W:{}|R:{}] Entry for Client '{}'. Checking available Parking Spaces...",
                message.getConversationId(),
                request.isRequestedWashing(),
                request.isRequestedRechargingOrRefueling(),
                request.getClientId());

            sendRequest(GetClientFromRegistryRequest
                    .builder()
                    .id(request.getClientId())
                    .build(),
                message,
                ParkingPersistenceAgent.class
            );
        }


    }

    private void handleHasFreeParkingSpacesResponse(final ACLMessage message, final HasFreeParkingSpacesResponse response) {
        if (response.isState()) {
            log.info(GREEN + ">>>>> [{}]" + RESET + " Available Space Found! Taking Photo of the Car...", message.getConversationId());
            sendRequest(
                EntrancePhotoRequest.builder().build(),
                message,
                EntranceCameraAgent.class
            );
        } else {
            log.info(GREEN + ">>>>> [{}]" + RESET + " Available Spaces Not Found! Rejecting Parking Session...", message.getConversationId());

            untrack(message);

            sendResponse(
                CarEnteringResponse
                    .builder()
                    .response(CarEnteringResponseState.PARKING_IS_FULL)
                    .build(),
                message,
                GateAgent.class
            );
        }
    }

    private void handleEntrancePhotoResponse(final ACLMessage message, final EntrancePhotoResponse response) {
        final TrackedEntity trackedEntity = getTracked(message);

        trackedEntity.setCarPhoto(response.getPhoto());
        log.info(GREEN + ">>>>> [{}]" + RESET + " Photo Taken! Detecting Car Details...", message.getConversationId());

        sendRequest(
            CarDetailsRequest
                .builder()
                .carId(trackedEntity.getCarId())
                .build(),
            message,
            CarDetailsScannerAgent.class
        );
    }

    private void handleCarDetailsResponse(final ACLMessage message, final CarDetailsResponse response) {
        final TrackedEntity trackedEntity = getTracked(message);

        log.info("\u001b[32m>>>>> [{}] Car Details Detected '{}'! Checking if car exists on registry.",
            message.getConversationId(), response.getLicensePlate());

        final var builder = Car.builder()
                               .id(trackedEntity.getCarId())
                               .licensePlate(response.getLicensePlate())
                               .model(response.getModel())
                               .brand(response.getBrand())
                               .type(response.getCarType())
                               .color(response.getColor());

        if (response.getCarType() == CarType.ELECTRIC) {
            builder.batteryCapacity(response.getFuelCapacity())
                   .batteryLevel(response.getFuelLevel());
        } else {
            builder.fuelTankCapacity(response.getFuelCapacity())
                   .fuelLevel(response.getFuelLevel());
        }

        final var car = builder.build();

        trackedEntity.getSession()
                     .setCar(car);

        log.info(GREEN + ">>>>> [{}]" + RESET + " Registering car if it doesnt exist on database and Requesting a free parking space...",
            message.getConversationId());

        sendRequest(
            RegisterCarIfNotExistsRequest
                .builder()
                .client(trackedEntity.getSession().getClient())
                .car(trackedEntity.getSession().getCar())
                .build(),
            message,
            ParkingPersistenceAgent.class);

        sendRequest(
            GetFreeParkingSpaceRequest.builder().build(),
            message,
            ParkingPersistenceAgent.class
        );
    }


    private void handleModelAndBrandDetectionResponse(final ACLMessage message, final FuelDetectionResponse response) {
        final TrackedEntity trackedEntity = getTracked(message);

        final Car.CarBuilder builder = Car.builder()
                                          .id(UUID.randomUUID())
                                          .carImage(trackedEntity.getCarPhoto())
                                          .model(response.getModel())
                                          .brand(response.getBrand())
                                          .licensePlate(trackedEntity.getLicensePlate())
                                          .color(response.getColor())
                                          .type(response.getType());

        if (response.getType() == CarType.ELECTRIC) {
            builder.batteryCapacity(response.getCapacity())
                   .batteryLevel(response.getLevel());
        } else {
            builder.fuelTankCapacity(response.getCapacity())
                   .fuelLevel(response.getLevel());
        }

        final Car car = builder.build();

        trackedEntity.getSession()
                     .setCar(car);

        log.info(GREEN + ">>>>> [{}]" + RESET + " [Model, Brand & Color]=[{},{} & {}] Detection Completed. "
                 + "Registering Car on Registry and Requesting a free parking space...",
            message.getConversationId(), response.getModel(), response.getBrand(), response.getColor());

        sendRequest(
            RegisterCarIfNotExistsRequest
                .builder()
                .client(trackedEntity.getSession().getClient())
                .car(trackedEntity.getSession().getCar())
                .build(),
            message,
            ParkingPersistenceAgent.class);

        sendRequest(
            GetFreeParkingSpaceRequest.builder().build(),
            message,
            ParkingPersistenceAgent.class
        );

    }

    private void handleRegisterCarIfNotExistsResponse(final ACLMessage message, final RegisterCarIfNotExistsResponse response) {
        final TrackedEntity trackedEntity = getTracked(message);

        trackedEntity.getSession().setCar(response.getCar());

        log.info(GREEN + ">>>>> [{}]" + RESET + " Retrieved Car From Registry ({})",
            message.getConversationId(),
            response.getCarRegistrationState().name());
        log.info("\tID           : {}", response.getCar().getId());
        log.info("\tBrand        : {}", response.getCar().getBrand());
        log.info("\tModel        : {}", response.getCar().getModel());
        log.info("\tLicense Plate: {}", response.getCar().getLicensePlate());
        log.info("\tColor        : {}", response.getCar().getColor());
        log.info("\tType         : {}", response.getCar().getType().name());
    }

    private void handleGetFreeParkingSpaceResponse(final ACLMessage message, final GetFreeParkingSpaceResponse response) {
        final TrackedEntity trackedEntity = getTracked(message);

        trackedEntity.getSession()
                     .setParkingSpot(response.getParkingSpace());

        log.info(GREEN + ">>>>> " + MAGENTA + "[Parking Coordination][{}]" + RESET + " Parking Space Acquired '{}' for Car '{}', Queuing for Parking...",
            message.getConversationId(),
            ParkingUtils.getParkingSpotString(response.getParkingSpace(), PARKING_SPACES / PARKING_GROUPS),
            trackedEntity.getSession().getCar().getLicensePlate());

        trackedEntityParkingAgentQueue.add(trackedEntity);
    }

    private void handleParkCarResponse(final ACLMessage message, final ParkCarResponse response) {
        final TrackedEntity trackedEntity = getTracked(message);

        final AID parkingAgent = getAgentByName(parkingAgentAvailabilityMap, message.getSender().getName());

        trackedEntity.getSession()
                     .setParkingSpot(response.getParkingSession().getParkingSpot());
        trackedEntity.getSession()
                     .setParkedSince(response.getParkingSession().getParkedSince());

        sendRequest(UpdateParkingSpaceRequest
                .builder()
                .session(trackedEntity.getSession())
                .parkedSince(trackedEntity.getSession().getParkedSince())
                .parkingSpace(trackedEntity.getSession().getParkingSpot())
                .build(),
            message,
            ParkingPersistenceAgent.class);

        if (parkingAgent == null) {
            throw new IllegalStateException();
        }

        log.info(GREEN + ">>>>> " + MAGENTA + "[Parking Coordination][{}]" + RESET + " Car '{}' Parked at Space: {}. Parking Agent is free.",
            message.getConversationId(),
            response.getParkingSession().getCar().getLicensePlate(),
            ParkingUtils.getParkingSpotString(
                response.getParkingSession().getParkingSpot(),
                PARKING_SPACES / PARKING_GROUPS
            ));
        parkingAgentAvailabilityMap.put(parkingAgent, true);

        sendResponse(
            CarEnteringResponse.builder()
                               .response(CarEnteringResponseState.OK)
                               .parkingSession(trackedEntity.getSession())
                               .build(),
            message,
            GateAgent.class
        );

        if (response.getParkingSession().getRefuelingOrRecharging() == TaskStatus.REQUESTED) {
            log.info(GREEN + ">>>>> " + CYAN + "[Refueling/Recharging Coordination][{}]" + RESET + " Sending Car '{}' to queue for Refueling/Recharging...",
                message.getConversationId(),
                trackedEntity.getSession().getCar().getLicensePlate());


            trackedEntityRefuelingAgentQueue.add(trackedEntity);
        } else if (response.getParkingSession().getWashing() == TaskStatus.REQUESTED) {
            log.info(GREEN + ">>>>> " + YELLOW + "[Washing Coordination][{}]" + RESET + " Sending Car '{}' to queue for Washing...",
                message.getConversationId(),
                trackedEntity.getSession().getCar().getLicensePlate());
            trackedEntityWashingAgentQueue.add(trackedEntity);
        }
    }

    private void handleCarExitingRequest(final ACLMessage message, final CarExitingRequest request) {
        track(
            message,
            TrackedEntityIntention.EXIT,
            builder -> builder.parkingSessionId(request.getParkingSessionId())
        );

        sendRequest(
            GetParkingSessionRequest.builder()
                                    .id(request.getParkingSessionId())
                                    .build(),
            message,
            ParkingPersistenceAgent.class
        );
    }

    private void handleRegisterNewClientResponse(final ACLMessage message, final RegisterNewClientResponse response) {
        final TrackedEntity trackedEntity = getTracked(message);

        trackedEntity
            .getSession()
            .setClient(response.getIdentifiedClient());

        log.info(GREEN + ">>>>> [{}]" + RESET + " Client '{}' Registered. Checking available Parking Spaces...",
            message.getConversationId(),
            response.getIdentifiedClient().getId());

        sendRequest(
            HasFreeParkingSpacesRequest.builder().build(),
            message,
            ParkingPersistenceAgent.class
        );
    }

    private void handleGetParkingSessionResponse(final ACLMessage message, final GetParkingSessionResponse response) {
        final TrackedEntity trackedEntity = getTracked(message);
        if (trackedEntity.getIntention() == TrackedEntityIntention.EXIT) {
            if (!response.isExists()) {
                log.warn(RED + "<<<<< [{}]" + RESET + " ParkSession with Id: '{}' Not Found!. Rejecting Exiting Procedure...",
                    message.getConversationId(),
                    trackedEntity.getParkingSessionId());
                sendResponse(
                    CarExitingResponse
                        .builder()
                        .response(CarExitingResponseState.CAR_NOT_ON_PARKING)
                        .build(),
                    message,
                    GateAgent.class
                );
            } else {
                log.info(RED + "<<<<< [{}]" + RESET + " Car '{}' Found!. Finalizing Expenses...",
                    message.getConversationId(),
                    response.getSession().getCar().getLicensePlate());

                trackedEntity.setSession(response.getSession());

                if (response.getSession().getWashing() == TaskStatus.REQUESTED) {
                    ignoreWashingMap.put(message.getConversationId(), trackedEntity);
                }
                if (response.getSession().getRefuelingOrRecharging() == TaskStatus.REQUESTED) {
                    ignoreRefuelingMap.put(message.getConversationId(), trackedEntity);
                }

                sendRequest(FinalizeParkingExpensesRequest
                        .builder()
                        .session(response.getSession())
                        .build(),
                    message,
                    ParkingPersistenceAgent.class);
            }
        }
    }

    private void handleGetClientFromRegistryResponse(final ACLMessage message, final GetClientFromRegistryResponse response) {
        final TrackedEntity trackedEntity = getTracked(message);

        if (trackedEntity.getIntention() == TrackedEntityIntention.ENTER) {
            if (response.isExists()) {
                log.info(GREEN + ">>>>> [{}]" + RESET + " Client found on database! Checking if there are free spaces...",
                    message.getConversationId());

                trackedEntity.getSession().setClient(response.getClient());

                sendRequest(
                    HasFreeParkingSpacesRequest.builder().build(),
                    message,
                    ParkingPersistenceAgent.class
                );
            } else {
                untrack(message);
                log.warn(GREEN + ">>>>> [{}]" + RESET + " Client not found on database! Restricting entry.",
                    message.getConversationId());
                sendResponse(
                    CarEnteringResponse.builder()
                                       .response(CarEnteringResponseState.CLIENT_NOT_FOUND)
                                       .build(),
                    message,
                    GateAgent.class
                );
            }
        } else {
            throw new UnsupportedOperationException("This response currently doesn't work on Exiting");
        }
    }

    private void handleFinalizeParkingExpensesResponse(final ACLMessage message, final FinalizeParkingExpensesResponse response) {
        final TrackedEntity trackedEntity = getTracked(message);

        trackedEntity.setSession(response.getParkingSession());

        log.info(RED + "<<<<< [{}]" + RESET + " Finalized Expenses for ParkSession with id: '{}'. Checking out...",
            message.getConversationId(),
            trackedEntity.getSession().getId());

        sendRequest(
            CheckoutRequest
                .builder()
                .paymentInformation(trackedEntity.getSession().getClient().getPaymentInformation())
                .amount(trackedEntity.getSession().getExpensesSum())
                .build(),
            message,
            CheckoutAgent.class
        );
    }

    private void handleCheckoutResponse(final ACLMessage message, final CheckoutResponse response) {
        final TrackedEntity trackedEntity = getTracked(message);

        final var expensesString = List.ofAll(trackedEntity.getSession().getExpenses())
                                       .map(expense -> String.format("%c:%f", expense.getType().name().toCharArray()[0], expense.getAmount()))
                                       .mkString("|");

        if (response.getStatus() == CheckoutStatus.CHECKED_OUT) {
            log.info(
                RED + "<<<<< " + MAGENTA + "[Parking Coordination][{}]" + RESET + " Checked out successfully for the amount of {} for Car '{}' [{}]. Queueing for unparking...",
                message.getConversationId(),
                response.getAmount(),
                trackedEntity.getSession().getCar().getLicensePlate(),
                expensesString);

            trackedEntityParkingAgentQueue.add(trackedEntity);
        } else {
            throw new UnsupportedOperationException(String.format("State %s not supported yet", response.getStatus()));
        }
    }

    private void handleUnparkCarResponse(final ACLMessage message, final UnparkCarResponse response) {
        final TrackedEntity trackedEntity = getTracked(message);

        untrack(message);

        final AID parkingAgent = getAgentByName(parkingAgentAvailabilityMap, message.getSender().getName());

        parkingAgentAvailabilityMap.put(parkingAgent, true);

        sendResponse(
            CarExitingResponse
                .builder()
                .parkingSession(trackedEntity.getSession())
                .response(CarExitingResponseState.OK)
                .build(),
            message,
            GateAgent.class
        );
    }

    private void handleWashingResponse(final ACLMessage message, final WashingResponse response) {
        final TrackedEntity trackedEntity = getTracked(message);
        log.info(GREEN + ">>>>> " + YELLOW + "[Washing Coordination][{}]" + RESET + " Car '{}' Washed!",
            message.getConversationId(),
            trackedEntity.getSession().getCar().getLicensePlate());

        trackedEntity.getSession()
                     .setWashing(TaskStatus.COMPLETED);

        final AID washingAgent = getAgentByName(washingAgentAvailabilityMap, message.getSender().getName());

        washingAgentAvailabilityMap.put(washingAgent, true);

        sendRequest(
            UpdateTaskStatusRequest
                .builder()
                .session(trackedEntity.getSession())
                .build(),
            message, ParkingPersistenceAgent.class);
    }

    private void handleRefuelingOrRechargingResponse(final ACLMessage message, final RefuelingOrRechargingResponse response) {
        final TrackedEntity trackedEntity = getTracked(message);

        log.info(GREEN + ">>>>> " + CYAN + "[Refueling/Recharging Coordination][{}]" + RESET + " Car '{}' Refueled/Recharged!",
            message.getConversationId(),
            trackedEntity.getSession().getCar().getLicensePlate());

        trackedEntity.getSession()
                     .setRefuelingOrRecharging(TaskStatus.COMPLETED);

        final AID refuelingAndRechargingAgent = getAgentByName(refuelingAndRechargingAgentAvailabilityMap, message.getSender().getName());

        refuelingAndRechargingAgentAvailabilityMap.put(refuelingAndRechargingAgent, true);

        sendRequest(
            UpdateTaskStatusRequest
                .builder()
                .session(trackedEntity.getSession())
                .build(),
            message, ParkingPersistenceAgent.class);

        if (trackedEntity.getSession().getWashing() == TaskStatus.REQUESTED) {
            log.info(GREEN + ">>>>> " + YELLOW + "[Washing Coordination][{}]" + RESET + " Sending Car '{}' to queue for Washing...",
                message.getConversationId(),
                trackedEntity.getSession().getCar().getLicensePlate());

            trackedEntityWashingAgentQueue.add(trackedEntity);
        }
    }

    // Recurring Orchestrations
    private void handleRefuelingAndRechargingOrchestration() {
        if (trackedEntityRefuelingAgentQueue.size() == 0) {
            return;
        }

        if ((int) refuelingAndRechargingAgentAvailabilityMap.values().stream().filter(Boolean::booleanValue).count() == 0) {
            log.info(
                GREEN + ">>>>> " + CYAN + "[Refueling/Recharging Coordination]" + RESET + " All Refueling/Recharging Agents are busy, will retry in 10 seconds...");
            return;
        }

        final AID refuelingAndRechargingAgent = getAvailableAgent(refuelingAndRechargingAgentAvailabilityMap);

        refuelingAndRechargingAgentAvailabilityMap.put(refuelingAndRechargingAgent, false);
        TrackedEntity trackedEntity;
        do {
            trackedEntity = trackedEntityRefuelingAgentQueue.poll();

            if (trackedEntity == null) {
                break;
            }

            if (!ignoreRefuelingMap.containsKey(trackedEntity.getConversationId())) {
                break;
            } else {
                ignoreRefuelingMap.remove(trackedEntity.getConversationId());
            }
        } while (true);

        if (trackedEntity == null) {
            return;
        }

        log.info(
            GREEN + ">>>>> " + CYAN + "[Refueling/Recharging Coordination][{}]" + RESET + " Refueling/Recharging agent {} Retrieving Car {} for Refueling/Recharging...",
            trackedEntity.getConversationId(),
            refuelingAndRechargingAgent.getName(), trackedEntity.getSession().getCar().getLicensePlate());

        trackedEntity
            .getSession()
            .setRefuelingOrRecharging(TaskStatus.ONGOING);

        sendRequest(
            UpdateTaskStatusRequest
                .builder()
                .session(trackedEntity.getSession())
                .build(),
            trackedEntity.getConversationId(),
            ParkingPersistenceAgent.class);

        sendRequest(
            RefuelingOrRechargingRequest
                .builder()
                .session(trackedEntity.getSession())
                .build(),
            trackedEntity.getConversationId(),
            refuelingAndRechargingAgent.getLocalName());
    }

    private void handleWashingOrchestration() {
        if (trackedEntityWashingAgentQueue.size() == 0) {
            return;
        }

        if ((int) washingAgentAvailabilityMap.values().stream().filter(Boolean::booleanValue).count() == 0) {
            log.info(GREEN + ">>>>> " + YELLOW + "[Washing Coordination]" + RESET + " All WashingAgents are busy, will retry in 10 seconds...");
            return;
        }

        final AID washingAgent = getAvailableAgent(washingAgentAvailabilityMap);

        washingAgentAvailabilityMap.put(washingAgent, false);

        TrackedEntity trackedEntity;
        do {
            trackedEntity = trackedEntityWashingAgentQueue.poll();

            if (trackedEntity == null) {
                break;
            }

            if (!ignoreWashingMap.containsKey(trackedEntity.getConversationId())) {
                break;
            } else {
                ignoreWashingMap.remove(trackedEntity.getConversationId());
            }
        } while (true);

        if (trackedEntity == null) {
            return;
        }

        log.info(GREEN + ">>>>> " + YELLOW + "[Washing Coordination][{}]" + RESET + " Washing agent {} Retrieving Car {} for Washing...",
            trackedEntity.getConversationId(),
            washingAgent.getName(), trackedEntity.getSession().getCar().getLicensePlate());

        trackedEntity
            .getSession()
            .setWashing(TaskStatus.ONGOING);

        sendRequest(
            UpdateTaskStatusRequest
                .builder()
                .session(trackedEntity.getSession())
                .build(),
            trackedEntity.getConversationId(),
            ParkingPersistenceAgent.class);

        sendRequest(
            WashingRequest
                .builder()
                .session(trackedEntity.getSession())
                .build(),
            trackedEntity.getConversationId(),
            washingAgent.getLocalName());

    }

    private void handleParkingAndUnparkingOrchestration() {
        if (trackedEntityParkingAgentQueue.size() == 0) {
            return;
        }

        if ((int) parkingAgentAvailabilityMap.values().stream().filter(Boolean::booleanValue).count() == 0) {
            log.info(GREEN + ">>>>> " + MAGENTA + "[Parking Coordination] All ParkingAgents are busy, will retry in 10 seconds...");
            return;
        }

        final AID parkingAgent = getAvailableAgent(parkingAgentAvailabilityMap);

        parkingAgentAvailabilityMap.put(parkingAgent, false);

        final TrackedEntity trackedEntity = trackedEntityParkingAgentQueue.poll();

        if (trackedEntity == null) {
            return;
        }

        if (trackedEntity.getIntention() == TrackedEntityIntention.ENTER) {

            log.info(GREEN + ">>>>> " + MAGENTA + "[Parking Coordination][{}]" + RESET + " Parking agent {} Retrieving Car {} for Parking...",
                trackedEntity.getConversationId(),
                parkingAgent.getName(), trackedEntity.getSession().getCar().getLicensePlate());

            sendRequest(
                ParkCarRequest
                    .builder()
                    .parkingSession(trackedEntity.getSession())
                    .build(),
                trackedEntity.getConversationId(),
                parkingAgent.getLocalName()
            );
        } else {
            log.info(RED + "<<<<< " + MAGENTA + "[Parking Coordination][{}]" + RESET + " Parking agent {} Retrieving Car {} for Unparking...",
                trackedEntity.getConversationId(),
                parkingAgent.getName(), trackedEntity.getSession().getCar().getLicensePlate());
            sendRequest(
                UnparkCarRequest
                    .builder()
                    .parkingSession(trackedEntity.getSession())
                    .build(),
                trackedEntity.getConversationId(),
                parkingAgent.getLocalName()
            );
        }

    }


    // Handle Agent Announcements
    private void handleParkingAgentAnnouncements(final ACLMessage message, final ParkingAgentOnlineEvent event) {
        log.info("A Parking Agent has been registered to coordinator: {}", message.getSender().getName());
        parkingAgentAvailabilityMap.put(message.getSender(), true);
    }

    private void handleRefuelingAndRechargingAgentAnnouncements(final ACLMessage message, final RefuelingAndRechargingAgentOnlineEvent event) {
        log.info("A Refueling and Recharging Agent has been registered to coordinator: {}", message.getSender().getName());
        refuelingAndRechargingAgentAvailabilityMap.put(message.getSender(), true);
    }

    private void handleWashingAgentAnnouncements(final ACLMessage message, final WashingAgentOnlineEvent event) {
        log.info("A Washing Agent has been registered to coordinator: {}", message.getSender().getName());
        washingAgentAvailabilityMap.put(message.getSender(), true);
    }


    private AID getAvailableAgent(final HashMap<AID, Boolean> map) {
        return map
            .entrySet()
            .stream()
            .filter(Map.Entry::getValue)
            .map(Map.Entry::getKey)
            .findFirst()
            .get();
    }

    private AID getAgentByName(final HashMap<AID, Boolean> map, final String name) {
        return map
            .keySet()
            .stream().filter(agent -> agent.getName().equals(name))
            .findFirst()
            .orElse(null);
    }

    // Utility

    private void track(final ACLMessage message, final TrackedEntityIntention intention) {
        track(message, intention, (ignored) -> {
        });
    }

    private void track(final ACLMessage message, final TrackedEntityIntention intention,
                       final Consumer<TrackedEntity.TrackedEntityBuilder> trackedEntityBuilderConsumer) {
        final TrackedEntity.TrackedEntityBuilder trackedEntityBuilder = TrackedEntity.builder()
                                                                                     .intention(intention)
                                                                                     .conversationId(message.getConversationId());
        trackedEntityBuilderConsumer.accept(trackedEntityBuilder);

        trackedCarHashMap.put(message.getConversationId(), trackedEntityBuilder.build());
    }

    private void untrack(final ACLMessage message) {
        trackedCarHashMap.remove(message.getConversationId());
    }

    private TrackedEntity getTracked(final ACLMessage message) {
        return trackedCarHashMap.get(message.getConversationId());
    }

}

package gr.devian.parkingAgents.agents;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.curiousoddman.rgxgen.RgxGen;
import com.github.javafaker.Faker;
import gr.devian.parkingAgents.agents.infra.ManagedAgent;
import gr.devian.parkingAgents.models.Address;
import gr.devian.parkingAgents.models.CarType;
import gr.devian.parkingAgents.models.Client;
import gr.devian.parkingAgents.models.PaymentInformation;
import gr.devian.parkingAgents.models.events.CarAnnouncedEvent;
import gr.devian.parkingAgents.models.events.CarLeavingEvent;
import gr.devian.parkingAgents.models.model.CarModel;
import gr.devian.parkingAgents.models.requests.CarInfoRequest;
import gr.devian.parkingAgents.models.requests.CarReleaseRequest;
import gr.devian.parkingAgents.models.requests.FuelInfoRequest;
import gr.devian.parkingAgents.models.requests.StoreParkingSessionRequest;
import gr.devian.parkingAgents.models.responses.CarInfoResponse;
import gr.devian.parkingAgents.models.responses.FuelInfoResponse;
import jade.lang.acl.ACLMessage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static gr.devian.parkingAgents.utils.TimeUtils.TIME_SCALING;

@Slf4j
public class CarAgent extends ManagedAgent {

    private final Random random = new Random(System.currentTimeMillis());
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Faker faker = new Faker(new Random(System.currentTimeMillis()));

    private CarModel carModel;
    private String model;
    private Color color;
    private Integer capacity;
    private double level;
    private CarType type;
    private UUID id;

    private String licensePlate;

    private Client client;

    private UUID parkSessionId = null;

    @Override
    @SneakyThrows
    protected void setupInternal() {

        final TypeReference<List<CarModel>> reference = new TypeReference<>() {
        };
        final List<CarModel> carModels = objectMapper
            .readValue(getClass().getClassLoader().getResourceAsStream("carDatabase.json"), reference);

        carModel = carModels.get(random.nextInt(carModels.size()));
        model = carModel.getModels().get(random.nextInt(carModel.getModels().size()));
        color = new Color((int) (Math.random() * 0x1000000));
        capacity = random.nextInt(120 - 50) + 50;
        type = CarType.values()[random.nextInt(CarType.values().length)];
        id = UUID.fromString(getLocalName());
        licensePlate = getLicensePlate();
        client = generateNewClient();
        level = capacity * random.nextDouble();

        addInitBehavior(this::handleEnterAnnouncement);

        addCyclicBehavior(
            Handle(CarInfoRequest.class, this::handleCarInfoRequest),
            Handle(FuelInfoRequest.class, this::handleFuelInfoRequest),
            Handle(StoreParkingSessionRequest.class, this::handleStoreParkingSessionRequest),
            Handle(CarReleaseRequest.class, this::handleCarReleaseRequest)
        );

        addTimedBehavior(Duration.ofMillis((long) (random.nextInt((2160000 - 1800000) + 1800000) * TIME_SCALING)), this::handleLeaveAnnouncement);
    }

    private void handleCarInfoRequest(final ACLMessage receivedMessage, final CarInfoRequest request) {
        sleepRandom(100, 200);
        sendResponseToSender(
            receivedMessage,
            CarInfoResponse
                .builder()
                .id(id)
                .licensePlate(licensePlate)
                .carType(type)
                .brand(carModel.getBrand())
                .model(model)
                .color(color)
                .fuelCapacity(capacity)
                .fuelLevel(level)
                .build()
        );
    }

    private Client generateNewClient() {
        return Client
            .builder()
            .firstName(faker.name().firstName())
            .lastName(faker.name().lastName())
            .birthday(faker.date().birthday())
            .address(Address
                .builder()
                .buildingNumber(faker.address().buildingNumber())
                .streetName(faker.address().streetName())
                .streetAddress(faker.address().streetAddress())
                .city(faker.address().city())
                .country(faker.address().country())
                .postalCode(faker.address().zipCode())
                .build())
            .paymentInformation(PaymentInformation.
                builder()
                .creditCardNumber(faker.business().creditCardNumber())
                .expiryDate(faker.business().creditCardExpiry())
                .holderName(faker.name().fullName())
                .build())
            .build();
    }

    private String getLicensePlate() {
        final RgxGen rgxGen = new RgxGen("[ABEHIKMNOPTXYZ]{3} [0-9]{4}");
        return rgxGen.generate(random);
    }

    private void handleFuelInfoRequest(final ACLMessage message, final FuelInfoRequest response) {
        sleepRandom(100, 200);
        sendResponseToSender(
            message,
            FuelInfoResponse
                .builder()
                .capacity(capacity)
                .level(rng.nextDouble(0, capacity))
                .build()
        );
    }

    private void handleStoreParkingSessionRequest(final ACLMessage message, final StoreParkingSessionRequest response) {
        parkSessionId = response.getParkingSessionId();
    }

    private void handleLeaveAnnouncement() {
        if (parkSessionId != null) {
            sendEvent(
                CarLeavingEvent.builder()
                               .parkSessionId(parkSessionId)
                               .build(),
                GateAgent.class);
        }
    }

    private void handleEnterAnnouncement() {
        final var carAnnouncement = CarAnnouncedEvent
            .builder()
            .client(client)
            .carId(id)
            .requestRefuelingOrRecharging(random.nextBoolean())
            .requestWashing(random.nextBoolean())
            .build();
        sendEvent(
            carAnnouncement,
            GateAgent.class
        );
    }

    @SneakyThrows
    private void handleCarReleaseRequest(final ACLMessage message, final CarReleaseRequest response) {
        doDelete();
    }
}

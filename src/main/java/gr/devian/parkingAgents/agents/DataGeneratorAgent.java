package gr.devian.parkingAgents.agents;

import com.github.javafaker.Faker;
import gr.devian.parkingAgents.agents.infra.ManagedAgent;
import gr.devian.parkingAgents.models.Address;
import gr.devian.parkingAgents.models.Client;
import gr.devian.parkingAgents.models.ParkingSession;
import gr.devian.parkingAgents.models.PaymentInformation;
import gr.devian.parkingAgents.models.events.CarArrivedEvent;
import gr.devian.parkingAgents.models.events.CarLeavingEvent;
import gr.devian.parkingAgents.models.responses.ParkCarResponse;
import gr.devian.parkingAgents.models.responses.RegisterNewClientResponse;
import gr.devian.parkingAgents.models.responses.UnparkCarResponse;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static gr.devian.parkingAgents.utils.TimeUtils.TIME_SCALING;

@Slf4j
public class DataGeneratorAgent extends ManagedAgent {
    private final Map<UUID, ParkingSession> parkedSessions = new ConcurrentHashMap<>();
    private final List<Client> clients = new LinkedList<>();
    private final Faker faker = new Faker(new Random(System.currentTimeMillis()));

    @Override
    protected void setupInternal() {
        addCyclicBehavior(
                Handle(ParkCarResponse.class, (message, parkCarResponse) -> {
                    parkedSessions.put(parkCarResponse.getParkingSession().getId(), parkCarResponse.getParkingSession());
                }),
                Handle(UnparkCarResponse.class, (message, parkCarResponse) -> {
                    parkedSessions.remove(parkCarResponse.getParkingSession().getId());
                }),
                Handle(RegisterNewClientResponse.class, (message, response) -> {
                    clients.add(response.getIdentifiedClient());
                })
        );
        addRecurringBehavior(
                () -> Duration.ofMillis(rng.nextLong((long)(20000 * TIME_SCALING), (long)(80000 * TIME_SCALING))),
                () -> {
                    final boolean shouldCreateNewClient = rng.nextBoolean();

                    if (shouldCreateNewClient || clients.isEmpty() || clients.size() < 20) {
                        sendEvent(
                                CarArrivedEvent
                                        .builder()
                                        .client(generateNewClient())
                                        .requestRefuelingOrRecharging(rng.nextBoolean())
                                        .requestWashing(rng.nextBoolean())
                                        .build(),
                                GateAgent.class
                        );
                    } else {
                        sendEvent(
                                CarArrivedEvent
                                        .builder()
                                        .clientId(clients.get(rng.nextInt(0, clients.size())).getId())
                                        .requestRefuelingOrRecharging(rng.nextBoolean())
                                        .requestWashing(rng.nextBoolean())
                                        .build(),
                                GateAgent.class
                        );
                    }
                });
        addRecurringBehavior(
                () -> Duration.ofMillis(rng.nextLong((long)(30000 * TIME_SCALING), (long)(180000 * TIME_SCALING))),
                () -> {
                    final boolean shouldClientLeave = rng.nextBoolean();

                    if (!shouldClientLeave || parkedSessions.size() < 20) {
                        return;
                    }

                    final ParkingSession parkingSession = parkedSessions.values().toArray(ParkingSession[]::new)[rng.nextInt(0, parkedSessions.size())];

                    parkedSessions.remove(parkingSession.getId());

                    sendEvent(
                            CarLeavingEvent.builder()
                                    .parkSessionId(parkingSession.getId())
                                    .build(),
                            GateAgent.class);
                }
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
}

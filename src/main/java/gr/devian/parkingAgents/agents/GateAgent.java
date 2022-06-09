package gr.devian.parkingAgents.agents;

import gr.devian.parkingAgents.agents.infra.ManagedAgent;
import gr.devian.parkingAgents.models.events.CarArrivedEvent;
import gr.devian.parkingAgents.models.events.CarLeavingEvent;
import gr.devian.parkingAgents.models.requests.CarEnteringRequest;
import gr.devian.parkingAgents.models.requests.CarExitingRequest;
import gr.devian.parkingAgents.models.responses.CarEnteringResponse;
import gr.devian.parkingAgents.models.responses.CarExitingResponse;
import gr.devian.parkingAgents.utils.ParkingUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import static gr.devian.parkingAgents.agents.ParkingPersistenceAgent.PARKING_GROUPS;
import static gr.devian.parkingAgents.agents.ParkingPersistenceAgent.PARKING_SPACES;
import static gr.devian.parkingAgents.utils.ConsoleUtils.*;

@Slf4j
public class GateAgent extends ManagedAgent {

    @Override
    protected void setupInternal() {
        addCyclicBehavior(
                Handle(CarArrivedEvent.class, (receivedMessage, event) -> {
                    final UUID uuid = UUID.randomUUID();
                    log.info(GREEN + ">>>>> [{}]" + RESET + " Car Arrived at the Parking", uuid);

                    sendRequestToCoordinator(
                            CarEnteringRequest.builder()
                                    .client(event.getClient())
                                    .clientId(event.getClientId())
                                    .requestedRechargingOrRefueling(event.isRequestRefuelingOrRecharging())
                                    .requestedWashing(event.isRequestWashing())
                                    .build(),
                            uuid
                    );
                }),
                Handle(CarLeavingEvent.class, (receivedMessage, event) -> {
                    final UUID uuid = UUID.randomUUID();
                    log.info(RED + "<<<<< [{}]" + RESET + " Client Requests to leave the parking [Session: {}]",
                            uuid,
                            event.getParkSessionId()
                    );

                    sendRequestToCoordinator(CarExitingRequest
                                    .builder()
                                    .parkingSessionId(event.getParkSessionId())
                                    .build(),
                            uuid);
                }),
                Handle(CarEnteringResponse.class, (receivedMessage, response) -> {
                    log.info(GREEN + ">>>>> [{}]" + RESET + " Car '{}' entered into the parking lot on Parking Spot {}",
                            receivedMessage.getConversationId(),
                            response.getParkingSession().getCar().getLicensePlate(),
                            ParkingUtils.getParkingSpotString(
                                    response.getParkingSession().getParkingSpot(),
                                    PARKING_SPACES / PARKING_GROUPS));

                }),
                Handle(CarExitingResponse.class, (receivedMessage, response) -> {
                    log.info(RED + "<<<<< [{}]" + RESET + " Car '{}' left the parking lot",
                            receivedMessage.getConversationId(),
                            response.getParkingSession().getCar().getLicensePlate());
                })
        );
    }
}

package gr.devian.parkingAgents.agents;

import gr.devian.parkingAgents.agents.infra.ManagedAgent;
import gr.devian.parkingAgents.models.ParkingSession;
import gr.devian.parkingAgents.models.events.ParkingAgentOnlineEvent;
import gr.devian.parkingAgents.models.requests.ParkCarRequest;
import gr.devian.parkingAgents.models.requests.UnparkCarRequest;
import gr.devian.parkingAgents.models.responses.ParkCarResponse;
import gr.devian.parkingAgents.models.responses.UnparkCarResponse;
import jade.lang.acl.ACLMessage;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
public class ParkingAgent extends ManagedAgent {

    @Override
    protected void setupInternal() {
        addInitBehavior(this::announceToCoordinator);
        addCyclicBehavior(
            Handle(ParkCarRequest.class, this::handleParkCarRequest),
            Handle(UnparkCarRequest.class, this::handleUnparkCarRequest)
        );
    }

    private void handleParkCarRequest(final ACLMessage receivedMessage, final ParkCarRequest request) {
        sleepRandom(20000, 180000);
        final ParkingSession session = request.getParkingSession()
                                              .toBuilder()
                                              .car(request.getParkingSession().getCar())
                                              .parkingSpot(request.getParkingSession().getParkingSpot())
                                              .parkedSince(LocalDateTime.now())
                                              .build();

        sendResponseToCoordinator(
            receivedMessage,
            ParkCarResponse
                .builder()
                .parkingSession(session)
                .build()
        );
    }

    private void handleUnparkCarRequest(final ACLMessage receivedMessage, final UnparkCarRequest request) {
        sleepRandom(20000, 180000);

        sendResponseToCoordinator(
            receivedMessage,
            UnparkCarResponse
                .builder()
                .parkingSession(request.getParkingSession())
                .build()
        );
    }

    private void announceToCoordinator() {
        sendEvent(ParkingAgentOnlineEvent.builder().build(), CoordinatorAgent.class);
    }
}

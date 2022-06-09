package gr.devian.parkingAgents.agents;

import gr.devian.parkingAgents.agents.infra.ManagedAgent;
import gr.devian.parkingAgents.models.TaskStatus;
import gr.devian.parkingAgents.models.events.RefuelingAndRechargingAgentOnlineEvent;
import gr.devian.parkingAgents.models.requests.RefuelingOrRechargingRequest;
import gr.devian.parkingAgents.models.responses.RefuelingOrRechargingResponse;
import jade.lang.acl.ACLMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RefuelingAndRechargingAgent extends ManagedAgent {

    @Override
    protected void setupInternal() {
        addInitBehavior(this::announceToCoordinator);
        addCyclicBehavior(
            Handle(RefuelingOrRechargingRequest.class, this::handleRefuelingOrRechargingRequest)
        );
    }

    private void handleRefuelingOrRechargingRequest(final ACLMessage receivedMessage, final RefuelingOrRechargingRequest request) {
        sleepRandom(10000, 90000);
        sendResponseToCoordinator(
            receivedMessage,
            RefuelingOrRechargingResponse
                .builder()
                .status(TaskStatus.COMPLETED)
                .build()
        );
    }

    private void announceToCoordinator() {
        sendEvent(RefuelingAndRechargingAgentOnlineEvent.builder().build(), CoordinatorAgent.class);
    }
}

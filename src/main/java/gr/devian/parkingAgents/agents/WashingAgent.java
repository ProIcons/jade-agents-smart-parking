package gr.devian.parkingAgents.agents;

import gr.devian.parkingAgents.agents.infra.ManagedAgent;
import gr.devian.parkingAgents.models.TaskStatus;
import gr.devian.parkingAgents.models.events.WashingAgentOnlineEvent;
import gr.devian.parkingAgents.models.requests.WashingRequest;
import gr.devian.parkingAgents.models.responses.WashingResponse;
import jade.lang.acl.ACLMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WashingAgent extends ManagedAgent {

    @Override
    protected void setupInternal() {
        addInitBehavior(this::announceToCoordinator);
        addCyclicBehavior(
                Handle(WashingRequest.class, this::handleWashingRequest)
        );
    }

    private void handleWashingRequest(final ACLMessage receivedMessage, final WashingRequest request) {
        sleepRandom(10000, 30000);
        sendResponseToCoordinator(
                receivedMessage,
                WashingResponse
                        .builder()
                        .status(TaskStatus.COMPLETED)
                        .build()
        );
    }

    private void announceToCoordinator() {
        sendEvent(WashingAgentOnlineEvent.builder().build(), CoordinatorAgent.class);
    }
}

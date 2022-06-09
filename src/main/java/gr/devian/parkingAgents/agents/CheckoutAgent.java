package gr.devian.parkingAgents.agents;

import gr.devian.parkingAgents.agents.infra.ManagedAgent;
import gr.devian.parkingAgents.models.requests.CheckoutRequest;
import gr.devian.parkingAgents.models.responses.CheckoutResponse;
import gr.devian.parkingAgents.models.responses.CheckoutStatus;
import jade.lang.acl.ACLMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CheckoutAgent extends ManagedAgent {

    @Override
    protected void setupInternal() {
        addCyclicBehavior(
            Handle(CheckoutRequest.class, this::handleCheckoutRequest)
        );
    }

    private void handleCheckoutRequest(final ACLMessage receivedMessage, final CheckoutRequest request) {
        sleepRandom(1000, 2000);

        sendResponseToCoordinator(
            receivedMessage,
            CheckoutResponse
                .builder()
                .amount(request.getAmount())
                .status(CheckoutStatus.CHECKED_OUT)
                .build()
        );
    }
}

package gr.devian.parkingAgents.agents.infra;

import gr.devian.parkingAgents.agents.CoordinatorAgent;
import gr.devian.parkingAgents.models.events.BaseEvent;
import gr.devian.parkingAgents.models.requests.BaseRequest;
import gr.devian.parkingAgents.models.responses.BaseResponse;
import gr.devian.parkingAgents.utils.MessageUtils;
import jade.lang.acl.ACLMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public abstract class ManagedAgent extends BaseAgent {

    protected <T extends BaseRequest> void sendRequestToCoordinator(final T payload, final UUID identifier) {
        final ACLMessage aclMessage = MessageUtils.createMessage(payload, ACLMessage.REQUEST, identifier.toString(), CoordinatorAgent.class);
        send(aclMessage);
    }

    protected <T extends BaseEvent> void sendEvent(final T payload, final Class<? extends BaseAgent> agentClass) {
        final ACLMessage aclMessage = MessageUtils.createMessage(payload, ACLMessage.INFORM, agentClass);
        send(aclMessage);
    }

    protected <T extends BaseResponse> void sendResponseToCoordinator(final ACLMessage originalMessage, final T payload) {
        final ACLMessage aclMessage = MessageUtils.createMessage(payload, ACLMessage.INFORM, originalMessage.getConversationId(), CoordinatorAgent.class);
        send(aclMessage);
    }
}

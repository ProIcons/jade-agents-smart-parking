package gr.devian.parkingAgents.agents.infra;

import gr.devian.parkingAgents.models.requests.BaseRequest;
import gr.devian.parkingAgents.models.responses.BaseResponse;
import gr.devian.parkingAgents.utils.MessageUtils;
import jade.lang.acl.ACLMessage;

import java.util.UUID;

public abstract class ManagerAgent extends BaseAgent {


    protected <T extends BaseRequest> void sendRequest(final T payload, final ACLMessage ref, final Class<? extends ManagedAgent> agentClass) {
        final ACLMessage aclMessage = MessageUtils.createMessage(payload, ACLMessage.REQUEST, ref.getConversationId(), agentClass);
        send(aclMessage);
    }

    protected <T extends BaseRequest> void sendRequest(final T payload, final String ref, final Class<? extends ManagedAgent> agentClass) {
        final ACLMessage aclMessage = MessageUtils.createMessage(payload, ACLMessage.REQUEST, ref, agentClass);
        send(aclMessage);
    }

    protected <T extends BaseRequest> void sendRequest(final T payload, final String ref, final String agent) {
        final ACLMessage aclMessage = MessageUtils.createMessage(payload, ACLMessage.REQUEST, ref, agent);
        send(aclMessage);
    }


    protected <T extends BaseResponse> void sendResponse(final T payload, final ACLMessage ref, final String agent) {
        sendResponse(payload, ref.getConversationId(), agent);
    }

    protected <T extends BaseResponse> void sendResponse(final T payload, final ACLMessage ref, final Class<? extends ManagedAgent> agent) {
        sendResponse(payload, ref.getConversationId(), agent.getSimpleName());
    }

    protected <T extends BaseResponse> void sendResponse(final T payload, final String ref, final Class<? extends ManagedAgent> agent) {
        sendResponse(payload, ref, agent.getSimpleName());
    }

    protected <T extends BaseResponse> void sendResponse(final T payload, final String ref, final String agent) {
        final ACLMessage aclMessage = MessageUtils.createMessage(payload, ACLMessage.REQUEST, ref, agent);
        send(aclMessage);
    }
}

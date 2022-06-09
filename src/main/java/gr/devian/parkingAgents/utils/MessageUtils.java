package gr.devian.parkingAgents.utils;

import gr.devian.parkingAgents.agents.DataGeneratorAgent;
import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Slf4j
public final class MessageUtils {
    private MessageUtils() {
    }

    public static <T> T getMessage(final ACLMessage message, final Class<T> payloadClass) {
        Objects.requireNonNull(message, "message is null");
        Objects.requireNonNull(payloadClass, "payloadClass is null");

        try (final ObjectInputStream inReader = new ObjectInputStream(new ByteArrayInputStream(message.getByteSequenceContent()))) {
            return payloadClass.cast(inReader.readObject());
        } catch (final IOException | ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <T extends Serializable> ACLMessage createMessage(final T payload, final int messageType, final Class<? extends Agent> receiver) {
        return createMessage(payload, messageType, UUID.randomUUID().toString(), receiver);
    }

    public static <T extends Serializable> ACLMessage createMessage(final T payload, final int messageType, final String receiver) {
        return createMessage(payload, messageType, UUID.randomUUID().toString(), receiver);
    }

    public static <T extends Serializable> ACLMessage createMessage(final T payload, final int messageType, final String conversationId,
                                                                    final Class<? extends Agent> receiver) {
        return createMessage(payload, messageType, conversationId, receiver.getSimpleName());
    }

    public static <T extends Serializable> ACLMessage createMessage(final T payload, final int messageType, final String conversationId,
                                                                    final String receiver) {
        Objects.requireNonNull(payload, "payload is null");
        Objects.requireNonNull(conversationId, "conversationId is null");
        Objects.requireNonNull(receiver, "receiver is null");

        final ACLMessage aclMessage = new ACLMessage(messageType);
        try {
            aclMessage.setContentObject(payload);
            aclMessage.setConversationId(conversationId);
            aclMessage.setOntology(payload.getClass().getCanonicalName());
            aclMessage.addReceiver(new AID(receiver, AID.ISLOCALNAME));
            aclMessage.addReceiver(new AID(DataGeneratorAgent.class.getSimpleName(), AID.ISLOCALNAME));
            return aclMessage;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}

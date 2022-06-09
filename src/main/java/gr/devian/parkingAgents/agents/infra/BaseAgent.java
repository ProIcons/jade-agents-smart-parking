package gr.devian.parkingAgents.agents.infra;

import gr.devian.parkingAgents.agents.CoordinatorAgent;
import gr.devian.parkingAgents.models.responses.AcknowledgeResponse;
import gr.devian.parkingAgents.utils.MessageUtils;
import io.vavr.collection.List;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static gr.devian.parkingAgents.utils.TimeUtils.TIME_SCALING;

@Slf4j
public abstract class BaseAgent extends Agent {
    protected final ThreadLocalRandom rng = ThreadLocalRandom.current();

    protected <T> Consumer<ACLMessage> Handle(final Class<T> payloadClass, final BiConsumer<ACLMessage, T> payloadConsumer) {
        return message -> {
            if (message.getOntology().equals(payloadClass.getCanonicalName())) {
                payloadConsumer.accept(message, MessageUtils.getMessage(message, payloadClass));
            }
        };
    }

    protected final void sleep(final long ms) {
        try {
            Thread.sleep(ms);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected final void addCyclicBehavior(final Consumer<ACLMessage>... consumers) {
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                final ACLMessage receivedMessage = receive();

                if (receivedMessage != null) {
                    List.ofAll(Arrays.stream(consumers)).forEach(consumer -> consumer.accept(receivedMessage));
                } else {
                    block();
                }
            }
        });
    }

    protected final void addRecurringBehavior(final Supplier<Duration> intervalSupplier, final Runnable runnable) {
        addBehaviour(new RecurringBehavior(this, intervalSupplier, runnable));
    }

    protected final void addRecurringBehavior(final Duration interval, final Runnable runnable) {
        addBehaviour(new TickerBehaviour(this, interval.toMillis()) {
            @Override
            protected void onTick() {
                runnable.run();
            }
        });
    }

    protected final void addInitBehavior(final Runnable runnable) {
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                runnable.run();
            }
        });
    }

    protected final void sleepRandom(final int lower, final int upper) {
        sleep(rng.nextInt((int) (lower * TIME_SCALING), (int) (upper * TIME_SCALING)));
    }

    protected abstract void setupInternal();


    @Override
    protected final void setup() {
        for (int i = 0; i < rng.nextInt(5000, 10000); i++) {
            rng.nextInt();
        }

        log.info("Agent {}: {}.", getLocalName(), getAgentState());
        setupInternal();
    }

    @Override
    protected final void takeDown() {
        log.info("Agent {}: {}.", getLocalName(), getAgentState());
    }


    protected void acknowledge(final ACLMessage message) {
        final ACLMessage aclMessage = MessageUtils.createMessage(
                new AcknowledgeResponse(),
                ACLMessage.INFORM,
                message.getConversationId(),
                CoordinatorAgent.class
        );
        send(aclMessage);
    }


    private class RecurringBehavior extends WakerBehaviour {
        private final Supplier<Duration> intervalSupplier;
        private final Runnable runnable;

        public RecurringBehavior(final Agent a, final Supplier<Duration> intervalSupplier, final Runnable runnable) {
            super(a, intervalSupplier.get().toMillis());
            this.intervalSupplier = intervalSupplier;
            this.runnable = runnable;
        }

        @Override
        public void onWake() {
            runnable.run();
            reschedule();
        }

        private void reschedule() {
            addBehaviour(new RecurringBehavior(BaseAgent.this, intervalSupplier, runnable));
        }

    }
}

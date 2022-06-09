package gr.devian.parkingAgents.agents;

import gr.devian.parkingAgents.agents.infra.ManagedAgent;
import jade.wrapper.AgentContainer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.UUID;

import static gr.devian.parkingAgents.utils.TimeUtils.TIME_SCALING;

@Slf4j
public class DataGeneratorAgent extends ManagedAgent {
    private AgentContainer agentContainer;

    @Override
    protected void setupInternal() {
        agentContainer = getContainerController();
        addRecurringBehavior(
            () -> Duration.ofMillis(rng.nextLong((long) (20000 * TIME_SCALING), (long) (80000 * TIME_SCALING))),
            this::createAgent);
    }

    @SneakyThrows
    private void createAgent() {
        final UUID carId = UUID.randomUUID();
        final var agent = agentContainer.createNewAgent(carId.toString(), CarAgent.class.getCanonicalName(), new Object[]{"-id", carId.toString()});
        agent.start();
    }
}

package gr.devian.parkingAgents;

import gr.devian.parkingAgents.agents.CarAgent;
import gr.devian.parkingAgents.agents.ParkingAgent;
import gr.devian.parkingAgents.agents.RefuelingAndRechargingAgent;
import gr.devian.parkingAgents.agents.WashingAgent;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import jade.core.Agent;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.util.ExtendedProperties;
import jade.util.leap.Properties;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.logging.LogManager;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class Application {

    private static final String PACKAGE_NAME = Application.class.getPackageName();
    private static final Reflections REFLECTIONS = new Reflections(PACKAGE_NAME);

    public static void main(final String[] args) {
        System.setProperty("log4j.shutdownHookEnabled", Boolean.toString(false));

        final Map<Class<? extends Agent>, Integer> agentInstancesMap = HashMap.of(
            ParkingAgent.class, 8,
            WashingAgent.class, 4,
            RefuelingAndRechargingAgent.class, 4,
            CarAgent.class, 0
        );

        LogManager.getLogManager().reset();
        final Properties props = new ExtendedProperties();
        props.setProperty("gui", "false");
        props.setProperty("main", "true");
        props.setProperty("agents", createAgents(agentInstancesMap));

        Runtime.instance().setCloseVM(true);
        Runtime.instance().createMainContainer(new ProfileImpl(props));
    }


    private static String createAgents(final Map<Class<? extends Agent>, Integer> agentInstancesMap) {
        return List.ofAll(REFLECTIONS.getSubTypesOf(Agent.class))
                   .filter(agentClass -> agentClass.getCanonicalName().startsWith(PACKAGE_NAME))
                   .filter(agentClass -> !Modifier.isAbstract(agentClass.getModifiers()))
                   .map(agentClass -> createAgent(agentClass, agentInstancesMap.getOrElse(agentClass, 1)))
                   .mkString("; ");
    }

    private static String createAgent(final Class<? extends Agent> agentClass, final int instances) {
        return IntStream.range(0, instances).mapToObj(instanceIndex -> instances <= 1
                            ? String.format("%s:%s", agentClass.getSimpleName(), agentClass.getCanonicalName())
                            : String.format("%s-%d:%s", agentClass.getSimpleName(), instanceIndex, agentClass.getCanonicalName()))
                        .collect(Collectors.joining("; "));
    }
}

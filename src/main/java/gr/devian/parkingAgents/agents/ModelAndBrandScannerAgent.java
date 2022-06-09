package gr.devian.parkingAgents.agents;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.devian.parkingAgents.agents.infra.ManagedAgent;
import gr.devian.parkingAgents.models.CarType;
import gr.devian.parkingAgents.models.model.CarModel;
import gr.devian.parkingAgents.models.requests.ModelAndBrandDetectionRequest;
import gr.devian.parkingAgents.models.responses.ModelAndBrandDetectionResponse;
import jade.lang.acl.ACLMessage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.util.List;

@Slf4j
public class ModelAndBrandScannerAgent extends ManagedAgent {

    @Override
    protected void setupInternal() {
        addCyclicBehavior(
                Handle(ModelAndBrandDetectionRequest.class, this::handleModelAndBrandDetectionRequest)
        );
    }

    @SneakyThrows
    private ModelAndBrandDetectionResponse getDetectionResponse(final byte[] imageByte) {
        final ObjectMapper objectMapper = new ObjectMapper();

        final TypeReference<List<CarModel>> reference = new TypeReference<>() {
        };
        final List<CarModel> carModels = objectMapper
                .readValue(getClass().getClassLoader().getResourceAsStream("carDatabase.json"), reference);

        final CarModel carModel = carModels.get(rng.nextInt(0, carModels.size()));
        final String model = carModel.getModels().get(rng.nextInt(0, carModel.getModels().size()));
        final Color color = new Color((int) (Math.random() * 0x1000000));
        final Integer capacity = rng.nextInt(50, 120);
        final CarType type = CarType.values()[rng.nextInt(0, CarType.values().length)];
        return ModelAndBrandDetectionResponse.builder()
                .brand(carModel.getBrand())
                .model(model)
                .color(color)
                .capacity(capacity)
                .level(rng.nextDouble(0, capacity))
                .type(type)
                .build();
    }

    private void handleModelAndBrandDetectionRequest(final ACLMessage receivedMessage, final ModelAndBrandDetectionRequest request) {
        sleepRandom(1000, 5000);

        sendResponseToCoordinator(
                receivedMessage,
                getDetectionResponse(request.getCarImage())
        );
    }
}

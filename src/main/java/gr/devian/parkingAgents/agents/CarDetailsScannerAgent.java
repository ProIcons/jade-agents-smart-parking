package gr.devian.parkingAgents.agents;

import gr.devian.parkingAgents.agents.infra.ManagedAgent;
import gr.devian.parkingAgents.models.requests.CarDetailsRequest;
import gr.devian.parkingAgents.models.requests.CarInfoRequest;
import gr.devian.parkingAgents.models.responses.CarDetailsResponse;
import gr.devian.parkingAgents.models.responses.CarInfoResponse;
import jade.lang.acl.ACLMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.UUID;

@Slf4j
public class CarDetailsScannerAgent extends ManagedAgent {

    private final HashMap<UUID, ACLMessage> carIdMap = new HashMap<>();

    @Override
    protected void setupInternal() {
        addCyclicBehavior(
            Handle(CarDetailsRequest.class, this::handleCarDetailsRequest),
            Handle(CarInfoResponse.class, this::handleCarInfoResponse)
        );
    }


    private void handleCarDetailsRequest(final ACLMessage receivedMessage, final CarDetailsRequest request) {
        sleepRandom(1000, 2000);

        sendRequest(
            CarInfoRequest
                .builder()
                .build(),
            receivedMessage,
            request.getCarId().toString()
        );

    }

    private void handleCarInfoResponse(final ACLMessage message, final CarInfoResponse response) {
        sleepRandom(1000, 2000);

        sendResponseToCoordinator(
            message,
            CarDetailsResponse
                .builder()
                .licensePlate(response.getLicensePlate())
                .brand(response.getBrand())
                .model(response.getModel())
                .carType(response.getCarType())
                .color(response.getColor())
                .fuelCapacity(response.getFuelCapacity())
                .fuelLevel(response.getFuelLevel())
                .id(response.getId())
                .build()
        );
    }
}

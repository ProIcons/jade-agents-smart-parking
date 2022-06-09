package gr.devian.parkingAgents.agents;

import com.github.curiousoddman.rgxgen.RgxGen;
import gr.devian.parkingAgents.agents.infra.ManagedAgent;
import gr.devian.parkingAgents.models.requests.LicenseDetectionRequest;
import gr.devian.parkingAgents.models.responses.LicenseDetectionResponse;
import jade.lang.acl.ACLMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;
import java.util.stream.IntStream;

@Slf4j
public class LicenseScannerAgent extends ManagedAgent {

    @Override
    protected void setupInternal() {
        addCyclicBehavior(
                Handle(LicenseDetectionRequest.class, this::handleLicenseDetectionRequest)
        );
    }

    private String getLicensePlate(final byte[] imageByte) {
        final RgxGen rgxGen = new RgxGen("[ABEHIKMNOPTXYZ]{3}[0-9]{4}");
        return rgxGen.generate(new Random(IntStream.range(0, imageByte.length).map(index -> imageByte[index]).sum()));
    }

    private void handleLicenseDetectionRequest(final ACLMessage receivedMessage, final LicenseDetectionRequest request) {
        sleepRandom(1000, 2000);

        sendResponseToCoordinator(
                receivedMessage,
                LicenseDetectionResponse
                        .builder()
                        .licensePlate(getLicensePlate(request.getCarImage()))
                        .build()
        );
    }
}

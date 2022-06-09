package gr.devian.parkingAgents.agents;

import gr.devian.parkingAgents.agents.infra.ManagedAgent;
import gr.devian.parkingAgents.models.requests.EntrancePhotoRequest;
import gr.devian.parkingAgents.models.responses.EntrancePhotoResponse;
import jade.lang.acl.ACLMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EntranceCameraAgent extends ManagedAgent {

    @Override
    protected void setupInternal() {
        addCyclicBehavior(
            Handle(EntrancePhotoRequest.class, this::handleEntrancePhotoRequest)
        );
    }

    private byte[] getPhoto() {
        final byte[] photoBytes = new byte[1024 * 1024];
        rng.nextBytes(photoBytes);
        return photoBytes;
    }

    private void handleEntrancePhotoRequest(final ACLMessage receivedMessage, final EntrancePhotoRequest request) {
        // Simulate real world detection
        sleepRandom(1000, 2000);

        sendResponseToCoordinator(
            receivedMessage,
            EntrancePhotoResponse
                .builder()
                .photo(getPhoto())
                .build()
        );
    }
}

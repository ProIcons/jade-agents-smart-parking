package gr.devian.parkingAgents.models.requests;

import gr.devian.parkingAgents.models.Client;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@Builder(toBuilder = true)
public class CarEnteringRequest extends BaseRequest {

    private Client client;
    private UUID clientId;

    private boolean requestedWashing;
    private boolean requestedRechargingOrRefueling;

    public boolean isNewClient() {
        return client != null;
    }

    public boolean isRegisteredClient() {
        return clientId != null;
    }
}

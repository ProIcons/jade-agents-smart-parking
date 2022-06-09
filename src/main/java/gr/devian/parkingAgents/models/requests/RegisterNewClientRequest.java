package gr.devian.parkingAgents.models.requests;

import gr.devian.parkingAgents.models.Client;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@Builder(toBuilder = true)
public class RegisterNewClientRequest extends BaseRequest {
    private Client client;
}

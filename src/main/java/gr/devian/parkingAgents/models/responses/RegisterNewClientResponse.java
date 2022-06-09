package gr.devian.parkingAgents.models.responses;

import gr.devian.parkingAgents.models.Client;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@Builder(toBuilder = true)
public class RegisterNewClientResponse extends BaseResponse {
    private Client identifiedClient;
}

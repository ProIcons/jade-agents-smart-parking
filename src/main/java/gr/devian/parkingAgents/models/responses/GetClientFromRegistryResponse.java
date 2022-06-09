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
public class GetClientFromRegistryResponse extends BaseResponse {
    private Client client;
    private boolean exists;
}

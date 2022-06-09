package gr.devian.parkingAgents.models.requests;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@Builder(toBuilder = true)
public class GetParkingSessionRequest extends BaseRequest {
    private UUID id;
}

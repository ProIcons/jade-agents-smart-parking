package gr.devian.parkingAgents.models.responses;

import gr.devian.parkingAgents.models.ParkingSession;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@Builder(toBuilder = true)
public class GetParkingSessionResponse extends BaseResponse {
    private boolean exists;
    private ParkingSession session;
}

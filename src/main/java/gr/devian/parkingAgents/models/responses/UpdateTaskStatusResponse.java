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
public class UpdateTaskStatusResponse extends BaseResponse {
    private ParkingSession parkingSession;
}

package gr.devian.parkingAgents.models.requests;

import gr.devian.parkingAgents.models.ParkingSession;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@Builder(toBuilder = true)
public class UpdateParkingSpaceRequest extends BaseRequest {
    private int parkingSpace;
    private LocalDateTime parkedSince;
    private ParkingSession session;
}

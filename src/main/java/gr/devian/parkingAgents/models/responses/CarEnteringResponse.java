package gr.devian.parkingAgents.models.responses;

import gr.devian.parkingAgents.models.ParkingSession;
import gr.devian.parkingAgents.models.responses.enumerations.CarEnteringResponseState;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@Builder(toBuilder = true)
public class CarEnteringResponse extends BaseResponse {
    private ParkingSession parkingSession;
    private CarEnteringResponseState response;
}

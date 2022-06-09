package gr.devian.parkingAgents.models.responses;

import gr.devian.parkingAgents.models.Car;
import gr.devian.parkingAgents.models.requests.enumerations.CarRegistrationState;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@Builder(toBuilder = true)
public class RegisterCarIfNotExistsResponse extends BaseResponse {
    private CarRegistrationState carRegistrationState;
    private Car car;
}

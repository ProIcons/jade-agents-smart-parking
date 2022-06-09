package gr.devian.parkingAgents.models.responses;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@Builder(toBuilder = true)
public class FuelInfoResponse extends BaseResponse {
    private Integer capacity;
    private double level;
}

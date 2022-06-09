package gr.devian.parkingAgents.models.responses;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@Builder(toBuilder = true)
public class HasFreeParkingSpacesResponse extends BaseResponse {
    private boolean state;
}

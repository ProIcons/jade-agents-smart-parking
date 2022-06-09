package gr.devian.parkingAgents.models.responses;

import gr.devian.parkingAgents.models.TaskStatus;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@Builder(toBuilder = true)
public class WashingResponse extends BaseResponse {
    private TaskStatus status;
}

package gr.devian.parkingAgents.models.requests;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@Builder(toBuilder = true)
public class ModelAndBrandDetectionRequest extends BaseRequest {
    private byte[] carImage;
}

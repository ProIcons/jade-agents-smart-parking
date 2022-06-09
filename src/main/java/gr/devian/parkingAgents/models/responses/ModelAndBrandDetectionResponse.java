package gr.devian.parkingAgents.models.responses;

import gr.devian.parkingAgents.models.CarType;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.awt.*;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@Builder(toBuilder = true)
public class ModelAndBrandDetectionResponse extends BaseResponse {
    private String model;
    private String brand;
    private Color color;
    private Integer capacity;
    private double level;
    private CarType type;
}

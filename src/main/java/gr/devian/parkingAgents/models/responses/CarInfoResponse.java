package gr.devian.parkingAgents.models.responses;

import gr.devian.parkingAgents.models.CarType;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.awt.*;
import java.util.UUID;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@Builder(toBuilder = true)
public class CarInfoResponse extends BaseResponse {
    private UUID id;
    private String licensePlate;
    private String brand;
    private String model;
    private Color color;
    private CarType carType;
    private Integer fuelCapacity;
    private double fuelLevel;
}

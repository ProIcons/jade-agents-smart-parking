package gr.devian.parkingAgents.models;

import lombok.*;
import lombok.experimental.Accessors;

import java.awt.*;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@EqualsAndHashCode
@Builder(toBuilder = true)
@ToString
@Accessors(chain = true)
@Setter
public class Car implements Serializable {
    private UUID id;
    private String licensePlate;
    @ToString.Exclude
    private Color color;
    private CarType type;
    private String brand;
    private String model;
    private Integer fuelTankCapacity;
    private Double fuelLevel;
    private Integer batteryCapacity;
    private Double batteryLevel;
    @Singular
    @ToString.Exclude
    private List<byte[]> carImages;

    public double getRefuelingOrRechargingRemaining() {
        if (type == CarType.ELECTRIC) {
            return 100d * batteryLevel / batteryCapacity;
        } else if (type == CarType.GAS) {
            return 100d * fuelLevel / fuelTankCapacity;
        } else {
            return 0;
        }
    }
}

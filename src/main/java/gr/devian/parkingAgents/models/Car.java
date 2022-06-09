package gr.devian.parkingAgents.models;

import lombok.*;

import java.awt.*;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@EqualsAndHashCode
@Builder(toBuilder = true)
@ToString
public class Car implements Serializable {
    private UUID id;
    private String licensePlate;
    @ToString.Exclude
    private Color color;
    private CarType type;
    private String brand;
    private String model;

    @Getter(AccessLevel.NONE)
    private Integer fuelTankCapacity;
    @Getter(AccessLevel.NONE)
    private Double fuelLevel;

    @Getter(AccessLevel.NONE)
    private Integer batteryCapacity;
    @Getter(AccessLevel.NONE)
    private Double batteryLevel;

    public double getRefuelingOrRechargingRemaining() {
        if (type == CarType.ELECTRIC) {
            return 100d * batteryLevel / batteryCapacity;
        } else if (type == CarType.GAS) {
            return 100d * fuelLevel / fuelTankCapacity;
        } else {
            return 0;
        }
    }

    @Singular
    @ToString.Exclude
    private List<byte[]> carImages;
}

package gr.devian.parkingAgents.models;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Builder
@Data
public class Address implements Serializable {
    private String streetName;
    private String streetAddress;
    private String buildingNumber;
    private String city;
    private String country;
    private String postalCode;
}

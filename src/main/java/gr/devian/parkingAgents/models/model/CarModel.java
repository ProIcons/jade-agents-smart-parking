package gr.devian.parkingAgents.models.model;

import lombok.Data;

import java.util.List;

@Data
public class CarModel {
    private String brand;
    private List<String> models;
}

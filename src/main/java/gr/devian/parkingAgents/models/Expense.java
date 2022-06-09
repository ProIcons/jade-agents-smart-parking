package gr.devian.parkingAgents.models;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class Expense implements Serializable {
    private ExpenseType type;
    private double amount;
}

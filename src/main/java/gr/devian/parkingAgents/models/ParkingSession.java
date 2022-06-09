package gr.devian.parkingAgents.models;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@ToString
public class ParkingSession implements Serializable {
    private UUID id;
    private Client client;
    private Car car;

    private LocalDateTime parkedSince;
    private int parkingSpot;

    private TaskStatus refuelingOrRecharging;
    private TaskStatus washing;

    private List<Expense> expenses;

    public double getExpensesSum() {
        if (expenses == null) {
            return 0d;
        }

        return expenses.stream().mapToDouble(Expense::getAmount)
                .sum();
    }
}

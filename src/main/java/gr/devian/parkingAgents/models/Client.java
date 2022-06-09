package gr.devian.parkingAgents.models;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
public class Client implements Serializable {
    private UUID id;
    private String firstName;
    private String lastName;
    private Date birthday;
    private Address address;
    private PaymentInformation paymentInformation;

    private List<Car> cars;
}

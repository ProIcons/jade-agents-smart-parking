package gr.devian.parkingAgents.models;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Builder
@Data
public class PaymentInformation implements Serializable {
    private String creditCardNumber;
    private String expiryDate;
    private String holderName;
}

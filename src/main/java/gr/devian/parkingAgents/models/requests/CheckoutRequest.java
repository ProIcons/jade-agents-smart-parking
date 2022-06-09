package gr.devian.parkingAgents.models.requests;

import gr.devian.parkingAgents.models.PaymentInformation;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@Builder(toBuilder = true)
public class CheckoutRequest extends BaseRequest {
    private PaymentInformation paymentInformation;
    private double amount;
}

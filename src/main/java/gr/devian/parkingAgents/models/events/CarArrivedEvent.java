package gr.devian.parkingAgents.models.events;

import gr.devian.parkingAgents.models.Client;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@Builder(toBuilder = true)
public class CarArrivedEvent extends BaseEvent {
    private Client client;
    private UUID clientId;
    private boolean requestWashing;
    private boolean requestRefuelingOrRecharging;
}

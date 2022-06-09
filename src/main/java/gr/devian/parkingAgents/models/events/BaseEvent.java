package gr.devian.parkingAgents.models.events;

import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
public abstract class BaseEvent implements Serializable {
    private final LocalDateTime issuedAt = LocalDateTime.now();
}

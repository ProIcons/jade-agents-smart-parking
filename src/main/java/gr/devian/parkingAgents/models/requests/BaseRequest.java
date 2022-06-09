package gr.devian.parkingAgents.models.requests;

import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
public abstract class BaseRequest implements Serializable {
    private final LocalDateTime issuedAt = LocalDateTime.now();
}

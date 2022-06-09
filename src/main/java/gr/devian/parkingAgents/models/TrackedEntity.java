package gr.devian.parkingAgents.models;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
public class TrackedEntity implements Serializable, Comparable<TrackedEntity> {
    private TrackedEntityIntention intention;
    private String conversationId;
    private String licensePlate;
    private byte[] carPhoto;
    private ParkingSession session;
    private UUID parkingSessionId;

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        final TrackedEntity that = (TrackedEntity) o;

        return new EqualsBuilder().append(conversationId, that.conversationId).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(conversationId).toHashCode();
    }

    @Override
    public int compareTo(final TrackedEntity o) {
        return conversationId.compareTo(o.getConversationId());
    }
}

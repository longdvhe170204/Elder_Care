package com.eldercare.backend.roombedallocation.internal.allocation.domain;

import com.eldercare.backend.common.data.DataGroup;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** T024: mốc nhắc kỹ thuật của dấu, chu kỳ đọc CFG khi job chạy. */
@Entity
@Table(name = "allocation_reminder")
@DataGroup(DataGroup.Group.BUSINESS)
public class AllocationReminder {
    @EmbeddedId
    private Key id;

    private Instant startedAt;
    private Instant lastRemindedAt;
    private int reminderCount;

    protected AllocationReminder() {}

    @Embeddable
    public static class Key implements Serializable {
        private String targetType;
        private long targetId;

        @Column(columnDefinition = "ENUM('WITHOUT_BED','AWAITING_REQUEST','AWAITING_SAME_PRICE','ROOM_UNSUITABLE')")
        private String flagType;

        protected Key() {}

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key
                    && targetId == key.targetId
                    && Objects.equals(targetType, key.targetType)
                    && Objects.equals(flagType, key.flagType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(targetType, targetId, flagType);
        }
    }
}

package com.cambae.backend.m03.internal.allocation.domain;

import com.cambae.backend.common.data.DataGroup;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/** T024: đích khóa theo người, không sao chép vị trí suy ra từ phân bổ. */
@Entity
@Table(name = "resident_position")
@DataGroup(DataGroup.Group.BUSINESS)
public class ResidentPosition {
    @Id
    private long residentId;

    private Instant withoutBedSince;

    @Version
    private int version;

    protected ResidentPosition() {}
}

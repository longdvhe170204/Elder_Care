package com.eldercare.backend.administration.sample;

import com.eldercare.backend.common.data.DataGroup;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Bản ghi nhóm 3 mẫu gắn tầng/khu vực, chỉ thêm (000 T048). */
@Entity
@Table(name = "sample_floor_record")
@DataGroup(DataGroup.Group.RECORD)
public class SampleFloorRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant occurredAt;

    private Instant deviceTime;

    private long actorId;

    private long floorAreaId;

    private String note;

    protected SampleFloorRecord() {}

    public SampleFloorRecord(Long id, Instant occurredAt, long actorId, long floorAreaId, String note) {
        this.id = id;
        this.occurredAt = occurredAt;
        this.actorId = actorId;
        this.floorAreaId = floorAreaId;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public String getNote() {
        return note;
    }
}

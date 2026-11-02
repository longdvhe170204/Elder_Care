package com.eldercare.backend.administration.sample;

import com.eldercare.backend.common.data.DataGroup;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Bản ghi nhóm 3 mẫu không gắn tầng/khu vực, chỉ thêm (000 T048). */
@Entity
@Table(name = "sample_office_record")
@DataGroup(DataGroup.Group.RECORD)
public class SampleOfficeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant occurredAt;

    private Instant deviceTime;

    private long actorId;

    private String note;

    protected SampleOfficeRecord() {}

    public SampleOfficeRecord(Instant occurredAt, long actorId, String note) {
        this.occurredAt = occurredAt;
        this.actorId = actorId;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public String getNote() {
        return note;
    }
}

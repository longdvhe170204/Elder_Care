package com.eldercare.backend.medication.internal.dose.domain;

import com.eldercare.backend.common.data.DataGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Hiệu quả của một lần dùng thuốc khi cần (BR-M07-04, Q-431); chỉ thêm, mỗi lần dùng tối đa một dòng. */
@Entity
@Table(name = "prn_effectiveness")
@DataGroup(DataGroup.Group.RECORD)
public class PrnEffectiveness {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dose_result_id", nullable = false, updatable = false)
    private Long doseResultId;

    @Column(name = "effectiveness", nullable = false, updatable = false, length = 2000)
    private String effectiveness;

    @Column(name = "recorded_by_id", nullable = false, updatable = false)
    private Long recordedById;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    protected PrnEffectiveness() {}

    public PrnEffectiveness(Long doseResultId, String effectiveness, Long recordedById, Instant recordedAt) {
        this.doseResultId = doseResultId;
        this.effectiveness = effectiveness;
        this.recordedById = recordedById;
        this.recordedAt = recordedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getDoseResultId() {
        return doseResultId;
    }

    public String getEffectiveness() {
        return effectiveness;
    }

    public Long getRecordedById() {
        return recordedById;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}

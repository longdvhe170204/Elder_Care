package com.cambae.backend.m15.sample;

import com.cambae.backend.common.data.DataGroup;
import com.cambae.backend.common.error.ConditionNotMetException;
import com.cambae.backend.common.error.UnmetCondition;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.List;

/** Nghiệp vụ mẫu, nhóm 2: trạng thái chỉ đổi qua lệnh, không có setter (000 T048). */
@Entity
@Table(name = "sample_case")
@DataGroup(DataGroup.Group.BUSINESS)
public class SampleCase {

    public static final String OPEN = "OPEN";
    public static final String CANCELLED = "CANCELLED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catalog_id")
    private SampleCatalogItem catalog;

    private String status;

    @Version
    private int version;

    private Instant createdAt;

    private long createdBy;

    protected SampleCase() {}

    public SampleCase(SampleCatalogItem catalog, Instant createdAt, long createdBy) {
        this.catalog = catalog;
        this.status = OPEN;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    /** Lệnh hủy: chỉ khi đang OPEN (FR-006). */
    void cancel() {
        if (!OPEN.equals(status)) {
            throw new ConditionNotMetException(List.of(new UnmetCondition("FR-006", "Chỉ hủy được khi đang mở")));
        }
        status = CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public SampleCatalogItem getCatalog() {
        return catalog;
    }

    public String getStatus() {
        return status;
    }
}

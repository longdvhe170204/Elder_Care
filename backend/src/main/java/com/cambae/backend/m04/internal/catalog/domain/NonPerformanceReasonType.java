package com.cambae.backend.m04.internal.catalog.domain;

import com.cambae.backend.common.data.CatalogBase;
import com.cambae.backend.common.data.DataGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** FR-028a: danh mục nhóm 1; dòng cố định do migration lập và MySQL bảo vệ. */
@Entity
@Table(name = "care_non_performance_reason_type")
@DataGroup(DataGroup.Group.CATALOG)
public class NonPerformanceReasonType extends CatalogBase {
    @Column(nullable = false, updatable = false)
    private boolean fixed;

    protected NonPerformanceReasonType() {}

    public NonPerformanceReasonType(String name) {
        super(name);
    }

    public boolean fixed() {
        return fixed;
    }
}

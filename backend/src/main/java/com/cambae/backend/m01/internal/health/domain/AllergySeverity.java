package com.cambae.backend.m01.internal.health.domain;

import com.cambae.backend.common.data.CatalogBase;
import com.cambae.backend.common.data.DataGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Mức độ dị ứng, danh mục nhóm 1 (BR-M01-11). */
@Entity
@Table(name = "allergy_severity")
@DataGroup(DataGroup.Group.CATALOG)
public class AllergySeverity extends CatalogBase {

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected AllergySeverity() {}

    public AllergySeverity(String name, int displayOrder) {
        super(name);
        this.displayOrder = displayOrder;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void change(String name, int displayOrder) {
        rename(name);
        this.displayOrder = displayOrder;
    }
}

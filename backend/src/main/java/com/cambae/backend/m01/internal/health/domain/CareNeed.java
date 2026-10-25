package com.cambae.backend.m01.internal.health.domain;

import com.cambae.backend.common.data.CatalogBase;
import com.cambae.backend.common.data.DataGroup;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Nhu cầu chăm sóc, danh mục nhóm 1 (Q-291). */
@Entity
@Table(name = "care_need")
@DataGroup(DataGroup.Group.CATALOG)
public class CareNeed extends CatalogBase {

    protected CareNeed() {}

    public CareNeed(String name) {
        super(name);
    }
}

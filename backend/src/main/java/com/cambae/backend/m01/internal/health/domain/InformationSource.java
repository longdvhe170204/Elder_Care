package com.cambae.backend.m01.internal.health.domain;

import com.cambae.backend.common.data.CatalogBase;
import com.cambae.backend.common.data.DataGroup;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Nguồn thông tin của mục sức khỏe, danh mục nhóm 1 (BR-M01-11). */
@Entity
@Table(name = "information_source")
@DataGroup(DataGroup.Group.CATALOG)
public class InformationSource extends CatalogBase {

    protected InformationSource() {}

    public InformationSource(String name) {
        super(name);
    }
}

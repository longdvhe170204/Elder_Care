package com.cambae.backend.m03.internal.structure.domain;

import com.cambae.backend.common.data.CatalogBase;
import com.cambae.backend.common.data.DataGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** T014/FR-001–FR-007: dữ liệu danh mục Floor; quyền và tham chiếu kiểm ở service. */
@Entity
@Table(name = "floor")
@DataGroup(DataGroup.Group.CATALOG)
public class Floor extends CatalogBase {
    @Column(name = "area_id")
    private long areaId;

    @Column(name = "code")
    private String code;

    @Column(name = "building_name")
    private String buildingName;

    protected Floor() {}

    public Floor(long areaId, String code, String name, String buildingName) {
        super(name);
        this.areaId = areaId;
        this.code = code;
        this.buildingName = buildingName;
    }

    public long areaId() {
        return areaId;
    }

    public String code() {
        return code;
    }

    public String buildingName() {
        return buildingName;
    }

    public void revise(long areaId, String code, String name, String buildingName) {
        this.areaId = areaId;
        this.code = code;
        this.buildingName = buildingName;
        rename(name);
    }
}

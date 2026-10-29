package com.eldercare.backend.roombedallocation.internal.structure.domain;

import com.eldercare.backend.common.data.CatalogBase;
import com.eldercare.backend.common.data.DataGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** T014/FR-001–FR-007: dữ liệu danh mục Area; quyền và tham chiếu kiểm ở service. */
@Entity
@Table(name = "area")
@DataGroup(DataGroup.Group.CATALOG)
public class Area extends CatalogBase {
    @Column(name = "code")
    private String code;

    protected Area() {}

    public Area(String code, String name) {
        super(name);
        this.code = code;
    }

    public String code() {
        return code;
    }

    public void revise(String code, String name) {
        this.code = code;
        rename(name);
    }
}

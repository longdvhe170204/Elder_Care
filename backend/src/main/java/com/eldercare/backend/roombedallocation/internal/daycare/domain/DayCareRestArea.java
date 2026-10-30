package com.eldercare.backend.roombedallocation.internal.daycare.domain;

import com.eldercare.backend.common.data.CatalogBase;
import com.eldercare.backend.common.data.DataGroup;
import jakarta.persistence.*;

/** Danh mục khu nghỉ; sức chứa đọc CFG, không lưu bản sao. */
@Entity
@Table(name = "day_care_rest_area")
@DataGroup(DataGroup.Group.CATALOG)
public class DayCareRestArea extends CatalogBase {
    private String code;

    @Column(name = "floor_id")
    private Long floorId;

    @Column(name = "area_id")
    private Long areaId;

    protected DayCareRestArea() {}

    public DayCareRestArea(String code, String name, Long floorId, Long areaId) {
        super(name);
        revise(code, name, floorId, areaId);
    }

    public void revise(String code, String name, Long floorId, Long areaId) {
        this.code = code;
        rename(name);
        this.floorId = floorId;
        this.areaId = areaId;
    }

    public String code() {
        return code;
    }

    public Long floorId() {
        return floorId;
    }

    public Long areaId() {
        return areaId;
    }
}

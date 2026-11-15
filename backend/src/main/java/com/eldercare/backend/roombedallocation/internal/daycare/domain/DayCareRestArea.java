package com.eldercare.backend.roombedallocation.internal.daycare.domain;

import com.eldercare.backend.common.data.CatalogBase;
import com.eldercare.backend.common.data.DataGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

/**
 * Danh mục khu nghỉ; sức chứa đọc CFG, không lưu bản sao. Từ 2026-11-13 (gộp bảng về 79, research R-15 của 000) là dòng
 * {@code catalog_item} loại {@code M03.DAY_CARE_REST_AREA}; mã (không bắt duy nhất như bảng cũ), tầng hoặc khu vực ở {@code attributes}. Đúng một trong tầng,
 * khu vực và chỉ một khu Hiệu lực do service kiểm dưới khóa {@code M03.SUC_CHUA_BAN_TRU} (thay CHECK, UNIQUE của bảng cũ).
 */
@Entity
@Table(name = "catalog_item")
@SQLRestriction("catalog_type = '" + DayCareRestArea.TYPE + "'")
@DataGroup(DataGroup.Group.CATALOG)
public class DayCareRestArea extends CatalogBase {
    public static final String TYPE = "M03.DAY_CARE_REST_AREA";

    /** Mã và vị trí của khu nghỉ trong cột {@code attributes}. */
    public record Attributes(String code, Long floorId, Long areaId) {}

    @Column(name = "catalog_type", updatable = false)
    private String catalogType = TYPE;

    @Column(name = "module", updatable = false)
    private String module = "M03";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes")
    private Attributes attributes = new Attributes(null, null, null);

    protected DayCareRestArea() {}

    public DayCareRestArea(String code, String name, Long floorId, Long areaId) {
        super(name);
        revise(code, name, floorId, areaId);
    }

    public void revise(String code, String name, Long floorId, Long areaId) {
        rename(name);
        this.attributes = new Attributes(code, floorId, areaId);
    }

    public String code() {
        return attributes.code();
    }

    public Long floorId() {
        return attributes.floorId();
    }

    public Long areaId() {
        return attributes.areaId();
    }
}

package com.eldercare.backend.roombedallocation.internal.structure.domain;

import com.eldercare.backend.common.data.CatalogBase;
import com.eldercare.backend.common.data.DataGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

/**
 * T014/FR-001–FR-007: dữ liệu danh mục BedTransferReason; quyền và tham chiếu kiểm ở service. Từ 2026-11-13 (gộp bảng
 * về 79, research R-15 của 000) là dòng {@code catalog_item} loại {@code M03.BED_TRANSFER_REASON}; cờ y tế/an toàn và cờ
 * cố định ở {@code attributes}. Entity ánh xạ thẳng dòng của loại mình để giữ lệnh cơ cấu chung của 003.
 */
@Entity
@Table(name = "catalog_item")
@SQLRestriction("catalog_type = '" + BedTransferReason.TYPE + "'")
@DataGroup(DataGroup.Group.CATALOG)
public class BedTransferReason extends CatalogBase {
    public static final String TYPE = "M03.BED_TRANSFER_REASON";

    /** Thuộc tính riêng của lý do trong cột {@code attributes}. */
    public record Attributes(boolean medicalSafety, boolean fixed) {}

    @Column(name = "catalog_type", updatable = false)
    private String catalogType = TYPE;

    @Column(name = "module", updatable = false)
    private String module = "M03";

    @Column(name = "code")
    private String code;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes")
    private Attributes attributes = new Attributes(false, false);

    protected BedTransferReason() {}

    public BedTransferReason(String code, String name, boolean medicalSafety) {
        super(name);
        this.code = code;
        this.attributes = new Attributes(medicalSafety, false);
    }

    public String code() {
        return code;
    }

    public boolean medicalSafety() {
        return attributes.medicalSafety();
    }

    public boolean fixed() {
        return attributes.fixed();
    }

    public void revise(String code, String name, boolean medicalSafety) {
        this.code = code;
        rename(name);
        this.attributes = new Attributes(medicalSafety, attributes.fixed());
    }
}

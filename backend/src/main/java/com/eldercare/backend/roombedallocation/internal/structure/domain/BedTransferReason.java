package com.eldercare.backend.roombedallocation.internal.structure.domain;

import com.eldercare.backend.common.data.CatalogBase;
import com.eldercare.backend.common.data.DataGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** T014/FR-001–FR-007: dữ liệu danh mục BedTransferReason; quyền và tham chiếu kiểm ở service. */
@Entity
@Table(name = "bed_transfer_reason")
@DataGroup(DataGroup.Group.CATALOG)
public class BedTransferReason extends CatalogBase {
    @Column(name = "code")
    private String code;

    @Column(name = "medical_safety")
    private boolean medicalSafety;

    @Column(name = "fixed")
    private boolean fixed;

    protected BedTransferReason() {}

    public BedTransferReason(String code, String name, boolean medicalSafety) {
        super(name);
        this.code = code;
        this.medicalSafety = medicalSafety;
    }

    public String code() {
        return code;
    }

    public boolean medicalSafety() {
        return medicalSafety;
    }

    public boolean fixed() {
        return fixed;
    }

    public void revise(String code, String name, boolean medicalSafety) {
        this.code = code;
        rename(name);
        this.medicalSafety = medicalSafety;
    }
}

package com.eldercare.backend.medication.internal.prescription.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import org.springframework.stereotype.Component;

/**
 * Bác sĩ có quyền kê đơn hợp lệ tại thời điểm lệnh (FR-005, BR-M06-05): thao tác chuyên môn kê đơn qua lớp pháp lý của
 * 002 (giấy phép còn hiệu lực đúng phạm vi, cơ sở có phạm vi khám chữa bệnh còn hiệu lực), như khám và điều trị.
 */
@Component
public class PrescribingAuthority {

    private static final PermissionKey PRESCRIBING = new PermissionKey(Function.PRESCRIPTION, Operation.PROFESSIONAL);

    private final PermissionApi permissions;
    private final CurrentUser currentUser;

    public PrescribingAuthority(PermissionApi permissions, CurrentUser currentUser) {
        this.permissions = permissions;
        this.currentUser = currentUser;
    }

    public boolean canPrescribe(long residentId) {
        return permissions
                .hasPermission(currentUser, PRESCRIBING, PrescriptionService.target(residentId))
                .allowed();
    }
}

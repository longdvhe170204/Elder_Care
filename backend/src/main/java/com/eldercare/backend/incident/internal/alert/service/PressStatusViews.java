package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.health.api.FacilityContactApi;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.SosStatusView;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Trạng thái cảnh báo cho người bấm nút SOS, nút hỗ trợ khẩn cấp ({@code SosStatus}; FR-047f, FR-047j): {@code SENT},
 * {@code RECEIVED} kèm họ tên người tiếp nhận, {@code ARRIVED}; lỗi kèm số liên hệ của viện và 115 (012 FR-085).
 */
@Component
class PressStatusViews {
    private final FacilityContactApi facilityContact;
    private final Optional<AccountApi> accounts;
    private final AlertLocationService locations;

    PressStatusViews(
            FacilityContactApi facilityContact,
            @Autowired(required = false) AccountApi accounts,
            AlertLocationService locations) {
        this.facilityContact = facilityContact;
        this.accounts = Optional.ofNullable(accounts);
        this.locations = locations;
    }

    SosStatusView of(long pressId, AlertRecord alert) {
        String status;
        if (alert.arrivedAt() != null) {
            status = "ARRIVED";
        } else if (alert.receiverId() != null
                && (alert.status() == AlertStatus.RECEIVED || alert.status() == AlertStatus.IN_PROGRESS)) {
            status = "RECEIVED";
        } else {
            status = "SENT";
        }
        Long named = alert.arrivedById() != null ? alert.arrivedById() : alert.receiverId();
        String receiverName = named == null
                ? null
                : accounts.flatMap(api -> api.fullName(named)).orElse(null);
        return new SosStatusView(
                pressId,
                alert.id(),
                status,
                receiverName,
                alert.arrivedAt(),
                facilityContact.current().phoneNumber(),
                locations.sendLocation(alert),
                locations.intervalSeconds());
    }

    BusinessException refused(HttpStatus status, String code, String detail) {
        return new BusinessException(status, code, detail, Map.of("contacts", contacts()));
    }

    private Map<String, Object> contacts() {
        var contacts = new HashMap<String, Object>();
        contacts.put("facilityPhone", facilityContact.current().phoneNumber());
        contacts.put("emergencyNumber", "115");
        return contacts;
    }
}

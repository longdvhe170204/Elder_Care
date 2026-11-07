package com.eldercare.backend.incident.internal.infection.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.infection.persistence.ContactListStore;
import org.springframework.stereotype.Component;

/**
 * Đối tượng cho lớp phạm vi của {@code @RequiresPermission} "Danh sách tiếp xúc" (Q-68, Phụ lục 27): người nghi nhiễm
 * của sự cố. Điều dưỡng chỉ trong phạm vi phân công (²⁴), Bác sĩ toàn viện; Quản lý viện, Trưởng tầng chỉ xem.
 */
@Component
class ContactListAccess {
    private static final String ENTITY = "M05.INCIDENT";

    private final IncidentStore incidents;
    private final ContactListStore lists;
    private final CurrentUser user;

    ContactListAccess(IncidentStore incidents, ContactListStore lists, CurrentUser user) {
        this.incidents = incidents;
        this.lists = lists;
        this.user = user;
    }

    Target ofIncident(long incidentId) {
        var incident = incidents.findSuspected(incidentId).orElseThrow(NotFoundException::new);
        return new Target(ENTITY, Long.toString(incidentId), incident.residentId(), incident.floorAreaId());
    }

    Target ofList(long listId) {
        return ofIncident(lists.require(listId).parent().entityId());
    }

    long accountId() {
        return user.accountId();
    }
}

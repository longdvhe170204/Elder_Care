package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.roombedallocation.api.events.AllocationNoLongerSuitable;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 003 T035 (BR-M03-23, FR-034): phụ lục đổi loại lưu trú áp dụng thì 004 gọi {@code
 * BedAllocationCommandApi.residenceTypeChanged} trong cùng transaction (003 không nghe {@code ResidenceTypeChanged} vì tạo vòng
 * admission ↔ roombedallocation); phân bổ ở loại phòng không phục vụ loại mới mang dấu "phòng không còn phù hợp", không bị hủy,
 * báo một lần. Giường, phân bổ là dữ liệu thật của 003.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_034_ResidenceTypeReassessIT extends ChangeTestBase {

    @Test
    void BR_13_residenceTypeChangeApplied_allocationFlaggedNotClosed_notifiedOnce() throws Exception {
        Setup s = activeSetup();
        long allocationId = activeAllocation(s.residentId(), realBed(realRoom(false), "OCCUPIED"));
        LocalDate effective = today().plusDays(1);
        createAndApprove(request(
                s.contractId(),
                "[\"RESIDENCE_TYPE\"]",
                "{\"residenceType\": \"SHORT_TERM_RESIDENTIAL\", \"endDate\": \"%s\"}"
                        .formatted(effective.plusDays(90)),
                effective));

        applyDueOn(effective);

        assertThat(jdbc.queryForMap("SELECT status, room_unsuitable FROM bed_allocation WHERE id=?", allocationId))
                .containsEntry("status", "ACTIVE")
                .containsEntry("room_unsuitable", true);
        assertThat(events.stream(AllocationNoLongerSuitable.class).filter(e -> e.allocationId() == allocationId))
                .singleElement()
                .satisfies(
                        e -> assertThat(e.conditions()).anyMatch(c -> c.code().equals("ALLOCATION_RESIDENCY_TYPE")));
    }
}

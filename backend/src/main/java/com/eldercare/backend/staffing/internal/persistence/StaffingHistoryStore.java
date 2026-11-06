package com.eldercare.backend.staffing.internal.persistence;

import static com.eldercare.backend.staffing.internal.domain.StaffingHistoryData.*;

import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.common.Reason;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;

/** T050: lịch sử chỉ thêm/đóng một lần qua API bảng chung, không suy quá khứ từ con trỏ hiện tại. */
@Repository
public class StaffingHistoryStore {
    private final StatusHistoryApi histories;

    public StaffingHistoryStore(StatusHistoryApi histories) {
        this.histories = histories;
    }

    public List<HistoryRow<LeadDetails>> shiftLeads(long shift) {
        return histories.list(SHIFT_LEAD, shift);
    }

    public void addShiftLead(long shift, long staff, Instant at, String basis, Long actor, String reason) {
        histories.add(SHIFT_LEAD, shift, entry(staff, at, basis, actor, reason));
    }

    public void closeShiftLead(long shift, Instant at) {
        histories.closeOpen(SHIFT_LEAD, shift, at);
    }

    public void replaceDepartmentLead(long department, long staff, Instant at, long actor, String reason) {
        histories.closeOpen(DEPARTMENT_LEAD, department, at);
        histories.add(DEPARTMENT_LEAD, department, entry(staff, at, "LEAD_CHANGED", actor, reason));
    }

    private static HistoryEntry<LeadDetails> entry(long staff, Instant at, String command, Long actor, String reason) {
        return new HistoryEntry<>(
                HistoryKind.PERIOD,
                null,
                null,
                staff,
                at,
                command,
                actor,
                false,
                reason == null ? null : new Reason(reason),
                null,
                null,
                new LeadDetails(null),
                null,
                null);
    }

    public void closeShiftLeadClamped(long shift, Instant at, Long staff) {
        histories
                .open(SHIFT_LEAD, shift)
                .filter(row -> staff == null || java.util.Objects.equals(staff, row.valueId()))
                .ifPresent(row ->
                        histories.closeOpen(SHIFT_LEAD, shift, at.isBefore(row.startsAt()) ? row.startsAt() : at));
    }
}

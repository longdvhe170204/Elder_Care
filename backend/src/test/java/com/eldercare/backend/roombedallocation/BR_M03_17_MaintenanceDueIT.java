package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.common.scheduling.ScheduledJob;
import com.eldercare.backend.support.TestUsers;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 019 T016 lượt 2 (US4 kịch bản 4; FR-014, BR-M03-17, CFG-M03-09 mặc định 30 ngày; SC-003; quickstart Q26): nhắc Phụ
 * trách bộ phận trước hạn bảo trì, đăng kiểm, bảo hiểm một lần; quá hạn thì gắn dấu và nhắc mỗi ngày; ghi lần mới Đạt
 * (`maintenanceKind`, `nextDueOn`, chủ dự án duyệt 2026-11-06) thì gỡ dấu, đặt hạn kế tiếp. Job không tạo bản ghi (Q-306).
 */
class BR_M03_17_MaintenanceDueIT extends AssetTestBase {

    @Autowired
    List<ScheduledJob> jobs;

    void runJob() {
        jobs.stream()
                .filter(j -> j.name().equals("REMIND_ASSET_DUE_DATES"))
                .findFirst()
                .orElseThrow()
                .runNow();
    }

    long vehicleType() {
        return jdbc.queryForObject(
                "SELECT id FROM catalog_item WHERE catalog_type='M03.ASSET_TYPE' AND code='VEHICLE'", Long.class);
    }

    long vehicle(LocalDate inspectionDue) throws Exception {
        return created("{\"code\":\"XE-" + TestUsers.nextId() + "\",\"typeId\":" + vehicleType()
                + ",\"name\":\"Xe 16 chỗ\",\"departmentId\":" + medical + ",\"source\":\"DONATED\","
                + "\"sourceDescription\":\"Tặng\",\"licensePlate\":\"51B-000.01\",\"seatCount\":16,"
                + "\"vehicleInspectionDueOn\":\"" + inspectionDue + "\",\"insuranceDueOn\":\"" + today().plusYears(1)
                + "\"}");
    }

    LocalDate today() {
        return LocalDate.ofInstant(time.now(), com.eldercare.backend.common.time.BusinessTime.ZONE);
    }

    List<RecipientGroup> reminders(long id, String event) {
        return events.stream(HasNotificationRequests.class)
                .flatMap(e -> e.notificationRequests().stream())
                .filter(n ->
                        n.eventType().equals(event) && n.sourceRecord().id().equals(Long.toString(id)))
                .flatMap(n -> n.recipientGroups().stream())
                .toList();
    }

    @Test
    void BR_M03_17_dueWithinThirtyDaysIsRemindedOnce() throws Exception {
        long due = vehicle(today().plusDays(30));
        long later = vehicle(today().plusDays(31));
        int assetsBefore = jdbc.queryForObject("SELECT COUNT(*) FROM asset", Integer.class);
        runJob();
        runJob();
        assertThat(reminders(due, "ASSET_DUE_SOON"))
                .containsExactly(new RecipientGroup.DepartmentLead(medical, new Substitution.Default()));
        assertThat(reminders(later, "ASSET_DUE_SOON")).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM asset", Integer.class))
                .as("Q-306: job không tạo tài sản")
                .isEqualTo(assetsBefore);
    }

    @Test
    void BR_M03_17_overdueIsFlaggedAndRemindedThenClearedByANewInspection() throws Exception {
        long id = vehicle(today().minusDays(1));
        runJob();
        runJob();
        assertThat(jdbc.queryForObject("SELECT vehicle_inspection_overdue FROM asset WHERE id=?", Boolean.class, id))
                .isTrue();
        assertThat(reminders(id, "ASSET_OVERDUE")).hasSize(1);

        command(id, "start-maintenance", "{\"maintenanceKind\":\"VEHICLE_INSPECTION\"}")
                .andExpect(status().isOk());
        String result = "{\"result\":\"PASS\",\"date\":\"" + today() + "\",\"content\":\"Đăng kiểm định kỳ\","
                + "\"performedBy\":\"Trung tâm đăng kiểm 50-03V\"";
        command(id, "record-maintenance-result", result + "}").andExpect(status().isUnprocessableEntity());
        command(id, "record-maintenance-result", result + ",\"nextDueOn\":\"" + today().plusMonths(6) + "\"}")
                .andExpect(status().isOk());
        assertThat(jdbc.queryForMap(
                        "SELECT status, vehicle_inspection_overdue, vehicle_inspection_due_on FROM asset WHERE id=?",
                        id))
                .containsEntry("status", "READY")
                .containsEntry("vehicle_inspection_overdue", false)
                .containsEntry("vehicle_inspection_due_on", java.sql.Date.valueOf(today().plusMonths(6)));
    }

    @Test
    void FR_014_vehicleOnlyKindsAreRejectedForOtherAssets() throws Exception {
        long id = created(donated("Tặng"));
        command(id, "start-maintenance", "{\"maintenanceKind\":\"INSURANCE\"}")
                .andExpect(status().isUnprocessableEntity());
        command(id, "start-maintenance", "{\"maintenanceKind\":\"INSPECTION\"}").andExpect(status().isOk());
    }
}

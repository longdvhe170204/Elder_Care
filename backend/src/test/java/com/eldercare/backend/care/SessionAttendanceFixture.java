package com.eldercare.backend.care;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.Gender;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.resident.FakeResidents;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;

/** 014 T013: dữ liệu buổi đang diễn ra hoặc đã qua cho test điểm danh (lệnh tạo buổi chỉ nhận thời điểm tương lai). */
final class SessionAttendanceFixture {
    final long floor = TestUsers.nextId();
    final long serviceId = TestUsers.nextId();
    final TestUsers.TestUser manager;
    private final JdbcTemplate jdbc;
    private final FakeResidents residents;
    private final BusinessTime time;

    SessionAttendanceFixture(
            JdbcTemplate jdbc,
            BusinessTime time,
            FakeAssignments assignments,
            SwitchableUser user,
            FakeResidents residents) {
        this.jdbc = jdbc;
        this.time = time;
        this.residents = residents;
        manager = new TestUsers(jdbc, time).staff(Role.FLOOR_MANAGER);
        assignments.assign(
                manager.staffId(),
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER,
                        floor,
                        null,
                        null,
                        null,
                        time.now().minusSeconds(86400),
                        null));
        user.set(manager.user());
    }

    long activity(boolean charged) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO simple_record(id,kind,module,status,content,created_at,created_by) VALUES(?,'M04.ACTIVITY','M04','ACTIVE',?,NOW(6),1)",
                id,
                "{\"name\":\"Tập đàn\",\"typeId\":1,\"scope\":\"FLOOR\",\"floorAreaId\":" + floor
                        + ",\"maxParticipants\":20,\"charged\":" + charged + ",\"serviceId\":"
                        + (charged ? serviceId : "null") + ",\"groupActivity\":true,\"createdByStaffId\":"
                        + manager.staffId() + "}");
        return id;
    }

    /** Buổi bắt đầu {@code startOffset} so với bây giờ, dài một giờ. */
    long session(long activity, Duration startOffset) {
        long id = TestUsers.nextId();
        var start = LocalDateTime.ofInstant(time.now().plus(startOffset), BusinessTime.ZONE);
        jdbc.update(
                "INSERT INTO activity_session(id,activity_id,starts_at,ends_at,source,floor_area_id,responsible_staff_id,status,created_at,created_by)"
                        + " VALUES(?,?,?,?,'SINGLE',?,?,'SCHEDULED',NOW(6),1)",
                id,
                activity,
                start,
                start.plusHours(1),
                floor,
                manager.staffId());
        return id;
    }

    long resident() {
        long r = TestUsers.nextId();
        residents.resident(r, "[Họ tên NCT]", Gender.FEMALE, ResidentStatus.IN_RESIDENCE);
        return r;
    }

    long registration(long session, long resident) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO activity_registration(id,session_id,resident_id,source,registered_by_staff_id,registered_at,status)"
                        + " VALUES(?,?,?,'MANUAL',?,NOW(6),'REGISTERED')",
                id,
                session,
                resident,
                manager.staffId());
        return id;
    }

    List<Long> residents(long session, int count) {
        var list = new ArrayList<Long>();
        for (int i = 0; i < count; i++) {
            long r = resident();
            registration(session, r);
            list.add(r);
        }
        return list;
    }
}

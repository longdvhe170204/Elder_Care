package com.eldercare.backend.support;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

/**
 * Dữ liệu thật liên module cho test chạy với bean thật thay bản giả (001 T073, 005 T061, 007 T063, 008 T043b, 2026-11-11):
 * khu, tầng, phòng, giường (003), hồ sơ (001), hợp đồng Hiệu lực (004), phân bổ giường (003), hồ sơ nhân viên, ca đã công bố,
 * thành viên, phụ trách ca, phân công người cao tuổi, Trưởng tầng (008). Ghi thẳng vào bảng như các fixture của chủ module
 * ({@code ContractFixtures}, {@code ReportScopeFixture}, {@code BR_M02_07_ReleaseBedIT}); id mới theo {@link TestUsers#nextId()}.
 */
public final class RealCareFixture {

    public record Place(long area, long floor, long room, long bed) {}

    private final JdbcTemplate jdbc;
    private final TestUsers users;

    public RealCareFixture(JdbcTemplate jdbc, BusinessTime time) {
        this.jdbc = jdbc;
        this.users = new TestUsers(jdbc, time);
    }

    public TestUsers users() {
        return users;
    }

    /** Tài khoản nhân viên có hồ sơ nhân viên 008. */
    public TestUser staff(Role role) {
        TestUser user = users.staff(role);
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,account_id) VALUES (?,?,?,?)",
                user.staffId(),
                "[Nhân viên thật " + user.staffId() + "]",
                role.name(),
                user.accountId());
        return user;
    }

    /** Khu, tầng, phòng cho mức Chăm sóc cơ bản, một giường Đang sử dụng. */
    public Place place() {
        long area = TestUsers.nextId(), floor = TestUsers.nextId(), type = TestUsers.nextId();
        long room = TestUsers.nextId(), bed = TestUsers.nextId();
        jdbc.update("INSERT INTO area(id,code,name) VALUES(?,?,?)", area, "A" + area, "Khu " + area);
        jdbc.update(
                "INSERT INTO floor(id,area_id,code,name) VALUES(?,?,?,?)", floor, area, "F" + floor, "Tầng " + floor);
        jdbc.update("INSERT INTO room_type(id,name,standard_beds) VALUES(?,?,2)", type, "Loại " + type);
        jdbc.update("INSERT INTO room_type_care_level VALUES(?,'BASIC')", type);
        jdbc.update(
                "INSERT INTO room(id,floor_id,room_type_id,code,name,gender_policy) VALUES(?,?,?,?,?,'UNRESTRICTED')",
                room,
                floor,
                type,
                "R" + room,
                "Phòng " + room);
        jdbc.update("UPDATE room SET allowed_care_levels=JSON_ARRAY('BASIC') WHERE id=?", room);
        jdbc.update("INSERT INTO bed(id,room_id,code,state) VALUES(?,?,'G1','OCCUPIED')", bed, room);
        return new Place(area, floor, room, bed);
    }

    /** Giường thứ hai cùng phòng. */
    public Place secondBed(Place place) {
        long bed = TestUsers.nextId();
        jdbc.update("INSERT INTO bed(id,room_id,code,state) VALUES(?,?,'G2','OCCUPIED')", bed, place.room());
        return new Place(place.area(), place.floor(), place.room(), bed);
    }

    /**
     * Người cao tuổi Đang lưu trú, mức Chăm sóc cơ bản, hợp đồng nội trú dài hạn Hiệu lực từ {@code since}, phân bổ giường
     * Đang hiệu lực từ {@code since}.
     */
    public long residentInBed(Place place, Instant since) {
        String code = UUID.randomUUID().toString().substring(0, 20);
        jdbc.update(
                "INSERT INTO resident (profile_code, full_name, date_of_birth, gender, status, status_since,"
                        + " current_care_level, created_at, created_by) VALUES (?, ?, '1940-05-01', 'FEMALE',"
                        + " 'IN_RESIDENCE', ?, 'BASIC', NOW(6), 1)",
                code,
                "[Người cao tuổi thật " + code.substring(0, 6) + "]",
                Timestamp.from(since));
        long resident = jdbc.queryForObject("SELECT id FROM resident WHERE profile_code = ?", Long.class, code);
        ResidentHistoryRows.insertStatus(
                jdbc,
                null,
                resident,
                "ADMISSION_IN_PROGRESS",
                "IN_RESIDENCE",
                "SEED",
                Timestamp.from(since),
                1L,
                true,
                null);
        LocalDate start = LocalDate.ofInstant(since, BusinessTime.ZONE);
        long contract = insert(
                "INSERT INTO contract (resident_id, residence_type, start_date, care_level, residency_fee_billing_period,"
                        + " status, signed_date, signing_representative_id, scan_file_id, created_by, created_at)"
                        + " VALUES (?, 'LONG_TERM_RESIDENTIAL', ?, 'BASIC', 'MONTH', 'ACTIVE', ?, 1, 1, 1, ?)",
                resident,
                start,
                start,
                LocalDateTime.now());
        jdbc.update(
                "UPDATE contract SET contract_number = ? WHERE id = ?",
                "HD-%d-%06d".formatted(start.getYear(), contract),
                contract);
        jdbc.update(
                "INSERT INTO contract_version (kind, contract_id, services, arrival_schedule, created_at)"
                        + " VALUES ('DRAFT', ?, '[]', '[]', UTC_TIMESTAMP(6))",
                contract);
        jdbc.update(
                "INSERT INTO contract_version (kind, contract_id, sequence_no, effective_from, content, residence_type,"
                        + " care_level, created_at, locked_at) VALUES ('CONTENT', ?, 1, ?, ?, 'LONG_TERM_RESIDENTIAL', 'BASIC',"
                        + " UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))",
                contract,
                start,
                "{\"residenceType\":\"LONG_TERM_RESIDENTIAL\",\"careLevel\":\"BASIC\",\"startDate\":\"" + start
                        + "\",\"services\":[],\"arrivalSchedule\":[]}");
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source)"
                        + " VALUES(?,?,'ACTIVE',?,'BASIC','CURRENT')",
                resident,
                place.bed(),
                Timestamp.from(since));
        return resident;
    }

    /** Ca của tầng đã công bố, từ {@code start}, dài {@code hours} giờ; mỗi ca một lịch tháng riêng. */
    public long shift(long floor, Instant start, int hours) {
        String month = YearMonth.from(start.atZone(BusinessTime.ZONE)).toString();
        long schedule = insert(
                "INSERT INTO shift_schedule(month,scope_type,scope_id,status) VALUES (?,'FLOOR',?,'PUBLISHED')",
                month,
                floor);
        long template = insert(
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) VALUES ('M09.SHIFT_TEMPLATE','M09',?,'ACTIVE',JSON_OBJECT('createdAt',?,'startTime','07:00:00','endTime','15:00:00','scopeType','FLOOR','requiresHandover',CAST('false' AS JSON),'originalName','[Mẫu ca thật]'))",
                "[Mẫu ca thật] #" + TestUsers.nextId(),
                start.toString());
        return insert(
                "INSERT INTO work_shift(schedule_id,template_id,scope_type,scope_id,starts_at,ends_at,requires_handover,status) VALUES (?,?,'FLOOR',?,?,?,FALSE,'PUBLISHED')",
                schedule,
                template,
                floor,
                utc(start),
                utc(start.plus(Duration.ofHours(hours))));
    }

    public void member(long shift, long staff) {
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, staff);
    }

    public void lead(long shift, long staff, Instant from) {
        jdbc.update(
                "INSERT INTO status_history(entity_type,entity_id,attribute,module,kind,value_id,starts_at,command,by_system) VALUES ('M09.SHIFT',?,'LEAD_STAFF','M09','PERIOD',?,?,'SCHEDULED',FALSE)",
                shift,
                staff,
                utc(from));
    }

    /** Phân công chính người cao tuổi trong ca cho {@code role}, từ {@code from} tới {@code to}, đang hiệu lực. */
    public void assignResident(long shift, long staff, Role role, long resident, Instant from, Instant to) {
        insert(
                "INSERT INTO staff_assignment(shift_id,staff_id,target_type,target_id,role,assignment_kind,starts_at,ends_at,status,created_by) VALUES (?,?,'RESIDENT',?,?,'PRIMARY',?,?,'ACTIVE',1)",
                shift,
                staff,
                resident,
                role.name(),
                utc(from),
                utc(to));
    }

    /** Như {@link #assignResident} cho Điều dưỡng, mang dấu Điều dưỡng phụ trách (cảnh báo giao cho người này, 007 FR-031). */
    public void assignResponsibleNurse(long shift, long staff, long resident, Instant from, Instant to) {
        insert(
                "INSERT INTO staff_assignment(shift_id,staff_id,target_type,target_id,role,assignment_kind,assigned_nurse,starts_at,ends_at,status,created_by) VALUES (?,?,'RESIDENT',?,'NURSING_STAFF','PRIMARY',TRUE,?,?,'ACTIVE',1)",
                shift,
                staff,
                resident,
                utc(from),
                utc(to));
    }

    /** Giao Trưởng tầng từ {@code from}, chưa kết thúc. */
    public void floorManager(long staff, long floor, Instant from) {
        jdbc.update(
                "INSERT INTO staff_assignment(kind,staff_id,floor_area_id,starts_at,assigned_by,status) VALUES ('FLOOR_MANAGER',?,?,?,1,'ACTIVE')",
                staff,
                floor,
                utc(from));
    }

    /** Cột thời gian của 008 lưu giờ UTC như {@code ShiftStore.sql}. */
    private static LocalDateTime utc(Instant at) {
        return LocalDateTime.ofInstant(at, java.time.ZoneOffset.UTC);
    }

    private long insert(String sql, Object... args) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    var ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
                    return ps;
                },
                keys);
        return keys.getKey().longValue();
    }
}

package com.eldercare.backend.nutrition;

import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.parameter.api.dto.ParameterKind;
import com.eldercare.backend.administration.parameter.api.dto.ParameterValue;
import com.eldercare.backend.admission.api.dto.AttendanceScheduleView;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.nutrition.internal.closing.domain.DailyMeal;
import com.eldercare.backend.resident.api.dto.OpenAbsence;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.resident.api.dto.ResidenceTypePeriod;
import com.eldercare.backend.roombedallocation.api.dto.LocationRef;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.admission.FakeContractSchedules;
import com.eldercare.backend.support.fake.resident.FakeAbsences;
import com.eldercare.backend.support.fake.resident.FakeContracts;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocations;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

/**
 * Nền chung cho IT chốt suất của 011 Phase 6 (một context): bản giả dùng chung của 003 (vị trí, khu bán trú), 004 (lượt
 * vắng, lịch đến, loại lưu trú); lượt ở lại (012 `StayMeals`) và chuyến đi (014 qua `TripMeals`) là bean thật từ 2026-11-11 (011
 * T042); giờ bữa CFG-M08-04 đặt qua spy của {@link ParameterApi}
 * (như {@code BR_M03_25_DayCareParameterIT}); đồng hồ giả lập. Tầng/khu mỗi test mới, nên người của test khác không lẫn vào
 * phần so sánh theo tầng.
 */
@Import(ClosingTestBase.Fakes.class)
abstract class ClosingTestBase extends MenuTestBase {

    static final String DAILY_MEALS = "/api/v1/daily-meals/";

    @TestConfiguration
    static class Fakes {
        @Bean
        @Primary
        FakeLocations closingLocations() {
            return new FakeLocations();
        }

        @Bean
        @Primary
        FakeAbsences closingAbsences() {
            return new FakeAbsences();
        }

        @Bean
        @Primary
        FakeContractSchedules closingSchedules() {
            return new FakeContractSchedules();
        }

        @Bean
        @Primary
        FakeContracts closingResidenceTypes() {
            return new FakeContracts();
        }
    }

    @Autowired
    FakeLocations locations;

    @Autowired
    FakeAbsences absences;

    @Autowired
    FakeContractSchedules schedules;

    @Autowired
    FakeContracts residenceTypes;

    @Autowired
    TestClockControl clock;

    @MockitoSpyBean
    ParameterApi parameters;

    /** Lệnh của người nhận tại tầng (phạm vi ASSIGNED theo ca của 008): test mở đúng quyền cần bằng spy. */
    @MockitoSpyBean
    com.eldercare.backend.administration.account.api.PermissionApi permissionApi;

    /** Ngày của các bữa trong test (thứ Tư); giờ bữa: sáng 06:00, trưa 11:00, tối 17:00. */
    LocalDate day;

    long floor2;
    long floor3;
    long room2;
    long room3;

    @BeforeEach
    void setUpClosing() throws Exception {
        // Dưới năm 2038: outbox của Spring Modulith (EVENT_PUBLICATION) lưu TIMESTAMP theo đồng hồ giả lập.
        day = time.today()
                .with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY))
                .plusWeeks(2 + TestUsers.nextId() % 150)
                .plusDays(2);
        String times = "{\"mealTimes\":{\"M08.MEAL." + breakfast + "\":\"06:00\",\"M08.MEAL." + lunch
                + "\":\"11:00\",\"M08.MEAL." + dinner + "\":\"17:00\"},"
                + "\"lateDeliveryThreshold\":{\"value\":30,\"unit\":\"MINUTES\"}}";
        doReturn(new ParameterValue(
                        ParameterCode.CFG_M08_04,
                        ParameterKind.TABLE,
                        JsonMapper.builder().build().readTree(times)))
                .when(parameters)
                .read(ParameterCode.CFG_M08_04);
        floor2 = TestUsers.nextId();
        floor3 = TestUsers.nextId();
        room2 = TestUsers.nextId();
        room3 = TestUsers.nextId();
        locations.floor(floor2, null).floor(floor3, null).room(room2, floor2).room(room3, floor3);
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Autowired
    jakarta.persistence.EntityManager entityManager;

    /** Đổi trạng thái người cao tuổi trong transaction của test: 001 đọc qua JPA nên phải xóa bộ đệm phiên. */
    void setStatus(long resident, String status) {
        entityManager.flush();
        jdbc.update("UPDATE resident SET status = ? WHERE id = ?", status, resident);
        entityManager.clear();
    }

    Instant at(LocalTime local) {
        return day.atTime(local).atZone(BusinessTime.ZONE).toInstant();
    }

    long dailyMeal(long meal) {
        return DailyMeal.id(meal, day);
    }

    /** Người cao tuổi Đang lưu trú ở giường thuộc phòng {@code room}. */
    long residentIn(long room) {
        long resident = f.resident("IN_RESIDENCE");
        locations.stay(resident, room, at(LocalTime.MIDNIGHT).minusSeconds(86_400 * 30L), null);
        return resident;
    }

    long absentResident(long room, Instant expectedReturn) {
        long resident = f.resident("TEMPORARILY_ABSENT");
        locations.stay(resident, room, at(LocalTime.MIDNIGHT).minusSeconds(86_400 * 30L), null);
        absences.open(resident, new OpenAbsence(TestUsers.nextId(), expectedReturn));
        return resident;
    }

    /** Người bán trú có lịch đến bao trùm bữa trưa, khu bán trú là {@code area}. */
    long dayCareResident(long area) {
        long resident = f.resident("IN_RESIDENCE");
        locations.floor(area, null).daycareRestLocation(new LocationRef("FLOOR", area));
        residenceTypes.period(
                resident, new ResidenceTypePeriod(ResidenceType.DAY_CARE, day.minusDays(30), Optional.empty()));
        schedules.schedule(resident, new AttendanceScheduleView(day, LocalTime.of(7, 30), LocalTime.of(16, 0)));
        return resident;
    }

    /** Thực đơn Công bố của tuần chứa {@link #day}, đủ mọi bữa của chế độ mặc định với món {@link #plain}. */
    long publishedWeek() throws Exception {
        return publishedMenu(day.minusDays(2), plain);
    }

    ResultActions close(long dailyMeal) throws Exception {
        f.asKitchen();
        return send(post(DAILY_MEALS + dailyMeal + "/close-portions"), "{}");
    }

    ResultActions precomputed(long dailyMeal) throws Exception {
        f.asKitchen();
        return send(get(DAILY_MEALS + dailyMeal + "/precomputed"), null);
    }

    ResultActions adjustmentsOf(long dailyMeal) throws Exception {
        f.asKitchen();
        return send(get(DAILY_MEALS + dailyMeal + "/adjustments"), null);
    }

    int slipCount(long dailyMeal) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM simple_record WHERE kind = 'M08.MEAL_SLIP' AND entity_type = 'M08.DAILY_MEAL'"
                        + " AND entity_id = ?",
                Integer.class,
                dailyMeal);
    }
}

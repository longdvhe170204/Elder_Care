package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.care.api.DayCareAttendanceApi;
import com.eldercare.backend.care.api.dto.AttendanceStatus;
import com.eldercare.backend.care.api.dto.AttendanceView;
import com.eldercare.backend.care.api.dto.DailyScheduleEntry;
import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.care.api.spi.DailyMealPort;
import com.eldercare.backend.care.internal.catalog.persistence.CareCatalogStore;
import com.eldercare.backend.care.internal.dailyschedule.domain.DailyRoutineAttributes;
import com.eldercare.backend.care.internal.dailyschedule.domain.DailyRoutineAttributes.Version;
import com.eldercare.backend.care.internal.dailyschedule.domain.DailyRoutineKinds;
import com.eldercare.backend.care.internal.dailyschedule.persistence.DailyScheduleStore;
import com.eldercare.backend.care.internal.dailyschedule.service.DailyCareSchedule;
import com.eldercare.backend.care.internal.session.persistence.SessionTimetableStore;
import com.eldercare.backend.care.internal.task.domain.CareTask;
import com.eldercare.backend.care.internal.task.domain.CareTaskData;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.time.BusinessTime;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/** 005 FR-055, FR-056 (Q-440): ghép giờ bữa, hoạt động, mốc sinh hoạt và công việc; trạng thái rút gọn, giá trị đo có điều kiện. */
class FR_055_DailyCareScheduleTest {
    private static final long RESIDENT = 9;
    private static final LocalDate DAY = LocalDate.of(2035, 5, 2);

    private final CareTaskStore tasks = mock(CareTaskStore.class);
    private final CareCatalogStore catalogs = mock(CareCatalogStore.class);
    private final SessionTimetableStore sessions = mock(SessionTimetableStore.class);
    private final DailyScheduleStore store = mock(DailyScheduleStore.class);
    private final DailyMealPort meals = mock(DailyMealPort.class);
    private final DayCareAttendanceApi attendance = mock(DayCareAttendanceApi.class);

    private static Instant at(int hour, int minute) {
        return DAY.atTime(hour, minute).atZone(BusinessTime.ZONE).toInstant();
    }

    private static CatalogItem<DailyRoutineAttributes> routine(long id, Version... versions) {
        return new CatalogItem<>(
                id,
                null,
                versions[0].name(),
                null,
                0,
                CatalogStatus.ACTIVE,
                new DailyRoutineAttributes(List.of(versions)),
                null,
                null,
                1);
    }

    private static CareTask task(long id, Instant plannedAt, TaskStatus status, String name) {
        var task = mock(CareTask.class);
        when(task.id()).thenReturn(id);
        when(task.plannedAt()).thenReturn(plannedAt);
        when(task.status()).thenReturn(status);
        when(task.data())
                .thenReturn(new CareTaskData(
                        1, name, "Nội dung", "MEASURED_VALUE", false, null, Set.of(), null, null, true));
        return task;
    }

    @SuppressWarnings("unchecked")
    private DailyCareSchedule schedule() {
        ObjectProvider<DailyMealPort> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(meals);
        when(catalogs.list(DailyRoutineKinds.DAILY_ROUTINES))
                .thenReturn(List.of(
                        routine(1, new Version(DAY.minusDays(3), "Thức dậy", LocalTime.of(6, 0), true)),
                        routine(
                                2,
                                new Version(DAY.minusDays(3), "Ngủ trưa", LocalTime.of(12, 30), true),
                                new Version(DAY.plusDays(1), "Ngủ trưa", LocalTime.of(13, 0), true)),
                        routine(
                                3,
                                new Version(DAY.minusDays(3), "Tập thể dục", LocalTime.of(6, 30), true),
                                new Version(DAY, "Tập thể dục", LocalTime.of(6, 30), false))));
        when(meals.mealsOf(RESIDENT, DAY)).thenReturn(List.of(new DailyMealPort.MealSlot(at(12, 0), "Ăn trưa")));
        when(sessions.registered(RESIDENT, DAY)).thenReturn(List.of());
        when(store.facilitySessions(DAY))
                .thenReturn(List.of(new com.eldercare.backend.care.internal.dailyschedule.domain.FacilitySession(
                        at(9, 0), "Hát karaoke")));
        when(store.measuredValues(eq(RESIDENT), any(), any())).thenReturn(Map.of(10L, new BigDecimal("120.0")));
        var dayTasks = List.of(
                task(10, at(8, 0), TaskStatus.COMPLETED, "Đo huyết áp"),
                task(11, at(8, 30), TaskStatus.DUE, "Phục hồi chức năng"),
                task(12, at(10, 0), TaskStatus.CANCELLED, "Đã hủy"),
                task(13, at(12, 0), TaskStatus.NOT_PERFORMED, "Hỗ trợ ăn uống"));
        when(tasks.ofResidentBetween(eq(RESIDENT), any(), any())).thenReturn(dayTasks);
        when(attendance.onDate(RESIDENT, DAY)).thenReturn(Optional.empty());
        return new DailyCareSchedule(tasks, catalogs, sessions, store, provider, attendance);
    }

    @Test
    void FR_055_mergesSourcesByTimeAndHidesCancelledTasks() {
        var items = schedule().onDate(RESIDENT, DAY, false);
        assertThat(items)
                .extracting(DailyScheduleEntry::name)
                .containsExactly(
                        "Thức dậy",
                        "Đo huyết áp",
                        "Phục hồi chức năng",
                        "Hát karaoke",
                        "Ăn trưa",
                        "Hỗ trợ ăn uống",
                        "Ngủ trưa");
        assertThat(items)
                .extracting(DailyScheduleEntry::status)
                .containsExactly(null, "DONE", "NOT_DONE", null, null, "NOT_PERFORMED", null);
        assertThat(items).extracting(DailyScheduleEntry::measuredValue).containsOnlyNulls();
    }

    @Test
    void FR_056_routineChangeAppliesFromItsDateOnly() {
        var items = schedule().onDate(RESIDENT, DAY, false);
        assertThat(items)
                .filteredOn(i -> i.name().equals("Ngủ trưa"))
                .singleElement()
                .extracting(DailyScheduleEntry::at)
                .isEqualTo(at(12, 30));
        assertThat(items).noneMatch(i -> i.name().equals("Tập thể dục"));
    }

    @Test
    void FR_055_measuredValueOnlyWhenAllowed() {
        var items = schedule().onDate(RESIDENT, DAY, true);
        assertThat(items)
                .filteredOn(i -> "Đo huyết áp".equals(i.name()))
                .singleElement()
                .extracting(DailyScheduleEntry::measuredValue)
                .isEqualTo("120");
    }

    @Test
    void FR_055_dayCareShowsOnlyItemsDuringPresence() {
        var schedule = schedule();
        when(attendance.onDate(RESIDENT, DAY))
                .thenReturn(Optional.of(new AttendanceView(
                        1,
                        RESIDENT,
                        DAY,
                        AttendanceStatus.values()[0],
                        Optional.of(LocalTime.of(8, 0)),
                        Optional.of(LocalTime.of(11, 0)),
                        Optional.of(LocalTime.of(11, 0)),
                        Optional.empty(),
                        Optional.empty(),
                        false)));
        assertThat(schedule.onDate(RESIDENT, DAY, false))
                .extracting(DailyScheduleEntry::name)
                .containsExactly("Đo huyết áp", "Phục hồi chức năng", "Hát karaoke");
    }

    @Test
    void FR_056_sameDayEditsKeepOnlyTheLastVersion() {
        var tomorrow = DAY.plusDays(1);
        var attributes = new DailyRoutineAttributes(List.of(new Version(DAY, "Thức dậy", LocalTime.of(6, 0), true)))
                .with(new Version(tomorrow, "Thức dậy", LocalTime.of(5, 30), true))
                .with(new Version(tomorrow, "Thức dậy sớm", LocalTime.of(5, 45), true));
        assertThat(attributes.versions()).hasSize(2);
        assertThat(attributes.on(DAY).orElseThrow().time()).isEqualTo(LocalTime.of(6, 0));
        assertThat(attributes.on(tomorrow).orElseThrow().name()).isEqualTo("Thức dậy sớm");
        assertThat(attributes.on(DAY.minusDays(1))).isEmpty();
    }
}

package com.eldercare.backend.nutrition;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.RetainedSampleMarker;
import com.eldercare.backend.nutrition.internal.closing.service.ClosingEventHandlers;
import com.eldercare.backend.nutrition.internal.schedule.service.M08JobService;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Nền chung cho IT phiếu bữa ăn của 011 Phase 7 (trên {@link ClosingTestBase}, một context): bản ghi lưu mẫu của bữa ngày
 * ghi thẳng qua {@link SimpleRecordApi} (lệnh lưu mẫu là Phase 11); người nhận tại tầng mở quyền "Nhận phiếu, báo sai lệch"
 * cho đúng một tầng/khu bằng spy {@code PermissionApi}, tầng khác trả lớp {@code SCOPE} không thỏa.
 */
abstract class SlipTestBase extends ClosingTestBase {

    static final String SLIPS = "/api/v1/meal-slips";

    @Autowired
    SimpleRecordApi simpleRecords;

    @Autowired
    ClosingEventHandlers handlers;

    @Autowired
    M08JobService jobs;

    /** Bữa ngày có bản ghi lưu mẫu (FR-060). */
    void sample(long dailyMeal) {
        simpleRecords.create(
                RetainedSampleMarker.KIND,
                new NewSimpleRecord<>(
                        null,
                        new EntityRef("M08.DAILY_MEAL", dailyMeal),
                        "RECORDED",
                        new RetainedSampleMarker(dailyMeal),
                        null,
                        null,
                        null,
                        1,
                        false));
    }

    /** Nhân viên chăm sóc có phân công ở {@code floor} trong ca: mọi thao tác "Nhận phiếu, báo sai lệch" ở tầng đó. */
    TestUser receiverOf(long floor) {
        return staffOf(floor, Function.MEAL_RECEIPT);
    }

    /** Nhân viên có quyền {@code function} chỉ ở tầng/khu {@code floor} (spy {@code PermissionApi}). */
    TestUser staffOf(long floor, Function function) {
        TestUser u = new TestUsers(jdbc, time).staff(Role.CAREGIVER);
        doReturn(PermissionResult.allowed(new PermissionBasis(PermissionBasis.Kind.ROLE, Optional.empty())))
                .when(permissionApi)
                .hasPermission(
                        argThat(c -> c != null && c.accountId() == u.accountId()),
                        argThat(k -> k != null && k.function() == function),
                        argThat(t -> t != null && Long.valueOf(floor).equals(t.floorAreaId())));
        doReturn(PermissionResult.denied(FailedLayer.SCOPE))
                .when(permissionApi)
                .hasPermission(
                        argThat(c -> c != null && c.accountId() == u.accountId()),
                        argThat(k -> k != null && k.function() == function),
                        argThat(t -> t != null && !Long.valueOf(floor).equals(t.floorAreaId())));
        return u;
    }

    void as(TestUser u) {
        user.set(u.user());
    }

    ResultActions slipsOf(long dailyMeal) throws Exception {
        return send(get(SLIPS + "?dailyMealId=" + dailyMeal), null);
    }

    long slipId(long dailyMeal, long floor) {
        return jdbc.queryForObject(
                "SELECT id FROM simple_record WHERE kind = 'M08.MEAL_SLIP' AND unique_key = ?",
                Long.class,
                "M08.MEAL_SLIP:" + dailyMeal + ":" + floor);
    }

    String slipStatus(long slip) {
        return jdbc.queryForObject("SELECT status FROM simple_record WHERE id = ?", String.class, slip);
    }

    ResultActions slipCommand(long slip, String command, String body) throws Exception {
        return send(post(SLIPS + "/" + slip + "/" + command), body);
    }

    ResultActions supplementCommand(long supplement, String command, String body) throws Exception {
        return send(post("/api/v1/supplementary-portions/" + supplement + "/" + command), body);
    }

    long specialOf(long resident) {
        return jdbc.queryForObject(
                "SELECT id FROM simple_record WHERE kind = 'M08.SPECIAL_PORTION' AND resident_id = ? AND status = 'ACTIVE'",
                Long.class,
                resident);
    }

    /** Bếp chuẩn bị mọi suất đặc biệt, đánh dấu phiếu chuẩn bị xong, ghi lưu mẫu rồi giao. */
    void prepareAndDeliver(long dailyMeal, long slip, List<Long> specialResidents) throws Exception {
        f.asKitchen();
        for (long r : specialResidents) {
            send(post("/api/v1/special-portions/" + specialOf(r) + "/mark-prepared"), "{}")
                    .andExpect(status().isOk());
        }
        slipCommand(slip, "mark-prepared", "{}").andExpect(status().isOk());
        sample(dailyMeal);
        slipCommand(slip, "deliver", "{}").andExpect(status().isOk());
    }

    /** Tổng suất theo chế độ ăn trên mọi phiếu của bữa (bếp xem). */
    int slipTotal(long dailyMeal) throws Exception {
        f.asKitchen();
        String body = slipsOf(dailyMeal).andReturn().getResponse().getContentAsString();
        List<Integer> counts = JsonPath.read(body, "$[*].portionsByDiet[*].portionCount");
        return counts.stream().mapToInt(Integer::intValue).sum();
    }

    /** Số trong JSON (JsonPath trả Integer khi vừa, Long khi lớn). */
    static Object num(long v) {
        return v == (int) v ? (Object) (int) v : (Object) v;
    }

    /** Người cao tuổi ăn kết cấu mềm (suất đặc biệt, FR-035) từ trước ngày của test. */
    long softResident(long room) throws Exception {
        long r = residentIn(room);
        f.activeAssignment(r, defaultDiet, "SOFT", List.of(), time.now());
        return r;
    }
}

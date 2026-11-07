package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.nutrition.api.events.AdjustmentsPending;
import com.eldercare.backend.nutrition.api.events.MealSlipLateNotice;
import com.eldercare.backend.nutrition.api.events.SlipDiscrepancyNotice;
import com.eldercare.backend.nutrition.internal.slip.service.MealSlipService;
import com.eldercare.backend.nutrition.web.response.AdjustmentResponse;
import com.eldercare.backend.nutrition.web.response.ClosingResultResponse;
import com.eldercare.backend.nutrition.web.response.KitchenMealSlipResponse;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 011 T028 (US5, FR-046, SC-008; quickstart Q37; tên {@code SC_008_DtoBepTest} trong tasks): mọi DTO bếp nhận (phiếu, nhãn,
 * phần bổ sung, phát sinh, kết quả chốt), quét đệ quy theo thành phần record, không có trường sức khỏe; thông báo của phiếu,
 * phát sinh tới bếp không có người cao tuổi, phần sức khỏe, từ dị ứng, bệnh lý, thành phần.
 */
class SC_008_KitchenDtoTest {

    private static final List<String> FORBIDDEN =
            List.of("allerg", "disease", "condition", "ingredient", "crosscheck", "health", "reason", "conflict");

    @Test
    void SC_008_kitchenDtos_haveNoHealthFields() {
        List<String> names = new ArrayList<>();
        for (Class<?> dto : List.of(
                KitchenMealSlipResponse.class,
                MealSlipService.KitchenSlip.class,
                AdjustmentResponse.class,
                ClosingResultResponse.class)) {
            collect(dto, dto.getSimpleName(), names, new HashSet<>());
        }
        assertThat(names).isNotEmpty();
        assertThat(names)
                .allSatisfy(n -> assertThat(FORBIDDEN)
                        .noneMatch(w -> n.toLowerCase(Locale.ROOT)
                                .substring(n.lastIndexOf('.') + 1)
                                .contains(w)));
    }

    @Test
    void SC_008_noticesToKitchen_haveNoResidentOrHealthContent() {
        Instant at = Instant.parse("2030-01-01T04:00:00Z");
        List<NotificationRequest> requests = new ArrayList<>();
        requests.addAll(new SlipDiscrepancyNotice(1, 2, false, false, null, "k1", 3L, at).notificationRequests());
        requests.addAll(new SlipDiscrepancyNotice(1, 2, false, true, null, "k2", 3L, at).notificationRequests());
        requests.addAll(new MealSlipLateNotice(1, 2, 3, "Trưa", at).notificationRequests());
        requests.addAll(new AdjustmentsPending(2, LocalDate.of(2030, 1, 1), 2, true, "k3", at).notificationRequests());
        assertThat(requests).allSatisfy(r -> {
            assertThat(r.recipientGroups())
                    .anySatisfy(g -> assertThat(g).isEqualTo(new RecipientGroup.FacilityRole(Role.KITCHEN_STAFF)));
            assertThat(r.residentId()).isEmpty();
            assertThat(r.content()).extracting(ContentPart::type).containsOnly(InformationType.GENERAL);
            assertThat(r.content())
                    .extracting(ContentPart::content)
                    .allSatisfy(
                            t -> assertThat(t.toLowerCase(Locale.ROOT)).doesNotContain("dị ứng", "bệnh", "thành phần"));
        });
    }

    /** Tên thành phần record, đệ quy vào record lồng và phần tử của danh sách. */
    private static void collect(Class<?> type, String path, List<String> out, Set<Class<?>> seen) {
        if (!type.isRecord() || !seen.add(type)) {
            return;
        }
        for (RecordComponent c : type.getRecordComponents()) {
            String name = path + "." + c.getName();
            out.add(name);
            collect(c.getType(), name, out, seen);
            Type generic = c.getGenericType();
            if (generic instanceof ParameterizedType p) {
                for (Type arg : p.getActualTypeArguments()) {
                    if (arg instanceof Class<?> k) {
                        collect(k, name + "[]", out, seen);
                    }
                }
            }
        }
    }
}

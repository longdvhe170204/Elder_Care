package com.eldercare.backend.family.internal.stay.service;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.family.internal.stay.domain.FamilyStay;
import com.eldercare.backend.family.internal.stay.domain.FamilyStay.Status;
import com.eldercare.backend.nutrition.api.dto.StayMealView;
import com.eldercare.backend.nutrition.api.events.StayMealChanged;
import com.eldercare.backend.nutrition.api.spi.StayMealPort;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lượt ở lại có đăng ký ăn cho 011 (012 FR-042 (a), research F-09; module-interfaces 011 mục 1.6): lượt Đang ở lại, có
 * đăng ký ăn, đã bắt đầu tại {@code t} và dự kiến kết thúc sau {@code t}. Sự kiện {@link StayMealChanged} phát khi Bắt
 * đầu, Gia hạn, Kết thúc, Hủy; khóa sự kiện theo lượt, trạng thái và phiên bản bản ghi nên 011 nhận mỗi lần đổi một lần.
 */
@Service
public class StayMeals implements StayMealPort {

    private final SimpleRecordApi records;

    StayMeals(SimpleRecordApi records) {
        this.records = records;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StayMealView> staysWithMealRegistration(Instant t) {
        return records.listByStatus(FamilyStay.KIND, Set.of(Status.STAYING.name())).stream()
                .filter(s -> s.content().mealRegistered())
                .filter(s -> !s.content().actualStartAt().isAfter(t)
                        && s.content().expectedEndAt().isAfter(t))
                .map(s -> new StayMealView(
                        s.id(),
                        s.residentId(),
                        s.content().familyMemberId(),
                        s.content().actualStartAt(),
                        s.content().expectedEndAt(),
                        true))
                .toList();
    }

    static StayMealChanged changed(SimpleRecord<FamilyStay> stay) {
        FamilyStay content = stay.content();
        return new StayMealChanged(
                stay.id(),
                stay.residentId(),
                content.familyMemberId(),
                stay.status(),
                content.mealRegistered(),
                content.from(),
                content.to(),
                stay.id() + ":" + stay.status() + ":" + stay.version());
    }
}

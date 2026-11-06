package com.eldercare.backend.nutrition.internal.review.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.care.api.events.ProlongedPoorEating;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.incident.api.events.WeightLossDetected;
import com.eldercare.backend.nutrition.api.events.ReviewDietNotice;
import com.eldercare.backend.nutrition.api.events.ReviewDietNotice.Source;
import com.eldercare.backend.nutrition.internal.assignment.domain.DietAssignment;
import com.eldercare.backend.nutrition.internal.assignment.persistence.DietAssignmentStore;
import com.eldercare.backend.nutrition.internal.catalog.persistence.NutritionCatalogStore;
import com.eldercare.backend.nutrition.internal.review.domain.ReviewNotice;
import com.eldercare.backend.nutrition.internal.review.persistence.ReviewNoticeStore;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Báo Bác sĩ xem lại chế độ ăn (mục I, FR-058, FR-059, BR-M08-05, Q-307; research M-11): ăn kém kéo dài của 005, sụt cân của
 * 007 mỗi sự kiện ghi một mốc kỹ thuật và báo Bác sĩ mức Nhẹ kèm nguồn và chế độ ăn hiện hành. Hệ thống không tạo yêu cầu,
 * không đặt hạn, không nhắc lại; Bác sĩ giữ nguyên (không cần lệnh) hoặc lập bản gán mới có thể chọn tham chiếu mốc này.
 */
@Service
public class ReviewNoticeService {

    private static final String ENTITY = "M08.REVIEW_NOTICE";
    /** Như bản gán (FR-063): bếp có quyền xem danh mục chế độ ăn nhưng không xem thông báo này. */
    private static final Set<Role> VIEWERS = Set.of(Role.DOCTOR, Role.NURSING_STAFF, Role.NURSING_HOME_MANAGER);

    private final ReviewNoticeStore store;
    private final DietAssignmentStore assignments;
    private final NutritionCatalogStore catalog;
    private final CurrentUser user;
    private final ApplicationEventPublisher events;

    public ReviewNoticeService(
            ReviewNoticeStore store,
            DietAssignmentStore assignments,
            NutritionCatalogStore catalog,
            CurrentUser user,
            ApplicationEventPublisher events) {
        this.store = store;
        this.assignments = assignments;
        this.catalog = catalog;
        this.user = user;
        this.events = events;
    }

    /** Ăn kém kéo dài (BR-M04-09): nguồn là bản ghi đầu của chuỗi bữa kém; một thông báo mỗi sự kiện. */
    @Transactional
    public void onProlongedPoorEating(ProlongedPoorEating event) {
        notice(
                event.residentId(),
                Source.PROLONGED_POOR_EATING,
                event.context().sourceId(),
                event.eventKey(),
                event.at());
    }

    /** Sụt cân (BR-M05-04): nguồn là lần đo mới nhất; một thông báo mỗi sự kiện. */
    @Transactional
    public void onWeightLossDetected(WeightLossDetected event) {
        notice(event.residentId(), Source.WEIGHT_LOSS, event.latestRecordId(), event.eventKey(), event.occurredAt());
    }

    @RequiresPermission(function = Function.DIET, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<ReviewNotice> list(Target target, Long residentId) {
        if (user.roles().stream().noneMatch(VIEWERS::contains)) {
            throw new PermissionDeniedException(
                    "PERMISSION_DENIED",
                    "Không có quyền xem thông báo xem lại chế độ ăn",
                    Map.of("failedLayer", "ROLE"));
        }
        return store.list(residentId);
    }

    public static Target target(Long residentId) {
        return new Target(ENTITY, "0", residentId, null);
    }

    /** Bản gán chỉ tham chiếu thông báo đã gửi về chính người đó (FR-059). */
    @Transactional(readOnly = true)
    public void requireOfResident(long noticeId, long residentId) {
        if (!store.belongsTo(noticeId, residentId)) {
            throw new InvalidValueException("Thông báo xem lại không có hoặc không thuộc người cao tuổi này");
        }
    }

    private void notice(long residentId, Source source, long sourceId, String eventKey, Instant at) {
        Optional<DietAssignment> active = assignments.active(residentId);
        Optional<Long> id = store.add(
                residentId,
                source,
                sourceId,
                eventKey,
                active.map(DietAssignment::id).orElse(null),
                at);
        id.ifPresent(noticeId ->
                events.publishEvent(new ReviewDietNotice(noticeId, residentId, source, dietText(active), at)));
    }

    private String dietText(Optional<DietAssignment> active) {
        if (active.isPresent()) {
            return catalog.diet(active.get().dietId()).name();
        }
        return catalog.diets().stream()
                .filter(d -> d.active() && d.attributes().isDefault())
                .findFirst()
                .map(d -> "mặc định của viện (" + d.name() + ")")
                .orElse("chưa có chế độ ăn");
    }
}

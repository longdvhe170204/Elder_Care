package com.eldercare.backend.nutrition.internal.familyfood.service;

import com.eldercare.backend.nutrition.api.events.DietAssignmentTookEffect;
import com.eldercare.backend.nutrition.internal.conflict.service.M08EventListener;
import com.eldercare.backend.nutrition.internal.familyfood.service.FamilyFoodService.Cause;
import com.eldercare.backend.resident.api.events.AllergyRecorded;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Đối chiếu lại đồ ăn gia đình Được sử dụng khi có dị ứng mới hoặc bản gán chế độ ăn mới có hiệu lực (FR-056). Sau commit của
 * lệnh nguồn, mỗi sự kiện một transaction mới, idempotent theo khóa sự kiện ({@link M08EventListener#once}).
 */
@Component
class FamilyFoodListener {

    private final M08EventListener listener;
    private final FamilyFoodService service;

    FamilyFoodListener(M08EventListener listener, FamilyFoodService service) {
        this.listener = listener;
        this.service = service;
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(AllergyRecorded event) {
        listener.once(
                "M08.FAMILY_FOOD.ALLERGY:" + event.itemId(), () -> service.recheck(event.residentId(), Cause.ALLERGY));
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(DietAssignmentTookEffect event) {
        listener.once(
                "M08.FAMILY_FOOD.DIET:" + event.assignmentId(), () -> service.recheck(event.residentId(), Cause.DIET));
    }
}

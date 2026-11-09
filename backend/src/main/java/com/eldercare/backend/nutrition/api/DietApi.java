package com.eldercare.backend.nutrition.api;

import com.eldercare.backend.nutrition.api.dto.CurrentDietView;
import com.eldercare.backend.nutrition.api.dto.DietAssignmentView;
import java.util.List;
import java.util.Optional;

/** Chế độ ăn của người cao tuổi cho 006, 007, 012, 016 (module-interfaces mục 1.2). */
public interface DietApi {

    /** Chế độ ăn hiện hành; mặc định của viện (kèm notAssigned) nếu chưa gán. */
    Optional<CurrentDietView> current(long residentId);

    /** Lịch sử bản gán; 012 hiển thị khi có quyền xem sức khỏe. */
    List<DietAssignmentView> history(long residentId);
}

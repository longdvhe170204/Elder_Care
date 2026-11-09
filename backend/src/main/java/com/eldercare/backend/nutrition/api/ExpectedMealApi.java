package com.eldercare.backend.nutrition.api;

import com.eldercare.backend.nutrition.api.dto.ExpectedPortionView;
import java.time.LocalDate;
import java.util.List;

/** Suất ăn dự kiến cho kênh người cao tuổi (012; FR-063, research M-16, module-interfaces mục 1.3a). */
public interface ExpectedMealApi {

    /** Món dự kiến theo ngày, bữa; tuần chưa có thực đơn công bố thì rỗng. Không chốt, không ghi gì. */
    List<ExpectedPortionView> ofResident(long residentId, LocalDate from, LocalDate to);
}

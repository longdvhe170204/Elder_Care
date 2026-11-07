package com.eldercare.backend.nutrition.api.spi;

import com.eldercare.backend.nutrition.api.dto.StayMealView;
import java.time.Instant;
import java.util.List;

/** 012 cài; chỉ đọc. Thiếu bean thì 011 không tính người thân, màn tính sẵn ghi "chưa có dữ liệu ở lại". */
public interface StayMealPort {
    /** Lượt ở lại đang diễn ra tại {@code t} có đăng ký ăn (FR-034 c). */
    List<StayMealView> staysWithMealRegistration(Instant t);
}

package com.cambae.backend.m07.internal.service;

import com.cambae.backend.m07.internal.persistence.M07JobRunStore;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Điều phối công việc theo phút và hằng ngày của Module M07 (research D-19).
 * Các User Story (US2, US4, US6, v.v.) sẽ đăng ký nghiệp vụ chi tiết vào đây.
 */
@Service
public class M07JobService {

    private final M07JobRunStore jobRunStore;

    public M07JobService(M07JobRunStore jobRunStore) {
        this.jobRunStore = jobRunStore;
    }

    /**
     * Chạy các tác vụ theo phút của M07:
     * (1) Chuyển trạng thái đơn thuốc tới giờ bắt đầu / ngừng;
     * (2) Vật hóa liều đầu cửa sổ;
     * (3) Chuyển ĐẾN_GIỜ -> TRỄ;
     * (4) Chuyển TRỄ -> BỎ_LỠ;
     * (5) Hết hạn đối chiếu;
     * (6) Cảnh báo đầu ca.
     */
    @Transactional
    public void runMinuteTasks(Instant now) {
        // Khung điều phối: được gọi định kỳ mỗi phút
    }

    /**
     * Chạy các tác vụ hằng ngày của M07:
     * (1) Chuyển đơn hết hạn ngày kết thúc -> HẾT_HẠN;
     * (2) Nhắc hết hạn đơn thuốc;
     * (3) Lô thuốc gửi qua hạn dùng -> CHỈ_GIỮ_HỘ.
     */
    @Transactional
    public void runDailyTasks(LocalDate today, Instant now) {
        String checkpoint = today.toString();
        if (jobRunStore.isCompleted("DAILY_CHECK", checkpoint)) {
            return;
        }

        // Thực thi các tác vụ hằng ngày của M07
        jobRunStore.recordCompletion("DAILY_CHECK", checkpoint, "COMPLETED", null, now);
    }
}

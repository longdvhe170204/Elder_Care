package com.eldercare.backend.admission.internal.transition.service;

import com.eldercare.backend.admission.internal.persistence.M02RunStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khóa theo người cho mọi lệnh 004 (research T-03): khóa dòng {@code resident_module_state} của M02 bằng {@code FOR UPDATE}, tạo
 * dòng ở lần khóa đầu. Thứ tự khóa toàn hệ thống: {@code resident} (001, chỉ khi lệnh đi qua 001), rồi dòng này, rồi dòng nghiệp vụ
 * của 004 (hợp đồng, hồ sơ chờ, lượt vắng, hồ sơ kết thúc), rồi vị trí và giường (003). Lệnh dẫn tới Hủy tiếp nhận đi qua 001 nên
 * không gọi hàm này trước.
 */
@Component
public class ResidencyLock {

    private final M02RunStore store;

    ResidencyLock(M02RunStore store) {
        this.store = store;
    }

    /** Chạy trong transaction của lệnh; khóa giữ tới khi transaction kết thúc. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(long residentId) {
        store.lockResident(residentId);
    }
}

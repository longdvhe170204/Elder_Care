package com.eldercare.backend.notification.internal.job.service;

/**
 * Một việc của job 20 giây ({@code ViecNhanhM13}; research B-10): tạo, chuyển lượt nhắc gọi, đóng khung gộp, giải
 * phóng giờ yên tĩnh, gửi lại. Mỗi việc tự khóa dòng và dựa vào {@code UNIQUE} nên chạy lại không gửi trùng (NFR-04).
 */
public interface M13FastTask {

    String name();

    /** Chạy một lượt, trả số bản ghi đã xử lý. */
    int run();
}

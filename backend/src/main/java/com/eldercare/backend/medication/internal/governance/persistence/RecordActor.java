package com.eldercare.backend.medication.internal.governance.persistence;

import com.eldercare.backend.administration.account.api.CurrentUser;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Người tạo dòng {@code simple_record} của M07 (006 T069): tài khoản đang đăng nhập, {@code 0} khi lệnh chạy không có
 * người dùng (job, đồng bộ hệ thống).
 */
@Component
public class RecordActor {

    private final ObjectProvider<CurrentUser> user;

    public RecordActor(ObjectProvider<CurrentUser> user) {
        this.user = user;
    }

    public long accountId() {
        CurrentUser current = user.getIfAvailable();
        if (current == null) {
            return 0L;
        }
        try {
            return current.accountId();
        } catch (RuntimeException noUser) {
            return 0L;
        }
    }
}

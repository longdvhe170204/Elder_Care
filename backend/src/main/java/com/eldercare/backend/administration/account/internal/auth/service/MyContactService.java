package com.eldercare.backend.administration.account.internal.auth.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.spi.PasswordResetSmsPort;
import com.eldercare.backend.administration.account.api.spi.StaffProfileApi;
import com.eldercare.backend.administration.account.internal.persistence.AccountStore;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhân viên tự sửa số điện thoại, email trên hồ sơ của mình ({@code PUT /me/contact}; chủ dự án duyệt 2026-11-09). Số
 * điện thoại dùng nhận mã tự đặt lại mật khẩu (BR-M15-25) nên bắt buộc mật khẩu hiện tại, và đổi số thì báo tới số cũ
 * qua kênh tin nhắn (chưa cấu hình thì không gửi, vẫn lưu). Định dạng như import hồ sơ (BR-M09-18): số di động 0 và 9
 * chữ số, email có @ và tên miền. 008 ghi hồ sơ và nhật ký.
 */
@Service
public class MyContactService {

    private static final DateTimeFormatter AT =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final AccountStore store;
    private final PasswordEncoder passwords;
    private final StaffProfileApi staffProfiles;
    private final ObjectProvider<PasswordResetSmsPort> sms;
    private final BusinessTime time;

    MyContactService(
            AccountStore store,
            PasswordEncoder passwords,
            StaffProfileApi staffProfiles,
            ObjectProvider<PasswordResetSmsPort> sms,
            BusinessTime time) {
        this.store = store;
        this.passwords = passwords;
        this.staffProfiles = staffProfiles;
        this.sms = sms;
        this.time = time;
    }

    @Transactional
    public void update(CurrentUser user, String currentPassword, String phoneNumber, String email) {
        long staffId = user.staffId()
                .filter(id -> user.type() == AccountType.STAFF)
                .orElseThrow(() -> new PermissionDeniedException("Chỉ tài khoản nhân viên sửa được liên hệ ở đây"));
        long accountId = user.accountId();
        store.lock(accountId).orElseThrow();
        if (currentPassword == null || !passwords.matches(currentPassword, store.passwordHash(accountId))) {
            throw new InvalidValueException("Mật khẩu hiện tại không đúng");
        }
        String phone = blankToNull(phoneNumber);
        if (phone != null) phone = phone.replaceAll("[\\s.\\-]", "");
        String mail = blankToNull(email);
        List<UnmetCondition> unmet = new ArrayList<>();
        if (phone != null && !phone.matches("0[0-9]{9}")) {
            unmet.add(new UnmetCondition("phoneNumber", "Số điện thoại cần 10 chữ số, bắt đầu bằng 0"));
        }
        if (mail != null && !mail.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            unmet.add(new UnmetCondition("email", "Email chưa đúng định dạng, ví dụ ten@vien.vn"));
        }
        if (!unmet.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "CONTACT_INVALID",
                    "Liên hệ chưa đúng định dạng",
                    Map.of("unmetConditions", unmet));
        }

        String oldPhone = staffProfiles.phoneNumber(staffId).orElse(null);
        staffProfiles.updateContact(staffId, phone, mail);
        if (oldPhone != null && !Objects.equals(digits(oldPhone), phone)) notifyOldNumber(oldPhone);
    }

    private void notifyOldNumber(String oldPhone) {
        PasswordResetSmsPort port = sms.getIfAvailable();
        if (port == null || !port.available()) return;
        port.send(
                oldPhone,
                "Elder_Care: so dien thoai cua tai khoan vua duoc doi luc " + AT.format(time.now())
                        + ". Neu khong phai ban, bao ngay Quan ly vien.");
    }

    private static String digits(String phone) {
        return phone.replaceAll("[\\s.\\-]", "");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}

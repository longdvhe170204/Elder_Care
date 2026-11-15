package com.eldercare.backend.administration.account.internal.auth.persistence;

import com.eldercare.backend.administration.account.internal.auth.domain.PasswordResetCode;
import com.eldercare.backend.administration.account.internal.auth.domain.PasswordResetCodeContent;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Mã đặt lại mật khẩu ở {@code simple_record} loại {@code M15.PASSWORD_RESET_CODE} (002 T105; FR-052a). Chỉ lưu băm; mỗi
 * mã dùng hoặc hủy một lần. Nơi gọi đã khóa dòng tài khoản nên các lệnh của một tài khoản chạy lần lượt.
 */
@Component
public class PasswordResetCodes {

    private final SimpleRecordApi records;
    private final BusinessTime time;

    PasswordResetCodes(SimpleRecordApi records, BusinessTime time) {
        this.records = records;
        this.time = time;
    }

    /** Số mã đã gửi từ {@code since} (đầu ngày) và lần gửi gần nhất, cho CFG-M15-14. */
    public int sentSince(long accountId, Instant since) {
        return (int) codesOf(accountId).stream()
                .filter(r -> !r.createdAt().isBefore(since))
                .count();
    }

    public Optional<Instant> lastSentAt(long accountId) {
        return codesOf(accountId).stream().map(SimpleRecord::createdAt).max(Comparator.naturalOrder());
    }

    /** Mã mới thay mọi mã chưa dùng của tài khoản. */
    public void insert(long accountId, byte[] codeHash, Instant expiresAt) {
        for (SimpleRecord<PasswordResetCodeContent> r : codesOf(accountId)) {
            if (PasswordResetCodeContent.ACTIVE.equals(r.status())) {
                change(r, PasswordResetCodeContent.REVOKED, r.content());
            }
        }
        records.create(
                PasswordResetCodeContent.KIND,
                new NewSimpleRecord<>(
                        null,
                        account(accountId),
                        PasswordResetCodeContent.ACTIVE,
                        new PasswordResetCodeContent(HexFormat.of().formatHex(codeHash), expiresAt, 0),
                        null,
                        null,
                        null,
                        accountId,
                        false));
    }

    /** Mã mới nhất chưa dùng, chưa hủy, chưa hết hạn; khóa dòng tới hết transaction. */
    public Optional<PasswordResetCode> lockActive(long accountId) {
        Instant now = time.now();
        return codesOf(accountId).stream()
                .filter(r -> PasswordResetCodeContent.ACTIVE.equals(r.status())
                        && r.content().expiresAt().isAfter(now))
                .max(Comparator.comparing((SimpleRecord<PasswordResetCodeContent> r) -> r.createdAt())
                        .thenComparing(SimpleRecord::id))
                .map(r -> records.lockForUpdate(PasswordResetCodeContent.KIND, r.id()))
                .map(r -> new PasswordResetCode(
                        r.id(),
                        HexFormat.of().parseHex(r.content().codeHash()),
                        r.content().failedAttempts()));
    }

    /** Thêm một lần sai; {@code revoke} thì hủy mã. */
    public void recordFailure(long codeId, boolean revoke) {
        SimpleRecord<PasswordResetCodeContent> r = records.lockForUpdate(PasswordResetCodeContent.KIND, codeId);
        change(
                r,
                revoke ? PasswordResetCodeContent.REVOKED : r.status(),
                r.content().failedAgain());
    }

    public void markUsed(long codeId) {
        SimpleRecord<PasswordResetCodeContent> r = records.lockForUpdate(PasswordResetCodeContent.KIND, codeId);
        change(r, PasswordResetCodeContent.USED, r.content());
    }

    private List<SimpleRecord<PasswordResetCodeContent>> codesOf(long accountId) {
        return records.listByParent(PasswordResetCodeContent.KIND, account(accountId));
    }

    private void change(SimpleRecord<PasswordResetCodeContent> r, String status, PasswordResetCodeContent content) {
        records.change(
                PasswordResetCodeContent.KIND,
                r.id(),
                r.version(),
                new RecordChange<>(status, content, null, null, false));
    }

    private static EntityRef account(long accountId) {
        return new EntityRef(PasswordResetCodeContent.ENTITY, accountId);
    }
}

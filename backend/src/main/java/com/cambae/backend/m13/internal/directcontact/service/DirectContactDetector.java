package com.cambae.backend.m13.internal.directcontact.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m13.api.dto.RecipientType;
import com.cambae.backend.m13.api.spi.FamilyContactView;
import com.cambae.backend.m13.api.spi.FamilyNotificationPort;
import com.cambae.backend.m13.internal.directcontact.persistence.DirectContactStore;
import com.cambae.backend.m13.internal.directcontact.persistence.DirectContactStore.Unreached;
import com.cambae.backend.m13.internal.job.service.M13FastTask;
import com.cambae.backend.m13.internal.recipient.service.RecipientPhones;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Đưa người thân không tới được vào danh sách "cần liên hệ trực tiếp" (việc 20 giây {@code DIRECT_CONTACTS}; FR-021,
 * Q-112): thông báo Trung bình, hoặc người thân gắn bản ghi nguồn, đã gửi đi mà không kênh nào tới được và không còn
 * lần gửi chờ hay chờ gửi lại. Mức Nhẹ (trừ nhóm (d)) chỉ ghi Gửi thất bại; Khẩn cấp theo lượt nhắc gọi (US1).
 */
@Service
public class DirectContactDetector implements M13FastTask {

    /**
     * Chỉ quét thông báo của bấy nhiêu ngày gần nhất (thông số kỹ thuật giới hạn lượt quét, không phải mốc nghiệp vụ):
     * việc chạy mỗi 20 giây nên thông báo cũ hơn đã được xét ở các lượt trước.
     */
    static final int LOOKBACK_DAYS = 2;

    private static final int BATCH = 200;

    private final DirectContactStore contacts;
    private final RecipientPhones phones;
    private final ObjectProvider<FamilyNotificationPort> family;
    private final ParameterApi parameters;
    private final BusinessTime time;

    DirectContactDetector(
            DirectContactStore contacts,
            RecipientPhones phones,
            ObjectProvider<FamilyNotificationPort> family,
            ParameterApi parameters,
            BusinessTime time) {
        this.contacts = contacts;
        this.phones = phones;
        this.family = family;
        this.parameters = parameters;
        this.time = time;
    }

    @Override
    public String name() {
        return "DIRECT_CONTACTS";
    }

    @Override
    public int run() {
        Instant now = time.now();
        int maxResends = parameters.read(ParameterCode.CFG_M13_05).asInt();
        List<Unreached> found = contacts.unreached(now.minus(LOOKBACK_DAYS, ChronoUnit.DAYS), maxResends, BATCH);
        FamilyNotificationPort port = family.getIfAvailable();
        int added = 0;
        for (Unreached u : found) {
            Optional<String> name = port == null
                    ? Optional.empty()
                    : port.contact(u.familyMemberId()).map(FamilyContactView::fullName);
            Optional<String> phone = phones.of(RecipientType.FAMILY_MEMBER, u.familyMemberId(), u.residentId());
            if (contacts.insert(
                    u.notificationId(),
                    u.familyMemberId(),
                    name.orElse(null),
                    phone.orElse(null),
                    u.shortContent(),
                    now)) {
                added++;
            }
        }
        return added;
    }
}

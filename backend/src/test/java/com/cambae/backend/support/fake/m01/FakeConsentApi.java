package com.cambae.backend.support.fake.m01;

import com.cambae.backend.m01.api.ConsentApi;
import com.cambae.backend.m01.api.dto.ConsentView;
import com.cambae.backend.m01.api.dto.ConsenterType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Bản giả {@link ConsentApi} (001 cài) cho module chỉ đọc bản đồng ý (009, 012): test đặt bản đồng ý Hiệu lực bằng
 * {@link #active}, rút bằng {@link #withdraw}, không cần hồ sơ người cao tuổi thật. Không nằm trong {@link FakeM01Config}
 * vì {@code @Primary} thay bản thật cho cả context; test cần thì {@code @Import(FakeConsentApi.Config.class)}. Điều kiện
 * chú thích "1" của 002 vẫn theo bản thật (đọc thẳng bảng của 001).
 */
public class FakeConsentApi implements ConsentApi {

    private final Map<Long, ConsentView> active = new HashMap<>();
    private long nextId = 1;

    /** Bản đồng ý Hiệu lực do người cao tuổi tự ký: người thân được xem sức khỏe, mục "nhận thông báo". */
    public FakeConsentApi active(long residentId, Set<Long> healthViewers, boolean notificationConsent) {
        active.put(
                residentId,
                new ConsentView(
                        nextId++,
                        residentId,
                        ConsenterType.RESIDENT,
                        Optional.empty(),
                        Set.copyOf(healthViewers),
                        false,
                        notificationConsent));
        return this;
    }

    public FakeConsentApi withdraw(long residentId) {
        active.remove(residentId);
        return this;
    }

    public void reset() {
        active.clear();
    }

    @Override
    public Optional<ConsentView> current(long residentId) {
        return Optional.ofNullable(active.get(residentId));
    }

    @Override
    public boolean includes(long residentId, long familyMemberId) {
        return current(residentId)
                .map(c -> c.familyMemberIdsAllowedHealthView().contains(familyMemberId))
                .orElse(false);
    }

    @Override
    public Optional<Long> legalRepresentative(long residentId) {
        return current(residentId)
                .filter(c -> c.consenterType() == ConsenterType.LEGAL_REPRESENTATIVE)
                .flatMap(ConsentView::consenterFamilyMemberId);
    }

    @TestConfiguration
    public static class Config {

        @Bean
        @Primary
        FakeConsentApi fakeConsentApi() {
            return new FakeConsentApi();
        }
    }
}

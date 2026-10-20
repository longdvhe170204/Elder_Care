package com.cambae.backend.support.fake.m15;

import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.FootnoteCondition;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Bản giả {@link FootnoteCondition} mã "1" (001 cài; BR-M01-08): bản đồng ý chia sẻ dữ liệu đang hiệu lực bao gồm người
 * thân với người cao tuổi. Không nằm trong {@link FakeM15Config} vì đăng ký điều kiện đổi kết quả lớp vai trò của mọi ô
 * có ¹; test cần thì {@code @Import(FakeConsents.Config.class)}.
 */
public class FakeConsents implements FootnoteCondition {

    private final Set<List<Long>> included = new HashSet<>();

    /** Bản đồng ý của người cao tuổi bao gồm người thân. */
    public FakeConsents include(long familyMemberId, long residentId) {
        included.add(List.of(familyMemberId, residentId));
        return this;
    }

    /** Rút lại đồng ý (hoặc bỏ người thân khỏi bản đồng ý). */
    public FakeConsents withdraw(long familyMemberId, long residentId) {
        included.remove(List.of(familyMemberId, residentId));
        return this;
    }

    @Override
    public String footnoteCode() {
        return "1";
    }

    @Override
    public boolean isSatisfied(CurrentUser user, Target target, Instant at) {
        return user.familyMemberId().isPresent()
                && target.residentId() != null
                && included.contains(List.of(user.familyMemberId().get(), target.residentId()));
    }

    @TestConfiguration
    public static class Config {

        @Bean
        FakeConsents fakeConsents() {
            return new FakeConsents();
        }
    }
}

package com.eldercare.backend.family.internal.relationship.config;

import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipApi;
import com.eldercare.backend.administration.account.api.spi.FootnoteCondition;
import com.eldercare.backend.family.internal.relationship.service.RepresentativeFootnoteCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Hai điều kiện chú thích "chỉ người đại diện" của Phụ lục 27 (² yêu cầu, xác nhận; ⁷ xem nội dung). */
@Configuration
class RepresentativeFootnoteConfig {

    @Bean
    FootnoteCondition representativeFootnote2(FamilyRelationshipApi relationships) {
        return new RepresentativeFootnoteCondition("2", relationships);
    }

    @Bean
    FootnoteCondition representativeFootnote7(FamilyRelationshipApi relationships) {
        return new RepresentativeFootnoteCondition("7", relationships);
    }
}

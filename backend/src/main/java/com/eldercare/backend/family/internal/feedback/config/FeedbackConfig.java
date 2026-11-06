package com.eldercare.backend.family.internal.feedback.config;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.common.file.FilePermissionProvider;
import com.eldercare.backend.family.internal.feedback.domain.Feedback;
import com.eldercare.backend.family.internal.feedback.persistence.FeedbackStore;
import com.eldercare.backend.family.internal.feedback.service.FeedbackService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provider của phản hồi (012 T008, T030): phạm vi theo người cao tuổi ({@code GanPhamViProvider} {@code M10.PHAN_HOI});
 * tệp đính kèm tải được khi xem được phản hồi. Mục nội dung gắn {@code M10.FEEDBACK} nên loại bỏ cùng chuỗi.
 */
@Configuration
class FeedbackConfig {

    @Bean
    ScopeAssignmentProvider feedbackScope(FeedbackStore store) {
        return new ScopeAssignmentProvider() {
            @Override
            public String entityType() {
                return Feedback.ENTITY_TYPE;
            }

            @Override
            public Target assignScope(long id) {
                return new Target(
                        Feedback.ENTITY_TYPE,
                        String.valueOf(id),
                        store.require(id).residentId(),
                        null);
            }
        };
    }

    @Bean
    FilePermissionProvider feedbackFiles(FeedbackService feedback) {
        return new FilePermissionProvider() {
            @Override
            public String ownerType() {
                return Feedback.ENTITY_TYPE;
            }

            @Override
            public boolean canDownload(long ownerId) {
                return feedback.canView(ownerId);
            }
        };
    }
}

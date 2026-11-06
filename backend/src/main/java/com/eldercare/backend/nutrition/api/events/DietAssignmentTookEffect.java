package com.eldercare.backend.nutrition.api.events;

import com.eldercare.backend.nutrition.api.dto.Texture;
import java.time.Instant;

/** Bản gán chế độ ăn có hiệu lực (FR-011): 005 nhắc xem kế hoạch ăn, 012 hiển thị. Phát sau commit qua outbox. */
public record DietAssignmentTookEffect(long residentId, long assignmentId, long dietId, Texture texture, Instant at) {}

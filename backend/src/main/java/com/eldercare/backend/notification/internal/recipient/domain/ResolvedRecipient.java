package com.eldercare.backend.notification.internal.recipient.domain;

import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.Q354Case;
import com.eldercare.backend.notification.api.dto.RecipientType;
import java.util.List;
import java.util.Set;

/**
 * Một người nhận đã xác định ({@code NguoiNhanDaXacDinh}; research B-04): đã gộp mọi căn cứ (FR-012).
 *
 * @param accountId tài khoản nhận trong ứng dụng hoặc cổng; {@code null} khi người thân chưa có tài khoản cổng
 * @param identityOnly nhận thay không có phạm vi với người cao tuổi: chỉ thông tin nhận dạng (FR-011)
 * @param q354Case mã trường hợp khi gửi người thân ngoài phạm vi bản đồng ý (FR-014a)
 */
public record ResolvedRecipient(
        RecipientType type,
        long id,
        Long accountId,
        List<String> basis,
        Set<InformationType> visibleParts,
        boolean identityOnly,
        Q354Case q354Case) {}

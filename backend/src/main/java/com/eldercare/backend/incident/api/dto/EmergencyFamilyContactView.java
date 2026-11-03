package com.eldercare.backend.incident.api.dto;

import java.util.Optional;

/** Liên hệ người thân phục vụ thẻ khẩn cấp và thứ tự gọi xác nhận nguyện vọng (007 mục 2.1). */
public record EmergencyFamilyContactView(long familyMemberId, String fullName, Optional<String> phoneNumber) {}

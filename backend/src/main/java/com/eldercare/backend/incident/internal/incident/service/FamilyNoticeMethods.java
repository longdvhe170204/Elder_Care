package com.eldercare.backend.incident.internal.incident.service;

import java.util.Set;

/** Cách báo người nhà được ghi nhận (BF-06): gọi điện, gặp trực tiếp, nhắn tin. */
final class FamilyNoticeMethods {
    static final Set<String> VALID = Set.of("PHONE", "IN_PERSON", "MESSAGE");

    private FamilyNoticeMethods() {}
}

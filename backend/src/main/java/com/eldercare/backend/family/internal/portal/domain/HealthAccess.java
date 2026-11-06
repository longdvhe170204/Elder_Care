package com.eldercare.backend.family.internal.portal.domain;

/**
 * Quyết định mở phần sức khỏe trên cổng ({@code QuyenCoTacDung} cho xem sức khỏe; FR-011, DBR-03): quyền đang bật và bản
 * đồng ý Hiệu lực bao gồm người thân tại thời điểm xem.
 */
public final class HealthAccess {

    public enum Decision {
        ALLOWED,
        /** Quyền xem sức khỏe không bật. */
        NO_HEALTH_VIEW_RIGHT,
        /** Quyền bật nhưng bản đồng ý không bao gồm người thân (hoặc không có bản Hiệu lực). */
        RIGHT_NOT_EFFECTIVE
    }

    private HealthAccess() {}

    public static Decision decide(boolean rightEnabled, boolean coveredByConsent) {
        if (!rightEnabled) {
            return Decision.NO_HEALTH_VIEW_RIGHT;
        }
        return coveredByConsent ? Decision.ALLOWED : Decision.RIGHT_NOT_EFFECTIVE;
    }
}

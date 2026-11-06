package com.eldercare.backend.family.web.request;

import com.eldercare.backend.common.Reason;

/** Thân lệnh trên ngoại lệ đón: lý do (duyệt, từ chối, hủy ngoại lệ Hiệu lực). */
public record PickupExceptionCommandRequest(Reason reason) {

    public static final PickupExceptionCommandRequest EMPTY = new PickupExceptionCommandRequest(null);
}

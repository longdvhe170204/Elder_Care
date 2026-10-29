package com.eldercare.backend.staffing.duty.internal.service;

import com.eldercare.backend.staffing.shift.internal.service.port.ShiftCommandPortFixture;

/** Bên ngoài đổi/xoay ca không được gọi port lệnh nội bộ. */
public class ForbiddenPortCallerFixture {
    public void apply(ShiftCommandPortFixture port) {
        port.createShift();
    }
}

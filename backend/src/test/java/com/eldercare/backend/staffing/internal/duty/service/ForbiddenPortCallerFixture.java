package com.eldercare.backend.staffing.internal.duty.service;

import com.eldercare.backend.staffing.internal.shift.service.ShiftCommandPortFixture;

/** Bên ngoài đổi/xoay ca không được gọi port lệnh nội bộ. */
public class ForbiddenPortCallerFixture {
    public void apply(ShiftCommandPortFixture port) {
        port.createShift();
    }
}

package com.cambae.backend.m09.duty.internal.service;

import com.cambae.backend.m09.shift.internal.service.port.ShiftCommandPortFixture;

/** Bên ngoài đổi/xoay ca không được gọi port lệnh nội bộ. */
public class ForbiddenPortCallerFixture {
    public void apply(ShiftCommandPortFixture port) {
        port.createShift();
    }
}

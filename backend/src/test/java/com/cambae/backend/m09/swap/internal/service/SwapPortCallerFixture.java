package com.cambae.backend.m09.swap.internal.service;

import com.cambae.backend.m09.shift.internal.service.port.ShiftCommandPortFixture;

/** Bên đổi ca được gọi port lệnh nội bộ. */
public class SwapPortCallerFixture {
    public void apply(ShiftCommandPortFixture port) {
        port.createShift();
    }
}

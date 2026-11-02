package com.eldercare.backend.staffing.swap.internal.service;

import com.eldercare.backend.staffing.shift.internal.service.port.ShiftCommandPortFixture;

/** Bên đổi ca được gọi port lệnh nội bộ. */
public class SwapPortCallerFixture {
    public void apply(ShiftCommandPortFixture port) {
        port.createShift();
    }
}

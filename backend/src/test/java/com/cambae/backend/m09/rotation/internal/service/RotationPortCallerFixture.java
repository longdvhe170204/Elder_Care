package com.cambae.backend.m09.rotation.internal.service;

import com.cambae.backend.m09.shift.internal.service.port.ShiftCommandPortFixture;

/** Bên xoay ca được gọi port lệnh nội bộ. */
public class RotationPortCallerFixture {
    public void apply(ShiftCommandPortFixture port) {
        port.createSchedule();
    }
}

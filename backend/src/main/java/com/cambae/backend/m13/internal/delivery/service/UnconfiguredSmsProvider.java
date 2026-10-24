package com.cambae.backend.m13.internal.delivery.service;

import com.cambae.backend.m13.api.spi.SmsProvider;
import com.cambae.backend.m13.api.spi.SmsResult;

/** Nhà cung cấp tin nhắn chưa cấu hình ({@code ChuaCauHinh}; Q-06): mọi tin ghi Kênh chưa cấu hình. */
public class UnconfiguredSmsProvider implements SmsProvider {

    @Override
    public SmsResult send(String phoneNumber, String content) {
        return SmsResult.of(SmsResult.Outcome.NOT_CONFIGURED);
    }
}

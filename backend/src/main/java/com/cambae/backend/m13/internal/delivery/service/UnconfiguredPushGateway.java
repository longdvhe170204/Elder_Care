package com.cambae.backend.m13.internal.delivery.service;

/** Chưa cấu hình khóa dịch vụ FCM: không đẩy; thông báo vẫn vào hộp thông báo (research B-05). */
public class UnconfiguredPushGateway implements PushGateway {

    @Override
    public Outcome push(String token, Message message) {
        return Outcome.NOT_CONFIGURED;
    }
}

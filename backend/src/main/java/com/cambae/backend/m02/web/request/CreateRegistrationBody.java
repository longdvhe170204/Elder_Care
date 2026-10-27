package com.cambae.backend.m02.web.request;

/** Thân lệnh lập khai báo (contracts/api-018.yaml): {@code renewal} mặc định {@code false}. */
public record CreateRegistrationBody(Boolean renewal) {

    public boolean isRenewal() {
        return Boolean.TRUE.equals(renewal);
    }
}

package com.eldercare.backend.administration.account.internal.auth.service;

import java.security.SecureRandom;

/**
 * Mật khẩu tạm khi tạo tài khoản, cấp lại mật khẩu, khởi tạo Quản lý viện đầu tiên: 12 ký tự ngẫu nhiên, có cả chữ và
 * số (CFG-M15-09: tối thiểu 8 ký tự, có chữ và số; FR-006a), bỏ ký tự dễ nhầm (0, O, 1, l, I). Chỉ trả một lần, không
 * lưu.
 */
public final class TemporaryPasswords {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private TemporaryPasswords() {}

    public static String generate() {
        while (true) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 12; i++) {
                sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
            }
            String password = sb.toString();
            if (password.chars().anyMatch(Character::isDigit)
                    && password.chars().anyMatch(Character::isLetter)) {
                return password;
            }
        }
    }
}

package com.cambae.backend.m15.account.api.spi;

import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.dto.Target;
import java.time.Instant;

/**
 * SPI: điều kiện của một chú thích Phụ lục 27, do module sở hữu điều kiện cài (002 contracts/module-interfaces.md mục
 * 1.8). Chú thích chưa có bản cài thì 002 từ chối với lớp {@code ROLE}.
 */
public interface FootnoteCondition {

    /** Mã chú thích, ví dụ {@code "1"}, {@code "10"}. */
    String footnoteCode();

    boolean isSatisfied(CurrentUser user, Target target, Instant at);
}

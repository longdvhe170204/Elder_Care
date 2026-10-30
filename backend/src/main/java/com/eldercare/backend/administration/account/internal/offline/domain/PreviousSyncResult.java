package com.eldercare.backend.administration.account.internal.offline.domain;

import java.util.Optional;

/** Khóa lặp lại của một bản ghi đã gặp trước đó: băm yêu cầu và kết quả (rỗng khi lần đầu còn đang chạy). */
public record PreviousSyncResult(String requestHash, Optional<String> resultJson) {}

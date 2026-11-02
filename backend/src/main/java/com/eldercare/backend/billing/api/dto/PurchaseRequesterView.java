package com.eldercare.backend.billing.api.dto;

import java.time.*;
import java.util.*;

/** Kiểu đọc liên module của 010; không phải bản cài nghiệp vụ hoặc bộ tạo phản hồi HTTP. */
public record PurchaseRequesterView(String type, long id) {}

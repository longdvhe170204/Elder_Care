package com.eldercare.backend.medication.internal.governance.persistence;

import java.util.List;

/** Một loại dữ liệu thuốc lưu giữ: bảng, điều kiện chọn dòng của loại (rỗng là cả bảng) và các bản ghi trỏ tới. */
public record RetentionSpec(String dataType, String table, String where, List<RetentionChild> children) {}

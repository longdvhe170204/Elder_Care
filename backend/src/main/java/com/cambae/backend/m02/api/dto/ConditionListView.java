package com.cambae.backend.m02.api.dto;

import java.util.List;

/** Danh sách điều kiện kèm cảnh báo không chặn (mã cảnh báo theo glossary mục 10.4). */
public record ConditionListView(List<ConditionView> conditions, List<String> warnings) {}

package com.eldercare.backend.care.internal.task.domain;

/** Bộ lọc của lịch sử công việc đã lập (màn Lập công việc ca); trường null là không lọc. */
public record CareTaskHistoryFilter(Long floor, Long resident, Long taskType, Long performer) {}

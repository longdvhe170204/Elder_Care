package com.eldercare.backend.care.web.request;

/** 005 FR-056: tên mục và giờ HH:mm của mốc sinh hoạt cố định. */
public record DailyRoutineWrite(String name, String time) {}

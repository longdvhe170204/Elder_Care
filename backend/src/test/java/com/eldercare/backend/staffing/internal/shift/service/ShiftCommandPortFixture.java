package com.eldercare.backend.staffing.internal.shift.service;

/** Chỉ dùng để kiểm chứng luật kiến trúc; không phải port nghiệp vụ của 008. */
public interface ShiftCommandPortFixture {
    void createSchedule();

    void createShift();

    void createAssignment();

    void createHandover();

    void addHandoverItem();

    void closeShift();
}

package com.eldercare.backend.common.scheduling;

/**
 * Job định kỳ chạy ngay được theo yêu cầu kiểm thử ({@code POST /testing/clock} với {@code runJobs}, profile test, demo;
 * NFR-13). {@link #name()} trùng giá trị enum {@code runJobs} của contracts/api-000.yaml.
 */
public interface ScheduledJob {

    String name();

    void runNow();
}

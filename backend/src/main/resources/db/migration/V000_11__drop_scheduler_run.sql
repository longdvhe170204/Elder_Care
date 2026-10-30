-- 000 T108 phần 2 (research R-15 mục 4): lần chạy job ghi ra logs/jobs.log (SchedulerRuns, mã lần chạy UUID trong MDC
-- jobRunId và basis.schedulerRunId của nhật ký); bỏ bảng scheduler_run.
DROP TABLE scheduler_run;

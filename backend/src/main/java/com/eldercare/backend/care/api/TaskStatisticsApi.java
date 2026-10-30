package com.eldercare.backend.care.api;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.care.api.dto.*;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import org.springframework.transaction.annotation.*;

/** T003: contracts 005 mục 1.2–1.10a; hành vi do story tương ứng cài. */
public interface TaskStatisticsApi {
    Stream<TaskReportView> inRange(ScopeFilter scope, Instant from, Instant to);

    Stream<AttendanceView> attendanceInRange(ScopeFilter scope, LocalDate from, LocalDate to);

    Stream<CareResultView> resultsInRange(long residentId, Set<ResultType> types, Instant from, Instant to);

    Set<Long> resultRecorders(long residentId, Instant from, Instant to);
}

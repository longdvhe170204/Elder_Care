package com.cambae.backend.m04.api;

import com.cambae.backend.m04.api.dto.*;
import java.time.*;
import java.util.*;
import org.springframework.transaction.annotation.*;

/** T003: contracts 005 mục 1.2–1.10a; hành vi do story tương ứng cài. */
public interface CarePlanApi {
    Optional<CarePlanView> effectiveOn(long residentId, LocalDate date);

    Optional<Integer> waterTargetMl(long residentId, LocalDate date);

    boolean hasEffectivePlan(long residentId);

    Set<Long> currentTaskTypes(long residentId);
}

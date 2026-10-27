package com.cambae.backend.m04.api;

import com.cambae.backend.m04.api.dto.*;
import java.time.*;
import java.util.*;
import org.springframework.transaction.annotation.*;

/** T003: contracts 005 mục 1.2–1.10a; hành vi do story tương ứng cài. */
public interface PublicCareResultApi {
    List<FamilyCareResultView> results(long residentId, Instant from, Instant to);

    EatingDrinkingSummary summary(long residentId, LocalDate from, LocalDate to);
}

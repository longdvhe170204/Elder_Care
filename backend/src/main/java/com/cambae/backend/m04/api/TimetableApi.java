package com.cambae.backend.m04.api;

import com.cambae.backend.m04.api.dto.*;
import com.cambae.backend.m04.api.spi.TimetableSourcePort.TimetableItemView;
import java.time.*;
import java.util.*;
import org.springframework.transaction.annotation.*;

/** T003: contracts 005 mục 1.2–1.10a; hành vi do story tương ứng cài. */
public interface TimetableApi {
    List<TimetableItemView> onDate(long residentId, LocalDate date);
}

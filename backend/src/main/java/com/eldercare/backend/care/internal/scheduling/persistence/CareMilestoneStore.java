package com.eldercare.backend.care.internal.scheduling.persistence;

import com.eldercare.backend.common.data.ReminderMarkers;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class CareMilestoneStore {
    private final ReminderMarkers markers;

    public CareMilestoneStore(ReminderMarkers markers) {
        this.markers = markers;
    }

    public boolean claim(String job, LocalDate date) {
        try {
            String marker = HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256")
                            .digest((job + ":" + date).getBytes(StandardCharsets.UTF_8)));
            return markers.mark(
                    "M04.DAILY_WORK", "M04.DAILY_WORK", 0, marker, Map.of("job", job, "milestone", date.toString()));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("JDK thiếu SHA-256", error);
        }
    }
}

package com.cambae.backend.m07.internal.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Pessimistic lock per resident on table {@code resident_medication} per research D-03.
 * Global locking order: ... -> resident_care (005) -> resident_medication (006) -> prescription -> medication_dose -> ...
 */
@Component
public class ResidentMedicationLock {

    private final JdbcTemplate jdbc;

    public ResidentMedicationLock(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Ensure record exists and lock row until the end of the current transaction.
     *
     * @param residentId resident logical ID
     */
    public void lock(long residentId) {
        jdbc.update(
                "INSERT INTO resident_medication (resident_id, version) VALUES (?, 0) "
                        + "ON DUPLICATE KEY UPDATE resident_id = resident_id",
                residentId);
        jdbc.queryForList(
                "SELECT resident_id FROM resident_medication WHERE resident_id = ? FOR UPDATE", Long.class, residentId);
    }
}

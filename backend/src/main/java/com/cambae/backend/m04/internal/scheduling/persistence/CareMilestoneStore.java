package com.cambae.backend.m04.internal.scheduling.persistence;

import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CareMilestoneStore {
    private final JdbcTemplate jdbc;

    public CareMilestoneStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean claim(String job, LocalDate date) {
        return jdbc.update("INSERT IGNORE INTO m04_job_milestone(job,milestone) VALUES (?,?)", job, date.toString())
                == 1;
    }
}

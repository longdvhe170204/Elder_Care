package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.support.TestUsers;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** T064: đo API riêng, không coi là SC-005 cần60người dùng và thao tác Hành chính thật. */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
class T064_BedListPerformanceIT extends StructureTestSupport {
    @Autowired
    MockMvc mvc;

    @Test
    void SC_005_serverBedListFor300BedsReturnsWithinOneSecondAfterWarmup() throws Exception {
        List<Object[]> rooms = new ArrayList<>();
        List<Object[]> levels = new ArrayList<>();
        List<Object[]> beds = new ArrayList<>();
        for (int r = 0; r < 100; r++) {
            long roomId = r == 0 ? room : TestUsers.nextId();
            if (r > 0) {
                rooms.add(new Object[] {roomId, floor, type, "P" + roomId, "Phòng kiểm thử " + r});
                levels.add(new Object[] {roomId});
            }
            for (int b = 0; b < 3; b++)
                if (r != 0 || b != 0) beds.add(new Object[] {TestUsers.nextId(), roomId, "G" + (b + 1)});
        }
        jdbc.batchUpdate("INSERT INTO room(id,floor_id,room_type_id,code,name) VALUES(?,?,?,?,?)", rooms);
        jdbc.batchUpdate("INSERT INTO room_allowed_care_level VALUES(?,'BASIC')", levels);
        jdbc.batchUpdate("INSERT INTO bed(id,room_id,code) VALUES(?,?,?)", beds);
        mvc.perform(get("/api/v1/beds").param("floorId", Long.toString(floor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.beds.length()").value(300));
        long[] samples = new long[10];
        for (int n = 0; n < samples.length; n++) {
            long started = System.nanoTime();
            mvc.perform(get("/api/v1/beds").param("floorId", Long.toString(floor)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.beds.length()").value(300));
            samples[n] = (System.nanoTime() - started) / 1_000_000;
        }
        Arrays.sort(samples);
        System.out.println("T064_BED_LIST_300 samples=" + Arrays.toString(samples) + " medianMs="
                + ((samples[4] + samples[5]) / 2.0) + " maxMs=" + samples[9]);
        assertThat(samples[9]).as("10lần đọc API300giường sau warmup,ms").isLessThan(1000);
    }
}

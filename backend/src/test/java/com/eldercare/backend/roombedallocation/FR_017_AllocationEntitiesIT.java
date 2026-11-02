package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.support.TestUsers;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** T024: mô hình JPA phải đọc đúng dữ liệu và khóa tổng hợp do migration tạo. */
class FR_017_AllocationEntitiesIT extends StructureTestSupport {
    @Autowired
    EntityManager entities;

    @Test
    void FR_017_allocationPositionAndReminderAreMappedToTheirPersistentData() {
        assertThat(entities.getMetamodel().getEntities())
                .extracting(EntityType::getName)
                .contains("BedAllocation", "ResidentPosition", "AllocationReminder");
        long resident = TestUsers.nextId();
        long allocation = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,?,'FUTURE','2026-11-01','BASIC','CONTRACT')",
                allocation,
                resident,
                bed);
        jdbc.update(
                "INSERT INTO resident_position(resident_id,without_bed_since,version) VALUES(?,'2026-10-23',3)",
                resident);
        jdbc.update(
                "INSERT INTO allocation_reminder(target_type,target_id,flag_type,started_at,reminder_count) VALUES('RESIDENT',?,'WITHOUT_BED','2026-10-23',2)",
                resident);
        assertThat(entities.createQuery(
                                "SELECT a.residentId,a.bedId,a.status,a.checkedCareLevel,a.careLevelSource FROM BedAllocation a WHERE a.id=:id",
                                Object[].class)
                        .setParameter("id", allocation)
                        .getSingleResult())
                .extracting(Object::toString)
                .containsExactly(Long.toString(resident), Long.toString(bed), "FUTURE", "BASIC", "CONTRACT");
        assertThat(entities.createQuery(
                                "SELECT p.version FROM ResidentPosition p WHERE p.residentId=:id", Integer.class)
                        .setParameter("id", resident)
                        .getSingleResult())
                .isEqualTo(3);
        assertThat(entities.createQuery(
                                "SELECT r.reminderCount FROM AllocationReminder r WHERE r.id.targetType='RESIDENT' AND r.id.targetId=:id AND r.id.flagType='WITHOUT_BED'",
                                Integer.class)
                        .setParameter("id", resident)
                        .getSingleResult())
                .isEqualTo(2);
    }
}

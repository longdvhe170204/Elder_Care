package com.eldercare.backend.roombedallocation.internal.daycare.domain;

import com.eldercare.backend.common.data.CatalogBase;
import com.eldercare.backend.common.data.DataGroup;
import jakarta.persistence.*;
import java.time.LocalTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Danh mục khung giờ; mọi lần sửa được đếm lại trên lịch đến hiện có. */
@Entity
@Table(name = "day_care_session")
@DataGroup(DataGroup.Group.CATALOG)
public class DayCareSessionEntry extends CatalogBase {
    @Column(name = "start_time")
    @JdbcTypeCode(SqlTypes.LOCAL_TIME)
    private LocalTime startTime;

    @Column(name = "end_time")
    @JdbcTypeCode(SqlTypes.LOCAL_TIME)
    private LocalTime endTime;

    protected DayCareSessionEntry() {}

    public DayCareSessionEntry(String name, LocalTime start, LocalTime end) {
        super(name);
        revise(name, start, end);
    }

    public void revise(String name, LocalTime start, LocalTime end) {
        rename(name);
        startTime = start;
        endTime = end;
    }

    public LocalTime startTime() {
        return startTime;
    }

    public LocalTime endTime() {
        return endTime;
    }
}

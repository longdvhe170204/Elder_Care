package com.eldercare.backend.roombedallocation.internal.bed.domain;

import com.eldercare.backend.common.data.DataGroup;
import com.eldercare.backend.roombedallocation.api.dto.BedStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** T014/FR-011: thuộc tính giường sửa qua lệnh; trạng thái chỉ do BedStateService ghi. */
@Entity
@Table(name = "bed")
@DataGroup(DataGroup.Group.BUSINESS)
public class Bed {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id")
    private long roomId;

    private String code;

    @Enumerated(EnumType.STRING)
    @Column(insertable = false, updatable = false)
    private BedStatus state = BedStatus.EMPTY;

    @Version
    private int version;

    protected Bed() {}

    public Bed(long roomId, String code) {
        this.roomId = roomId;
        this.code = code;
    }

    public Long id() {
        return id;
    }

    public long roomId() {
        return roomId;
    }

    public String code() {
        return code;
    }

    public BedStatus state() {
        return state;
    }

    public int version() {
        return version;
    }

    public void revise(long roomId, String code) {
        this.roomId = roomId;
        this.code = code;
    }
}

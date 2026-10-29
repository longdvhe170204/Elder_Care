package com.eldercare.backend.roombedallocation.internal.structure.domain;

import com.eldercare.backend.common.data.CatalogBase;
import com.eldercare.backend.common.data.DataGroup;
import com.eldercare.backend.roombedallocation.api.dto.GenderPolicy;
import com.eldercare.backend.roombedallocation.api.spi.CareLevelCode;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/** T014/FR-001–FR-007: dữ liệu danh mục Room; quyền và tham chiếu kiểm ở service. */
@Entity
@Table(name = "room")
@DataGroup(DataGroup.Group.CATALOG)
public class Room extends CatalogBase {
    @Column(name = "floor_id")
    private long floorId;

    @Column(name = "room_type_id")
    private long roomTypeId;

    @Column(name = "code")
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_policy")
    private GenderPolicy genderPolicy;

    @Column(name = "isolated", updatable = false)
    private boolean isolated;

    @ElementCollection
    @CollectionTable(name = "room_allowed_care_level", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "care_level")
    @Enumerated(EnumType.STRING)
    private Set<CareLevelCode> careLevels = new HashSet<>();

    protected Room() {}

    public Room(
            long floorId,
            long roomTypeId,
            String code,
            String name,
            GenderPolicy genderPolicy,
            Set<CareLevelCode> careLevels) {
        super(name);
        revise(floorId, roomTypeId, code, name, genderPolicy, careLevels);
    }

    public long floorId() {
        return floorId;
    }

    public long roomTypeId() {
        return roomTypeId;
    }

    public String code() {
        return code;
    }

    public GenderPolicy genderPolicy() {
        return genderPolicy;
    }

    public boolean isolated() {
        return isolated;
    }

    public Set<CareLevelCode> careLevels() {
        return Set.copyOf(careLevels);
    }

    public void revise(
            long floorId, long roomTypeId, String code, String name, GenderPolicy policy, Set<CareLevelCode> levels) {
        this.floorId = floorId;
        this.roomTypeId = roomTypeId;
        this.code = code;
        this.genderPolicy = policy;
        rename(name);
        careLevels.clear();
        careLevels.addAll(levels);
    }
}

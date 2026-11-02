package com.eldercare.backend.roombedallocation.internal.structure.domain;

import com.eldercare.backend.common.data.CatalogBase;
import com.eldercare.backend.common.data.DataGroup;
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

/** T014/FR-001–FR-007: dữ liệu danh mục RoomType; quyền và tham chiếu kiểm ở service. */
@Entity
@Table(name = "room_type")
@DataGroup(DataGroup.Group.CATALOG)
public class RoomType extends CatalogBase {
    @Column(name = "standard_beds", columnDefinition = "SMALLINT")
    private int standardBeds;

    @Column(name = "serves_long_term")
    private boolean servesLongTerm;

    @Column(name = "serves_short_term")
    private boolean servesShortTerm;

    @ElementCollection
    @CollectionTable(name = "room_type_care_level", joinColumns = @JoinColumn(name = "room_type_id"))
    @Column(name = "care_level")
    @Enumerated(EnumType.STRING)
    private Set<CareLevelCode> careLevels = new HashSet<>();

    protected RoomType() {}

    public RoomType(
            String name,
            int standardBeds,
            Set<CareLevelCode> careLevels,
            boolean servesLongTerm,
            boolean servesShortTerm) {
        super(name);
        revise(name, standardBeds, careLevels, servesLongTerm, servesShortTerm);
    }

    public int standardBeds() {
        return standardBeds;
    }

    public boolean servesLongTerm() {
        return servesLongTerm;
    }

    public boolean servesShortTerm() {
        return servesShortTerm;
    }

    public Set<CareLevelCode> careLevels() {
        return Set.copyOf(careLevels);
    }

    public void revise(String name, int standardBeds, Set<CareLevelCode> levels, boolean longTerm, boolean shortTerm) {
        if (standardBeds < 1 || (!longTerm && !shortTerm))
            throw new IllegalArgumentException("Loại phòng không hợp lệ");
        rename(name);
        this.standardBeds = standardBeds;
        this.servesLongTerm = longTerm;
        this.servesShortTerm = shortTerm;
        this.careLevels.clear();
        this.careLevels.addAll(levels);
    }
}

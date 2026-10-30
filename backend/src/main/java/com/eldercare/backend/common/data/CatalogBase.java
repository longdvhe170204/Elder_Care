package com.eldercare.backend.common.data;

import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.UnmetCondition;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.util.List;
import java.util.Optional;

/**
 * Lớp cơ sở của mục danh mục, nhóm 1 (data-model mục 1.1; FR-002, FR-003, BR-M15-06). Bảng có cột {@code name},
 * {@code status}, {@code version} và cột sinh {@code active_name} có UNIQUE. Lệnh xóa thuộc module: xóa mục đã được
 * tham chiếu bị khóa ngoại RESTRICT chặn và trả 409 {@code CATALOG_ITEM_IN_USE}. Module ghi nhật ký mỗi lần thay đổi
 * (FR-043).
 */
@MappedSuperclass
public abstract class CatalogBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CatalogStatus status = CatalogStatus.ACTIVE;

    @Version
    private int version;

    protected CatalogBase() {}

    protected CatalogBase(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CatalogStatus getStatus() {
        return status;
    }

    public int getVersion() {
        return version;
    }

    /** Nghiệp vụ mới chỉ được chọn mục đang Hiệu lực (FR-003). */
    public boolean isActive() {
        return status == CatalogStatus.ACTIVE;
    }

    public void rename(String name) {
        this.name = name;
    }

    /** Ngừng hiệu lực: không chọn được cho nghiệp vụ mới, vẫn hiển thị trong bản ghi cũ (FR-003). */
    public void deactivate() {
        if (!isActive()) {
            throw condition("FR-003", "Mục đã ngừng hiệu lực");
        }
        status = CatalogStatus.INACTIVE;
    }

    /**
     * Kích hoạt lại; bị từ chối khi trong cùng danh mục đã có mục khác đang Hiệu lực trùng tên (FR-003, BR-M15-06).
     *
     * @param activeWithSameName mục đang Hiệu lực cùng tên, module tra trước khi gọi
     */
    public void reactivate(Optional<? extends CatalogBase> activeWithSameName) {
        if (isActive()) {
            throw condition("FR-003", "Mục đang hiệu lực");
        }
        activeWithSameName.ifPresent(other -> {
            throw condition("BR-M15-06", "Trùng tên với mục đang Hiệu lực #" + other.getId() + ": " + other.getName());
        });
        status = CatalogStatus.ACTIVE;
    }

    private static ConditionNotMetException condition(String code, String description) {
        return new ConditionNotMetException(List.of(new UnmetCondition(code, description)));
    }
}

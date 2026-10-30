package com.eldercare.backend.administration.shared.api;

import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.administration.shared.api.dto.Change;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.util.List;
import java.util.Optional;

/**
 * Danh mục dùng chung {@code catalog_item} (nhóm 1; FR-002, FR-003, BR-M15-06; module-interfaces 000 mục 1.8). Không ghi
 * nhật ký: module gọi ghi bằng giá trị trước, sau trả về. Lệnh ghi chạy trong transaction của lệnh gọi.
 */
public interface CatalogApi {

    /** 409 khi trùng tên đang Hiệu lực hoặc trùng mã trong cùng loại và cùng cha. */
    <T> CatalogItem<T> create(SharedKind<T> kind, NewCatalogItem<T> item);

    /** 404 khi không có hoặc thuộc loại khác. */
    <T> CatalogItem<T> require(SharedKind<T> kind, long id);

    /** Như {@link #require}, thêm 409 khi mục Ngừng hiệu lực (chọn cho nghiệp vụ mới, FR-003). */
    <T> CatalogItem<T> requireActive(SharedKind<T> kind, long id);

    <T> Optional<CatalogItem<T>> findByCode(SharedKind<T> kind, Long parentId, String code);

    /** Theo {@code sort_order}, tên; {@code parentId} {@code null} là mọi mục của loại. */
    <T> List<CatalogItem<T>> list(SharedKind<T> kind, Long parentId, boolean activeOnly);

    /** 412 khi {@code version} khác phiên bản hiện hành (FR-021). */
    <T> Change<CatalogItem<T>> update(
            SharedKind<T> kind, long id, int version, String name, int sortOrder, T attributes);

    <T> Change<CatalogItem<T>> deactivate(SharedKind<T> kind, long id, int version);

    /** 409 nêu mục trùng tên đang Hiệu lực (FR-003). */
    <T> Change<CatalogItem<T>> reactivate(SharedKind<T> kind, long id, int version);

    /** Xác nhận chuyên môn của mục (ví dụ món ăn do Bác sĩ xác nhận). */
    <T> Change<CatalogItem<T>> confirm(SharedKind<T> kind, long id, int version, long confirmedBy);

    /** 409 {@code CATALOG_ITEM_IN_USE} khi loại không cho xóa hoặc còn bảng khác tham chiếu (FR-002). */
    <T> CatalogItem<T> delete(SharedKind<T> kind, long id);
}

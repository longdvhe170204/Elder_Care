package com.eldercare.backend.administration.shared;

import static com.eldercare.backend.administration.shared.SharedTestKinds.CATALOG;
import static com.eldercare.backend.administration.shared.SharedTestKinds.FIXED_CATALOG;
import static com.eldercare.backend.administration.shared.SharedTestKinds.OTHER_CATALOG;
import static com.eldercare.backend.administration.shared.SharedTestKinds.unique;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.shared.SharedTestKinds.Attributes;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.administration.shared.api.dto.Change;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.VersionMismatchException;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * Hợp đồng của {@link CatalogApi} (FR-002, FR-003, BR-M15-06; module-interfaces 000 mục 1.8), chạy trên bản thật
 * ({@code CatalogApiIT}) và bản giả ({@code FakeCatalogApiTest}) theo constitution XVI (L).
 */
public interface CatalogApiContract {

    CatalogApi catalog();

    /** Chạy trong transaction (bản thật), như lệnh của module gọi. */
    <R> R tx(Supplier<R> work);

    private CatalogItem<Attributes> create(String name, Long parentId) {
        return tx(() ->
                catalog().create(CATALOG, new NewCatalogItem<>(null, name, parentId, 0, new Attributes("đỏ", 1))));
    }

    @Test
    default void create_thenRequire_returnsItemWithAttributes() {
        String code = unique("C");
        CatalogItem<Attributes> created = tx(() -> catalog()
                .create(CATALOG, new NewCatalogItem<>(code, unique("Mục"), null, 3, new Attributes("xanh", 2))));

        CatalogItem<Attributes> read = tx(() -> catalog().require(CATALOG, created.id()));

        assertThat(read).isEqualTo(created);
        assertThat(read.status()).isEqualTo(CatalogStatus.ACTIVE);
        assertThat(read.attributes()).isEqualTo(new Attributes("xanh", 2));
        assertThat(read.sortOrder()).isEqualTo(3);
        assertThat(tx(() -> catalog().findByCode(CATALOG, null, code))).contains(created);
    }

    @Test
    default void BR_M15_06_activeNameIsUniquePerKindAndParent() {
        String name = unique("Trùng");
        CatalogItem<Attributes> first = create(name, null);

        assertThatThrownBy(() -> create(name, null)).isInstanceOf(ConditionNotMetException.class);
        // Cùng tên ở loại khác, hoặc dưới cha khác, được phép
        tx(() -> catalog().create(OTHER_CATALOG, new NewCatalogItem<>(null, name, null, 0, new Attributes("đỏ", 1))));
        create(name, first.id());
    }

    @Test
    default void codeIsUniquePerKindAndParent() {
        String code = unique("C");
        tx(() -> catalog().create(CATALOG, new NewCatalogItem<>(code, unique("A"), null, 0, new Attributes("đỏ", 1))));

        assertThatThrownBy(() -> tx(() -> catalog()
                        .create(CATALOG, new NewCatalogItem<>(code, unique("B"), null, 0, new Attributes("đỏ", 1)))))
                .isInstanceOf(ConditionNotMetException.class);
    }

    @Test
    default void require_otherKindOrMissing_isNotFound() {
        CatalogItem<Attributes> item = create(unique("Mục"), null);

        assertThatThrownBy(() -> tx(() -> catalog().require(OTHER_CATALOG, item.id())))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> tx(() -> catalog().require(CATALOG, Long.MAX_VALUE)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    default void invalidAttributes_areRejected() {
        assertThatThrownBy(() -> tx(() -> catalog()
                        .create(CATALOG, new NewCatalogItem<>(null, unique("Mục"), null, 0, new Attributes(" ", 1)))))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    default void FR_003_deactivated_isNotSelectable_reactivateChecksDuplicateName() {
        String name = unique("Ngừng");
        CatalogItem<Attributes> item = create(name, null);

        Change<CatalogItem<Attributes>> off = tx(() -> catalog().deactivate(CATALOG, item.id(), item.version()));

        assertThat(off.before().status()).isEqualTo(CatalogStatus.ACTIVE);
        assertThat(off.after().status()).isEqualTo(CatalogStatus.INACTIVE);
        assertThatThrownBy(() -> tx(() -> catalog().requireActive(CATALOG, item.id())))
                .isInstanceOf(ConditionNotMetException.class);
        assertThat(tx(() -> catalog().list(CATALOG, null, true)))
                .extracting(CatalogItem::id)
                .doesNotContain(item.id());
        assertThat(tx(() -> catalog().list(CATALOG, null, false)))
                .extracting(CatalogItem::id)
                .contains(item.id());

        CatalogItem<Attributes> sameName = create(name, null);
        assertThatThrownBy(() -> tx(() ->
                        catalog().reactivate(CATALOG, item.id(), off.after().version())))
                .isInstanceOf(ConditionNotMetException.class);

        tx(() -> catalog().deactivate(CATALOG, sameName.id(), sameName.version()));
        Change<CatalogItem<Attributes>> on =
                tx(() -> catalog().reactivate(CATALOG, item.id(), off.after().version()));
        assertThat(on.after().status()).isEqualTo(CatalogStatus.ACTIVE);
    }

    @Test
    default void FR_021_update_withStaleVersion_isRejected() {
        CatalogItem<Attributes> item = create(unique("Sửa"), null);
        String newName = unique("Đã sửa");

        Change<CatalogItem<Attributes>> change =
                tx(() -> catalog().update(CATALOG, item.id(), item.version(), newName, 5, new Attributes("vàng", 9)));

        assertThat(change.before()).isEqualTo(item);
        assertThat(change.after().name()).isEqualTo(newName);
        assertThat(change.after().attributes()).isEqualTo(new Attributes("vàng", 9));
        assertThat(change.after().version()).isGreaterThan(item.version());
        assertThatThrownBy(() -> tx(
                        () -> catalog().update(CATALOG, item.id(), item.version(), unique("X"), 0, item.attributes())))
                .isInstanceOf(VersionMismatchException.class);
    }

    @Test
    default void confirm_recordsWhoConfirmed() {
        CatalogItem<Attributes> item = create(unique("Món"), null);

        Change<CatalogItem<Attributes>> change = tx(() -> catalog().confirm(CATALOG, item.id(), item.version(), 42L));

        assertThat(change.after().confirmedBy()).isEqualTo(42L);
        assertThat(change.after().confirmedAt()).isNotNull();
    }

    @Test
    default void FR_002_delete_unused_removesItem() {
        CatalogItem<Attributes> item = create(unique("Xóa"), null);

        assertThat(tx(() -> catalog().delete(CATALOG, item.id()))).isEqualTo(item);
        assertThatThrownBy(() -> tx(() -> catalog().require(CATALOG, item.id())))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    default void FR_002_delete_referencedOrNotDeletableKind_isInUse() {
        CatalogItem<Attributes> parent = create(unique("Cha"), null);
        create(unique("Con"), parent.id());
        CatalogItem<Void> fixed =
                tx(() -> catalog().create(FIXED_CATALOG, new NewCatalogItem<>(null, unique("Cố định"), null, 0, null)));

        assertThatThrownBy(() -> tx(() -> catalog().delete(CATALOG, parent.id())))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo("CATALOG_ITEM_IN_USE");
        assertThatThrownBy(() -> tx(() -> catalog().delete(FIXED_CATALOG, fixed.id())))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo("CATALOG_ITEM_IN_USE");
    }
}

package com.eldercare.backend.roombedallocation;

import static com.eldercare.backend.roombedallocation.api.dto.AssetStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import com.eldercare.backend.roombedallocation.internal.asset.domain.BedAssetRules;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedAssetCommand;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** 019 T019 (FR-015, Q-242, Q-245, Q-256): ánh xạ trạng thái tài sản giường sang lệnh giường và người nhận Khẩn cấp. */
class BedAssetRulesTest {

    @Test
    void FR_015_assetTransitionsMapToBedCommands() {
        assertThat(BedAssetRules.bedCommand(READY, DAMAGED)).contains(BedAssetCommand.MAINTENANCE);
        assertThat(BedAssetRules.bedCommand(READY, UNDER_MAINTENANCE)).contains(BedAssetCommand.MAINTENANCE);
        assertThat(BedAssetRules.bedCommand(DAMAGED, UNDER_MAINTENANCE))
                .as("giường đã Đang bảo trì từ lúc báo hỏng")
                .isEmpty();
        assertThat(BedAssetRules.bedCommand(UNDER_MAINTENANCE, DAMAGED)).isEmpty();
        assertThat(BedAssetRules.bedCommand(UNDER_MAINTENANCE, READY)).contains(BedAssetCommand.READY);
        assertThat(BedAssetRules.bedCommand(READY, DISPOSED)).contains(BedAssetCommand.RETIREMENT);
        assertThat(BedAssetRules.bedCommand(DAMAGED, DISPOSED)).contains(BedAssetCommand.RETIREMENT);
        assertThat(BedAssetRules.bedCommand(UNDER_MAINTENANCE, DISPOSED)).contains(BedAssetCommand.RETIREMENT);
        assertThat(BedAssetRules.bedCommand(READY, IN_USE)).as("Q-242").isEqualTo(Optional.empty());
    }

    @Test
    void Q_245_unsafeDamageGoesToFloorManagerAndShiftLeadWhenTheFloorHasASuitableBed() {
        var same = new LocationView(1L, "Khu A", 7L, "Tầng 2", 70L, "Phòng 203", 701L, "G2");
        var other = new LocationView(1L, "Khu A", 8L, "Tầng 3", 80L, "Phòng 301", 801L, "G1");
        assertThat(BedAssetRules.unsafeRecipients(7L, List.of(same, other)))
                .containsExactly(
                        new RecipientGroup.FloorManager(7L, new Substitution.Default()),
                        new RecipientGroup.ShiftLead(7L, new Substitution.Default()));
    }

    @Test
    void Q_256_noSuitableBedOnTheFloorAddsManagerAndOfficeStaffWithOtherFloorBeds() {
        var other = new LocationView(1L, "Khu A", 8L, "Tầng 3", 80L, "Phòng 301", 801L, "G1");
        assertThat(BedAssetRules.unsafeRecipients(7L, List.of(other)))
                .containsExactly(
                        new RecipientGroup.FloorManager(7L, new Substitution.Default()),
                        new RecipientGroup.ShiftLead(7L, new Substitution.Default()),
                        new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER),
                        new RecipientGroup.FacilityRole(Role.OFFICE_STAFF));
        assertThat(BedAssetRules.otherFloorBeds(7L, List.of(other))).contains("Tầng 3", "Phòng 301", "G1");
        assertThat(BedAssetRules.otherFloorBeds(7L, List.of())).isEqualTo("không còn giường phù hợp trong viện");
    }
}

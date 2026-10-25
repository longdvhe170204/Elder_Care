package com.cambae.backend.m03.internal.bed.domain;

import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.m03.api.dto.LocationView;
import com.cambae.backend.m15.auditlog.api.Basis;
import java.util.List;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** T004: 003 cài, 019 gọi; mọi hàm nằm trong transaction của lệnh tài sản, không đọc bảng trực tiếp. */
public interface BedAssetCommandPort {
    @Transactional(propagation = Propagation.MANDATORY)
    List<UnmetCondition> checkAssetCommand(
            long bedId, BedAssetCommand command, boolean damageWhilePreparing, boolean repeatedDamage);

    @Transactional(propagation = Propagation.MANDATORY)
    void applyAssetCommand(long bedId, BedAssetCommand command, Basis basis);

    @Transactional(propagation = Propagation.MANDATORY)
    void residentTransferFlagAdded(long bedId, List<Long> affectedResidentIds);

    @Transactional(propagation = Propagation.MANDATORY)
    void residentTransferFlagRemoved(long bedId);

    @Transactional(propagation = Propagation.MANDATORY)
    List<LocationView> suitableEmptyBedsForOccupant(long bedId);
}

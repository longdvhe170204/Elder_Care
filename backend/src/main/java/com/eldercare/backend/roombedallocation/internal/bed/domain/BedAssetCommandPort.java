package com.eldercare.backend.roombedallocation.internal.bed.domain;

import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
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

    /** Q-243: giường đã từng có phân bổ thì không đổi phòng; tài sản giường đi theo phòng của giường. */
    @Transactional(propagation = Propagation.MANDATORY)
    boolean everAllocated(long bedId);

    @Transactional(propagation = Propagation.MANDATORY)
    void residentTransferFlagAdded(long bedId);

    @Transactional(propagation = Propagation.MANDATORY)
    void residentTransferFlagRemoved(long bedId);

    @Transactional(propagation = Propagation.MANDATORY)
    List<LocationView> suitableEmptyBedsForOccupant(long bedId);

    /** 019 FR-015: trạng thái hiện tại của giường cho nhắc CFG-M03-02 (chủ dự án duyệt 2026-11-06). */
    @Transactional(propagation = Propagation.MANDATORY)
    com.eldercare.backend.roombedallocation.api.dto.BedStatus bedState(long bedId);
}

package com.eldercare.backend.administration.auditlog.api.spi;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import java.util.Map;

/**
 * Phân loại trường của một loại đối tượng theo nhóm thông tin, để che trường khi xem nhật ký (contracts/module-interfaces.md
 * mục 1.3; FR-049, BR-M15-11). Module sở hữu loại đối tượng cài. Trường không khai báo là {@link InfoGroup#INTERNAL_PUBLIC};
 * loại đối tượng không có provider thì chỉ Quản lý viện xem được lịch sử.
 */
public interface FieldClassificationProvider {

    String entityType();

    Map<String, InfoGroup> fieldGroups();
}

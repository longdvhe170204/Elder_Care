package com.eldercare.backend.notification.internal.inbox.service;

import com.eldercare.backend.notification.api.spi.SourceRecordAccessPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * {@code sourceRecord.canOpen} của hộp thông báo (FR-033, FR-034): hỏi bản cài {@link SourceRecordAccessPort} của đúng loại
 * nguồn (module-interfaces mục 2.1). Không có bản cài, hoặc bản cài ném lỗi, thì không mở được.
 */
@Component
public class SourceRecordAccess {

    private final ObjectProvider<SourceRecordAccessPort> ports;

    SourceRecordAccess(ObjectProvider<SourceRecordAccessPort> ports) {
        this.ports = ports;
    }

    public boolean canOpen(String sourceRecordType, String sourceRecordId) {
        if (sourceRecordType == null || sourceRecordId == null) {
            return false;
        }
        for (SourceRecordAccessPort port : ports.orderedStream().toList()) {
            if (sourceRecordType.equals(port.sourceRecordType())) {
                try {
                    return port.canOpen(sourceRecordId);
                } catch (RuntimeException e) {
                    return false;
                }
            }
        }
        return false;
    }
}

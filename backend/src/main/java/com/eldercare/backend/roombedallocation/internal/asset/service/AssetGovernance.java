package com.eldercare.backend.roombedallocation.internal.asset.service;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.InvalidPermissionDataException;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.shared.api.SharedRowRetention;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetKinds;
import com.eldercare.backend.roombedallocation.internal.asset.persistence.AssetStore;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 019 T004 phần {@code M03.ASSET} (module-interfaces mục 3; W-08, W-09): phạm vi là tầng của vị trí hiện hành; lưu giữ
 * không gắn hồ sơ theo CFG-M15-04 tính từ lúc thanh lý (xóa cùng lịch sử ở bảng chung); nguyên giá, chi phí bảo trì là
 * thông tin tài chính.
 */
@Configuration
public class AssetGovernance {

    @Bean
    ScopeAssignmentProvider assetScope(AssetStore store) {
        return new ScopeAssignmentProvider() {
            @Override
            public String entityType() {
                return AssetKinds.ENTITY;
            }

            @Override
            public Target assignScope(long id) {
                var row = store.find(id)
                        .orElseThrow(() -> new InvalidPermissionDataException(AssetKinds.ENTITY, "MISSING_RECORD", id));
                return new Target(AssetKinds.ENTITY, Long.toString(id), null, row.floorAreaId());
            }
        };
    }

    @Bean
    RetentionProvider assetRetention(AssetStore store, RetentionDatabase database, SharedRowRetention sharedRows) {
        return new RetentionProvider() {
            @Override
            public String dataType() {
                return AssetKinds.ENTITY;
            }

            @Override
            public RetentionKind retentionKind() {
                return new RetentionKind.NotResidentLinked();
            }

            @Override
            public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
                return store.disposedBefore(cutoff, batch).stream()
                        .map(r -> new Candidate(r[0], Instant.ofEpochMilli(r[1])));
            }

            /** Yêu cầu mua sắm (M11) và lịch xe giữ id tài sản; tài sản Đã thanh lý không còn lịch xe mới. */
            @Override
            public Set<DataRef> referencesTo(long id) {
                return Set.of();
            }

            @Override
            public void delete(Collection<Long> ids) {
                sharedRows.deleteFor(ids.stream()
                        .map(id -> new EntityRef(AssetKinds.ENTITY, id))
                        .toList());
                store.delete(database.jdbc(), ids);
            }
        };
    }

    @Bean
    FieldClassificationProvider assetFields() {
        return field(AssetKinds.ENTITY, Map.of("acquisitionCost", InfoGroup.FINANCE));
    }

    @Bean
    FieldClassificationProvider assetMaintenanceFields() {
        return field(AssetKinds.MAINTENANCE.code(), Map.of("cost", InfoGroup.FINANCE));
    }

    private static FieldClassificationProvider field(String type, Map<String, InfoGroup> groups) {
        return new FieldClassificationProvider() {
            @Override
            public String entityType() {
                return type;
            }

            @Override
            public Map<String, InfoGroup> fieldGroups() {
                return groups;
            }
        };
    }
}

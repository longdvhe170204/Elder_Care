package com.eldercare.backend.roombedallocation.internal.asset.persistence;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.roombedallocation.api.dto.AssetFlag;
import com.eldercare.backend.roombedallocation.api.dto.OverdueAssetView;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** T023: đọc dấu hiện tại, lọc hạn trước ngày và tầng; số đếm GROUP BY tại DB, không tải danh sách tài sản. */
@Repository
public class AssetStatisticsStore {
    private final JdbcTemplate jdbc;

    public AssetStatisticsStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<OverdueAssetView> overdue(ScopeFilter scope, LocalDate on) {
        return jdbc.query(query(scope) + " ORDER BY asset_id, flag", this::view, args(scope, on));
    }

    /** Hạn trong [from, to], chưa mang dấu quá hạn của hạn đó, tài sản chưa thanh lý (016 T008). */
    public List<OverdueAssetView> dueSoon(ScopeFilter scope, LocalDate from, LocalDate to) {
        String sql = dueSoon("maintenance_overdue", "maintenance_due_on", AssetFlag.MAINTENANCE_OVERDUE, scope)
                + " UNION ALL "
                + dueSoon(
                        "vehicle_inspection_overdue",
                        "vehicle_inspection_due_on",
                        AssetFlag.VEHICLE_INSPECTION_OVERDUE,
                        scope)
                + " UNION ALL " + dueSoon("insurance_overdue", "insurance_due_on", AssetFlag.INSURANCE_OVERDUE, scope)
                + " ORDER BY asset_id, flag";
        var args = new ArrayList<Object>();
        for (int i = 0; i < 3; i++) {
            args.add(from);
            args.add(to);
            if (!scope.facilityWide()) args.addAll(scope.floorAreaIds());
        }
        return jdbc.query(sql, this::view, args.toArray());
    }

    private OverdueAssetView view(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new OverdueAssetView(
                rs.getLong("asset_id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getLong("department_id"),
                rs.getObject("floor_area_id", Long.class),
                AssetFlag.valueOf(rs.getString("flag")),
                rs.getObject("due_on", LocalDate.class));
    }

    private String dueSoon(String flagColumn, String dueColumn, AssetFlag flag, ScopeFilter scope) {
        return "SELECT id AS asset_id,code,name,department_id,floor_area_id,'" + flag.name() + "' AS flag," + dueColumn
                + " AS due_on FROM asset WHERE status <> 'DISPOSED' AND NOT " + flagColumn + " AND " + dueColumn
                + " BETWEEN ? AND ?" + scopeFilter(scope);
    }

    public Map<AssetFlag, Long> countOverdue(ScopeFilter scope, LocalDate on) {
        var result = new EnumMap<AssetFlag, Long>(AssetFlag.class);
        jdbc.query(
                "SELECT flag,COUNT(*) AS total FROM (" + query(scope) + ") overdue GROUP BY flag",
                rs -> {
                    result.put(AssetFlag.valueOf(rs.getString("flag")), rs.getLong("total"));
                },
                args(scope, on));
        return Map.copyOf(result);
    }

    private String query(ScopeFilter scope) {
        return select("maintenance_overdue", "maintenance_due_on", AssetFlag.MAINTENANCE_OVERDUE, scope)
                + " UNION ALL "
                + select(
                        "vehicle_inspection_overdue",
                        "vehicle_inspection_due_on",
                        AssetFlag.VEHICLE_INSPECTION_OVERDUE,
                        scope)
                + " UNION ALL " + select("insurance_overdue", "insurance_due_on", AssetFlag.INSURANCE_OVERDUE, scope);
    }

    private String select(String flagColumn, String dueColumn, AssetFlag flag, ScopeFilter scope) {
        return "SELECT id AS asset_id,code,name,department_id,floor_area_id,'" + flag.name() + "' AS flag," + dueColumn
                + " AS due_on FROM asset WHERE " + flagColumn + " AND " + dueColumn + " < ?" + scopeFilter(scope);
    }

    private static String scopeFilter(ScopeFilter scope) {
        return scope.facilityWide()
                ? ""
                : scope.floorAreaIds().isEmpty()
                        ? " AND 1=0"
                        : " AND floor_area_id IN ("
                                + String.join(
                                        ",",
                                        Collections.nCopies(scope.floorAreaIds().size(), "?")) + ")";
    }

    private Object[] args(ScopeFilter scope, LocalDate on) {
        var args = new ArrayList<Object>();
        for (int i = 0; i < 3; i++) {
            args.add(on);
            if (!scope.facilityWide()) args.addAll(scope.floorAreaIds());
        }
        return args.toArray();
    }
}

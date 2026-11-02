package com.eldercare.backend.support.demo;

import com.eldercare.backend.BackendApplication;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.approval.ApprovalFixtures;
import com.eldercare.backend.administration.approval.api.spi.RequestTypeHandler;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.administration.sample.SampleService;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chạy tay quickstart 000 mục 3 (K1–K21) ở profile {@code demo} với loại yêu cầu mẫu {@code M15.SAMPLE} và bản ghi mẫu
 * {@code M15.SAMPLE_RECORD}, vốn chỉ có trong mã test (000 T103; chủ dự án chọn chạy bằng test-run 2026-10-31):
 *
 * <pre>./mvnw spring-boot:test-run -Dspring-boot.run.main-class=com.eldercare.backend.support.demo.QuickstartDemoApplication
 *     -Dspring-boot.run.profiles=demo</pre>
 *
 * Người dùng và quyền là bản thật của 002. Lớp test khác không được nạp, trừ dữ liệu mẫu ở {@code administration.sample}
 * và các lớp lồng dưới đây. Đặt ở gói con để {@code @SpringBootTest} không thấy hai {@code @SpringBootConfiguration} khi
 * tìm lên từ gói của test; gói gốc của quét, entity, repository khai rõ.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@AutoConfigurationPackage(basePackages = "com.eldercare.backend")
@ComponentScan(
        basePackages = "com.eldercare.backend",
        excludeFilters =
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = QuickstartDemoApplication.SampleOnly.class))
public class QuickstartDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuickstartDemoApplication.class, args);
    }

    /** Bỏ {@link BackendApplication} (tự quét không lọc) và mọi lớp test trừ dữ liệu mẫu, lớp của chính file này. */
    static class SampleOnly implements TypeFilter {

        @Override
        public boolean match(MetadataReader reader, MetadataReaderFactory factory) throws IOException {
            String name = reader.getClassMetadata().getClassName();
            if (name.equals(BackendApplication.class.getName())) {
                return true;
            }
            if (!reader.getResource().getURI().toString().contains("/test-classes/")
                    || name.startsWith(QuickstartDemoApplication.class.getName())) {
                return false;
            }
            return !name.startsWith("com.eldercare.backend.administration.sample.")
                    || reader.getAnnotationMetadata().hasAnnotation(TestConfiguration.class.getName());
        }
    }

    /** Handler {@code M15.SAMPLE} của test yêu cầu phê duyệt; công tắc đổi qua endpoint dưới đây. */
    @Configuration(proxyBeanMethods = false)
    @Profile("demo")
    static class SampleRequests {

        @Bean
        ApprovalFixtures.Control approvalControl() {
            return new ApprovalFixtures.Control();
        }

        @Bean
        RequestTypeHandler sampleRequestHandler(ApprovalFixtures.Control control, JdbcTemplate jdbc) {
            return new ApprovalFixtures.SampleRequestHandler(control, jdbc);
        }
    }

    /** Công tắc của handler mẫu cho K11, K12, K13, K21; chỉ có khi chạy lớp này, không thuộc hợp đồng. */
    @RestController
    @Profile("demo")
    @RequestMapping("/api/v1/testing/sample-request-control")
    static class SampleRequestControlController {

        private final ApprovalFixtures.Control control;

        SampleRequestControlController(ApprovalFixtures.Control control) {
            this.control = control;
        }

        @PostMapping
        Settings change(@RequestBody Settings settings) {
            control.configure(
                    settings.conditionUnmet(),
                    settings.failOnApply(),
                    settings.allowSelfApproval(),
                    settings.roleRestriction());
            return settings;
        }

        record Settings(
                boolean conditionUnmet, boolean failOnApply, boolean allowSelfApproval, Set<Role> roleRestriction) {}
    }

    /** Hồ sơ mẫu {@code M15.SAMPLE_PROFILE} của K18: địa chỉ là thông tin liên hệ, dị ứng là thông tin sức khỏe. */
    @Bean
    @Profile("demo")
    FieldClassificationProvider sampleProfileFields() {
        return new FieldClassificationProvider() {
            @Override
            public String entityType() {
                return SAMPLE_PROFILE;
            }

            @Override
            public Map<String, InfoGroup> fieldGroups() {
                return Map.of("address", InfoGroup.CONTACT, "allergies", InfoGroup.HEALTH);
            }
        };
    }

    static final String SAMPLE_PROFILE = "M15.SAMPLE_PROFILE";

    /**
     * Dữ liệu mẫu cho K14 → K18, K20 (module thật chưa có màn ghi bản ghi mẫu): ghi bằng người dùng đang đăng nhập; K15 thử
     * sửa, xóa trực tiếp bảng nhóm 3 bằng kết nối {@code eldercare_app} của ứng dụng. Không thuộc hợp đồng.
     */
    @RestController
    @Profile("demo")
    @RequestMapping("/api/v1/testing/sample-data")
    static class SampleDataController {

        private final JdbcTemplate jdbc;
        private final ObjectProvider<CurrentUser> currentUser;
        private final AuditLogApi auditLog;
        private final SampleService samples;

        SampleDataController(
                JdbcTemplate jdbc,
                ObjectProvider<CurrentUser> currentUser,
                AuditLogApi auditLog,
                SampleService samples) {
            this.jdbc = jdbc;
            this.currentUser = currentUser;
            this.auditLog = auditLog;
            this.samples = samples;
        }

        @PostMapping("/floor-records")
        Map<String, Object> floorRecord(@RequestBody Map<String, Object> body) {
            jdbc.update(
                    "INSERT INTO sample_floor_record (occurred_at, actor_id, floor_area_id, note)"
                            + " VALUES (UTC_TIMESTAMP(6), ?, ?, ?)",
                    currentUser.getObject().accountId(),
                    ((Number) body.get("floorAreaId")).longValue(),
                    body.get("note"));
            return Map.of("id", jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
        }

        @PostMapping("/office-records")
        Map<String, Object> officeRecord(@RequestBody Map<String, Object> body) {
            jdbc.update(
                    "INSERT INTO sample_office_record (occurred_at, actor_id, note) VALUES (UTC_TIMESTAMP(6), ?, ?)",
                    currentUser.getObject().accountId(),
                    body.get("note"));
            return Map.of("id", jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
        }

        /** K15: kết quả của {@code UPDATE}, {@code DELETE} trực tiếp, mong đợi lỗi quyền hoặc trigger {@code 45000}. */
        @PostMapping("/floor-records/{id}/direct-change")
        Map<String, Object> directChange(@PathVariable long id) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("update", attempt("UPDATE sample_floor_record SET note = 'sửa trực tiếp' WHERE id = ?", id));
            result.put("delete", attempt("DELETE FROM sample_floor_record WHERE id = ?", id));
            return result;
        }

        private String attempt(String sql, long id) {
            try {
                return "OK, " + jdbc.update(sql, id) + " dòng";
            } catch (DataAccessException e) {
                return e.getMostSpecificCause().getMessage();
            }
        }

        @PostMapping("/profile-changes")
        @Transactional
        Map<String, Object> profileChange() {
            String id = UUID.randomUUID().toString();
            auditLog.write(AuditLogEntry.builder()
                    .action(new AuditAction("SAMPLE_PROFILE_UPDATED", true))
                    .entity(SAMPLE_PROFILE, id)
                    .before(Map.of("address", "12 Lê Lợi", "allergies", "Chưa ghi nhận"))
                    .after(Map.of("address", "34 Trần Phú", "allergies", "Penicillin"))
                    .reason(new Reason("Cập nhật theo giấy tờ, phát hiện dị ứng penicillin"))
                    .build());
            return Map.of("entityType", SAMPLE_PROFILE, "entityId", id);
        }

        @PostMapping("/catalog-items")
        Map<String, Object> createCatalogItem(@RequestBody Map<String, Object> body) {
            return Map.of("id", samples.createCatalogItem((String) body.get("name")));
        }

        @PostMapping("/catalog-items/{id}/cases")
        Map<String, Object> useCatalogItem(@PathVariable long id) {
            return Map.of("id", samples.createCase(id, currentUser.getObject().accountId()));
        }

        @PostMapping("/catalog-items/{id}/deactivate")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void deactivate(@PathVariable long id) {
            samples.deactivate(id);
        }

        @PostMapping("/catalog-items/{id}/reactivate")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void reactivate(@PathVariable long id) {
            samples.reactivate(id);
        }

        @DeleteMapping("/catalog-items/{id}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void delete(@PathVariable long id) {
            samples.deleteCatalogItem(id);
        }
    }
}

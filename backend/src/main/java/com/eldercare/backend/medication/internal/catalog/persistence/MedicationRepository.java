package com.eldercare.backend.medication.internal.catalog.persistence;

import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.medication.internal.catalog.domain.Medication;
import com.eldercare.backend.medication.internal.catalog.domain.MedicationAttributes;
import com.eldercare.backend.medication.internal.catalog.domain.MedicationStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Kho danh mục thuốc M07 trên bảng dùng chung {@code catalog_item}. */
@Repository
public class MedicationRepository {

    private final CatalogApi catalog;

    public MedicationRepository(CatalogApi catalog) {
        this.catalog = catalog;
    }

    public Optional<Medication> findById(Long id) {
        try {
            return Optional.of(Medication.fromCatalog(catalog.require(MedicationAttributes.KIND, id)));
        } catch (NotFoundException exception) {
            return Optional.empty();
        }
    }

    public boolean existsById(Long id) {
        return findById(id).isPresent();
    }

    @Transactional
    public Medication save(Medication medication) {
        if (medication.getId() == null) {
            CatalogItem<MedicationAttributes> created = catalog.create(
                    MedicationAttributes.KIND,
                    new NewCatalogItem<>(
                            catalogCode(medication), medication.catalogName(), null, 0, medication.attributes()));
            medication.synchronizeFromCatalog(created);
            return medication;
        }

        CatalogItem<MedicationAttributes> changed;
        if (medication.getStatus() == MedicationStatus.INACTIVE) {
            changed = catalog.deactivate(MedicationAttributes.KIND, medication.getId(), medication.getVersion())
                    .after();
        } else {
            changed = catalog.update(
                            MedicationAttributes.KIND,
                            medication.getId(),
                            medication.getVersion(),
                            medication.catalogName(),
                            0,
                            medication.attributes())
                    .after();
        }
        medication.synchronizeFromCatalog(changed);
        return medication;
    }

    public boolean existsByNameIgnoreCaseAndConcentrationIgnoreCaseAndDosageFormIgnoreCase(
            String name, String concentration, String dosageForm) {
        return identityExists(name, concentration, dosageForm, null);
    }

    public boolean existsByNameIgnoreCaseAndConcentrationIgnoreCaseAndDosageFormIgnoreCaseAndIdNot(
            String name, String concentration, String dosageForm, Long id) {
        return identityExists(name, concentration, dosageForm, id);
    }

    public List<Medication> findByStatus(MedicationStatus status) {
        return search(null, status);
    }

    public List<Medication> search(String query, MedicationStatus status) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return items().stream()
                .map(Medication::fromCatalog)
                .filter(medication -> status == null || medication.getStatus() == status)
                .filter(medication -> normalizedQuery.isEmpty()
                        || medication.getName().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || medication
                                .getConcentration()
                                .toLowerCase(Locale.ROOT)
                                .contains(normalizedQuery)
                        || medication.getDosageForm().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .sorted(Comparator.comparing(Medication::getName).thenComparing(Medication::getId))
                .toList();
    }

    private boolean identityExists(String name, String concentration, String dosageForm, Long excludedId) {
        return items().stream()
                .filter(item -> excludedId == null || item.id() != excludedId)
                .filter(item -> item.status() == CatalogStatus.ACTIVE)
                .map(CatalogItem::attributes)
                .anyMatch(attributes -> attributes.name().equalsIgnoreCase(name)
                        && attributes.concentration().equalsIgnoreCase(concentration)
                        && attributes.dosageForm().equalsIgnoreCase(dosageForm));
    }

    private List<CatalogItem<MedicationAttributes>> items() {
        return catalog.list(MedicationAttributes.KIND, null, false);
    }

    private static String catalogCode(Medication medication) {
        String identity = String.join(
                        "|", medication.getName(), medication.getConcentration(), medication.getDosageForm())
                .toLowerCase(Locale.ROOT);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8));
            return "M07-" + HexFormat.of().formatHex(digest).substring(0, 35);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM không hỗ trợ SHA-256", exception);
        }
    }
}

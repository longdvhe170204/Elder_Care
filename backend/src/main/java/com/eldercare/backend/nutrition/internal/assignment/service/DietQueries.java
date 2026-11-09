package com.eldercare.backend.nutrition.internal.assignment.service;

import com.eldercare.backend.nutrition.api.DietApi;
import com.eldercare.backend.nutrition.api.dto.CurrentDietView;
import com.eldercare.backend.nutrition.api.dto.DietAssignmentView;
import com.eldercare.backend.nutrition.api.dto.Texture;
import com.eldercare.backend.nutrition.internal.assignment.domain.DietAssignment;
import com.eldercare.backend.nutrition.internal.assignment.persistence.DietAssignmentStore;
import com.eldercare.backend.nutrition.internal.catalog.domain.Diet;
import com.eldercare.backend.nutrition.internal.catalog.persistence.NutritionCatalogStore;
import com.eldercare.backend.resident.api.ResidentApi;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bản cài {@link DietApi} (module-interfaces mục 1.2): chế độ ăn hiện hành từ bản gán Hiệu lực; chưa gán thì chế độ ăn
 * mặc định của viện, kết cấu thường, không hạn chế riêng, dấu {@code notAssigned} (FR-014). Người ở trạng thái cuối
 * không có chế độ ăn hiện hành.
 */
@Service
class DietQueries implements DietApi {

    private final DietAssignmentStore store;
    private final NutritionCatalogStore catalog;
    private final ResidentApi residents;

    DietQueries(DietAssignmentStore store, NutritionCatalogStore catalog, ResidentApi residents) {
        this.store = store;
        this.catalog = catalog;
        this.residents = residents;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CurrentDietView> current(long residentId) {
        Optional<DietAssignment> active = store.active(residentId);
        if (active.isPresent()) {
            DietAssignment a = active.get();
            return Optional.of(new CurrentDietView(
                    residentId, a.dietId(), catalog.diet(a.dietId()).name(), a.texture(), restrictions(a), false));
        }
        if (residents.isFinalStatus(residentId)) {
            return Optional.empty();
        }
        return catalog.diets().stream()
                .filter(d -> d.active() && d.attributes().isDefault())
                .findFirst()
                .map((Diet d) -> new CurrentDietView(residentId, d.id(), d.name(), Texture.REGULAR, Set.of(), true));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DietAssignmentView> history(long residentId) {
        return store.ofResident(residentId).stream()
                .map(a -> new DietAssignmentView(
                        a.id(),
                        residentId,
                        a.dietId(),
                        a.texture(),
                        restrictions(a),
                        a.personalInstruction(),
                        a.status(),
                        a.requestedEffectiveFrom(),
                        a.actualEffectiveAt()))
                .toList();
    }

    private static Set<Long> restrictions(DietAssignment a) {
        return new LinkedHashSet<>(a.personalRestrictions());
    }
}

package com.eldercare.backend.resident.api;

import com.eldercare.backend.resident.api.dto.AllergenGroup;
import com.eldercare.backend.resident.api.dto.AllergenView;
import com.eldercare.backend.resident.api.dto.CreateAllergen;
import java.util.List;
import java.util.Optional;

/** Danh mục dị nguyên cho 006, 011. */
public interface AllergenApi {

    Optional<AllergenView> allergen(long id);

    List<AllergenView> activeAllergens(AllergenGroup group);

    /** 011 FR-003: Bác sĩ thêm thành phần thực phẩm. */
    long addAllergen(CreateAllergen command);
}

package com.eldercare.backend.nutrition.api.dto;

import java.util.Set;

/**
 * Chế độ ăn hiện hành của người cao tuổi: kết cấu và hạn chế riêng. Chưa gán thì là chế độ ăn mặc định của viện với
 * notAssigned = true (FR-014, dấu CHUA_GAN).
 */
public record CurrentDietView(
        long residentId,
        long dietId,
        String dietName,
        Texture texture,
        Set<Long> personalRestrictions,
        boolean notAssigned) {}

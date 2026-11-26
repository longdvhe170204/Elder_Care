ALTER TABLE prescription
    ADD COLUMN chronic_disease TINYINT(1) NOT NULL DEFAULT 0 AFTER indication,
    ADD COLUMN measurement_conditions JSON NULL AFTER chronic_disease;

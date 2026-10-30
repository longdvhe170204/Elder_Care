-- 006: fields of the supplied medication intake that V006_08 left out (data-model section 9a, FR-054, api-006
-- NewSuppliedMedicationIntake): medication, concentration, dosage form, expiry, condition, deliverer; result values as
-- api-006 (PASSED, NOT_PASSED); NOT_PASSED needs discrepancy and reason. No writer existed before, so the table is empty.

ALTER TABLE supplied_medication_intake
    ADD COLUMN medication_id BIGINT NOT NULL AFTER prescription_id,
    ADD COLUMN concentration VARCHAR(100) NOT NULL AFTER medication_name,
    ADD COLUMN dosage_form VARCHAR(100) NOT NULL AFTER concentration,
    ADD COLUMN expiry_date DATE NOT NULL AFTER unit,
    ADD COLUMN item_condition VARCHAR(500) NOT NULL AFTER expiry_date,
    ADD COLUMN deliverer_name VARCHAR(255) NOT NULL AFTER reason,
    MODIFY COLUMN intake_result ENUM('PASSED', 'NOT_PASSED') NOT NULL,
    ADD CONSTRAINT chk_intake_not_passed_details
        CHECK (intake_result = 'PASSED' OR (discrepancy IS NOT NULL AND reason IS NOT NULL));

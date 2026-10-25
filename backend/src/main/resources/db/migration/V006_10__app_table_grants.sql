-- 006: write grants for eldercare_app on the tables of V006_03 -> V006_08, which had none (000 V000_00: schema level is
-- SELECT only, INSERT, UPDATE, DELETE are granted per table). Group 3 tables get INSERT only; their triggers are in
-- V006_02 -> V006_09. reconciliation_item keeps DELETE because lines of an open reconciliation are replaced
-- (MedicationReconciliationService.updateItems).
-- family_medication_reminder has no writer yet, so it gets no grant.

-- Doses (V006_03)
GRANT INSERT, UPDATE ON medication_dose TO 'eldercare_app'@'%';
GRANT INSERT ON dose_result TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON dose_review TO 'eldercare_app'@'%';

-- Reconciliation (V006_04)
GRANT INSERT, UPDATE ON medication_reconciliation TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE, DELETE ON reconciliation_item TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON reconciliation_correction_request TO 'eldercare_app'@'%';

-- Absence and handover (V006_05)
GRANT INSERT, UPDATE ON medication_absence_interval TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON medication_handover TO 'eldercare_app'@'%';
GRANT INSERT ON medication_handover_item TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON medication_handover_dose TO 'eldercare_app'@'%';
GRANT INSERT ON medication_return TO 'eldercare_app'@'%';
GRANT INSERT ON medication_return_item TO 'eldercare_app'@'%';
GRANT INSERT ON no_medication_order TO 'eldercare_app'@'%';

-- PRN (V006_06)
GRANT INSERT ON prn_administration TO 'eldercare_app'@'%';

-- Family medication (V006_07)
GRANT INSERT, UPDATE ON family_medication_batch TO 'eldercare_app'@'%';
GRANT INSERT, UPDATE ON family_medication_prescription TO 'eldercare_app'@'%';
GRANT INSERT ON family_medication_movement TO 'eldercare_app'@'%';

-- Supplied medication intake (V006_08)
GRANT INSERT ON supplied_medication_intake TO 'eldercare_app'@'%';

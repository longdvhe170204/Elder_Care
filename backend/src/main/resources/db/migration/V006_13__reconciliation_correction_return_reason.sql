-- 006: reason when a reconciliation correction is returned (api-006 POST /reconciliation-corrections/{id}/return
-- requires a reason, FR-039); V006_04 had no column for it.
ALTER TABLE reconciliation_correction_request
    ADD COLUMN return_reason VARCHAR(2000) NULL;

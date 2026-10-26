-- 006: columns of the PRN administration that V006_06 left out (data-model section 6 `lan_dung_prn`: phan_ung, ghi_chu,
-- nhan_ghi_sau_mat_ket_noi; api-006 NewPrnAdministration adverseReaction, note, recordedAfterDisconnect).
ALTER TABLE prn_administration
    ADD COLUMN adverse_reaction VARCHAR(2000) NULL AFTER reason,
    ADD COLUMN note VARCHAR(2000) NULL AFTER adverse_reaction,
    ADD COLUMN recorded_after_disconnect BOOLEAN NOT NULL DEFAULT FALSE AFTER offline_device_time;

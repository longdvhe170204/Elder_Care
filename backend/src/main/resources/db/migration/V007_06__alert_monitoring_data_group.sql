-- Khai lại nhóm dữ liệu theo R-15: alert, monitoring_setting là nhóm 2 (nghiệp vụ có trạng thái), V007_03, V007_05 ghi STATEFUL.
ALTER TABLE alert COMMENT = 'DataGroup=BUSINESS';
ALTER TABLE monitoring_setting COMMENT = 'DataGroup=BUSINESS';

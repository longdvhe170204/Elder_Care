-- 000 T031: nạp 119 tham số của Phụ lục 25 (docs/business/nghiep-vu.md), sinh bằng script và rà tay.
-- Mỗi mã một dòng parameter và một dòng parameter_history (người thực hiện: Bộ lập lịch, actor_id NULL).
-- Giá trị null: "Theo cơ sở" hoặc "Viện nhập khi cài đặt"; Quản lý viện đặt sau khi cài (đọc khi chưa đặt thì lỗi).
-- Dạng TABLE: feature sở hữu có thể chốt lại cấu trúc bằng migration của mình (plan 000, Ghi chú triển khai).
-- constraints chỉ gồm ràng buộc kỹ thuật và ràng buộc có nguồn (CFG-M15-06 > CFG-M15-05; FR-015, Q-315).

-- CFG-M01-01: 2 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M01-01', 'Quá hạn trở lại sau tạm vắng', 'DURATION', 'HOURS', '{"min":1}', '{"value":2,"unit":"HOURS"}', '{"value":2,"unit":"HOURS"}', 'BR-M01-01', 0);
-- CFG-M01-02: 90 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M01-02', 'Chu kỳ đánh giá lại định kỳ; thời hạn hiệu lực của đánh giá khi tiếp nhận', 'DURATION', 'DAYS', '{"min":1}', '{"value":90,"unit":"DAYS"}', '{"value":90,"unit":"DAYS"}', 'BR-M01-02, 5.6', 0);
-- CFG-M01-03: 48 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M01-03', 'Hạn đánh giá lại kể từ lúc bác sĩ được báo', 'DURATION', 'HOURS', '{"min":1}', '{"value":48,"unit":"HOURS"}', '{"value":48,"unit":"HOURS"}', 'BR-M01-02', 0);
-- CFG-M01-04: 30 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M01-04', 'Khóa tài khoản người thân sau kết thúc lưu trú', 'DURATION', 'DAYS', '{"min":1}', '{"value":30,"unit":"DAYS"}', '{"value":30,"unit":"DAYS"}', '5.6', 0);
-- CFG-M01-06: 1 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M01-06', 'Chu kỳ nhắc khi đã tiếp nhận mà chưa có bản đồng ý Hiệu lực', 'DURATION', 'DAYS', '{"min":1}', '{"value":1,"unit":"DAYS"}', '{"value":1,"unit":"DAYS"}', '5.1, 5.6', 0);
-- CFG-M02-02: 48 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M02-02', 'Thời hạn giữ chỗ tạm cho hồ sơ chờ', 'DURATION', 'HOURS', '{"min":1}', '{"value":48,"unit":"HOURS"}', '{"value":48,"unit":"HOURS"}', 'BR-M02-02', 0);
-- CFG-M02-03: 30 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M02-03', 'Nhắc xác nhận hồ sơ chờ không cập nhật', 'DURATION', 'DAYS', '{"min":1}', '{"value":30,"unit":"DAYS"}', '{"value":30,"unit":"DAYS"}', 'BR-M02-03', 0);
-- CFG-M02-04: 30 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M02-04', 'Nhắc trước khi hợp đồng dài hạn hết hạn', 'DURATION', 'DAYS', '{"min":1}', '{"value":30,"unit":"DAYS"}', '{"value":30,"unit":"DAYS"}', 'BR-M02-05', 0);
-- CFG-M02-05: Theo bảng 6.7
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M02-05', 'Bảng chính sách phí khi vắng', 'TABLE', NULL, NULL, '{"rows":[{"residencyType":"LONG_TERM","absenceType":"HOME_OR_OUTING","fromDay":1,"toDay":null,"feeRatePercent":100,"holdBed":"YES"},{"residencyType":"LONG_TERM","absenceType":"HOSPITAL","fromDay":1,"toDay":7,"feeRatePercent":100,"holdBed":"YES"},{"residencyType":"LONG_TERM","absenceType":"HOSPITAL","fromDay":8,"toDay":30,"feeRatePercent":70,"holdBed":"YES"},{"residencyType":"LONG_TERM","absenceType":"HOSPITAL","fromDay":31,"toDay":null,"feeRatePercent":"MANAGER_DECIDES","holdBed":"MANAGER_DECIDES"},{"residencyType":"SHORT_TERM","absenceType":"ANY","fromDay":1,"toDay":null,"feeRatePercent":"BY_CONTRACT","holdBed":"BY_CONTRACT"},{"residencyType":"DAY_CARE","absenceType":"NOTIFIED_IN_ADVANCE","fromDay":null,"toDay":null,"feeRatePercent":0,"holdBed":null},{"residencyType":"DAY_CARE","absenceType":"UNANNOUNCED","fromDay":null,"toDay":null,"feeRatePercent":50,"holdBed":null}]}', '{"rows":[{"residencyType":"LONG_TERM","absenceType":"HOME_OR_OUTING","fromDay":1,"toDay":null,"feeRatePercent":100,"holdBed":"YES"},{"residencyType":"LONG_TERM","absenceType":"HOSPITAL","fromDay":1,"toDay":7,"feeRatePercent":100,"holdBed":"YES"},{"residencyType":"LONG_TERM","absenceType":"HOSPITAL","fromDay":8,"toDay":30,"feeRatePercent":70,"holdBed":"YES"},{"residencyType":"LONG_TERM","absenceType":"HOSPITAL","fromDay":31,"toDay":null,"feeRatePercent":"MANAGER_DECIDES","holdBed":"MANAGER_DECIDES"},{"residencyType":"SHORT_TERM","absenceType":"ANY","fromDay":1,"toDay":null,"feeRatePercent":"BY_CONTRACT","holdBed":"BY_CONTRACT"},{"residencyType":"DAY_CARE","absenceType":"NOTIFIED_IN_ADVANCE","fromDay":null,"toDay":null,"feeRatePercent":0,"holdBed":null},{"residencyType":"DAY_CARE","absenceType":"UNANNOUNCED","fromDay":null,"toDay":null,"feeRatePercent":50,"holdBed":null}]}', 'BR-M02-06', 0);
-- CFG-M02-06: 24 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M02-06', 'Thời hạn báo vắng trước của bán trú', 'DURATION', 'HOURS', '{"min":1}', '{"value":24,"unit":"HOURS"}', '{"value":24,"unit":"HOURS"}', '3.4, 6.7', 0);
-- CFG-M02-07: 2 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M02-07', 'Coi là vắng không báo sau giờ đến dự kiến', 'DURATION', 'HOURS', '{"min":1}', '{"value":2,"unit":"HOURS"}', '{"value":2,"unit":"HOURS"}', '3.4', 0);
-- CFG-M02-08: Mỗi 7 ngày chờ +1 điểm; tình huống đặc biệt theo cơ sở
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M02-08', 'Bảng điểm ưu tiên danh sách chờ (điểm theo ba mức chăm sóc ở mục 4)', 'TABLE', NULL, NULL, '{"waitingPeriod":{"value":7,"unit":"DAYS"},"pointsPerWaitingPeriod":1,"careLevelPoints":null,"specialSituationPoints":null}', '{"waitingPeriod":{"value":7,"unit":"DAYS"},"pointsPerWaitingPeriod":1,"careLevelPoints":null,"specialSituationPoints":null}', '6.2, BR-M02-10', 0);
-- CFG-M02-09: 1 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M02-09', 'Chu kỳ nhắc danh sách việc sau qua đời', 'DURATION', 'DAYS', '{"min":1}', '{"value":1,"unit":"DAYS"}', '{"value":1,"unit":"DAYS"}', '6.8', 0);
-- CFG-M02-10: 7 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M02-10', 'Số ngày hợp đồng quá hạn trước khi báo Quản lý viện', 'DURATION', 'DAYS', '{"min":1}', '{"value":7,"unit":"DAYS"}', '{"value":7,"unit":"DAYS"}', 'BR-M02-05', 0);
-- CFG-M02-11: 30 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M02-11', 'Thời gian lưu trú dự kiến tối thiểu để đăng ký tạm trú; dưới ngưỡng thì thông báo lưu trú', 'DURATION', 'DAYS', '{"min":1}', '{"value":30,"unit":"DAYS"}', '{"value":30,"unit":"DAYS"}', '6.10, BR-M02-11, BR-M02-14', 0);
-- CFG-M02-12: 1 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M02-12', 'Hạn khai báo tạm trú, lưu trú sau Hoàn tất tiếp nhận', 'DURATION', 'DAYS', '{"min":1}', '{"value":1,"unit":"DAYS"}', '{"value":1,"unit":"DAYS"}', 'BR-M02-11', 0);
-- CFG-M02-13: 30 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M02-13', 'Nhắc gia hạn tạm trú trước ngày hết hạn', 'DURATION', 'DAYS', '{"min":1}', '{"value":30,"unit":"DAYS"}', '{"value":30,"unit":"DAYS"}', 'BR-M02-12', 0);
-- CFG-M03-01: Theo cơ sở
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M03-01', 'Sức chứa khu nghỉ bán trú mỗi buổi', 'INTEGER', 'PERSONS', '{"min":0}', 'null', 'null', 'BR-M03-03', 0);
-- CFG-M03-02: 1 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M03-02', 'Chu kỳ nhắc chuyển giường khi phòng không còn phù hợp mức chăm sóc', 'DURATION', 'DAYS', '{"min":1}', '{"value":1,"unit":"DAYS"}', '{"value":1,"unit":"DAYS"}', 'BR-M01-03, BR-M03-15, BR-M03-19 (nhắc khi người trở về chưa có giường)', 0);
-- CFG-M03-04: 4 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M03-04', 'Hạn xác nhận giường sẵn sàng', 'DURATION', 'HOURS', '{"min":1}', '{"value":4,"unit":"HOURS"}', '{"value":4,"unit":"HOURS"}', 'BR-M03-09', 0);
-- CFG-M03-07: 24 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M03-07', 'Khoảng lùi tối đa khi nhập bù phân bổ giường không cần duyệt', 'DURATION', 'HOURS', '{"min":1}', '{"value":24,"unit":"HOURS"}', '{"value":24,"unit":"HOURS"}', '7.3', 0);
-- CFG-M03-08: 30 phút
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M03-08', 'Độ trễ tối đa của việc tự động theo lịch (chuyển giường, bắt đầu/kết thúc phân bổ) trước khi báo Quản lý viện', 'DURATION', 'MINUTES', '{"min":1}', '{"value":30,"unit":"MINUTES"}', '{"value":30,"unit":"MINUTES"}', '7.4', 0);
-- CFG-M03-09: 30 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M03-09', 'Nhắc trước hạn bảo trì, kiểm định, đăng kiểm, bảo hiểm tài sản', 'DURATION', 'DAYS', '{"min":1}', '{"value":30,"unit":"DAYS"}', '{"value":30,"unit":"DAYS"}', 'BR-M03-17', 0);
-- CFG-M04-01: 1 giờ trước giờ bắt đầu ca
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M04-01', 'Mốc sớm nhất Người phụ trách ca lập công việc của ca', 'DURATION', 'HOURS', '{"min":1}', '{"value":1,"unit":"HOURS"}', '{"value":1,"unit":"HOURS"}', 'BR-M04-01', 0);
-- CFG-M04-02: ±15 phút
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M04-02', 'Khung thời gian cho phép mặc định của công việc', 'DURATION', 'MINUTES', '{"min":1}', '{"value":15,"unit":"MINUTES"}', '{"value":15,"unit":"MINUTES"}', '8.1, BR-M04-05', 0);
-- CFG-M04-03: 16:00 / 60% mục tiêu
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M04-03', 'Mốc kiểm tra lượng nước / ngưỡng tối thiểu', 'TABLE', NULL, NULL, '{"checkTime":"16:00","minPercentOfTarget":60}', '{"checkTime":"16:00","minPercentOfTarget":60}', 'BR-M04-08', 0);
-- CFG-M04-04: 3
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M04-04', 'Số bữa ăn kém liên tiếp để cảnh báo', 'INTEGER', NULL, '{"min":0}', '3', '3', 'BR-M04-09', 0);
-- CFG-M04-05: 3
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M04-05', 'Số ngày tâm trạng tiêu cực liên tiếp', 'INTEGER', NULL, '{"min":0}', '3', '3', 'BR-M04-11', 0);
-- CFG-M04-06: 2 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M04-06', 'Ngưỡng ghi nhận muộn', 'DURATION', 'HOURS', '{"min":1}', '{"value":2,"unit":"HOURS"}', '{"value":2,"unit":"HOURS"}', 'BR-M04-12; nhãn "ghi nhận muộn" và mốc nhắc của điểm danh buổi hoạt động (8.8)', 0);
-- CFG-M04-07: 30 phút
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M04-07', 'Quá giờ về của hoạt động ngoài viện', 'DURATION', 'MINUTES', '{"min":1}', '{"value":30,"unit":"MINUTES"}', '{"value":30,"unit":"MINUTES"}', 'BR-M04-17', 0);
-- CFG-M04-10: 7 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M04-10', 'Số ngày không tham gia hoạt động nhóm để cảnh báo cô lập', 'DURATION', 'DAYS', '{"min":1}', '{"value":7,"unit":"DAYS"}', '{"value":7,"unit":"DAYS"}', 'BR-M04-22', 0);
-- CFG-M04-11: 5% mỗi ca
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M04-11', 'Tỷ lệ tối thiểu công việc người kiểm tra chọn để kiểm tra chất lượng', 'PERCENTAGE', 'PERCENT', '{"min":0,"max":100}', '5', '5', 'BR-M04-23', 0);
-- CFG-M04-12: 30 phút
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M04-12', 'Khoảng chờ từ lúc công việc Thường Quá hạn tới khi báo người phụ trách ca', 'DURATION', 'MINUTES', '{"min":1}', '{"value":30,"unit":"MINUTES"}', '{"value":30,"unit":"MINUTES"}', '8.7', 0);
-- CFG-M04-13: 1 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M04-13', 'Thời gian sớm nhất được điểm danh rời viện trước giờ rời dự kiến của chuyến đi', 'DURATION', 'HOURS', '{"min":1}', '{"value":1,"unit":"HOURS"}', '{"value":1,"unit":"HOURS"}', '8.9', 0);
-- CFG-M05-01: 15 phút
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M05-01', 'Thời hạn tiếp nhận cảnh báo mức trung bình', 'DURATION', 'MINUTES', '{"min":1}', '{"value":15,"unit":"MINUTES"}', '{"value":15,"unit":"MINUTES"}', '9.3, BR-M05-01', 0);
-- CFG-M05-02: 3 lần / 24 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M05-02', 'Số lần lặp cảnh báo trung bình để báo lặp lại', 'TABLE', NULL, NULL, '{"count":3,"window":{"value":24,"unit":"HOURS"}}', '{"count":3,"window":{"value":24,"unit":"HOURS"}}', 'BR-M05-03', 0);
-- CFG-M05-03: 5% trong 30 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M05-03', 'Ngưỡng sụt cân', 'TABLE', NULL, NULL, '{"percent":5,"window":{"value":30,"unit":"DAYS"}}', '{"percent":5,"window":{"value":30,"unit":"DAYS"}}', 'BR-M05-04', 0);
-- CFG-M05-04: 2
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M05-04', 'Số lần từ chối cùng một thuốc liên tiếp', 'INTEGER', NULL, '{"min":0}', '2', '2', 'BR-M05-04, BR-M07-13', 0);
-- CFG-M05-05: 3
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M05-05', 'Số đêm mất ngủ liên tiếp', 'INTEGER', NULL, '{"min":0}', '3', '3', 'BR-M05-04', 0);
-- CFG-M05-06: Mỗi 4 giờ trong 72 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M05-06', 'Theo dõi sau ngã', 'TABLE', NULL, NULL, '{"interval":{"value":4,"unit":"HOURS"},"duration":{"value":72,"unit":"HOURS"}}', '{"interval":{"value":4,"unit":"HOURS"},"duration":{"value":72,"unit":"HOURS"}}', 'BR-M05-07', 0);
-- CFG-M05-07: 5 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M05-07', 'Khoảng thời gian truy vết tiếp xúc (người cùng phòng, hoạt động chung, nhân viên phân công, người thân thăm; spec 007)', 'DURATION', 'DAYS', '{"min":1}', '{"value":5,"unit":"DAYS"}', '{"value":5,"unit":"DAYS"}', 'BR-M05-10', 0);
-- CFG-M05-08: Đo nhiệt độ 2 lần/ngày trong 7 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M05-08', 'Theo dõi người tiếp xúc', 'TABLE', NULL, NULL, '{"measurement":"TEMPERATURE","timesPerDay":2,"duration":{"value":7,"unit":"DAYS"}}', '{"measurement":"TEMPERATURE","timesPerDay":2,"duration":{"value":7,"unit":"DAYS"}}', 'BR-M05-11', 0);
-- CFG-M06-01: 7 ngày sau khi lưu trú
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M06-01', 'Nhắc thiết lập ngưỡng cá nhân', 'DURATION', 'DAYS', '{"min":1}', '{"value":7,"unit":"DAYS"}', '{"value":7,"unit":"DAYS"}', 'BR-M06-02', 0);
-- CFG-M06-02: 60 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M06-02', 'Cảnh báo trước khi giấy phép cơ sở/người hành nghề hết hạn (mọi giấy phép hành nghề)', 'DURATION', 'DAYS', '{"min":1}', '{"value":60,"unit":"DAYS"}', '{"value":60,"unit":"DAYS"}', 'BR-M06-05', 0);
-- CFG-M06-03: Theo danh mục chỉ số
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M06-03', 'Khoảng giá trị hợp lệ vật lý của từng chỉ số', 'TABLE', NULL, NULL, '{}', '{}', 'BR-M06-04', 0);
-- CFG-M06-04: 10 phút
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M06-04', 'Thời gian chờ đo lại giá trị ở mức nguy hiểm; hết hạn thì bản ghi được xác nhận với nhãn "chưa đo lại"', 'DURATION', 'MINUTES', '{"min":1}', '{"value":10,"unit":"MINUTES"}', '{"value":10,"unit":"MINUTES"}', 'BR-M06-04', 0);
-- CFG-M07-01: ±30 phút
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M07-01', 'Cửa sổ thời gian mặc định của liều', 'DURATION', 'MINUTES', '{"min":1}', '{"value":30,"unit":"MINUTES"}', '{"value":30,"unit":"MINUTES"}', '11.2, BR-M07-02', 0);
-- CFG-M07-02: 30 phút
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M07-02', 'Thời gian từ Trễ chuyển sang Bỏ lỡ', 'DURATION', 'MINUTES', '{"min":1}', '{"value":30,"unit":"MINUTES"}', '{"value":30,"unit":"MINUTES"}', 'BR-M07-02', 0);
-- CFG-M07-04: 3 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M07-04', 'Nhắc trước khi đơn thuốc hết hạn', 'DURATION', 'DAYS', '{"min":1}', '{"value":3,"unit":"DAYS"}', '{"value":3,"unit":"DAYS"}', 'BR-M07-08', 0);
-- CFG-M07-05: 4 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M07-05', 'Hạn hoàn thành đối chiếu thuốc', 'DURATION', 'HOURS', '{"min":1}', '{"value":4,"unit":"HOURS"}', '{"value":4,"unit":"HOURS"}', 'BR-M07-09', 0);
-- CFG-M07-06: Còn dưới 5 ngày dùng
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M07-06', 'Nhắc gia đình khi thuốc gửi sắp hết', 'DURATION', 'DAYS', '{"min":1}', '{"value":5,"unit":"DAYS"}', '{"value":5,"unit":"DAYS"}', 'BR-M07-11', 0);
-- CFG-M07-07: Còn dưới 2 ngày dùng (đối chiếu khi triển khai, 24.4)
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M07-07', 'Mốc nhắc lần hai khi thuốc gia đình gửi sắp hết', 'DURATION', 'DAYS', '{"min":1}', '{"value":2,"unit":"DAYS"}', '{"value":2,"unit":"DAYS"}', 'BR-M07-11', 0);
-- CFG-M08-01: 2 giờ trước bữa
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M08-01', 'Mốc Nhân viên bếp được chốt suất ăn', 'DURATION', 'HOURS', '{"min":1}', '{"value":2,"unit":"HOURS"}', '{"value":2,"unit":"HOURS"}', 'BR-M08-01', 0);
-- CFG-M08-03: 3 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M08-03', 'Số ngày lặp món liên tiếp để cảnh báo', 'DURATION', 'DAYS', '{"min":1}', '{"value":3,"unit":"DAYS"}', '{"value":3,"unit":"DAYS"}', 'BR-M08-07', 0);
-- CFG-M08-04: Theo cơ sở / 30 phút
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M08-04', 'Giờ bữa dự kiến / ngưỡng giao trễ', 'TABLE', NULL, NULL, '{"mealTimes":null,"lateDeliveryThreshold":{"value":30,"unit":"MINUTES"}}', '{"mealTimes":null,"lateDeliveryThreshold":{"value":30,"unit":"MINUTES"}}', 'BR-M08-12', 0);
-- CFG-M08-05: 30 phút
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M08-05', 'Nhắc xác nhận phát sinh trước bữa', 'DURATION', 'MINUTES', '{"min":1}', '{"value":30,"unit":"MINUTES"}', '{"value":30,"unit":"MINUTES"}', 'BR-M08-13', 0);
-- CFG-M08-06: 24 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M08-06', 'Thời gian giữ mẫu thức ăn', 'DURATION', 'HOURS', '{"min":1}', '{"value":24,"unit":"HOURS"}', '{"value":24,"unit":"HOURS"}', 'BR-M08-15', 0);
-- CFG-M08-07: 2 ngày trước ngày đầu tuần
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M08-07', 'Nhắc công bố thực đơn tuần kế tiếp', 'DURATION', 'DAYS', '{"min":1}', '{"value":2,"unit":"DAYS"}', '{"value":2,"unit":"DAYS"}', 'BR-M08-16', 0);
-- CFG-M08-08: 2 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M08-08', 'Nhắc lô nguyên liệu sắp hết hạn dùng', 'DURATION', 'DAYS', '{"min":1}', '{"value":2,"unit":"DAYS"}', '{"value":2,"unit":"DAYS"}', 'BR-M08-18', 0);
-- CFG-M08-09: Hằng tháng
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M08-09', 'Chu kỳ kiểm kê kho nguyên liệu', 'DURATION', 'MONTHS', '{"min":1}', '{"value":1,"unit":"MONTHS"}', '{"value":1,"unit":"MONTHS"}', 'BR-M08-19', 0);
-- CFG-M09-01: Theo cơ sở
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M09-01', 'Ngưỡng tỷ lệ phục vụ: trọng số tối đa trên mỗi nhân viên, theo mẫu ca (Q-79, Q-205; trọng số ở CFG-M09-12)', 'TABLE', NULL, NULL, 'null', 'null', 'BR-M09-02', 0);
-- CFG-M09-02: 16 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M09-02', 'Số giờ làm liên tục tối đa trước khi cảnh báo', 'DURATION', 'HOURS', '{"min":1}', '{"value":16,"unit":"HOURS"}', '{"value":16,"unit":"HOURS"}', 'BR-M09-03', 0);
-- CFG-M09-03: 60 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M09-03', 'Cảnh báo trước khi chứng chỉ đào tạo nhân viên hết hạn (giấy phép hành nghề dùng CFG-M06-02)', 'DURATION', 'DAYS', '{"min":1}', '{"value":60,"unit":"DAYS"}', '{"value":60,"unit":"DAYS"}', 'BR-M09-05', 0);
-- CFG-M09-04: 30 phút
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M09-04', 'Mốc lập bàn giao trước khi kết ca', 'DURATION', 'MINUTES', '{"min":1}', '{"value":30,"unit":"MINUTES"}', '{"value":30,"unit":"MINUTES"}', 'BR-M09-06; mốc nhắc lập danh sách kiểm tra chất lượng (BR-M04-23)', 0);
-- CFG-M09-05: 30 phút từ đầu ca
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M09-05', 'Nhắc khi ca sau chưa xác nhận bàn giao', 'DURATION', 'MINUTES', '{"min":1}', '{"value":30,"unit":"MINUTES"}', '{"value":30,"unit":"MINUTES"}', 'BR-M09-07', 0);
-- CFG-M09-06: 15 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M09-06', 'Nhắc lập lịch ca tháng trước đầu tháng', 'DURATION', 'DAYS', '{"min":1}', '{"value":15,"unit":"DAYS"}', '{"value":15,"unit":"DAYS"}', 'BR-M09-09', 0);
-- CFG-M09-07: ≥ 1 điều dưỡng / tầng / ca
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M09-07', 'Yêu cầu phủ ca tối thiểu theo vai trò (mẫu ca, phạm vi, vai trò, số người). Dòng mặc định áp mọi tầng/khu vực, mọi mẫu ca; ca toàn viện không có dòng mặc định; chỉ tính nhân viên có giấy phép, đào tạo còn hiệu lực (Q-188). Mẫu xoay ca là danh mục riêng (13.2), không thuộc tham số này', 'TABLE', NULL, NULL, '{"rows":[{"shiftTemplate":"ALL","scope":"FLOOR","role":"NURSING_STAFF","minCount":1}]}', '{"rows":[{"shiftTemplate":"ALL","scope":"FLOOR","role":"NURSING_STAFF","minCount":1}]}', '13.2, BR-M09-11', 0);
-- CFG-M09-08: 24 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M09-08', 'Báo Quản lý viện khi ca dưới ngưỡng phục vụ sắp bắt đầu', 'DURATION', 'HOURS', '{"min":1}', '{"value":24,"unit":"HOURS"}', '{"value":24,"unit":"HOURS"}', 'BR-M09-02, Q-89', 0);
-- CFG-M09-09: 24 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M09-09', 'Khoảng tối đa tìm ca sau để nhận bàn giao', 'DURATION', 'HOURS', '{"min":1}', '{"value":24,"unit":"HOURS"}', '{"value":24,"unit":"HOURS"}', '13.5, Q-89', 0);
-- CFG-M09-10: 3 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M09-10', 'Nhắc công bố lịch ca tháng trước ngày đầu tháng', 'DURATION', 'DAYS', '{"min":1}', '{"value":3,"unit":"DAYS"}', '{"value":3,"unit":"DAYS"}', 'BR-M09-09', 0);
-- CFG-M09-11: 24 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M09-11', 'Nhắc yêu cầu đổi ca, nghỉ đột xuất chưa quyết định trước giờ bắt đầu ca', 'DURATION', 'HOURS', '{"min":1}', '{"value":24,"unit":"HOURS"}', '{"value":24,"unit":"HOURS"}', 'BR-M09-10, BR-M09-11', 0);
-- CFG-M09-12: Theo cơ sở (ví dụ cơ bản = 1, đặc biệt = 2,5)
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M09-12', 'Trọng số chăm sóc theo từng mức chăm sóc (Q-205; ba mức ở mục 4; nhu cầu chăm sóc không có trọng số)', 'TABLE', NULL, NULL, 'null', 'null', 'Mục 4, BR-M09-02', 0);
-- CFG-M10-01: Cao 24 giờ / thường 72 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M10-01', 'Hạn xử lý phản hồi', 'TABLE', NULL, NULL, '{"high":{"value":24,"unit":"HOURS"},"normal":{"value":72,"unit":"HOURS"}}', '{"high":{"value":24,"unit":"HOURS"},"normal":{"value":72,"unit":"HOURS"}}', 'BR-M10-05', 0);
-- CFG-M10-02: 7 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M10-02', 'Tự đóng phản hồi khi người gửi không phản hồi', 'DURATION', 'DAYS', '{"min":1}', '{"value":7,"unit":"DAYS"}', '{"value":7,"unit":"DAYS"}', '14.7', 0);
-- CFG-M10-03: Thứ 2 hằng tuần / 48 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M10-03', 'Kỳ bản tin (mốc nhắc lập) / hạn duyệt bản tin', 'TABLE', NULL, NULL, '{"reminderDay":"MONDAY","approvalDeadline":{"value":48,"unit":"HOURS"}}', '{"reminderDay":"MONDAY","approvalDeadline":{"value":48,"unit":"HOURS"}}', 'BR-M10-08', 0);
-- CFG-M10-04: 09:00–11:00 và 15:00–17:00; 20 người mỗi khung
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M10-04', 'Khung giờ thăm và sức chứa mỗi khung', 'TABLE', NULL, NULL, '{"slots":[{"from":"09:00","to":"11:00"},{"from":"15:00","to":"17:00"}],"capacityPerSlot":20}', '{"slots":[{"from":"09:00","to":"11:00"},{"from":"15:00","to":"17:00"}],"capacityPerSlot":20}', '14.2, BR-M10-02', 0);
-- CFG-M10-05: Tối thiểu 2 giờ, tối đa 14 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M10-05', 'Thời hạn đăng ký thăm trước', 'TABLE', NULL, NULL, '{"min":{"value":2,"unit":"HOURS"},"max":{"value":14,"unit":"DAYS"}}', '{"min":{"value":2,"unit":"HOURS"},"max":{"value":14,"unit":"DAYS"}}', '14.2, BR-M10-02', 0);
-- CFG-M10-06: 3
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M10-06', 'Số người tối đa mỗi lượt thăm', 'INTEGER', NULL, '{"min":0}', '3', '3', '14.2', 0);
-- CFG-M10-07: 30 phút
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M10-07', 'Nhắc ghi giờ ra sau khi hết khung thăm', 'DURATION', 'MINUTES', '{"min":1}', '{"value":30,"unit":"MINUTES"}', '{"value":30,"unit":"MINUTES"}', '14.2', 0);
-- CFG-M10-08: 4 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M10-08', 'Hiệu lực của ngoại lệ đón; cũng là hiệu lực của xác nhận người nhận khác đồ gửi (spec 013)', 'DURATION', 'HOURS', '{"min":1}', '{"value":4,"unit":"HOURS"}', '{"value":4,"unit":"HOURS"}', '14.3, BR-M10-03, 16.4', 0);
-- CFG-M10-09: 2 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M10-09', 'Thời gian bản ghi đón chưa dùng còn làm căn cứ', 'DURATION', 'HOURS', '{"min":1}', '{"value":2,"unit":"HOURS"}', '{"value":2,"unit":"HOURS"}', '14.3', 0);
-- CFG-M10-10: 1
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M10-10', 'Số lượt người thân ở lại tối đa cùng lúc cho một người cao tuổi', 'INTEGER', NULL, '{"min":0}', '1', '1', '14.4', 0);
-- CFG-M10-11: 48 giờ / 96 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M10-11', 'Nhắc / báo Quản lý viện khi yêu cầu BR-M10-07 Chờ xác nhận lâu', 'TABLE', NULL, NULL, '{"remind":{"value":48,"unit":"HOURS"},"notifyManager":{"value":96,"unit":"HOURS"}}', '{"remind":{"value":48,"unit":"HOURS"},"notifyManager":{"value":96,"unit":"HOURS"}}', 'BR-M10-07', 0);
-- CFG-M10-12: 96 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M10-12', 'Mốc Trưởng tầng được duyệt thay bản tin', 'DURATION', 'HOURS', '{"min":1}', '{"value":96,"unit":"HOURS"}', '{"value":96,"unit":"HOURS"}', 'BR-M10-08', 0);
-- CFG-M11-01: Ngày cuối của tháng liền sau kỳ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M11-01', 'Hạn chốt bảng chi phí của kỳ vừa kết thúc (không đổi ranh giới kỳ; Q-132)', 'TABLE', NULL, NULL, '{"monthsAfterPeriodEnd":1,"dayOfMonth":"LAST"}', '{"monthsAfterPeriodEnd":1,"dayOfMonth":"LAST"}', '15.7', 0);
-- CFG-M11-02: 500.000 đồng
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M11-02', 'Hạn mức mua hộ cần đồng ý/duyệt', 'MONEY', 'VND', '{"min":0}', '500000', '500000', 'BR-M11-07', 0);
-- CFG-M11-03: 30% so với kỳ trước
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M11-03', 'Biến động tổng chi phí kỳ để cảnh báo', 'PERCENTAGE', 'PERCENT', '{"min":0,"max":100}', '30', '30', 'BR-M11-07', 0);
-- CFG-M11-04: 15 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M11-04', 'Ngưỡng số ngày còn đủ tiền để báo sắp hết tiền', 'DURATION', 'DAYS', '{"min":1}', '{"value":15,"unit":"DAYS"}', '{"value":15,"unit":"DAYS"}', 'BR-M11-13', 0);
-- CFG-M11-05: 7 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M11-05', 'Chu kỳ nhắc lại số dư thấp, còn nợ; số ngày còn nợ trước khi báo Quản lý viện', 'DURATION', 'DAYS', '{"min":1}', '{"value":7,"unit":"DAYS"}', '{"value":7,"unit":"DAYS"}', 'BR-M11-13', 0);
-- CFG-M11-06: 10% (đối chiếu khi triển khai, 24.4)
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M11-06', 'Mức vượt tối đa so với số tiền mua hộ đã đồng ý mà không cần đồng ý bổ sung', 'PERCENTAGE', 'PERCENT', '{"min":0,"max":100}', '10', '10', 'BR-M11-07', 0);
-- CFG-M11-07: 10%
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M11-07', 'Mức vượt tối đa của số tiền mua thực tế so với số được duyệt của yêu cầu mua sắm', 'PERCENTAGE', 'PERCENT', '{"min":0,"max":100}', '10', '10', 'BR-M11-18', 0);
-- CFG-M11-08: 30 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M11-08', 'Thời gian yêu cầu mua sắm Đã duyệt chưa ghi đã mua thì nhắc', 'DURATION', 'DAYS', '{"min":1}', '{"value":30,"unit":"DAYS"}', '{"value":30,"unit":"DAYS"}', 'BR-M11-20', 0);
-- CFG-M11-09: 7 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M11-09', 'Thời gian yêu cầu mua sắm Chờ đối chiếu thì nhắc Kế toán', 'DURATION', 'DAYS', '{"min":1}', '{"value":7,"unit":"DAYS"}', '{"value":7,"unit":"DAYS"}', 'BR-M11-20', 0);
-- CFG-M11-10: Viện nhập khi cài đặt
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M11-10', 'Tài khoản thu của viện (ngân hàng, số tài khoản, tên chủ tài khoản) dùng cho sao kê và mã QR nộp tiền (Q-335)', 'TABLE', NULL, NULL, 'null', 'null', '15.9', 0);
-- CFG-M12-01: Điện thoại, trang sức, tiền mặt
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M12-01', 'Danh mục đồ có giá trị bắt buộc chụp ảnh', 'TABLE', NULL, NULL, '{"categories":["Điện thoại","Trang sức","Tiền mặt"]}', '{"categories":["Điện thoại","Trang sức","Tiền mặt"]}', 'BR-M12-05', 0);
-- CFG-M12-02: 7 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M12-02', 'Chu kỳ nhắc đồ gửi chưa trả sau khi hồ sơ ở trạng thái cuối', 'DURATION', 'DAYS', '{"min":1}', '{"value":7,"unit":"DAYS"}', '{"value":7,"unit":"DAYS"}', '16.4, 6.8', 0);
-- CFG-M12-03: 90 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M12-03', 'Thời gian tối thiểu từ khi hồ sơ ở trạng thái cuối tới khi được lập đề nghị xử lý đồ không người nhận', 'DURATION', 'DAYS', '{"min":1}', '{"value":90,"unit":"DAYS"}', '{"value":90,"unit":"DAYS"}', 'BR-M12-08', 0);
-- CFG-M12-04: Hằng tháng, hạn hoàn thành 3 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M12-04', 'Chu kỳ và hạn kiểm kê đồ có giá trị', 'TABLE', NULL, NULL, '{"cycle":{"value":1,"unit":"MONTHS"},"completionDeadline":{"value":3,"unit":"DAYS"}}', '{"cycle":{"value":1,"unit":"MONTHS"},"completionDeadline":{"value":3,"unit":"DAYS"}}', 'BR-M12-07', 0);
-- CFG-M12-05: 4 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M12-05', 'Thời gian chờ người đại diện phản hồi trước khi Quản lý viện được duyệt thay xác nhận người nhận khác', 'DURATION', 'HOURS', '{"min":1}', '{"value":4,"unit":"HOURS"}', '{"value":4,"unit":"HOURS"}', 'BR-M12-04', 0);
-- CFG-M12-06: Mỗi người đại diện 2 lần qua 2 kênh
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M12-06', 'Bằng chứng liên hệ tối thiểu trước khi Quản lý viện duyệt thay', 'TABLE', NULL, NULL, '{"attemptsPerRepresentative":2,"channels":2}', '{"attemptsPerRepresentative":2,"channels":2}', 'BR-M12-04', 0);
-- CFG-M12-07: 2 lần, cách nhau không dưới CFG-M12-02
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M12-07', 'Bằng chứng liên hệ tối thiểu trước khi lập đề nghị xử lý đồ không người nhận', 'TABLE', NULL, NULL, '{"attempts":2,"minGapParameter":"CFG-M12-02"}', '{"attempts":2,"minGapParameter":"CFG-M12-02"}', 'BR-M12-08', 0);
-- CFG-M13-01: 5 phút
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M13-01', 'Thời hạn xác nhận thông báo khẩn cấp trước khi gọi điện', 'DURATION', 'MINUTES', '{"min":1}', '{"value":5,"unit":"MINUTES"}', '{"value":5,"unit":"MINUTES"}', 'BR-M13-02', 0);
-- CFG-M13-02: 21:00–07:00
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M13-02', 'Giờ yên tĩnh cho thông báo người thân', 'TABLE', NULL, NULL, '{"from":"21:00","to":"07:00"}', '{"from":"21:00","to":"07:00"}', 'BR-M13-03', 0);
-- CFG-M13-03: 5 phút
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M13-03', 'Khung gộp tin nhắn mức Trung bình cho cùng người nhận', 'DURATION', 'MINUTES', '{"min":1}', '{"value":5,"unit":"MINUTES"}', '{"value":5,"unit":"MINUTES"}', 'BR-M13-01', 0);
-- CFG-M13-04: 5 phút
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M13-04', 'Hạn ghi kết quả gọi điện trước khi nhắc người kế tiếp', 'DURATION', 'MINUTES', '{"min":1}', '{"value":5,"unit":"MINUTES"}', '{"value":5,"unit":"MINUTES"}', 'BR-M13-02', 0);
-- CFG-M13-05: 1 lần
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M13-05', 'Số lần tự gửi lại khi kênh ngoài ứng dụng lỗi', 'INTEGER', 'TIMES', '{"min":0}', '1', '1', '17', 0);
-- CFG-M13-06: 07:30–17:00, thứ 2 → thứ 7
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M13-06', 'Giờ hành chính cho vai trò không xếp ca (Quản lý viện, Hành chính, Kế toán; Q-332)', 'TABLE', NULL, NULL, '{"from":"07:30","to":"17:00","days":["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY"]}', '{"from":"07:30","to":"17:00","days":["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY"]}', 'BR-M13-01', 0);
-- CFG-M14-01: 12 tháng
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M14-01', 'Độ dài tối đa của khoảng thời gian một lần xem, xuất báo cáo', 'DURATION', 'MONTHS', '{"min":1}', '{"value":12,"unit":"MONTHS"}', '{"value":12,"unit":"MONTHS"}', '18.7', 0);
-- CFG-M15-01: 5 lần / khóa 15 phút
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M15-01', 'Khóa tạm tài khoản khi đăng nhập sai', 'TABLE', NULL, NULL, '{"maxFailedAttempts":5,"lockDuration":{"value":15,"unit":"MINUTES"}}', '{"maxFailedAttempts":5,"lockDuration":{"value":15,"unit":"MINUTES"}}', 'BR-M15-05', 0);
-- CFG-M15-02: 180 ngày
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M15-02', 'Tài khoản người thân không hoạt động', 'DURATION', 'DAYS', '{"min":1}', '{"value":180,"unit":"DAYS"}', '{"value":180,"unit":"DAYS"}', 'BR-M15-05', 0);
-- CFG-M15-03: 10 năm sau khi kết thúc lưu trú
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M15-03', 'Thời hạn lưu giữ hồ sơ người cao tuổi, sức khỏe, thuốc, sự cố', 'DURATION', 'YEARS', '{"min":1}', '{"value":10,"unit":"YEARS"}', '{"value":10,"unit":"YEARS"}', 'NFR-07', 0);
-- CFG-M15-04: 10 năm
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M15-04', 'Thời hạn lưu giữ nhật ký hệ thống', 'DURATION', 'YEARS', '{"min":1}', '{"value":10,"unit":"YEARS"}', '{"value":10,"unit":"YEARS"}', 'NFR-07, 19.4', 0);
-- CFG-M15-05: 48 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M15-05', 'Nhắc người duyệt khi yêu cầu ở Chờ duyệt quá lâu', 'DURATION', 'HOURS', '{"min":1}', '{"value":48,"unit":"HOURS"}', '{"value":48,"unit":"HOURS"}', '6.6', 0);
-- CFG-M15-06: 96 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M15-06', 'Báo Quản lý viện khi yêu cầu vẫn chưa được duyệt (lớn hơn CFG-M15-05)', 'DURATION', 'HOURS', '{"min":1,"greaterThan":"CFG-M15-05"}', '{"value":96,"unit":"HOURS"}', '{"value":96,"unit":"HOURS"}', '6.6', 0);
-- CFG-M15-07: 2 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M15-07', 'Khoảng phạm vi dữ liệu trước và sau giờ ca', 'DURATION', 'HOURS', '{"min":1}', '{"value":2,"unit":"HOURS"}', '{"value":2,"unit":"HOURS"}', 'BR-M15-02', 0);
-- CFG-M15-08: 24 giờ
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M15-08', 'Thời gian ngoại tuyến tối đa được tự động chấp nhận', 'DURATION', 'HOURS', '{"min":1}', '{"value":24,"unit":"HOURS"}', '{"value":24,"unit":"HOURS"}', '8.6, Q-01', 0);
-- CFG-M15-09: Tối thiểu 8 ký tự, có cả chữ và số
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M15-09', 'Độ mạnh tối thiểu của mật khẩu', 'TABLE', NULL, NULL, '{"minLength":8,"requireLetter":true,"requireDigit":true}', '{"minLength":8,"requireLetter":true,"requireDigit":true}', 'BR-M15-21', 0);
-- CFG-M15-10: Web 30 phút / ứng dụng nhân viên 12 giờ / ứng dụng người thân 30 ngày / ứng dụng người cao tuổi không tự đăng xuất (BR-M15-24)
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M15-10', 'Thời gian không thao tác thì tự đăng xuất, theo kênh', 'TABLE', NULL, NULL, '{"WEB":{"value":30,"unit":"MINUTES"},"STAFF_APP":{"value":12,"unit":"HOURS"},"FAMILY_APP":{"value":30,"unit":"DAYS"},"RESIDENT_APP":{"noLimit":true}}', '{"WEB":{"value":30,"unit":"MINUTES"},"STAFF_APP":{"value":12,"unit":"HOURS"},"FAMILY_APP":{"value":30,"unit":"DAYS"},"RESIDENT_APP":{"noLimit":true}}', 'BR-M15-21', 0);
-- CFG-M15-11: 5 phút
INSERT INTO parameter (code, description, kind, unit, constraints, default_value, current_value, used_in, version)
VALUES ('CFG-M15-11', 'Hiệu lực mã QR đăng nhập của người cao tuổi', 'DURATION', 'MINUTES', '{"min":1}', '{"value":5,"unit":"MINUTES"}', '{"value":5,"unit":"MINUTES"}', '19.1, BR-M15-24', 0);

INSERT INTO parameter_history (code, value, effective_from, actor_id, reason, is_default_restore)
SELECT code, current_value, UTC_TIMESTAMP(6), NULL, NULL, FALSE FROM parameter;

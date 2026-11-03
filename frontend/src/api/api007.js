// File sinh bởi frontend/scripts/gen-api.mjs từ hợp đồng; không sửa tay.

// Nguồn: specs/007-health-alert-incident/contracts (Elder Care – Feature 007 (sức khỏe, cảnh báo, sự cố))
import { request } from './client.js'

/** Danh mục chỉ số, kèm khoảng hợp lệ hiện hành từ CFG-M06-03 và dấu missingValidRange (FR-001) */
export const getVitalSignCatalog = (params) => request('GET', '/vital-sign-catalog', params)

/** Thêm chỉ số (Quản lý viện); khoảng hợp lệ thêm ở CFG-M06-03 (FR-001, FR-002) */
export const postVitalSignCatalog = (params) => request('POST', '/vital-sign-catalog', params)

/** Sửa tên, đơn vị, nhóm sinh hiệu (Quản lý viện) (FR-002) */
export const putVitalSignCatalogById = (params) => request('PUT', '/vital-sign-catalog/{id}', params)

/** Ngừng hiệu lực chỉ số (FR-002) */
export const postVitalSignCatalogByIdDeactivate = (params) => request('POST', '/vital-sign-catalog/{id}/deactivate', params)

/** Danh mục quy tắc xu hướng (FR-039) */
export const getTrendRules = (params) => request('GET', '/trend-rules', params)

/** Bật/tắt, đổi mức (Quản lý viện; lý do bắt buộc) (FR-039, BR-M05-04, BR-M15-04) */
export const putTrendRulesByCode = (params) => request('PUT', '/trend-rules/{code}', params)

/** Danh mục loại sự cố (mức mặc định, dấu không thuộc sức khỏe) (FR-042) */
export const getIncidentTypes = (params) => request('GET', '/incident-types', params)

/** Thêm loại sự cố (Quản lý viện) (FR-042) */
export const postIncidentTypes = (params) => request('POST', '/incident-types', params)

/** Sửa loại sự cố, gồm dấu nonHealth (nhật ký trước/sau) (FR-042) */
export const putIncidentTypesById = (params) => request('PUT', '/incident-types/{id}', params)

/** Danh mục loại diễn biến, có dấu resuscitation (FR-048a, BR-M05-20) */
export const getProgressionTypes = (params) => request('GET', '/progression-types', params)

/** Thêm loại diễn biến (Quản lý viện) (FR-048a) */
export const postProgressionTypes = (params) => request('POST', '/progression-types', params)

/** Lịch đo của người cao tuổi (Bác sĩ, Điều dưỡng, Quản lý viện; Trưởng tầng trong phạm vi) (FR-003, FR-080) */
export const getResidentsByResidentIdMeasurementSchedules = (params) => request('GET', '/residents/{residentId}/measurement-schedules', params)

/** Tạo lịch đo (Bác sĩ) (FR-003, FR-004, FR-006) */
export const postResidentsByResidentIdMeasurementSchedules = (params) => request('POST', '/residents/{residentId}/measurement-schedules', params)

/** Các mốc đo dự kiến trong khoảng (tính, không lưu) (FR-004) */
export const getMeasurementSchedulesByIdMeasurementTimes = (params) => request('GET', '/measurement-schedules/{id}/measurement-times', params)

/** Thay đổi lịch đo (Bác sĩ; lý do bắt buộc) (FR-005, FR-006) */
export const postMeasurementSchedulesByIdChange = (params) => request('POST', '/measurement-schedules/{id}/change', params)

/** Ngừng lịch đo (Bác sĩ; lý do bắt buộc) (FR-006) */
export const postMeasurementSchedulesByIdStop = (params) => request('POST', '/measurement-schedules/{id}/stop', params)

/** Lịch sử chỉ số (giá trị hiện hành, giá trị gốc nếu đã đính chính, người đo, ngưỡng áp dụng, kết quả so) (FR-010, FR-080, BR-M06-06) */
export const getResidentsByResidentIdVitalSigns = (params) => request('GET', '/residents/{residentId}/vital-signs', params)

/** Ghi chỉ số ngoài lịch (Nhân viên chăm sóc, Điều dưỡng trong phạm vi) (FR-010, FR-011, FR-012, FR-013, FR-015, BR-M06-03, BR-M06-04, BR-M04-12) */
export const postResidentsByResidentIdVitalSigns = (params) => request('POST', '/residents/{residentId}/vital-signs', params)

/** Ghi lần đo lại cho bản ghi "chờ đo lại" (FR-016, FR-017, FR-018, BR-M06-04) */
export const postVitalSignRecordsByIdRemeasure = (params) => request('POST', '/vital-sign-records/{id}/remeasure', params)

/** Danh sách người cao tuổi quá CFG-M06-01 chưa có ngưỡng cá nhân (Bác sĩ) (FR-025, BR-M06-02) */
export const getVitalSignsMissingPersonalThreshold = (params) => request('GET', '/vital-signs/missing-personal-threshold', params)

/** Phiên bản ngưỡng (mặc định hoặc của một người), cả lịch sử (FR-020, FR-022, FR-080) */
export const getThresholds = (params) => request('GET', '/thresholds', params)

/** Thiết lập ngưỡng (Bác sĩ) (FR-020, FR-021, FR-022, FR-023, BR-M06-01) */
export const postThresholds = (params) => request('POST', '/thresholds', params)

/** Hủy phiên bản Chờ hiệu lực (Bác sĩ; lý do) (FR-022) */
export const postThresholdsByIdCancel = (params) => request('POST', '/thresholds/{id}/cancel', params)

/** Ngừng ngưỡng cá nhân Hiệu lực (Bác sĩ; lý do); từ đó so theo ngưỡng mặc định (FR-022) */
export const postThresholdsByIdStop = (params) => request('POST', '/thresholds/{id}/stop', params)

/** Danh sách cảnh báo theo phạm vi (lọc trạng thái, mức, tầng, người phụ trách, leo thang tối đa) (FR-027, FR-034, FR-036, FR-080) */
export const getAlerts = (params) => request('GET', '/alerts', params)

/** Tạo cảnh báo thủ công (Trưởng tầng, Bác sĩ, Điều dưỡng; mô tả bắt buộc) (FR-026, FR-028, FR-030, BR-M05-02) */
export const postAlerts = (params) => request('POST', '/alerts', params)

/** Chi tiết cảnh báo, nguồn, lịch sử, lần nhắc, allowedCommands[] (FR-027, FR-033) */
export const getAlertsById = (params) => request('GET', '/alerts/{id}', params)

/** Tiếp nhận (FR-029, FR-036, BR-M05-01) */
export const postAlertsByIdReceive = (params) => request('POST', '/alerts/{id}/receive', params)

/** Giao người phụ trách (Quản lý viện khi là người phụ trách; ghi chú bắt buộc) (FR-029, FR-031, FR-036) */
export const postAlertsByIdAssign = (params) => request('POST', '/alerts/{id}/assign', params)

/** Bắt đầu xử lý (FR-029, FR-036) */
export const postAlertsByIdStartHandling = (params) => request('POST', '/alerts/{id}/start-handling', params)

/** Nâng / hạ mức (lý do bắt buộc; hạ từ Khẩn cấp chỉ Bác sĩ) (FR-029, FR-038a, BR-M05-03) */
export const postAlertsByIdChangeLevel = (params) => request('POST', '/alerts/{id}/change-level', params)

/** Đóng (kết quả bắt buộc; lý do với cảnh báo công việc Bắt buộc quá hạn; nguy kịch cần kết quả xác nhận lại và lựa chọn) (FR-029, FR-050d, BR-M04-06) */
export const postAlertsByIdClose = (params) => request('POST', '/alerts/{id}/close', params)

/** Chuyển sự cố (từ Đang xử lý); sự cố liên kết mức mặc định bằng mức cảnh báo (FR-029, FR-040) */
export const postAlertsByIdConvertToIncident = (params) => request('POST', '/alerts/{id}/convert-to-incident', params)

/** Kích hoạt khẩn cấp từ cảnh báo (Mới, Leo thang, Đã tiếp nhận); bắt buộc trực tuyến (FR-029, FR-047, FR-047a, BR-M05-06, BR-M05-13) */
export const postAlertsByIdActivateEmergency = (params) => request('POST', '/alerts/{id}/activate-emergency', params)

/** Danh sách sự cố theo phạm vi (lọc trạng thái, mức, loại, tầng, người cao tuổi) (FR-045, FR-080) */
export const getIncidents = (params) => request('GET', '/incidents', params)

/** Ghi nhận sự cố (mọi nhân viên có quyền "T"; ngoại lệ ngoài phạm vi 002 FR-044a); bắt buộc trực tuyến (FR-040, FR-041, FR-042, FR-043, FR-047, FR-051, FR-060, BR-M05-06, BR-M05-07, BR-M05-10, BR-M05-17, BR-M05-18) */
export const postIncidents = (params) => request('POST', '/incidents', params)

/** Hồ sơ sự cố (nội dung, diễn biến, đổi mức, người xử lý, người được thông báo từ 009, allowedCommands[]) (FR-040, FR-048b) */
export const getIncidentsById = (params) => request('GET', '/incidents/{id}', params)

/** Tiếp nhận xử lý (Mới → Đang xử lý) (FR-044, FR-045) */
export const postIncidentsByIdReceive = (params) => request('POST', '/incidents/{id}/receive', params)

/** Giao người xử lý (Mới → Đang xử lý; hoặc giao lại khi Đang xử lý, Đang theo dõi) (FR-045, FR-045a, FR-044) */
export const postIncidentsByIdAssignHandler = (params) => request('POST', '/incidents/{id}/assign-handler', params)

/** Tham gia xử lý sự cố Khẩn cấp đang mở (không tạo sự cố mới) (FR-041, FR-048b) */
export const postIncidentsByIdJoinHandling = (params) => request('POST', '/incidents/{id}/join-handling', params)

/** Ghi diễn biến (đánh giá, hành động, biện pháp hồi sức, thông báo, theo dõi, gọi hỗ trợ/cấp cứu, báo cơ quan y tế) (FR-045, FR-048a, FR-048b, FR-066, BR-M05-13, BR-M05-20) */
export const postIncidentsByIdProgressions = (params) => request('POST', '/incidents/{id}/progressions', params)

/** Nâng / hạ mức (lý do; hạ từ Khẩn cấp chỉ Bác sĩ; nâng lên Khẩn cấp kích hoạt mục G; hạ khỏi Khẩn cấp dừng nhắc gọi điện) (FR-044, FR-045, BR-M05-22) */
export const postIncidentsByIdChangeLevel = (params) => request('POST', '/incidents/{id}/change-level', params)

/** Chuyển theo dõi (cần ít nhất một diễn biến đánh giá và một diễn biến xử lý) (FR-045) */
export const postIncidentsByIdMoveToMonitoring = (params) => request('POST', '/incidents/{id}/move-to-monitoring', params)

/** Mở lại xử lý (lý do bắt buộc) (FR-045) */
export const postIncidentsByIdReopen = (params) => request('POST', '/incidents/{id}/reopen', params)

/** Đóng sự cố (kết quả bắt buộc; Trung bình, Khẩn cấp chỉ Điều dưỡng, Bác sĩ; lây nhiễm chọn infectionOutcome và không còn vùng chỉ gắn sự cố này) (FR-045, FR-066, BR-M05-09, BR-M05-12) */
export const postIncidentsByIdClose = (params) => request('POST', '/incidents/{id}/close', params)

/** Kết thúc theo dõi sau ngã (Bác sĩ; lý do); công việc theo dõi Chưa đến hạn bị 005 hủy (FR-052, BR-M05-07) */
export const postIncidentsByIdEndPostFallMonitoring = (params) => request('POST', '/incidents/{id}/end-post-fall-monitoring', params)

/** Thẻ thông tin khẩn cấp (bản chụp lúc kích hoạt và nội dung hiện hành); người không có quyền xem sức khỏe chỉ thấy nhận dạng (FR-047, FR-048, FR-050f, BR-M05-13) */
export const getIncidentsByIdEmergencyCard = (params) => request('GET', '/incidents/{id}/emergency-card', params)

/** Xác nhận đã đối chiếu nguyện vọng (chỉ người được xem đủ thẻ); phản hồi hiển thị lại nguyện vọng (FR-048a, BR-M05-13) */
export const postIncidentsByIdConfirmWishCheck = (params) => request('POST', '/incidents/{id}/confirm-wish-check', params)

/** Chuyển viện từ sự cố (Điều dưỡng, Bác sĩ; sự cố Mới, Đang xử lý) (FR-049, FR-050, BR-M05-14) */
export const postIncidentsByIdTransferOut = (params) => request('POST', '/incidents/{id}/transfer-out', params)

/** Bản tóm tắt chuyển viện (in được; Bác sĩ, Điều dưỡng, Trưởng tầng trong phạm vi, Quản lý viện) (FR-050) */
export const getTransferSummariesById = (params) => request('GET', '/transfer-summaries/{id}', params)

/** Ghi dấu nguy kịch (Bác sĩ) hoặc dấu tạm (Điều dưỡng khi không có Bác sĩ trực) (FR-050a, FR-050b, BR-M05-15, BR-M05-16) */
export const postResidentsByResidentIdCriticalFlags = (params) => request('POST', '/residents/{residentId}/critical-flags', params)

/** Bác sĩ xác nhận dấu tạm (FR-050a) */
export const postCriticalFlagsByIdConfirm = (params) => request('POST', '/critical-flags/{id}/confirm', params)

/** Gỡ dấu (Bác sĩ; lý do); cảnh báo nguy kịch vẫn phải đóng theo FR-050d (FR-050a) */
export const postCriticalFlagsByIdRemove = (params) => request('POST', '/critical-flags/{id}/remove', params)

/** Ghi kết quả xác nhận lại nguyện vọng (Bác sĩ; Điều dưỡng trong phạm vi) (FR-050c, FR-050g, BR-M05-16) */
export const postAlertsByIdWishReconfirmation = (params) => request('POST', '/alerts/{id}/wish-reconfirmation', params)

/** Ghi lựa chọn thực hiện và thực hiện lệnh tương ứng (FR-050c, FR-050d, BR-M05-15) */
export const postAlertsByIdExecuteChoice = (params) => request('POST', '/alerts/{id}/execute-choice', params)

/** Tra cứu dữ liệu tiếp xúc theo nhóm trong CFG-M05-07 trước thời điểm phát hiện (không lưu, không đề xuất) (FR-060, BR-M05-10) */
export const getIncidentsByIdContactLookup = (params) => request('GET', '/incidents/{id}/contact-lookup', params)

/** Danh sách tiếp xúc (Bác sĩ, Điều dưỡng, Quản lý viện; Trưởng tầng trong phạm vi chỉ xem) (FR-061) */
export const getIncidentsByIdContactList = (params) => request('GET', '/incidents/{id}/contact-list', params)

/** Lập danh sách (Nháp) (FR-061) */
export const postIncidentsByIdContactList = (params) => request('POST', '/incidents/{id}/contact-list', params)

/** Thêm người (Nháp) hoặc bổ sung người (Đã xác nhận, lý do bắt buộc) (FR-061, FR-062, BR-M05-11) */
export const postContactListsByIdContacts = (params) => request('POST', '/contact-lists/{id}/contacts', params)

/** Bỏ người khỏi danh sách Nháp (FR-061) */
export const postContactListsByIdContactsByContactIdRemove = (params) => request('POST', '/contact-lists/{id}/contacts/{contactId}/remove', params)

/** Loại người khỏi danh sách Đã xác nhận (lý do); gỡ dấu tiếp xúc, công việc đo nhiệt độ Chưa đến hạn bị 005 hủy (FR-061) */
export const postContactListsByIdContactsByContactIdExclude = (params) => request('POST', '/contact-lists/{id}/contacts/{contactId}/exclude', params)

/** Xác nhận danh sách (≥ 1 người, hoặc noContacts kèm lý do) (FR-061, FR-062, BR-M05-11) */
export const postContactListsByIdConfirm = (params) => request('POST', '/contact-lists/{id}/confirm', params)

/** Người thân cần liên hệ trực tiếp (Hành chính; chỉ họ tên, quan hệ, số điện thoại, nội dung cần báo) (FR-062, BR-M05-24) */
export const getIncidentsByIdFamilyContactsToNotify = (params) => request('GET', '/incidents/{id}/family-contacts-to-notify', params)

/** Vùng đang khoanh và lịch sử (Bác sĩ, Quản lý viện, Trưởng tầng, Điều dưỡng, Hành chính) (FR-064, FR-080) */
export const getIsolations = (params) => request('GET', '/isolations', params)

/** Khoanh vùng (Bác sĩ, Quản lý viện; tầng hoặc một khu vực; lý do; gắn ≥ 1 sự cố lây nhiễm chưa đóng) (FR-063, FR-064, FR-065, BR-M05-11) */
export const postIsolations = (params) => request('POST', '/isolations', params)

/** Gắn thêm sự cố lây nhiễm chưa đóng vào vùng (FR-066) */
export const postIsolationsByIdAttachIncident = (params) => request('POST', '/isolations/{id}/attach-incident', params)

/** Gỡ khoanh vùng (Bác sĩ, Quản lý viện; lý do) (FR-064, BR-M05-12) */
export const postIsolationsByIdLift = (params) => request('POST', '/isolations/{id}/lift', params)

/** Giấy phép cơ sở và lịch sử (Quản lý viện, Bác sĩ) (FR-070, FR-075) */
export const getFacilityLicenses = (params) => request('GET', '/facility-licenses', params)

/** Lưu giấy phép mới (gia hạn, đổi phạm vi, sửa sai thay thế bản cũ; Quản lý viện) (FR-070, FR-071, FR-075, BR-M06-05) */
export const postFacilityLicenses = (params) => request('POST', '/facility-licenses', params)

/** Số liên hệ của viện (FR-070) */
export const getFacilityContact = (params) => request('GET', '/facility-contact', params)

/** Đổi số liên hệ của viện (Quản lý viện; nhật ký) (FR-070, BR-M05-24) */
export const putFacilityContact = (params) => request('PUT', '/facility-contact', params)

/** Bản ghi khám và điều trị (FR-072, FR-080) */
export const getResidentsByResidentIdExaminations = (params) => request('GET', '/residents/{residentId}/examinations', params)

/** Ghi khám, đánh giá, chẩn đoán, chỉ định, điều trị, kết quả (FR-072, FR-073, BR-M06-05) */
export const postResidentsByResidentIdExaminations = (params) => request('POST', '/residents/{residentId}/examinations', params)

/** Người cao tuổi bấm nút SOS; tạo hoặc gộp cảnh báo Khẩn cấp (FR-026, FR-047d, FR-047e, BR-M05-27) */
export const postSos = (params) => request('POST', '/sos', params)

/** Trạng thái cảnh báo SOS đang mở gần nhất của người cao tuổi (FR-047f) */
export const getSosCurrent = (params) => request('GET', '/sos/current', params)

/** Người cao tuổi chọn nhanh nơi đang ở sau khi bấm (một lần) (FR-047f) */
export const postSosByIdPlace = (params) => request('POST', '/sos/{id}/place', params)

/** App người cao tuổi gửi vị trí GPS cho lần bấm SOS (bổ sung 2026-10-22, Q-373) (FR-047h, BR-M05-28) */
export const postSosByIdLocation = (params) => request('POST', '/sos/{id}/location', params)

/** Nhân viên đang trong ca bấm nút hỗ trợ khẩn cấp; tạo hoặc gộp cảnh báo Khẩn cấp STAFF_ASSIST_REQUEST (FR-026, FR-047i, BR-M05-29) */
export const postEmergencyAssist = (params) => request('POST', '/emergency-assist', params)

/** Trạng thái cảnh báo nút hỗ trợ đang mở gần nhất của người bấm (đang tới, đã tới) (FR-047j) */
export const getEmergencyAssistCurrent = (params) => request('GET', '/emergency-assist/current', params)

/** Người bấm chọn loại sự việc, tầng, nơi, người cao tuổi sau khi gửi (mỗi trường đặt một lần) (FR-047i) */
export const postEmergencyAssistByIdDetails = (params) => request('POST', '/emergency-assist/{id}/details', params)

/** App nhân viên gửi vị trí GPS cho lần bấm nút hỗ trợ (như /sos/{id}/location) (FR-047h, BR-M05-28) */
export const postEmergencyAssistByIdLocation = (params) => request('POST', '/emergency-assist/{id}/location', params)

/** Người phụ trách ghi "Đã tới nơi" (cảnh báo từ nút SOS hoặc nút hỗ trợ; một lần; không đổi trạng thái) (FR-047j, FR-029, BR-M05-30) */
export const postAlertsByIdMarkArrived = (params) => request('POST', '/alerts/{id}/mark-arrived', params)

/** Thông tin đọc cho tổng đài 115 (cảnh báo Khẩn cấp) (FR-047k) */
export const getAlertsByIdCall115Info = (params) => request('GET', '/alerts/{id}/call-115-info', params)

/** Ghi lượt bấm "Gọi 115" trước khi app mở trình quay số (cảnh báo Khẩn cấp) (FR-047k, BR-M05-31) */
export const postAlertsByIdCall115 = (params) => request('POST', '/alerts/{id}/call-115', params)

/** Thông tin đọc cho tổng đài 115 (sự cố Khẩn cấp) (FR-047k) */
export const getIncidentsByIdCall115Info = (params) => request('GET', '/incidents/{id}/call-115-info', params)

/** Ghi lượt bấm "Gọi 115" (sự cố Khẩn cấp) (FR-047k, FR-048b, BR-M05-31) */
export const postIncidentsByIdCall115 = (params) => request('POST', '/incidents/{id}/call-115', params)

/** Người bấm chọn kết quả gọi (một lần); Đã gọi được thì thêm diễn biến CALL_EMERGENCY vào sự cố liên kết (FR-047k, FR-048b) */
export const postCall115AttemptsByIdResult = (params) => request('POST', '/call-115-attempts/{id}/result', params)

/** Thông tin hiện ở màn lệnh Chuyển viện (nguyện vọng khi điều trị, nguyện vọng cuối đời, người liên hệ chính); không gợi ý cơ sở (FR-049) */
export const getIncidentsByIdTransferOutInfo = (params) => request('GET', '/incidents/{id}/transfer-out/info', params)

/** Tải phiếu tóm tắt chuyển viện PDF đã lưu (đúng file đã dựng trong lệnh) (FR-050h, BR-M05-32) */
export const getTransferSummariesByIdDocument = (params) => request('GET', '/transfer-summaries/{id}/document', params)

/** In lại phiếu; trả file đã lưu có dấu "Bản in lại"; ghi lần in và nhật ký (FR-050h, BR-M05-32) */
export const postTransferSummariesByIdReprint = (params) => request('POST', '/transfer-summaries/{id}/reprint', params)

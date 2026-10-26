// File sinh bởi frontend/scripts/gen-api.mjs từ hợp đồng; không sửa tay.

// Nguồn: specs/006-medication-management/contracts (Elder Care – Feature 006 (quản lý thuốc))
import { request } from './client.js'

/** Tra danh mục thuốc (FR-001, FR-003) */
export const getMedications = (params) => request('GET', '/medications', params)

/** Thêm thuốc (Quản lý viện) (FR-001, FR-002) */
export const postMedications = (params) => request('POST', '/medications', params)

/** Xem thuốc kèm ETag để sửa (bổ sung khi dịch 2026-10-26 cho If-Match của PUT) (FR-001, FR-002) */
export const getMedicationsById = (params) => request('GET', '/medications/{id}', params)

/** Sửa thuốc (Quản lý viện) (FR-002) */
export const putMedicationsById = (params) => request('PUT', '/medications/{id}', params)

/** Ngừng hiệu lực thuốc (FR-002) */
export const postMedicationsByIdDeactivate = (params) => request('POST', '/medications/{id}/deactivate', params)

/** Danh sách và lịch sử đơn (chuỗi thay thế) (FR-016a, FR-049, FR-009) */
export const getResidentsByResidentIdPrescriptions = (params) => request('GET', '/residents/{residentId}/prescriptions', params)

/** Nhập đơn (nội bộ hoặc bên ngoài) (FR-004, FR-005, FR-006, FR-007, FR-008, FR-010, FR-011, FR-037, BR-M07-05, BR-M07-06) */
export const postResidentsByResidentIdPrescriptions = (params) => request('POST', '/residents/{residentId}/prescriptions', params)

/** Xem trước kết quả kiểm tra trùng hoạt chất và dị ứng (không ghi) (FR-007, FR-008) */
export const postResidentsByResidentIdPrescriptionsCheck = (params) => request('POST', '/residents/{residentId}/prescriptions/check', params)

/** Xem đơn, kèm vấn đề đã xác nhận, căn cứ, lô gắn, allowedCommands[] (FR-004, FR-013, FR-016a) */
export const getPrescriptionsById = (params) => request('GET', '/prescriptions/{id}', params)

/** Sửa ghi chú hướng dẫn (trường duy nhất sửa được) (FR-014) */
export const patchPrescriptionsByIdInstructionNote = (params) => request('PATCH', '/prescriptions/{id}/instruction-note', params)

/** Tạm dừng đơn (FR-011, FR-013) */
export const postPrescriptionsByIdSuspend = (params) => request('POST', '/prescriptions/{id}/suspend', params)

/** Tiếp tục đơn (FR-011, FR-037) */
export const postPrescriptionsByIdResume = (params) => request('POST', '/prescriptions/{id}/resume', params)

/** Ngừng đơn (ngay hoặc từ một thời điểm tương lai) (FR-011, FR-015, FR-016, FR-037, BR-M07-07, BR-M07-15) */
export const postPrescriptionsByIdDiscontinue = (params) => request('POST', '/prescriptions/{id}/discontinue', params)

/** Đổi liều / đổi thuốc (ngừng đơn cũ và tạo đơn thay thế trong một lệnh) (FR-011, FR-016, FR-016a, FR-007, FR-043, BR-M07-06, BR-M07-07) */
export const postPrescriptionsByIdChangeDosage = (params) => request('POST', '/prescriptions/{id}/change-dosage', params)

/** Gia hạn (ngày kết thúc muộn hơn hoặc bỏ ngày kết thúc) (FR-015, FR-017, BR-M07-08) */
export const postPrescriptionsByIdExtend = (params) => request('POST', '/prescriptions/{id}/extend', params)

/** Lịch thuốc của một người cao tuổi trong khoảng (tính từ đơn, ghép liều đã vật hóa) (FR-018, FR-019, FR-020, FR-021, FR-049, BR-M07-01) */
export const getResidentsByResidentIdMedicationSchedule = (params) => request('GET', '/residents/{residentId}/medication-schedule', params)

/** Liều của vòng phát thuốc cho Điều dưỡng hiện tại (được phân công, liều chung của tầng, Người phụ trách ca) (FR-026, FR-026a, FR-028) */
export const getMedicationDosesRounds = (params) => request('GET', '/medication-doses/rounds', params)

/** Xem liều, kèm danh sách lô được dùng (không chọn sẵn), kết quả, chuỗi đính chính (FR-020, FR-044) */
export const getMedicationDosesByDoseKey = (params) => request('GET', '/medication-doses/{doseKey}', params)

/** Xác nhận Đã dùng (Đến giờ, Trễ, Mang theo) (FR-021, FR-026, FR-026a, FR-027, FR-029, FR-031, FR-033, FR-044, FR-048a, BR-M07-11, BR-M07-12, BR-M07-13, BR-M07-14) */
export const postMedicationDosesByDoseKeyAdminister = (params) => request('POST', '/medication-doses/{doseKey}/administer', params)

/** Ghi Từ chối (bắt buộc lý do) (FR-021, FR-026, FR-032, BR-M07-13) */
export const postMedicationDosesByDoseKeyRefuse = (params) => request('POST', '/medication-doses/{doseKey}/refuse', params)

/** Ghi Không thực hiện (bắt buộc lý do) (FR-021, FR-023, FR-026, FR-027) */
export const postMedicationDosesByDoseKeySkip = (params) => request('POST', '/medication-doses/{doseKey}/skip', params)

/** Bản ghi thuốc kiểm soát đặc biệt "ghi sau mất kết nối" chờ xem lại trong phạm vi (FR-029) */
export const getDoseReviews = (params) => request('GET', '/dose-reviews', params)

/** Gắn kết quả xem lại (Điều dưỡng khác người ghi) (FR-029) */
export const postDoseReviewsByIdResult = (params) => request('POST', '/dose-reviews/{id}/result', params)

/** Các lần dùng của đơn PRN (cả chuỗi thay thế) và thời điểm sớm nhất được dùng tiếp (FR-024, FR-025) */
export const getPrescriptionsByIdPrnAdministrations = (params) => request('GET', '/prescriptions/{id}/prn-administrations', params)

/** Ghi một lần dùng PRN (FR-024, FR-025, FR-025a, FR-027, BR-M07-04) */
export const postPrescriptionsByIdPrnAdministrations = (params) => request('POST', '/prescriptions/{id}/prn-administrations', params)

/** Các phiếu đối chiếu; kèm hạn đối chiếu hiện hành và dữ liệu nguồn (đơn chờ đối chiếu, thuốc đang dùng khi tiếp nhận) để hiển thị (FR-034, FR-034a, FR-049) */
export const getResidentsByResidentIdMedicationReconciliations = (params) => request('GET', '/residents/{residentId}/medication-reconciliations', params)

/** Lập phiếu (Bác sĩ, Điều dưỡng) (FR-034, FR-034a, FR-034b, FR-035, BR-M07-09, BR-M07-25) */
export const postResidentsByResidentIdMedicationReconciliations = (params) => request('POST', '/residents/{residentId}/medication-reconciliations', params)

/** Ghi lần tiếp nhận thuốc cung ứng (Điều dưỡng) cho một dòng của yêu cầu mua sắm loại "thuốc" đã ghi đã mua (FR-054, FR-055, BR-M07-24, BR-M11-17) */
export const postSuppliedMedicationIntakes = (params) => request('POST', '/supplied-medication-intakes', params)

/** Các lần tiếp nhận theo yêu cầu mua sắm (FR-054) */
export const getSuppliedMedicationIntakes = (params) => request('GET', '/supplied-medication-intakes', params)

/** Xem phiếu kèm dòng, dấu "thuốc đã bỏ khỏi hồ sơ" (tính lúc đọc), lịch sử, allowedCommands[] (FR-035, FR-038, FR-034a) */
export const getMedicationReconciliationsById = (params) => request('GET', '/medication-reconciliations/{id}', params)

/** Thay danh sách dòng và quyết định (chỉ khi Mở) (FR-035, FR-036, FR-007) */
export const putMedicationReconciliationsByIdItems = (params) => request('PUT', '/medication-reconciliations/{id}/items', params)

/** Gửi xác nhận (FR-034, FR-034a, FR-035) */
export const postMedicationReconciliationsByIdSubmit = (params) => request('POST', '/medication-reconciliations/{id}/submit', params)

/** Trả lại về Mở (bắt buộc lý do) (FR-035) */
export const postMedicationReconciliationsByIdReturn = (params) => request('POST', '/medication-reconciliations/{id}/return', params)

/** Xác nhận phiếu và áp quyết định từng dòng trong một lệnh (FR-036, FR-038, FR-034a, BR-M07-09) */
export const postMedicationReconciliationsByIdConfirm = (params) => request('POST', '/medication-reconciliations/{id}/confirm', params)

/** Lập đề nghị đính chính phiếu Đã xác nhận (người thực hiện hoặc người xác nhận của phiếu) (FR-039, BR-M07-17) */
export const postMedicationReconciliationsByIdProposeCorrection = (params) => request('POST', '/medication-reconciliations/{id}/propose-correction', params)

/** Xác nhận đề nghị đính chính phiếu (tạo bản đính chính của 000 trong cùng lệnh) (FR-039) */
export const postReconciliationCorrectionsByIdConfirm = (params) => request('POST', '/reconciliation-corrections/{id}/confirm', params)

/** Trả lại đề nghị đính chính phiếu (FR-039) */
export const postReconciliationCorrectionsByIdReturn = (params) => request('POST', '/reconciliation-corrections/{id}/return', params)

/** Danh sách lô thuốc gia đình gửi, kèm số ngày dùng còn lại theo nhóm dùng chung (FR-040, FR-046, FR-049) */
export const getResidentsByResidentIdFamilyMedications = (params) => request('GET', '/residents/{residentId}/family-medications', params)

/** Tiếp nhận thuốc gia đình gửi (một lô) (FR-040, FR-041, FR-045) */
export const postResidentsByResidentIdFamilyMedications = (params) => request('POST', '/residents/{residentId}/family-medications', params)

/** Xem lô kèm đơn gắn và biến động số lượng (FR-041, FR-045) */
export const getFamilyMedicationsById = (params) => request('GET', '/family-medications/{id}', params)

/** Xác nhận sử dụng, gắn một hoặc nhiều đơn nguồn gia đình gửi (FR-040, FR-041, FR-043, FR-046, BR-M07-10, BR-M07-18) */
export const postFamilyMedicationsByIdConfirmUse = (params) => request('POST', '/family-medications/{id}/confirm-use', params)

/** Chỉ giữ hộ (bắt buộc lý do) (FR-041, BR-M07-10) */
export const postFamilyMedicationsByIdHoldOnly = (params) => request('POST', '/family-medications/{id}/hold-only', params)

/** Ngừng sử dụng (về Chỉ giữ hộ, gỡ liên kết đơn) (FR-041) */
export const postFamilyMedicationsByIdStopUse = (params) => request('POST', '/family-medications/{id}/stop-use', params)

/** Kiểm kê điều chỉnh số lượng (rơi vỡ, đếm lại) (FR-045) */
export const postFamilyMedicationsByIdAdjustInventory = (params) => request('POST', '/family-medications/{id}/adjust-inventory', params)

/** Hoàn trả (từ Chờ đối chiếu, Chỉ giữ hộ; số lượng bằng số còn) (FR-041, FR-045, BR-M07-19) */
export const postFamilyMedicationsByIdReturn = (params) => request('POST', '/family-medications/{id}/return', params)

/** Hủy theo yêu cầu gia đình (người đại diện yêu cầu; có người chứng kiến) (FR-041, FR-045, BR-M07-19) */
export const postFamilyMedicationsByIdDispose = (params) => request('POST', '/family-medications/{id}/dispose', params)

/** Thuốc cần mang cho khoảng vắng hoặc chuyến đi (đơn có liều Mang theo, đơn PRN, lô dùng được) (FR-048a, FR-048b) */
export const getResidentsByResidentIdMedicationsToBring = (params) => request('GET', '/residents/{residentId}/medications-to-bring', params)

/** Các lần giao thuốc mang theo (FR-048a) */
export const getResidentsByResidentIdMedicationHandovers = (params) => request('GET', '/residents/{residentId}/medication-handovers', params)

/** Giao thuốc mang theo (FR-048a, BR-M07-11, BR-M07-14, BR-M07-22) */
export const postResidentsByResidentIdMedicationHandovers = (params) => request('POST', '/residents/{residentId}/medication-handovers', params)

/** Nhận lại thuốc mang theo (kể cả số lượng 0 kèm lý do) (FR-048a, FR-047) */
export const postMedicationHandoversByIdReturn = (params) => request('POST', '/medication-handovers/{id}/return', params)

/** Lệnh "Không mang thuốc" cho một chuyến đi (Điều dưỡng) (FR-048b, BR-M07-03) */
export const postResidentsByResidentIdNoMedicationOrders = (params) => request('POST', '/residents/{residentId}/no-medication-orders', params)

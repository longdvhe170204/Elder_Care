// File sinh bởi frontend/scripts/gen-api.mjs từ hợp đồng; không sửa tay.

// Nguồn: specs/001-elder-profile-assessment/contracts (Elder Care – Feature 001 (hồ sơ và đánh giá người cao tuổi))
import { request } from './client.js'

/** Tra cứu hồ sơ trong phạm vi (UC-02) (FR-007, BR-M15-01) */
export const getResidents = (params) => request('GET', '/residents', params)

/** Tạo hồ sơ người cao tuổi (Hành chính) (FR-001, FR-002, FR-003, FR-004, FR-005, BR-M01-04) */
export const postResidents = (params) => request('POST', '/residents', params)

/** Xem hồ sơ (FR-007, FR-008, FR-009, FR-022, FR-023e, BR-M01-08, BR-M15-01) */
export const getResidentsById = (params) => request('GET', '/residents/{id}', params)

/** Cập nhật thông tin cá nhân (Hành chính) (FR-006, BR-M01-05) */
export const postResidentsByIdUpdatePersonalInfo = (params) => request('POST', '/residents/{id}/update-personal-info', params)

/** Chuỗi hồ sơ trước của cùng người (FR-004) (FR-004) */
export const getResidentsByIdLinkedProfiles = (params) => request('GET', '/residents/{id}/linked-profiles', params)

/** Lịch sử trạng thái (FR-046, BR-M01-04) */
export const getResidentsByIdStatusHistory = (params) => request('GET', '/residents/{id}/status-history', params)

/** Xem trước điều kiện của một lệnh (không đổi dữ liệu) (FR-045) */
export const getResidentsByIdConditionsByCommand = (params) => request('GET', '/residents/{id}/conditions/{command}', params)

/** Hoàn tất tiếp nhận (Đang tiếp nhận → Đang lưu trú) (FR-012, FR-023d, FR-034a, FR-044, FR-045, FR-046, FR-047, BR-M01-04, BR-M01-06, BR-M02-22) */
export const postResidentsByIdCompleteAdmission = (params) => request('POST', '/residents/{id}/complete-admission', params)

/** Hủy tiếp nhận (FR-044, FR-046, FR-047) */
export const postResidentsByIdCancelAdmission = (params) => request('POST', '/residents/{id}/cancel-admission', params)

/** Cho tạm vắng (Hành chính, Trưởng tầng; không qua duyệt) (FR-044, FR-047, FR-047a, BR-M01-06, BR-M10-03) */
export const postResidentsByIdGrantTemporaryAbsence = (params) => request('POST', '/residents/{id}/grant-temporary-absence', params)

/** Ghi nhận trở về (từ Tạm vắng hoặc Điều trị tại bệnh viện) (FR-040, FR-041, FR-044, FR-047a, BR-M01-02, BR-M07-09) */
export const postResidentsByIdRecordReturn = (params) => request('POST', '/residents/{id}/record-return', params)

/** Chuyển viện (Điều dưỡng, Bác sĩ) (FR-044, FR-047, BR-M01-06) */
export const postResidentsByIdTransferOut = (params) => request('POST', '/residents/{id}/transfer-out', params)

/** Kết thúc lưu trú (Hành chính, Bác sĩ) (FR-044, FR-045, FR-047) */
export const postResidentsByIdEndResidency = (params) => request('POST', '/residents/{id}/end-residency', params)

/** Ghi nhận qua đời (FR-044, FR-047, FR-047b) */
export const postResidentsByIdRecordDeath = (params) => request('POST', '/residents/{id}/record-death', params)

/** Mức chăm sóc tại một ngày (mặc định hôm nay) kèm căn cứ (FR-052, FR-053) */
export const getResidentsByIdCareLevel = (params) => request('GET', '/residents/{id}/care-level', params)

/** Lịch sử mức chăm sóc (FR-053) */
export const getResidentsByIdCareLevelHistory = (params) => request('GET', '/residents/{id}/care-level-history', params)

/** Bản đồng ý hiện hành và lịch sử (FR-010, FR-016) */
export const getResidentsByIdConsents = (params) => request('GET', '/residents/{id}/consents', params)

/** Ghi nhận bản đồng ý (Hành chính); thay thế bản Hiệu lực cũ trong cùng lệnh (FR-010, FR-011, FR-013, FR-014a, BR-M01-08) */
export const postResidentsByIdConsents = (params) => request('POST', '/residents/{id}/consents', params)

/** Rút lại đồng ý (được làm cả trên hồ sơ trạng thái cuối) (FR-014, FR-014a, FR-015, BR-M01-05, BR-M01-08) */
export const postConsentsByIdWithdraw = (params) => request('POST', '/consents/{id}/withdraw', params)

/** Hồ sơ sức khỏe ban đầu (kèm đính chính hiện hành) (FR-017) */
export const getResidentsByIdInitialHealthRecord = (params) => request('GET', '/residents/{id}/initial-health-record', params)

/** Lập hoặc sửa bản nháp (Bác sĩ, Điều dưỡng); If-Match khi sửa (FR-017) */
export const putResidentsByIdInitialHealthRecord = (params) => request('PUT', '/residents/{id}/initial-health-record', params)

/** Bác sĩ xác nhận hồ sơ sức khỏe ban đầu (FR-017) */
export const postResidentsByIdInitialHealthRecordConfirm = (params) => request('POST', '/residents/{id}/initial-health-record/confirm', params)

/** Đính chính (chuyển tới CorrectionApi của 000; trạng thái cuối luôn qua phê duyệt) (FR-017, FR-049a) */
export const postInitialHealthRecordsByIdCorrect = (params) => request('POST', '/initial-health-records/{id}/correct', params)

/** Danh sách mục sức khỏe (người thân chỉ thấy mục Hiệu lực, FR-022) (FR-018, FR-022) */
export const getResidentsByIdHealthItems = (params) => request('GET', '/residents/{id}/health-items', params)

/** Ghi nhận mục dị ứng, bệnh nền, tiền sử bệnh (Bác sĩ, Điều dưỡng) (FR-018, FR-018a, FR-019, FR-020, FR-021, BR-M01-07, BR-M01-11, BR-M01-12) */
export const postResidentsByIdHealthItems = (params) => request('POST', '/residents/{id}/health-items', params)

/** Loại trừ mục (Bác sĩ, Điều dưỡng); không có lệnh sửa hay xóa (DBR-04) (FR-019, FR-020, BR-M01-07) */
export const postHealthItemsByIdExclude = (params) => request('POST', '/health-items/{id}/exclude', params)

/** Phiên bản Hiệu lực và lịch sử; Hành chính, Kế toán chỉ nhận hasEndOfLifeWish (FR-023e) (FR-023b, FR-023e) */
export const getResidentsByIdEndOfLifeWishes = (params) => request('GET', '/residents/{id}/end-of-life-wishes', params)

/** Ghi phiên bản mới (Bác sĩ, Điều dưỡng); phiên bản cũ chuyển Được thay thế (FR-023a, FR-023b, FR-023c, FR-023g, FR-023h) */
export const postResidentsByIdEndOfLifeWishes = (params) => request('POST', '/residents/{id}/end-of-life-wishes', params)

/** Lịch sử đánh giá (chỉ vai trò có quyền xem Đánh giá; Nhân viên chăm sóc 403) (FR-008, FR-025) */
export const getResidentsByIdAssessments = (params) => request('GET', '/residents/{id}/assessments', params)

/** Tạo lần đánh giá ở Nháp (Bác sĩ, Điều dưỡng) (FR-026, FR-034, FR-035, BR-M01-13) */
export const postResidentsByIdAssessments = (params) => request('POST', '/residents/{id}/assessments', params)

/** Chi tiết lần đánh giá kèm dữ liệu tham khảo (hồ sơ ban đầu, mục Hiệu lực, chỉ số gần nhất) (FR-024) */
export const getAssessmentsById = (params) => request('GET', '/assessments/{id}', params)

/** Sửa nháp (người tạo hoặc Bác sĩ) (FR-025, FR-029, FR-032) */
export const putAssessmentsById = (params) => request('PUT', '/assessments/{id}', params)

/** Bác sĩ xác nhận (FR-028, FR-031, FR-033, FR-035, FR-036, FR-036a, FR-038, FR-043, BR-M01-03, BR-M01-09, BR-M01-10) */
export const postAssessmentsByIdConfirm = (params) => request('POST', '/assessments/{id}/confirm', params)

/** Hủy nháp (người tạo hoặc Bác sĩ) (FR-026, BR-M01-13) */
export const postAssessmentsByIdDiscardDraft = (params) => request('POST', '/assessments/{id}/discard-draft', params)

/** Đính chính lần đánh giá đã xác nhận (qua CorrectionApi của 000) (FR-032, FR-049a) */
export const postAssessmentsByIdCorrect = (params) => request('POST', '/assessments/{id}/correct', params)

/** Trạng thái cần / quá hạn đánh giá lại và các căn cứ đã báo (FR-041, FR-042, FR-043, BR-M01-02) */
export const getResidentsByIdReassessment = (params) => request('GET', '/residents/{id}/reassessment', params)

/** Danh sách người cao tuổi cần đánh giá lại (Bác sĩ), sắp theo hạn (FR-039, FR-041) */
export const getReassessmentsDue = (params) => request('GET', '/reassessments/due', params)

/** Danh sách mục (care-levels cố định, chỉ đọc) */
export const getCatalogsByType = (params) => request('GET', '/catalogs/{type}', params)

/** Tạo mục (quyền theo data-model mục 5) (FR-018, FR-018a, BR-M01-11, BR-M15-06) */
export const postCatalogsByType = (params) => request('POST', '/catalogs/{type}', params)

/** Sửa mục */
export const putCatalogsByTypeById = (params) => request('PUT', '/catalogs/{type}/{id}', params)

/** Ngừng hiệu lực (mục đã được tham chiếu không xóa được) */
export const postCatalogsByTypeByIdDeactivate = (params) => request('POST', '/catalogs/{type}/{id}/deactivate', params)

/** Kích hoạt lại (trùng tên mục đang Hiệu lực thì 409) */
export const postCatalogsByTypeByIdReactivate = (params) => request('POST', '/catalogs/{type}/{id}/reactivate', params)

/** Tải tệp lên (PDF, JPEG, PNG; tối đa 10 MB); tệp chưa gắn sau 24 giờ bị dọn */
export const postFiles = (params) => request('POST', '/files', params)

/** Tải tệp xuống; quyền theo bản ghi chủ sở hữu (FilePermissionProvider) */
export const getFilesById = (params) => request('GET', '/files/{id}', params)

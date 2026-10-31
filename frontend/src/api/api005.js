// File sinh bởi frontend/scripts/gen-api.mjs từ hợp đồng; không sửa tay.

// Nguồn: specs/005-daily-care-tasks/contracts (Elder Care – Feature 005 (chăm sóc hằng ngày))
import { request } from './client.js'

/** Lịch sử phiên bản kế hoạch của một người cao tuổi (FR-007, FR-008, FR-009) */
export const getResidentsByResidentIdCarePlans = (params) => request('GET', '/residents/{residentId}/care-plans', params)

/** Lập phiên bản Nháp (Điều dưỡng) (FR-001, FR-003, FR-004) */
export const postResidentsByResidentIdCarePlans = (params) => request('POST', '/residents/{residentId}/care-plans', params)

/** Xem phiên bản kèm mục (FR-001, FR-002, FR-007) */
export const getCarePlansById = (params) => request('GET', '/care-plans/{id}', params)

/** Thay toàn bộ danh sách mục của bản Nháp (FR-002, FR-003, FR-004, BR-M04-19) */
export const putCarePlansByIdItems = (params) => request('PUT', '/care-plans/{id}/items', params)

/** Thay toàn bộ danh sách thỏa thuận lựa chọn có rủi ro của bản Nháp (bổ sung 2026-10-18) (FR-002a, FR-004, BR-M04-19, BR-M01-14, BR-M01-15) */
export const putCarePlansByIdRiskAgreements = (params) => request('PUT', '/care-plans/{id}/risk-agreements', params)

/** Gửi duyệt kèm ngày hiệu lực đề xuất (FR-003, FR-005) */
export const postCarePlansByIdSubmit = (params) => request('POST', '/care-plans/{id}/submit', params)

/** Duyệt (Bác sĩ; Điều dưỡng được gán quyền) (FR-003, FR-004, FR-005, BR-M04-19) */
export const postCarePlansByIdApprove = (params) => request('POST', '/care-plans/{id}/approve', params)

/** Trả lại về Nháp (bắt buộc lý do) (FR-003) */
export const postCarePlansByIdReturn = (params) => request('POST', '/care-plans/{id}/return', params)

/** Hủy phiên bản Nháp hoặc Chờ duyệt (người lập, người có quyền duyệt) (FR-003) */
export const postCarePlansByIdCancel = (params) => request('POST', '/care-plans/{id}/cancel', params)

/** Thu hồi duyệt trước ngày hiệu lực (FR-003) */
export const postCarePlansByIdWithdrawApproval = (params) => request('POST', '/care-plans/{id}/withdraw-approval', params)

/** So sánh hai phiên bản (mục thêm, bớt, đổi) (FR-008) */
export const getCarePlansCompare = (params) => request('GET', '/care-plans/compare', params)

/** Danh mục loại công việc (FR-038) */
export const getTaskTypes = (params) => request('GET', '/task-types', params)

/** Tạo loại công việc (Quản lý viện) (FR-038) */
export const postTaskTypes = (params) => request('POST', '/task-types', params)

/** Sửa loại công việc (chỉ áp cho mục, công việc tạo sau; chứng chỉ áp ngay) (FR-038) */
export const putTaskTypesById = (params) => request('PUT', '/task-types/{id}', params)

/** Ngừng hiệu lực loại công việc (FR-038) */
export const postTaskTypesByIdDeactivate = (params) => request('POST', '/task-types/{id}/deactivate', params)

/** Danh mục loại lý do Không thực hiện (bổ sung 2026-10-18) (FR-028a) */
export const getNonPerformanceReasonTypes = (params) => request('GET', '/non-performance-reason-types', params)

/** Tạo loại lý do (Quản lý viện) (FR-028a) */
export const postNonPerformanceReasonTypes = (params) => request('POST', '/non-performance-reason-types', params)

/** Sửa tên loại lý do; loại cố định 409 REASON_TYPE_FIXED (FR-028a) */
export const putNonPerformanceReasonTypesById = (params) => request('PUT', '/non-performance-reason-types/{id}', params)

/** Ngừng hiệu lực loại lý do; loại cố định 409 REASON_TYPE_FIXED (FR-028a) */
export const postNonPerformanceReasonTypesByIdDeactivate = (params) => request('POST', '/non-performance-reason-types/{id}/deactivate', params)

/** Giá trị kết quả (chất lượng ngủ, tâm trạng, hành vi) kèm nhóm quy tắc (FR-034, FR-042) */
export const getResultValues = (params) => request('GET', '/result-values', params)

/** Thêm giá trị (Quản lý viện) (FR-034) */
export const postResultValues = (params) => request('POST', '/result-values', params)

/** Sửa giá trị kết quả (FR-034, FR-038) */
export const putResultValuesById = (params) => request('PUT', '/result-values/{id}', params)

/** Ngừng hiệu lực giá trị kết quả (FR-034, FR-038) */
export const postResultValuesByIdDeactivate = (params) => request('POST', '/result-values/{id}/deactivate', params)

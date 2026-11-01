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

/** Trạng thái có mặt theo ngày của khu bán trú (FR-012, FR-013, FR-016, FR-049) */
export const getDayCareAttendances = (params) => request('GET', '/day-care-attendances', params)

/** Điểm danh đến (Hành chính, Nhân viên chăm sóc, Điều dưỡng) (FR-013, FR-018, FR-016, BR-M04-02) */
export const postDayCareAttendancesCheckIn = (params) => request('POST', '/day-care-attendances/check-in', params)

/** Điểm danh về với bản ghi đón của 012 (FR-013, FR-019, BR-M04-02) */
export const postDayCareAttendancesByIdCheckOut = (params) => request('POST', '/day-care-attendances/{id}/check-out', params)

/** Đính chính giờ đến/về (loại M04.DIEM_DANH_BAN_TRU) (FR-015) */
export const postDayCareAttendanceRecordsByIdCorrect = (params) => request('POST', '/day-care-attendance-records/{id}/correct', params)

/** Nhận công việc chung, hoặc Người phụ trách ca nhận việc về mình (FR-031, FR-033, BR-M04-13, BR-M09-01) */
export const postCareTasksByIdClaim = (params) => request('POST', '/care-tasks/{id}/claim', params)

/** Checklist của tôi trong ca (FR-029, FR-030, FR-031, FR-035) */
export const getChecklist = (params) => request('GET', '/checklist', params)

/** Ghi nhận hoàn thành (FR-026, FR-033, FR-034, FR-035, FR-036, FR-040a, FR-041, FR-044, BR-M04-06, BR-M04-10, BR-M04-12) */
export const postCareTasksByIdRecord = (params) => request('POST', '/care-tasks/{id}/record', params)

/** Ghi nhận không thực hiện (bắt buộc loại lý do; ghi chú tùy chọn) (FR-026, FR-027, FR-028a, FR-033, FR-044) */
export const postCareTasksByIdNotPerform = (params) => request('POST', '/care-tasks/{id}/not-perform', params)

/** Ghi nhận phát sinh (TT, ĐD, CS) (FR-025, FR-026, FR-035) */
export const postAdHocCareRecordings = (params) => request('POST', '/ad-hoc-care-recordings', params)

/** Lịch sử kết quả ghi nhận (gồm Bác sĩ chỉ xem) (FR-037, FR-049) */
export const getResidentsByResidentIdCareResults = (params) => request('GET', '/residents/{residentId}/care-results', params)

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

/** Dữ liệu nguồn của ca để người lập đối chiếu (FR-017) */
export const getShiftsByShiftIdSourceData = (params) => request('GET', '/shifts/{shiftId}/source-data', params)

/** Công việc đã lập của ca (Người phụ trách ca, Trưởng tầng, Quản lý viện) (FR-017, FR-045) */
export const getShiftsByShiftIdCareTasks = (params) => request('GET', '/shifts/{shiftId}/care-tasks', params)

/** Lập công việc của ca theo lô (FR-002, FR-006, FR-017, FR-018, FR-021, FR-023, FR-024, FR-029, BR-M04-01, BR-M04-02, BR-M04-24, BR-M04-26, BR-M04-28, BR-M09-01) */
export const postShiftsByShiftIdCareTasksPlan = (params) => request('POST', '/shifts/{shiftId}/care-tasks/plan', params)

/** Tạo công việc phát sinh (Trưởng tầng, Điều dưỡng) (FR-025, FR-023) */
export const postCareTasksAdHoc = (params) => request('POST', '/care-tasks/ad-hoc', params)

/** Lập công việc làm lại sau kiểm tra chất lượng Không đạt (người kiểm tra của 014) (FR-026a, BR-M04-23) */
export const postCareTasksByIdRework = (params) => request('POST', '/care-tasks/{id}/rework', params)

/** Xem công việc, kết quả và lịch sử xử lý (FR-023, FR-028, FR-046) */
export const getCareTasksById = (params) => request('GET', '/care-tasks/{id}', params)

/** Hủy công việc Chưa đến hạn (chỉ Trưởng tầng, bắt buộc lý do) (FR-026, FR-022, BR-M04-03) */
export const postCareTasksByIdCancel = (params) => request('POST', '/care-tasks/{id}/cancel', params)

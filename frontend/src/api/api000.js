// File sinh bởi frontend/scripts/gen-api.mjs từ hợp đồng; không sửa tay.

// Nguồn: specs/000-shared-foundation/contracts (Elder Care – Feature 000 (nền tảng dùng chung))
import { request } from './client.js'

/** Danh sách tham số với giá trị hiện hành và mặc định (FR-011) */
export const getParameters = (params) => request('GET', '/parameters', params)

/** Chi tiết một tham số (FR-011, FR-012) */
export const getParametersByCode = (params) => request('GET', '/parameters/{code}', params)

/** Đổi giá trị (kể cả khôi phục mặc định) (FR-013, FR-014, FR-015, FR-016, FR-020, FR-021, FR-022, BR-M15-04) */
export const postParametersByCodeChangeValue = (params) => request('POST', '/parameters/{code}/change-value', params)

/** Lịch sử giá trị; hoặc giá trị tại một thời điểm quá khứ (FR-012, FR-019) */
export const getParametersByCodeHistory = (params) => request('GET', '/parameters/{code}/history', params)

/** Lập yêu cầu (trạng thái Nháp) (FR-031, FR-032) */
export const postApprovalRequests = (params) => request('POST', '/approval-requests', params)

/** Danh sách yêu cầu trong phạm vi người dùng */
export const getApprovalRequests = (params) => request('GET', '/approval-requests', params)

/** Chi tiết yêu cầu */
export const getApprovalRequestsById = (params) => request('GET', '/approval-requests/{id}', params)

/** Sửa nội dung khi còn Nháp (FR-033) */
export const postApprovalRequestsByIdEditContent = (params) => request('POST', '/approval-requests/{id}/edit-content', params)

/** Gửi duyệt (Nháp → Chờ duyệt) (FR-033) */
export const postApprovalRequestsByIdSubmit = (params) => request('POST', '/approval-requests/{id}/submit', params)

/** Duyệt (Chờ duyệt → Đã duyệt (chờ hiệu lực)); áp dụng ngay nếu ngày hiệu lực ≤ hôm nay (FR-034, FR-034a, FR-035, FR-037, FR-040, BR-M15-01) */
export const postApprovalRequestsByIdApprove = (params) => request('POST', '/approval-requests/{id}/approve', params)

/** Từ chối (FR-034, FR-036, FR-040) */
export const postApprovalRequestsByIdReject = (params) => request('POST', '/approval-requests/{id}/reject', params)

/** Hủy (Nháp/Chờ duyệt bởi người yêu cầu; Đã duyệt bởi người có quyền duyệt) (FR-031, BR-M15-08) */
export const postApprovalRequestsByIdCancel = (params) => request('POST', '/approval-requests/{id}/cancel', params)

/** Chuỗi đính chính của một bản ghi gốc và giá trị hiện hành (FR-027, FR-028) */
export const getCorrectionsByRecordTypeByOriginalRecordId = (params) => request('GET', '/corrections/{recordType}/{originalRecordId}', params)

/** Tạo bản đính chính (cách A/B → 201; cách C → 202 kèm yêu cầu phê duyệt) (FR-023, FR-024, FR-025, FR-026, FR-029) */
export const postCorrectionsByRecordTypeByOriginalRecordId = (params) => request('POST', '/corrections/{recordType}/{originalRecordId}', params)

/** Tra cứu nhật ký toàn viện (chỉ Quản lý viện) (FR-049) */
export const getAuditLogs = (params) => request('GET', '/audit-logs', params)

/** Lịch sử thay đổi của một đối tượng (Quản lý viện; nhân viên trong phạm vi, có che trường) (FR-049, BR-M15-01, BR-M15-11) */
export const getAuditLogsEntitiesByEntityTypeByEntityId = (params) => request('GET', '/audit-logs/entities/{entityType}/{entityId}', params)

/** Đọc đồng hồ hệ thống (chỉ profile test/demo) */
export const getTestingClock = (params) => request('GET', '/testing/clock', params)

/** Đặt hoặc tua đồng hồ giả lập (chỉ profile test/demo) */
export const postTestingClock = (params) => request('POST', '/testing/clock', params)

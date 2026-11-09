// File sinh bởi frontend/scripts/gen-api.mjs từ hợp đồng; không sửa tay.

// Nguồn: specs/015-shift-rotation-swap/contracts (Elder Care – Feature 015 (đổi ca, nghỉ đột xuất))
import { request } from './client.js'

/** Yêu cầu đổi ca của tôi (người nhường, người nhận) hoặc cần tôi duyệt (FR-022) */
export const getShiftSwaps = (params) => request('GET', '/shift-swaps', params)

/** Lập yêu cầu đổi hai chiều hoặc nhận thay (người nhường) (FR-019, FR-021, FR-022, BR-M09-10) */
export const postShiftSwaps = (params) => request('POST', '/shift-swaps', params)

/** Ca của người nhận thỏa FR-019 để chọn Y (chỉ ngày, giờ, mẫu ca) (FR-020) */
export const getShiftSwapsRecipientShifts = (params) => request('GET', '/shift-swaps/recipient-shifts', params)

/** Kết quả kiểm tra FR-021 với dữ liệu hiện tại (người nhận chỉ thấy phần của mình; mục (g) chỉ người duyệt) (FR-021) */
export const getShiftSwapsByIdChecks = (params) => request('GET', '/shift-swaps/{id}/checks', params)

/** Lệnh trên yêu cầu đổi ca theo bảng FR-022 (FR-021, FR-022, FR-023, FR-024, BR-M09-10, BR-M09-01, BR-M09-03) */
export const postShiftSwapsByIdByCommand = (params) => request('POST', '/shift-swaps/{id}/{command}', params)

/** Yêu cầu nghỉ của tôi, hoặc cần tôi duyệt (FR-031) */
export const getLeaveRequests = (params) => request('GET', '/leave-requests', params)

/** Lập yêu cầu nghỉ (theo ca hoặc theo ngày; cho mình hoặc lập thay); tách theo phạm vi (FR-026, FR-026a) */
export const postLeaveRequests = (params) => request('POST', '/leave-requests', params)

/** Người phụ trách, phân công, chuyến đi chồng của từng ca (chỉ người duyệt) (FR-027) */
export const getLeaveRequestsByIdPreApproval = (params) => request('GET', '/leave-requests/{id}/pre-approval', params)

/** approve (kết quả từng ca/ngày; người phụ trách mới nếu có), reject (lý do), cancel (người nghỉ hoặc người lập thay) (FR-028, FR-029, FR-031) */
export const postLeaveRequestsByIdByCommand = (params) => request('POST', '/leave-requests/{id}/{command}', params)

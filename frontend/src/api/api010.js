// File sinh bởi frontend/scripts/gen-api.mjs từ hợp đồng; không sửa tay.

// Nguồn: specs/010-incurred-costs/contracts (Elder Care – Feature 010 (chi phí phát sinh))
import { request } from './client.js'

/** Tra cứu bản ghi nguồn tính phí theo người cao tuổi, loại sự kiện, khoảng ngày (không có bộ lọc "chưa lập khoản", không gợi ý) (FR-005, FR-005a, FR-005b, BR-M11-01) */
export const getCostsBillingSources = (params) => request('GET', '/costs/billing-sources', params)

/** Lập khoản theo sự kiện, một hoặc hàng loạt (Hành chính) (FR-002, FR-003, FR-005, FR-006, FR-007, FR-008, FR-009, FR-010, FR-011, FR-012, BR-M11-01, BR-M11-02, BR-M11-03, BR-M11-09) */
export const postCostsItemsFromEvents = (params) => request('POST', '/costs/items/from-events', params)

/** Lập khoản nhập tay (mục ngoài danh mục; lý do bắt buộc) (FR-021, FR-013, BR-M11-04) */
export const postCostsItemsManual = (params) => request('POST', '/costs/items/manual', params)

/** Lập khoản điều chỉnh cho khoản gốc (số tiền do Hành chính nhập; tổng gốc + điều chỉnh không âm) (FR-025, FR-026, FR-027, FR-002a, FR-003, BR-M11-05) */
export const postCostsItemsByIdAdjust = (params) => request('POST', '/costs/items/{id}/adjust', params)

/** Chi tiết khoản, bản ghi nguồn, lịch sử tính lại, kiểm tra, duyệt, trả lại, khoản điều chỉnh và tổng hiện hành (FR-014, FR-018, FR-019) */
export const getCostsItemsById = (params) => request('GET', '/costs/items/{id}', params)

/** Kiểm tra từng khoản hoặc hàng loạt theo bộ lọc (Hành chính); khoản thiếu đơn giá bị bỏ qua (FR-015) */
export const postCostsItemsCheck = (params) => request('POST', '/costs/items/check', params)

/** Bỏ kiểm tra (bảng Đang mở; lý do) (FR-015) */
export const postCostsItemsByIdUncheck = (params) => request('POST', '/costs/items/{id}/uncheck', params)

/** Duyệt từng khoản hoặc hàng loạt theo bảng (Quản lý viện); nhập tay, mua hộ, điều chỉnh phải duyệt riêng (FR-015, FR-024, BR-M11-04) */
export const postCostsItemsApprove = (params) => request('POST', '/costs/items/approve', params)

/** Trả lại khoản Đã kiểm tra hoặc Đã duyệt về Nháp (Quản lý viện; lý do) (FR-015) */
export const postCostsItemsByIdReturn = (params) => request('POST', '/costs/items/{id}/return', params)

/** Hủy khoản Nháp do Hành chính lập (lý do) (FR-016) */
export const postCostsItemsByIdCancel = (params) => request('POST', '/costs/items/{id}/cancel', params)

/** Biến động nguồn của khoản đã chốt (Hành chính dùng để lập điều chỉnh; không có số chênh lệch tính sẵn) (FR-027) */
export const getCostsPostFinalizationChanges = (params) => request('GET', '/costs/post-finalization-changes', params)

/** Danh sách bảng theo kỳ, người cao tuổi, trạng thái, dấu (FR-002, FR-002a) */
export const getCostsStatements = (params) => request('GET', '/costs/statements', params)

/** Bảng chi phí với khoản (lọc dấu, nguồn sinh, loại), tổng theo loại, tổng cộng (FR-002, FR-015) */
export const getCostsStatementsById = (params) => request('GET', '/costs/statements/{id}', params)

/** Gửi chốt (Hành chính); trả cảnh báo biến động nếu có (không chặn) (FR-030, FR-032, BR-M11-06, BR-M11-07) */
export const postCostsStatementsByIdSubmitForFinalization = (params) => request('POST', '/costs/statements/{id}/submit-for-finalization', params)

/** Trả lại bảng Chờ chốt (Quản lý viện; lý do; chọn khoản về Nháp) (FR-015) */
export const postCostsStatementsByIdReturn = (params) => request('POST', '/costs/statements/{id}/return', params)

/** Chốt bảng (Quản lý viện); hoặc toàn bộ, hoặc không (FR-030, FR-033, FR-034a, FR-040, FR-042, BR-M11-06) */
export const postCostsStatementsByIdFinalize = (params) => request('POST', '/costs/statements/{id}/finalize', params)

/** Kỳ chi phí của viện và trạng thái (FR-001) */
export const getCostsPeriods = (params) => request('GET', '/costs/periods', params)

/** Đề nghị mua hộ (Hành chính, Quản lý viện; người đại diện thấy đề nghị cần mình đồng ý) (FR-023, FR-023a) */
export const getPurchaseAssistanceRequests = (params) => request('GET', '/purchase-assistance-requests', params)

/** Lập đề nghị mua hộ (Hành chính) (FR-023) */
export const postPurchaseAssistanceRequests = (params) => request('POST', '/purchase-assistance-requests', params)

/** Gửi (≤ CFG-M11-02 → Được phép mua; lớn hơn → Chờ đồng ý) (FR-023, FR-023a, BR-M11-07) */
export const postPurchaseAssistanceRequestsByIdSend = (params) => request('POST', '/purchase-assistance-requests/{id}/send', params)

/** Đồng ý (người đại diện qua cổng hoặc Quản lý viện); quyết định đầu tiên thắng (FR-023, FR-023a) */
export const postPurchaseAssistanceRequestsByIdAgree = (params) => request('POST', '/purchase-assistance-requests/{id}/agree', params)

/** Từ chối (lý do) (FR-023) */
export const postPurchaseAssistanceRequestsByIdReject = (params) => request('POST', '/purchase-assistance-requests/{id}/reject', params)

/** Xin đồng ý bổ sung (số mới lớn hơn số được phép; lý do) (FR-024) */
export const postPurchaseAssistanceRequestsByIdRequestAdditionalConsent = (params) => request('POST', '/purchase-assistance-requests/{id}/request-additional-consent', params)

/** Rút yêu cầu bổ sung khi chưa có quyết định (lý do) (FR-024) */
export const postPurchaseAssistanceRequestsByIdWithdrawAdditionalConsent = (params) => request('POST', '/purchase-assistance-requests/{id}/withdraw-additional-consent', params)

/** Hủy đề nghị chưa ghi đã mua (lý do) (FR-023) */
export const postPurchaseAssistanceRequestsByIdCancel = (params) => request('POST', '/purchase-assistance-requests/{id}/cancel', params)

/** Ghi đã mua; lập khoản Mua hộ Nháp (vượt ngoài CFG-M11-06 thì chặn, trừ lý do mua gấp thuốc, vật tư y tế) (FR-024, BR-M11-07) */
export const postPurchaseAssistanceRequestsByIdRecordPurchased = (params) => request('POST', '/purchase-assistance-requests/{id}/record-purchased', params)

/** Bảng chi phí đã chốt cho cổng người thân (quyền xem chi phí; tên thuốc theo quyền sức khỏe) (FR-037, FR-019) */
export const getResidentsByResidentIdCostsStatements = (params) => request('GET', '/residents/{residentId}/costs/statements', params)

/** Chi phí tạm tính theo loại cho khoảng ngày (nhãn "tạm tính, chưa chốt") (FR-038, FR-022) */
export const getResidentsByResidentIdCostsProvisional = (params) => request('GET', '/residents/{residentId}/costs/provisional', params)

/** Xuất dữ liệu đã chốt của kỳ cho kế toán (Kế toán); tệp XLSX hoặc CSV (FR-039, FR-041) */
export const postCostsPeriodsByIdExport = (params) => request('POST', '/costs/periods/{id}/export', params)

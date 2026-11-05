// File sinh bởi frontend/scripts/gen-api.mjs từ hợp đồng; không sửa tay.

// Nguồn: specs/013-personal-belongings/contracts (Elder Care – Feature 013 (đồ gửi))
import { request } from './client.js'

/** Loại đồ (kèm dấu đồ có giá trị theo CFG-M12-01 hiện hành) (FR-001) */
export const getBelongingsTypes = (params) => request('GET', '/belongings/types', params)

/** Tạo loại đồ (Quản lý viện) (FR-001) */
export const postBelongingsTypes = (params) => request('POST', '/belongings/types', params)

/** Sửa, ngừng loại đồ; đổi dấu "cần đồng ý khi giao sử dụng" bắt buộc lý do, lưu lịch sử (FR-001, FR-015a) */
export const putBelongingsTypesById = (params) => request('PUT', '/belongings/types/{id}', params)

/** Danh mục mức tình trạng (FR-001a) */
export const getBelongingsConditionLevels = (params) => request('GET', '/belongings/condition-levels', params)

/** Thêm, sửa, ngừng mức tình trạng (Quản lý viện); có id là sửa mức đó, status INACTIVE là ngừng (như vị trí lưu giữ) (FR-001a) */
export const postBelongingsConditionLevels = (params) => request('POST', '/belongings/condition-levels', params)

/** Vị trí lưu giữ (FR-002) */
export const getBelongingsStorageLocations = (params) => request('GET', '/belongings/storage-locations', params)

/** Tạo, sửa, ngừng vị trí (Hành chính, Quản lý viện); ngừng bị chặn khi còn đồ Đang giữ hoặc Hư hỏng (FR-002) */
export const postBelongingsStorageLocations = (params) => request('POST', '/belongings/storage-locations', params)

/** Đồ gửi của người cao tuổi với trạng thái, người giữ, vị trí, bản ghi gần nhất (FR-011) */
export const getResidentsByResidentIdBelongings = (params) => request('GET', '/residents/{residentId}/belongings', params)

/** Tiếp nhận đồ gửi (tùy chọn giao ngay cho người cao tuổi sử dụng) (FR-003, FR-004, FR-005, FR-006, FR-007, FR-008, BR-M12-01, BR-M12-05, BR-M12-06, DBR-22) */
export const postResidentsByResidentIdBelongings = (params) => request('POST', '/residents/{residentId}/belongings', params)

/** Chi tiết đồ gửi và lịch sử bàn giao (cả đồ tách cùng lần tiếp nhận) (FR-011, FR-012) */
export const getBelongingsById = (params) => request('GET', '/belongings/{id}', params)

/** Lệnh bàn giao trên một hoặc nhiều đồ (FR-008, FR-011, FR-011b, FR-012, FR-013, FR-014, FR-015, FR-015a, FR-021, FR-024, BR-M12-01, BR-M12-02, BR-M12-05, BR-M12-06) */
export const postBelongingsHandovers = (params) => request('POST', '/belongings/handovers', params)

/** Liên kết sự cố ghi sau với bản ghi Báo thất lạc / Ghi hư hỏng (một lần) (FR-021, FR-024) */
export const postBelongingsHandoversByIdLinkIncident = (params) => request('POST', '/belongings/handovers/{id}/link-incident', params)

/** Đồ có giá trị nhân viên đang giữ (cho người lập bàn giao ca) (FR-014a) */
export const getStaffByStaffIdHeldBelongings = (params) => request('GET', '/staff/{staffId}/held-belongings', params)

/** Hành chính ghi đồng ý cho tự giữ theo bản ký scan của người đại diện (FR-015a, FR-017b, BR-M12-06) */
export const postSelfCustodyConsents = (params) => request('POST', '/self-custody-consents', params)

/** Rút đồng ý (Hành chính ghi bản ký của người đại diện hiện tại); đồ đang sử dụng → báo thu lại (FR-015a) */
export const postSelfCustodyConsentsByIdWithdraw = (params) => request('POST', '/self-custody-consents/{id}/withdraw', params)

/** Trả một hoặc nhiều đồ cho cùng người nhận (chữ ký, xác minh danh tính, tình trạng lúc trả) (FR-008, FR-016, FR-016a, FR-017, FR-018, FR-019, FR-020, BR-M12-04, BR-M12-05) */
export const postBelongingsReturns = (params) => request('POST', '/belongings/returns', params)

/** Yêu cầu xác nhận người nhận khác (lọc người cao tuổi, trạng thái) (FR-017) */
export const getRecipientConfirmations = (params) => request('GET', '/recipient-confirmations', params)

/** Lập yêu cầu xác nhận người nhận khác (Hành chính); kèm bản ký của người đại diện thì Hiệu lực ngay (FR-017, FR-017b, BR-M12-04) */
export const postRecipientConfirmations = (params) => request('POST', '/recipient-confirmations', params)

/** Ghi một lần liên hệ người đại diện (thời điểm, kênh, kết quả) làm bằng chứng cho duyệt thay (FR-017a) */
export const postRecipientConfirmationsByIdContactEvidence = (params) => request('POST', '/recipient-confirmations/{id}/contact-evidence', params)

/** approve-on-behalf (duyệt thay; Quản lý viện; FR-017a (a) hoặc (b); lý do; không phải người lập), cancel (hủy; Hành chính; lý do) (FR-017a, FR-017b) */
export const postRecipientConfirmationsByIdByCommand = (params) => request('POST', '/recipient-confirmations/{id}/{command}', params)

/** Điều kiện (b) kết thúc lưu trú và mục "xử lý đồ gửi" kèm căn cứ (FR-025, FR-026, BR-M12-03) */
export const getResidentsByResidentIdBelongingsEndingCondition = (params) => request('GET', '/residents/{residentId}/belongings/ending-condition', params)

/** Kỳ kiểm kê hiện tại, vị trí có đồ có giá trị Đang giữ và phiếu của kỳ (không gợi ý vị trí nào cần lập trước) (FR-028a, FR-028c) */
export const getBelongingInventoryChecks = (params) => request('GET', '/belonging-inventory-checks', params)

/** Lập phiếu kiểm kê cho một vị trí (Hành chính); phiếu gồm mọi đồ có giá trị Đang giữ tại vị trí (FR-028a, BR-M12-07) */
export const postBelongingInventoryChecks = (params) => request('POST', '/belonging-inventory-checks', params)

/** Chi tiết phiếu kiểm kê (FR-028b) */
export const getBelongingInventoryChecksById = (params) => request('GET', '/belonging-inventory-checks/{id}', params)

/** complete (hoàn thành: kết quả từng đồ, đồ thừa, đồ không rõ chủ), cancel (hủy: mọi đồ đã rời vị trí; lý do) (FR-028b, BR-M12-07) */
export const postBelongingInventoryChecksByIdByCommand = (params) => request('POST', '/belonging-inventory-checks/{id}/{command}', params)

/** Đồ gửi, trạng thái, lịch sử bàn giao, ảnh cho người đại diện và người có quyền được phép đón (FR-030, BR-M12-09) */
export const getFamilyPortalResidentsByResidentIdBelongings = (params) => request('GET', '/family-portal/residents/{residentId}/belongings', params)

/** Người đại diện xác nhận (confirm) hoặc từ chối (reject, lý do) yêu cầu xác nhận người nhận khác (FR-017, FR-030) */
export const postFamilyPortalRecipientConfirmationsByIdByCommand = (params) => request('POST', '/family-portal/recipient-confirmations/{id}/{command}', params)

/** Người đại diện ghi đồng ý cho tự giữ một đồ thuộc loại cần đồng ý (FR-015a, FR-030) */
export const postFamilyPortalSelfCustodyConsents = (params) => request('POST', '/family-portal/self-custody-consents', params)

/** Người đại diện hiện tại rút đồng ý (lý do) (FR-015a, FR-030) */
export const postFamilyPortalSelfCustodyConsentsByIdWithdraw = (params) => request('POST', '/family-portal/self-custody-consents/{id}/withdraw', params)

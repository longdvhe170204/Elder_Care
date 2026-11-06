// File sinh bởi frontend/scripts/gen-api.mjs từ hợp đồng; không sửa tay.

// Nguồn: specs/019-inventory-assets/contracts (Elder Care – Feature 019 (mua sắm, tài sản, lịch xe))
import { request } from './client.js'

/** Yêu cầu mua sắm (bộ phận của tôi; Quản lý viện, Kế toán theo vai trò) (FR-008) */
export const getProcurementRequests = (params) => request('GET', '/procurement-requests', params)

/** Lập yêu cầu mua sắm Nháp (Phụ trách bộ phận; Điều dưỡng chỉ loại MEDICATION, FR-008d); yêu cầu bổ sung tham chiếu yêu cầu gốc (FR-008, FR-008a, FR-008d, BR-M11-17) */
export const postProcurementRequests = (params) => request('POST', '/procurement-requests', params)

/** Sửa yêu cầu Nháp (FR-008) */
export const putProcurementRequestsById = (params) => request('PUT', '/procurement-requests/{id}', params)

/** Lệnh trên yêu cầu mua sắm theo bảng FR-008 (FR-008, FR-008a, FR-008b, BR-M11-17, BR-M11-18, BR-M11-19) */
export const postProcurementRequestsByIdByCommand = (params) => request('POST', '/procurement-requests/{id}/{command}', params)

/** Tài sản theo bộ phận, loại, trạng thái, dấu quá hạn (FR-011, FR-014) */
export const getAssets = (params) => request('GET', '/assets', params)

/** Ghi tăng tài sản (không phải loại giường); mua mới gắn yêu cầu mua sắm Đã duyệt và nguyên giá (FR-011, FR-008c, DBR-32, DBR-36) */
export const postAssets = (params) => request('POST', '/assets', params)

/** Tạo, sửa, ngừng loại tài sản (Quản lý viện) (FR-011) */
export const postAssetTypes = (params) => request('POST', '/asset-types', params)

/** Chi tiết tài sản và lịch sử (vị trí, bộ phận, bảo trì, hư hỏng) (FR-012) */
export const getAssetsById = (params) => request('GET', '/assets/{id}', params)

/** Sửa thông tin mô tả (nhóm 1); Phụ trách bộ phận Chăm sóc bổ sung trường của tài sản giường (FR-011) */
export const putAssetsById = (params) => request('PUT', '/assets/{id}', params)

/** Lệnh trên tài sản theo bảng FR-013 (FR-012, FR-013, FR-015, FR-016, BR-M03-15, BR-M03-17, BR-M03-18) */
export const postAssetsByIdByCommand = (params) => request('POST', '/assets/{id}/{command}', params)

/** Lịch xe theo xe, khoảng (FR-017) */
export const getVehicleSchedules = (params) => request('GET', '/vehicle-schedules', params)

/** Đặt lịch xe (Trưởng tầng, Hành chính, Quản lý viện) (FR-017, FR-018, BR-M03-16) */
export const postVehicleSchedules = (params) => request('POST', '/vehicle-schedules', params)

/** reschedule, cancel (lý do), record-departure, record-return – người đặt, Quản lý viện (FR-017, FR-018, BR-M03-16) */
export const postVehicleSchedulesByIdByCommand = (params) => request('POST', '/vehicle-schedules/{id}/{command}', params)

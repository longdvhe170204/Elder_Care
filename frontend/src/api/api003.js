// File sinh bởi frontend/scripts/gen-api.mjs từ hợp đồng; không sửa tay.

// Nguồn: specs/003-room-bed-allocation/contracts (Elder Care – Feature 003 (phòng, giường và phân bổ giường))
import { request } from './client.js'

/** Danh sách khu vực (FR-001, FR-002) */
export const getAreas = (params) => request('GET', '/areas', params)

/** Tạo khu vực (Quản lý viện) (FR-001, FR-002, FR-003) */
export const postAreas = (params) => request('POST', '/areas', params)

/** Sửa khu vực (FR-002, FR-003) */
export const putAreasById = (params) => request('PUT', '/areas/{id}', params)

/** Xóa khu vực chưa từng được tham chiếu (FR-005) */
export const deleteAreasById = (params) => request('DELETE', '/areas/{id}', params)

/** Xem chi tiết kèm ETag (FR-002) */
export const getAreasById = (params) => request('GET', '/areas/{id}', params)

/** Ngừng hiệu lực khu vực (FR-005) */
export const postAreasByIdDeactivate = (params) => request('POST', '/areas/{id}/deactivate', params)

/** Kích hoạt lại khu vực (FR-005) */
export const postAreasByIdReactivate = (params) => request('POST', '/areas/{id}/reactivate', params)

/** Danh sách tầng (FR-001, FR-002) */
export const getFloors = (params) => request('GET', '/floors', params)

/** Tạo tầng (Quản lý viện) (FR-001, FR-003) */
export const postFloors = (params) => request('POST', '/floors', params)

/** Sửa tầng (FR-002, FR-007) */
export const putFloorsById = (params) => request('PUT', '/floors/{id}', params)

/** Xóa tầng chưa từng được tham chiếu (FR-005) */
export const deleteFloorsById = (params) => request('DELETE', '/floors/{id}', params)

/** Xem chi tiết kèm ETag (FR-002) */
export const getFloorsById = (params) => request('GET', '/floors/{id}', params)

/** Ngừng hiệu lực tầng (FR-005) */
export const postFloorsByIdDeactivate = (params) => request('POST', '/floors/{id}/deactivate', params)

/** Kích hoạt lại tầng (FR-005) */
export const postFloorsByIdReactivate = (params) => request('POST', '/floors/{id}/reactivate', params)

/** Danh mục loại phòng (FR-004) */
export const getRoomTypes = (params) => request('GET', '/room-types', params)

/** Tạo loại phòng (Quản lý viện) (FR-004) */
export const postRoomTypes = (params) => request('POST', '/room-types', params)

/** Sửa loại phòng (FR-004) */
export const putRoomTypesById = (params) => request('PUT', '/room-types/{id}', params)

/** Xem chi tiết kèm ETag (FR-002) */
export const getRoomTypesById = (params) => request('GET', '/room-types/{id}', params)

/** Xóa mục chưa từng được tham chiếu (FR-005) */
export const deleteRoomTypesById = (params) => request('DELETE', '/room-types/{id}', params)

/** Ngừng hiệu lực loại phòng (FR-004, FR-005) */
export const postRoomTypesByIdDeactivate = (params) => request('POST', '/room-types/{id}/deactivate', params)

/** Danh sách phòng (FR-002, FR-004) */
export const getRooms = (params) => request('GET', '/rooms', params)

/** Tạo phòng (Quản lý viện) (FR-001, FR-003, FR-004) */
export const postRooms = (params) => request('POST', '/rooms', params)

/** Xem phòng (FR-002) */
export const getRoomsById = (params) => request('GET', '/rooms/{id}', params)

/** Sửa phòng (FR-004, FR-006, FR-007, BR-M03-01) */
export const putRoomsById = (params) => request('PUT', '/rooms/{id}', params)

/** Xóa phòng chưa từng được tham chiếu (FR-005) */
export const deleteRoomsById = (params) => request('DELETE', '/rooms/{id}', params)

/** Ngừng hiệu lực phòng (FR-005) */
export const postRoomsByIdDeactivate = (params) => request('POST', '/rooms/{id}/deactivate', params)

/** Kích hoạt lại phòng (FR-005) */
export const postRoomsByIdReactivate = (params) => request('POST', '/rooms/{id}/reactivate', params)

/** Danh mục lý do chuyển giường (FR-030) */
export const getBedTransferReasons = (params) => request('GET', '/bed-transfer-reasons', params)

/** Thêm lý do chuyển giường (Quản lý viện) (FR-030) */
export const postBedTransferReasons = (params) => request('POST', '/bed-transfer-reasons', params)

/** Sửa lý do chuyển giường (FR-030) */
export const putBedTransferReasonsById = (params) => request('PUT', '/bed-transfer-reasons/{id}', params)

/** Xem chi tiết kèm ETag (FR-002) */
export const getBedTransferReasonsById = (params) => request('GET', '/bed-transfer-reasons/{id}', params)

/** Xóa mục chưa từng được tham chiếu (FR-005) */
export const deleteBedTransferReasonsById = (params) => request('DELETE', '/bed-transfer-reasons/{id}', params)

/** Ngừng hiệu lực lý do chuyển giường (FR-030) */
export const postBedTransferReasonsByIdDeactivate = (params) => request('POST', '/bed-transfer-reasons/{id}/deactivate', params)

/** Bảng giường (Quản lý viện, Trưởng tầng, Hành chính) (FR-002, FR-013b, FR-024a, FR-025a, FR-034) */
export const getBeds = (params) => request('GET', '/beds', params)

/** Tạo giường (Quản lý viện) (FR-001, FR-003, FR-004, BR-M03-22) */
export const postBeds = (params) => request('POST', '/beds', params)

/** Xem giường (FR-002, FR-011) */
export const getBedsById = (params) => request('GET', '/beds/{id}', params)

/** Sửa mã giường; đổi phòng khi giường chưa từng có phân bổ (FR-003, FR-007) */
export const putBedsById = (params) => request('PUT', '/beds/{id}', params)

/** Kích hoạt lại mục danh mục (FR-005) */
export const postRoomTypesByIdReactivate = (params) => request('POST', '/room-types/{id}/reactivate', params)

/** Kích hoạt lại mục danh mục (FR-005) */
export const postBedTransferReasonsByIdReactivate = (params) => request('POST', '/bed-transfer-reasons/{id}/reactivate', params)

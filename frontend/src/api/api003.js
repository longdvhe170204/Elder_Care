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

/** Số giường theo trạng thái và số giường Trống theo điều kiện (FR-011) */
export const getBedsSummary = (params) => request('GET', '/beds/summary', params)

/** Xem giường (FR-002, FR-011) */
export const getBedsById = (params) => request('GET', '/beds/{id}', params)

/** Sửa mã giường; đổi phòng khi giường chưa từng có phân bổ (FR-003, FR-007) */
export const putBedsById = (params) => request('PUT', '/beds/{id}', params)

/** Tạm ngừng sử dụng (Quản lý viện) (FR-011) */
export const postBedsByIdSuspend = (params) => request('POST', '/beds/{id}/suspend', params)

/** Sử dụng lại (Quản lý viện) (FR-011, FR-044, BR-M03-09) */
export const postBedsByIdReuse = (params) => request('POST', '/beds/{id}/reuse', params)

/** Lịch sử trạng thái giường (FR-016, FR-016a) */
export const getBedsByIdStateHistory = (params) => request('GET', '/beds/{id}/state-history', params)

/** Đính chính lần Xác nhận giường sẵn sàng (ghi chú, khử khuẩn, bảo hộ) (FR-044) */
export const postBedsByIdStateHistoryByHistoryIdCorrections = (params) => request('POST', '/beds/{id}/state-history/{historyId}/corrections', params)

/** Danh sách giường thỏa FR-018 cho một người cao tuổi (FR-018, FR-021, FR-022, BR-M03-01) */
export const getBedAllocationsSuitableBeds = (params) => request('GET', '/bed-allocations/suitable-beds', params)

/** Danh sách phân bổ trong phạm vi quyền người xem (FR-017, FR-021, FR-037) */
export const getBedAllocations = (params) => request('GET', '/bed-allocations', params)

/** Phân bổ giường (Hành chính; Trưởng tầng với giường trong phạm vi) (FR-017, FR-018, FR-019, FR-020, FR-021, FR-023, FR-025a, FR-011a, BR-M03-01, BR-M03-02, BR-M03-19, BR-M03-20) */
export const postBedAllocations = (params) => request('POST', '/bed-allocations', params)

/** Xem phân bổ (FR-017) */
export const getBedAllocationsById = (params) => request('GET', '/bed-allocations/{id}', params)

/** Hủy phân bổ tương lai (FR-023, FR-021) */
export const postBedAllocationsByIdCancel = (params) => request('POST', '/bed-allocations/{id}/cancel', params)

/** Chuyển giường trực tiếp (Hành chính; Trưởng tầng khi người và giường mới trong phạm vi) (FR-027, FR-028, FR-029, FR-030, FR-031, FR-032, FR-033, BR-M03-01, BR-M03-04, BR-M03-07) */
export const postBedAllocationsByIdTransfer = (params) => request('POST', '/bed-allocations/{id}/transfer', params)

/** Đính chính phân bổ đã đóng hoặc đã hủy (FR-026, BR-M03-07, BR-M03-24) */
export const postBedAllocationsByIdCorrections = (params) => request('POST', '/bed-allocations/{id}/corrections', params)

/** Vị trí hiện tại, phân bổ tương lai và dấu của một người cao tuổi (FR-017, FR-025a, FR-037) */
export const getResidentsByResidentIdLocation = (params) => request('GET', '/residents/{residentId}/location', params)

/** Người A ở giường nào tại T, hoặc trong khoảng [from, to) (FR-035, FR-037) */
export const getLocationHistoryResidentsByResidentId = (params) => request('GET', '/location-history/residents/{residentId}', params)

/** Mỗi giường của phòng/tầng/khu vực có ai tại T hoặc trong ngày D (FR-035, FR-037) */
export const getLocationHistoryBeds = (params) => request('GET', '/location-history/beds', params)

/** Những ai ở cùng phòng với A trong khoảng [from, to) (FR-035, FR-036, FR-037, BR-M05-10) */
export const getLocationHistoryRoommates = (params) => request('GET', '/location-history/roommates', params)

/** Kích hoạt lại mục danh mục (FR-005) */
export const postRoomTypesByIdReactivate = (params) => request('POST', '/room-types/{id}/reactivate', params)

/** Kích hoạt lại mục danh mục (FR-005) */
export const postBedTransferReasonsByIdReactivate = (params) => request('POST', '/bed-transfer-reasons/{id}/reactivate', params)

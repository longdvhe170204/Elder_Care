// File sinh bởi frontend/scripts/gen-api.mjs từ hợp đồng; không sửa tay.

// Nguồn: specs/018-residence-registration/contracts (Elder Care – Feature 018 (khai báo tạm trú, lưu trú))
import { request } from './client.js'

/** Người cần khai báo (gồm chưa lập), quá hạn, sắp hết hạn, cần khai báo xóa (Hành chính, Quản lý viện) (FR-001, FR-002, FR-006, FR-008, FR-011) */
export const getResidenceRegistrationsToDo = (params) => request('GET', '/residence-registrations/to-do', params)

/** Khai báo của người cao tuổi và lịch sử (FR-003) */
export const getResidentsByResidentIdResidenceRegistrations = (params) => request('GET', '/residents/{residentId}/residence-registrations', params)

/** Lập khai báo (hoặc khai báo gia hạn) ở Cần khai báo; loại xác định khi lập; hạn theo mốc nghĩa vụ (FR-001, FR-001a, FR-002, FR-004, FR-006, FR-007, BR-M02-11, BR-M02-12, BR-M02-14, DBR-33) */
export const postResidentsByResidentIdResidenceRegistrations = (params) => request('POST', '/residents/{residentId}/residence-registrations', params)

/** Lệnh trên khai báo theo bảng FR-005 (FR-001, FR-003, FR-005, FR-008, BR-M02-11, BR-M02-13) */
export const postResidenceRegistrationsByIdByCommand = (params) => request('POST', '/residence-registrations/{id}/{command}', params)

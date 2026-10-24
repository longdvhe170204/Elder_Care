// File sinh bởi frontend/scripts/gen-api.mjs từ hợp đồng; không sửa tay.

// Nguồn: specs/016-reports-dashboard/contracts (Elder Care – Feature 016 (báo cáo và dashboard))
import { request } from './client.js'

/** Dashboard theo vai trò và phạm vi hiện hành; khối không được phép không có trong phản hồi (FR-010, FR-012, FR-013, FR-020, FR-021) */
export const getDashboard = (params) => request('GET', '/dashboard', params)

/** Danh mục chỉ tiêu người xem được thấy, kèm định nghĩa hiển thị (FR-003, FR-010) */
export const getMetrics = (params) => request('GET', '/metrics', params)

/** Báo cáo một nhóm theo khoảng và bộ lọc; khoảng dài hơn CFG-M14-01 bị từ chối (FR-022, FR-023, FR-024, FR-025, FR-030, FR-031, FR-032, FR-033, FR-034, FR-035, FR-036, FR-037, FR-040, FR-041, FR-042, FR-042a, FR-043, FR-044, FR-050, FR-051, FR-052, FR-053, FR-054, FR-055, FR-055a, FR-060, FR-070, FR-071, FR-072, FR-073) */
export const getReportsByGroup = (params) => request('GET', '/reports/{group}', params)

/** Danh sách bản ghi tạo nên một con số (cùng phạm vi, bộ lọc, mốc tính); dòng người ngoài phạm vi hiện hành ở dạng rút gọn (Q-196) (FR-009, FR-012, FR-014) */
export const getMetricDetailsByMetricCode = (params) => request('GET', '/metric-details/{metricCode}', params)

/** Xuất báo cáo (XLSX); Quản lý viện xuất có định danh phải nhập mục đích; vai trò khác chỉ tổng hợp (FR-060, FR-061, FR-062) */
export const postReportsByGroupExport = (params) => request('POST', '/reports/{group}/export', params)

/** Lần xuất (Quản lý viện thấy mọi lần; người khác thấy lần của mình) (FR-062) */
export const getReportsExports = (params) => request('GET', '/reports/exports', params)

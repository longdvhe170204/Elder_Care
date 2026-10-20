// File sinh bởi frontend/scripts/gen-api.mjs từ hợp đồng; không sửa tay.

// Nguồn: specs/002-account-permission/contracts (Elder Care – Feature 002 (tài khoản và phân quyền))
import { request } from './client.js'

/** Đăng nhập (FR-007, FR-008, FR-010, FR-014, FR-014a, FR-015, BR-M15-05, BR-M15-15) */
export const postAuthLogin = (params) => request('POST', '/auth/login', params)

/** Kết thúc phiên hiện tại (FR-013) */
export const postAuthLogout = (params) => request('POST', '/auth/logout', params)

/** Người dùng tự đổi mật khẩu (bắt buộc ở lần đầu, FR-006) (FR-006) */
export const postAuthChangePassword = (params) => request('POST', '/auth/change-password', params)

/** Thông tin người dùng hiện tại */
export const getMe = (params) => request('GET', '/me', params)

/** Tập khóa quyền hiện hành của người dùng, để client ẩn hiện chức năng (FR-042) */
export const getMePermissions = (params) => request('GET', '/me/permissions', params)

/** Danh sách tài khoản (Quản lý viện; Hành chính chỉ thấy tài khoản người thân) (FR-001, FR-051) */
export const getAccounts = (params) => request('GET', '/accounts', params)

/** Tạo tài khoản nhân viên (chỉ Quản lý viện) (FR-002, FR-003, FR-004, FR-004a, FR-006) */
export const postAccounts = (params) => request('POST', '/accounts', params)

/** Tạo tài khoản người thân (Hành chính hoặc Quản lý viện) (FR-050, FR-051, FR-052) */
export const postFamilyAccounts = (params) => request('POST', '/family-accounts', params)

/** Chi tiết tài khoản, kèm allowedCommands */
export const getAccountsById = (params) => request('GET', '/accounts/{id}', params)

/** Khóa tài khoản (Quản lý viện) (FR-011, FR-012, FR-013) */
export const postAccountsByIdLock = (params) => request('POST', '/accounts/{id}/lock', params)

/** Mở khóa tài khoản Đã khóa (Quản lý viện) (FR-011) */
export const postAccountsByIdUnlock = (params) => request('POST', '/accounts/{id}/unlock', params)

/** Mở khóa sớm tài khoản đang Khóa tạm (FR-019c, BR-M15-05) */
export const postAccountsByIdEarlyUnlock = (params) => request('POST', '/accounts/{id}/early-unlock', params)

/** Kích hoạt lại tài khoản người thân Không hoạt động (FR-011, FR-051, FR-052) */
export const postAccountsByIdReactivate = (params) => request('POST', '/accounts/{id}/reactivate', params)

/** Cấp lại mật khẩu (nhân viên - Quản lý viện; người thân - Hành chính, Quản lý viện) (FR-006, FR-051, FR-052) */
export const postAccountsByIdResetPassword = (params) => request('POST', '/accounts/{id}/reset-password', params)

/** Gán thêm vai trò (Quản lý viện) (FR-017, FR-026, FR-027, BR-M15-04, BR-M15-17) */
export const postAccountsByIdAssignRole = (params) => request('POST', '/accounts/{id}/assign-role', params)

/** Gỡ vai trò (Quản lý viện); không gỡ vai trò cuối cùng; không làm mất Quản lý viện cuối cùng; tài khoản người cao tuổi: 409 RESIDENT_ACCOUNT_HAS_NO_ROLES (bổ sung 2026-10-18) (FR-004, FR-012, FR-026, FR-027) */
export const postAccountsByIdRemoveRole = (params) => request('POST', '/accounts/{id}/remove-role', params)

/** Tập quyền của tài khoản, hiện hành hoặc tại một thời điểm quá khứ (Quản lý viện) (FR-026a, BR-M15-16) */
export const getAccountsByIdPermissions = (params) => request('GET', '/accounts/{id}/permissions', params)

/** Thu hẹp quyền theo tài khoản (FR-025, FR-026, BR-M15-18) */
export const postAccountsByIdRestrict = (params) => request('POST', '/accounts/{id}/restrict', params)

/** Khôi phục một quyền đã thu hẹp theo tài khoản về mức ma trận (FR-025) */
export const postAccountsByIdRestorePermission = (params) => request('POST', '/accounts/{id}/restore-permission', params)

/** Gán quyền Duyệt kế hoạch chăm sóc cho tài khoản Điều dưỡng (ngoại lệ duy nhất vượt ma trận, Q-07) (FR-024) */
export const postAccountsByIdGrantCarePlanApproval = (params) => request('POST', '/accounts/{id}/grant-care-plan-approval', params)

/** Gỡ quyền Duyệt kế hoạch chăm sóc đã gán thêm (FR-024) */
export const postAccountsByIdRevokeCarePlanApproval = (params) => request('POST', '/accounts/{id}/revoke-care-plan-approval', params)

/** Danh sách 9 vai trò (FR-016) */
export const getRoles = (params) => request('GET', '/roles', params)

/** Quyền tối đa (ma trận) và quyền hiện hành của vai trò sau thu hẹp (FR-018, FR-020) */
export const getRolesByCodePermissions = (params) => request('GET', '/roles/{code}/permissions', params)

/** Thu hẹp quyền theo vai trò (FR-025, FR-026, BR-M15-18) */
export const postRolesByCodeRestrict = (params) => request('POST', '/roles/{code}/restrict', params)

/** Khôi phục quyền đã thu hẹp theo vai trò (FR-025) */
export const postRolesByCodeRestorePermission = (params) => request('POST', '/roles/{code}/restore-permission', params)

/** Danh mục nghiệp vụ chuyên môn (FR-036) */
export const getProfessionalActivities = (params) => request('GET', '/professional-activities', params)

/** Thêm mục (Quản lý viện) (FR-036) */
export const postProfessionalActivities = (params) => request('POST', '/professional-activities', params)

/** Sửa mục (Quản lý viện) (FR-036) */
export const putProfessionalActivitiesById = (params) => request('PUT', '/professional-activities/{id}', params)

/** Ngừng hiệu lực; mục mặc định không ngừng được (FR-036) */
export const postProfessionalActivitiesByIdDeactivate = (params) => request('POST', '/professional-activities/{id}/deactivate', params)

/** Đồng bộ lô bản ghi ngoại tuyến của app nhân viên (FR-041a, FR-041c, BR-M15-20) */
export const postSync = (params) => request('POST', '/sync', params)

/** Bản ghi chờ xem lại thuộc quyền quyết định của người dùng (FR-041c) */
export const getPendingReviewRecords = (params) => request('GET', '/pending-review-records', params)

/** Chấp nhận; bản ghi có hiệu lực từ lúc chấp nhận (FR-041c) */
export const postPendingReviewRecordsByIdAccept = (params) => request('POST', '/pending-review-records/{id}/accept', params)

/** Từ chối, lý do bắt buộc; bản ghi được lưu nhưng không có hiệu lực (FR-041c) */
export const postPendingReviewRecordsByIdReject = (params) => request('POST', '/pending-review-records/{id}/reject', params)

/** Tạo tài khoản người cao tuổi theo lời chính người cao tuổi (Hành chính hoặc Quản lý viện) (FR-067, FR-068) */
export const postResidentAccounts = (params) => request('POST', '/resident-accounts', params)

/** Tạo mã QR đăng nhập dùng một lần (Hành chính, Quản lý viện, Điều dưỡng có phạm vi) (FR-069) */
export const postAccountsByIdLoginQrCode = (params) => request('POST', '/accounts/{id}/login-qr-code', params)

/** Người cao tuổi đăng nhập bằng mã QR; gắn thiết bị (FR-069, FR-070, FR-013a, BR-M15-24) */
export const postAuthQrLogin = (params) => request('POST', '/auth/qr-login', params)

/** Gỡ thiết bị đang gắn (Hành chính hoặc Quản lý viện), ví dụ khi người cao tuổi báo mất máy (FR-070) */
export const postAccountsByIdUnbindDevice = (params) => request('POST', '/accounts/{id}/unbind-device', params)

/** Khóa tài khoản người cao tuổi theo yêu cầu của chính người cao tuổi hoặc có lý do (Hành chính, Quản lý viện) (FR-072, BR-M15-23) */
export const postAccountsByIdLockResident = (params) => request('POST', '/accounts/{id}/lock-resident', params)

/** Mở khóa tài khoản người cao tuổi khi chính người cao tuổi yêu cầu (FR-073) */
export const postAccountsByIdUnlockResident = (params) => request('POST', '/accounts/{id}/unlock-resident', params)

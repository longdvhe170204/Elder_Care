// File sinh bởi frontend/scripts/gen-api.mjs từ hợp đồng; không sửa tay.

// Nguồn: specs/014-activity-care-quality/contracts (Elder Care – Feature 014 (hoạt động và chất lượng chăm sóc))
import { request } from './client.js'

/** Loại hoạt động, nhóm sở thích liên quan (FR-001) */
export const getActivityTypes = (params) => request('GET', '/activity-types', params)

/** Tạo, sửa, ngừng loại hoạt động (Quản lý viện) (FR-001) */
export const postActivityTypes = (params) => request('POST', '/activity-types', params)

/** Hoạt động theo tầng hoặc toàn viện (FR-002) */
export const getActivities = (params) => request('GET', '/activities', params)

/** Khai báo hoạt động (Trưởng tầng) (FR-002, FR-002a) */
export const postActivities = (params) => request('POST', '/activities', params)

/** Sửa hoạt động; giảm số lượng dưới số đăng ký cần chọn đăng ký cần hủy kèm lý do (FR-002b, FR-003) */
export const putActivitiesById = (params) => request('PUT', '/activities/{id}', params)

/** deactivate (hủy buổi chưa điểm danh từ ngày ngừng), change-template (phiên bản mẫu mới, lý do, ngày áp dụng ≥ hôm nay), create-sessions-from-template (khoảng ngày), add-regular-participant, remove-regular-participant (FR-004, FR-005, FR-006, FR-007, FR-008, FR-009, FR-010, BR-M04-21) */
export const postActivitiesByIdByCommand = (params) => request('POST', '/activities/{id}/{command}', params)

/** Buổi theo ngày, tầng, hoạt động (FR-011) */
export const getActivitySessions = (params) => request('GET', '/activity-sessions', params)

/** Tạo buổi lẻ (Trưởng tầng); chuyến đi kèm điểm đến, giờ rời, giờ về, phương tiện (FR-011, FR-034) */
export const postActivitySessions = (params) => request('POST', '/activity-sessions', params)

/** Chi tiết buổi, đăng ký, điểm danh (FR-019, FR-028) */
export const getActivitySessionsById = (params) => request('GET', '/activity-sessions/{id}', params)

/** reschedule (thời điểm, địa điểm, người phụ trách; lý do), cancel (lý do), complete-attendance (FR-012, FR-013, FR-014, FR-029, FR-038a, BR-M04-15) */
export const postActivitySessionsByIdByCommand = (params) => request('POST', '/activity-sessions/{id}/{command}', params)

/** Đăng ký người cao tuổi vào buổi (FR-015, FR-016, FR-016a, FR-017, FR-018, FR-052, BR-M04-15, BR-M05-11) */
export const postActivitySessionsByIdRegistrations = (params) => request('POST', '/activity-sessions/{id}/registrations', params)

/** Hủy đăng ký trước giờ bắt đầu (lý do) (FR-017) */
export const postActivityRegistrationsByIdCancel = (params) => request('POST', '/activity-registrations/{id}/cancel', params)

/** Ghi kết quả điểm danh (một hoặc nhiều người; thêm người lúc điểm danh) (FR-027, FR-028, FR-029, FR-030, FR-032, FR-033, BR-M04-18) */
export const postActivitySessionsByIdAttendance = (params) => request('POST', '/activity-sessions/{id}/attendance', params)

/** Hành chính ghi nhận đồng ý về muộn của người đại diện (bản ký) cho đúng một buổi (FR-016a) */
export const postLateReturnConsents = (params) => request('POST', '/late-return-consents', params)

/** Hành chính ghi rút đồng ý theo yêu cầu người đại diện (trước giờ bắt đầu) (FR-016a) */
export const postLateReturnConsentsByIdWithdraw = (params) => request('POST', '/late-return-consents/{id}/withdraw', params)

/** Danh sách chuyến cho trưởng đoàn, người đi cùng, trưởng tầng (trạng thái từng người, liên hệ chính, thuốc mang theo theo quyền) (FR-039) */
export const getTripsById = (params) => request('GET', '/trips/{id}', params)

/** Phân công trưởng đoàn, người đi cùng (Trưởng tầng) (FR-035) */
export const postTripsByIdAssignment = (params) => request('POST', '/trips/{id}/assignment', params)

/** Đánh giá khả năng tham gia (Trưởng tầng của tầng người cao tuổi) (FR-036, FR-038, BR-M04-16) */
export const postTripsByIdEligibilityAssessments = (params) => request('POST', '/trips/{id}/eligibility-assessments', params)

/** Lệnh trên chuyến đi (FR-035a, FR-040, FR-041, FR-042, FR-043, FR-044, FR-046, FR-046a, FR-047, FR-048, FR-050, BR-M04-16, BR-M04-17) */
export const postTripsByIdByCommand = (params) => request('POST', '/trips/{id}/{command}', params)

/** Sở thích hiện hành và lịch sử (FR-051) */
export const getResidentsByResidentIdPreferences = (params) => request('GET', '/residents/{residentId}/preferences', params)

/** Ghi sở thích; nhóm và nguồn đã có mục hiện hành cần confirmReplacement (sửa 2026-10-18, Q-363) (FR-051) */
export const postResidentsByResidentIdPreferences = (params) => request('POST', '/residents/{residentId}/preferences', params)

/** Bỏ sở thích (lý do) (FR-051) */
export const postPreferencesByIdRemove = (params) => request('POST', '/preferences/{id}/remove', params)

/** Hồ sơ tinh thần theo khoảng (chỉ xem) (FR-053) */
export const getResidentsByResidentIdWellbeingProfile = (params) => request('GET', '/residents/{residentId}/wellbeing-profile', params)

/** Công việc đủ điều kiện của ca theo tầng, số mục tối thiểu, danh sách hiện có (chỉ người kiểm tra, Quản lý viện); không xếp hạng hay gợi ý (FR-059, FR-060) */
export const getQualityChecksShiftsByShiftId = (params) => request('GET', '/quality-checks/shifts/{shiftId}', params)

/** Lập danh sách kiểm tra (đủ tỷ lệ CFG-M04-11) hoặc thêm mục (FR-059, FR-060, BR-M04-23) */
export const postQualityChecksShiftsByShiftId = (params) => request('POST', '/quality-checks/shifts/{shiftId}', params)

/** Ghi kết quả Đạt / Không đạt (kèm công việc làm lại) / Không kiểm tra được (FR-061, FR-062, FR-064, FR-066, BR-M04-23) */
export const postQualityCheckItemsByIdResult = (params) => request('POST', '/quality-check-items/{id}/result', params)

/** Buổi đã đăng ký, chuyến đi, số buổi đã tham gia (loại "chung"); không có mức tham gia, cảnh báo, kết quả kiểm tra (FR-068) */
export const getFamilyPortalResidentsByResidentIdActivities = (params) => request('GET', '/family-portal/residents/{residentId}/activities', params)

/** Người đại diện ghi đồng ý về muộn cho một buổi của người bán trú (FR-016a) */
export const postFamilyPortalLateReturnConsents = (params) => request('POST', '/family-portal/late-return-consents', params)

/** Người đại diện rút đồng ý trước giờ bắt đầu (FR-016a) */
export const postFamilyPortalLateReturnConsentsByIdWithdraw = (params) => request('POST', '/family-portal/late-return-consents/{id}/withdraw', params)

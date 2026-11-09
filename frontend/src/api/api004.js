// File sinh bởi frontend/scripts/gen-api.mjs từ hợp đồng; không sửa tay.

// Nguồn: specs/004-admission-residency/contracts (Elder Care – Feature 004 (tiếp nhận và lưu trú))
import { request } from './client.js'

/** Danh sách lượt đăng ký (Hành chính, Quản lý viện) (FR-006) */
export const getAdmissionApplications = (params) => request('GET', '/admission-applications', params)

/** Lập lượt đăng ký tiếp nhận (Hành chính) (FR-001, FR-006) */
export const postAdmissionApplications = (params) => request('POST', '/admission-applications', params)

/** Xem lượt đăng ký (FR-001, FR-006) */
export const getAdmissionApplicationsById = (params) => request('GET', '/admission-applications/{id}', params)

/** Cập nhật lượt đăng ký (FR-001, BR-M02-15) */
export const postAdmissionApplicationsByIdUpdate = (params) => request('POST', '/admission-applications/{id}/update', params)

/** Ghi ý kiến của người cao tuổi về việc vào ở (Hành chính); ý kiến Hiệu lực cũ chuyển Được thay thế (bổ sung 2026-10-18) (FR-001, FR-004a, BR-M01-14) */
export const postAdmissionApplicationsByIdAdmissionOpinion = (params) => request('POST', '/admission-applications/{id}/admission-opinion', params)

/** Danh sách điều kiện tiếp nhận (a) → (f) (FR-004, FR-004a, FR-005, FR-021, FR-075, FR-076, BR-M02-22) */
export const getAdmissionApplicationsByIdAdmissionConditions = (params) => request('GET', '/admission-applications/{id}/admission-conditions', params)

/** Tiến trình theo các bước 6.1, suy ra từ dữ liệu (FR-003) */
export const getAdmissionApplicationsByIdProgress = (params) => request('GET', '/admission-applications/{id}/progress', params)

/** Danh sách chờ (Hành chính, Quản lý viện) (FR-009, FR-018) */
export const getWaitingListEntries = (params) => request('GET', '/waiting-list-entries', params)

/** Đưa vào danh sách chờ (Hành chính) (FR-007, FR-008, FR-009) */
export const postWaitingListEntries = (params) => request('POST', '/waiting-list-entries', params)

/** Xem hồ sơ chờ (FR-009) */
export const getWaitingListEntriesById = (params) => request('GET', '/waiting-list-entries/{id}', params)

/** Đánh dấu hoặc gỡ dấu Ưu tiên, bắt buộc lý do (Q-394) (FR-009, BR-M02-10) */
export const postWaitingListEntriesByIdMarkPriority = (params) => request('POST', '/waiting-list-entries/{id}/mark-priority', params)

/** Danh sách hồ sơ chờ phù hợp với một giường Trống (FR-012, FR-013, BR-M02-01, BR-M02-10) */
export const getBedsByBedIdMatchingEntries = (params) => request('GET', '/beds/{bedId}/matching-entries', params)

/** Ghi nhận đã liên hệ, kèm giường được giữ (FR-014, BR-M02-02, BR-M02-10) */
export const postWaitingListEntriesByIdRecordContacted = (params) => request('POST', '/waiting-list-entries/{id}/record-contacted', params)

/** Gia đình từ chối giường này nhưng vẫn chờ (FR-012, FR-013) */
export const postWaitingListEntriesByIdDeclineBed = (params) => request('POST', '/waiting-list-entries/{id}/decline-bed', params)

/** Ghi nhận gia đình không tiếp tục chờ (Từ chối) (FR-002) */
export const postWaitingListEntriesByIdDecline = (params) => request('POST', '/waiting-list-entries/{id}/decline', params)

/** Hủy chờ (FR-002) */
export const postWaitingListEntriesByIdCancelWaiting = (params) => request('POST', '/waiting-list-entries/{id}/cancel-waiting', params)

/** Xác nhận gia đình còn nhu cầu (đặt lại mốc CFG-M02-03) (FR-017, BR-M02-03) */
export const postWaitingListEntriesByIdConfirmStillNeeded = (params) => request('POST', '/waiting-list-entries/{id}/confirm-still-needed', params)

/** Danh sách hợp đồng (FR-028, FR-029) */
export const getContracts = (params) => request('GET', '/contracts', params)

/** Lập hợp đồng Nháp (Hành chính) (FR-019, FR-025, BR-M02-22) */
export const postContracts = (params) => request('POST', '/contracts', params)

/** Xem hợp đồng (bản nháp hoặc hợp đồng gốc kèm danh sách phụ lục) (FR-019, FR-028) */
export const getContractsById = (params) => request('GET', '/contracts/{id}', params)

/** Sửa nội dung hợp đồng Nháp (FR-019, FR-020, FR-025, FR-025a, BR-M02-09) */
export const putContractsById = (params) => request('PUT', '/contracts/{id}', params)

/** Điều khoản khác chuẩn của hợp đồng Nháp (giá trị chuẩn, giá trị đề xuất) (FR-025a) */
export const getContractsByIdNonStandardTerms = (params) => request('GET', '/contracts/{id}/non-standard-terms', params)

/** Gửi Quản lý viện duyệt điều khoản khác chuẩn (FR-025a) */
export const postContractsByIdSubmitTermsForApproval = (params) => request('POST', '/contracts/{id}/submit-terms-for-approval', params)

/** Gửi ký (FR-020, FR-025, FR-025a) */
export const postContractsByIdSendForSignature = (params) => request('POST', '/contracts/{id}/send-for-signature', params)

/** Trả về nháp (FR-020) */
export const postContractsByIdReturnToDraft = (params) => request('POST', '/contracts/{id}/return-to-draft', params)

/** Ghi nhận đã ký (hợp đồng Hiệu lực) (FR-021, FR-022, FR-036, BR-M02-08, BR-M03-03) */
export const postContractsByIdRecordSigned = (params) => request('POST', '/contracts/{id}/record-signed', params)

/** Hủy hợp đồng Nháp hoặc Chờ ký (FR-020) */
export const postContractsByIdCancel = (params) => request('POST', '/contracts/{id}/cancel', params)

/** Nội dung hợp đồng hiệu lực tại ngày D (FR-024, BR-M02-09) */
export const getContractsByIdContent = (params) => request('GET', '/contracts/{id}/content', params)

/** Danh sách phụ lục của hợp đồng (FR-042, FR-047) */
export const getContractsByIdAddenda = (params) => request('GET', '/contracts/{id}/addenda', params)

/** Khoản cần đặt cọc và trạng thái đặt cọc (do 017 tính) (FR-030, FR-031) */
export const getContractsByIdDeposit = (params) => request('GET', '/contracts/{id}/deposit', params)

/** Danh mục dịch vụ (FR-034) */
export const getServices = (params) => request('GET', '/services', params)

/** Tạo dịch vụ (Quản lý viện) (FR-034) */
export const postServices = (params) => request('POST', '/services', params)

/** Sửa dịch vụ (FR-034) */
export const putServicesById = (params) => request('PUT', '/services/{id}', params)

/** Ngừng hiệu lực dịch vụ (FR-034) */
export const postServicesByIdDeactivate = (params) => request('POST', '/services/{id}/deactivate', params)

/** Các phiên bản đơn giá (FR-035) */
export const getServicesByIdPriceVersions = (params) => request('GET', '/services/{id}/price-versions', params)

/** Tạo phiên bản đơn giá (FR-035, FR-036, BR-M11-02) */
export const postServicesByIdPriceVersions = (params) => request('POST', '/services/{id}/price-versions', params)

/** Hợp đồng Hiệu lực có đơn giá đã ghi khác phiên bản mới nhất (FR-036) */
export const getServicesByIdMismatchedPriceContracts = (params) => request('GET', '/services/{id}/mismatched-price-contracts', params)

/** Lập yêu cầu thay đổi lưu trú ở Nháp (FR-037, FR-038, FR-039, FR-040, FR-043, BR-M01-03, BR-M02-16) */
export const postResidencyChangeRequests = (params) => request('POST', '/residency-change-requests', params)

/** Xem yêu cầu thay đổi lưu trú (FR-038) */
export const getResidencyChangeRequestsById = (params) => request('GET', '/residency-change-requests/{id}', params)

/** Sửa yêu cầu ở Nháp (Hành chính hoàn thiện yêu cầu của người đại diện) (FR-038, FR-039) */
export const putResidencyChangeRequestsById = (params) => request('PUT', '/residency-change-requests/{id}', params)

/** Lượt vắng của một người cao tuổi (FR-048, FR-049) */
export const getAbsences = (params) => request('GET', '/absences', params)

/** Xem lượt vắng, kèm kết quả theo ngày và quyết định giữ giường (FR-048, FR-052, FR-055) */
export const getAbsencesById = (params) => request('GET', '/absences/{id}', params)

/** Gia hạn dự kiến trở lại (Hành chính, Trưởng tầng) (FR-049, BR-M01-01) */
export const postAbsencesByIdExtendExpectedReturn = (params) => request('POST', '/absences/{id}/extend-expected-return', params)

/** Giải phóng giường của lượt vắng (Quản lý viện; Q-395) (FR-055, BR-M02-07) */
export const postAbsencesByIdReleaseBed = (params) => request('POST', '/absences/{id}/release-bed', params)

/** Đính chính lượt vắng đã đóng (thời điểm rời, về) (FR-061) */
export const postAbsencesByIdCorrect = (params) => request('POST', '/absences/{id}/correct', params)

/** Ghi nhận gia đình báo vắng bán trú (Hành chính) (FR-060) */
export const postDayCareAbsenceNotices = (params) => request('POST', '/day-care-absence-notices', params)

/** Danh sách ngày khu bán trú nghỉ (FR-060a) */
export const getDayCareClosureDays = (params) => request('GET', '/day-care-closure-days', params)

/** Khai báo ngày khu bán trú nghỉ (Hành chính) (FR-060a) */
export const postDayCareClosureDays = (params) => request('POST', '/day-care-closure-days', params)

/** Hủy khai báo ngày nghỉ (FR-060a) */
export const postDayCareClosureDaysByIdCancel = (params) => request('POST', '/day-care-closure-days/{id}/cancel', params)

/** Lập hồ sơ kết thúc lưu trú (Hành chính; Bác sĩ với Xuất viện, Chuyển cơ sở vì lý do y tế) (FR-062, FR-063, FR-066) */
export const postResidencyEndings = (params) => request('POST', '/residency-endings', params)

/** Xem hồ sơ kết thúc (FR-062, FR-067, FR-068) */
export const getResidencyEndingsById = (params) => request('GET', '/residency-endings/{id}', params)

/** Danh sách điều kiện kết thúc (a) → (h) (FR-063, FR-064, FR-068) */
export const getResidencyEndingsByIdConditions = (params) => request('GET', '/residency-endings/{id}/conditions', params)

/** Đổi ngày kết thúc dự kiến (FR-066, FR-066a, FR-067) */
export const postResidencyEndingsByIdChangeDate = (params) => request('POST', '/residency-endings/{id}/change-date', params)

/** Hủy hồ sơ kết thúc (FR-066, FR-067) */
export const postResidencyEndingsByIdCancel = (params) => request('POST', '/residency-endings/{id}/cancel', params)

/** Ghi nhận bàn giao người cao tuổi (điều kiện e) (FR-063) */
export const postResidencyEndingsByIdHandover = (params) => request('POST', '/residency-endings/{id}/handover', params)

/** Lập yêu cầu ngoại lệ cho một điều kiện (a)–(d), (h) (FR-064) */
export const postResidencyEndingsByIdRequestException = (params) => request('POST', '/residency-endings/{id}/request-exception', params)

/** Bản ghi qua đời (FR-069, BR-M02-21) */
export const getResidentsByResidentIdDeathRecord = (params) => request('GET', '/residents/{residentId}/death-record', params)

/** Bổ sung nguyên nhân hoặc sửa sai bản ghi qua đời bằng đính chính (FR-069) */
export const postDeathRecordsByIdCorrect = (params) => request('POST', '/death-records/{id}/correct', params)

/** Danh sách việc bắt buộc sau qua đời (FR-071, FR-072) */
export const getResidentsByResidentIdPostDeathTasks = (params) => request('GET', '/residents/{residentId}/post-death-tasks', params)

/** Lập yêu cầu ngoại lệ cho mục quyết toán số dư và tiền cọc khi gia đình còn nợ (Hành chính, Kế toán; bổ sung 2026-10-22, Q-387) (FR-071a) */
export const postResidentsByResidentIdPostDeathTasksRequestSettlementException = (params) => request('POST', '/residents/{residentId}/post-death-tasks/request-settlement-exception', params)

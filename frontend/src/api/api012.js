// File sinh bởi frontend/scripts/gen-api.mjs từ hợp đồng; không sửa tay.

// Nguồn: specs/012-family-portal/contracts (Elder Care – Feature 012 (người thân và cổng thông tin gia đình))
import { request } from './client.js'

/** Tìm hồ sơ người thân theo họ tên, số điện thoại, số giấy tờ (Hành chính) (FR-001) */
export const getFamilyMembers = (params) => request('GET', '/family-members', params)

/** Lập hồ sơ người thân (Hành chính) (FR-001) */
export const postFamilyMembers = (params) => request('POST', '/family-members', params)

/** Hồ sơ người thân và các quan hệ (FR-001, FR-002) */
export const getFamilyMembersById = (params) => request('GET', '/family-members/{id}', params)

/** Sửa thông tin liên hệ (nhật ký trước/sau); bỏ giấy tờ bị chặn khi đang có quyền được phép đón (FR-001, FR-007) */
export const putFamilyMembersById = (params) => request('PUT', '/family-members/{id}', params)

/** Quan hệ của người cao tuổi, vai trò, quyền đang bật và quyền có tác dụng (FR-002, FR-007, FR-011, DBR-02, DBR-03) */
export const getResidentsByResidentIdRelationships = (params) => request('GET', '/residents/{residentId}/relationships', params)

/** Lập quan hệ (mọi quyền tắt, trừ FR-009, FR-010); tùy chọn đặt liên hệ chính, người đại diện đầu tiên (FR-002, FR-003, FR-005, FR-008, DBR-02) */
export const postResidentsByResidentIdRelationships = (params) => request('POST', '/residents/{residentId}/relationships', params)

/** Lệnh trên quan hệ (FR-004, FR-005, FR-006, FR-012, DBR-02) */
export const postRelationshipsByIdByCommand = (params) => request('POST', '/relationships/{id}/{command}', params)

/** Ghi phiếu đăng ký người thân (bản scan bắt buộc); mỗi quyền thành một yêu cầu Hiệu lực cách "bản ký" (FR-008, BR-M10-07) */
export const postResidentsByResidentIdFamilyRegistrationForms = (params) => request('POST', '/residents/{residentId}/family-registration-forms', params)

/** Danh sách được phép đón tại một thời điểm (mặc định hiện tại); lịch sử cho nhân viên (FR-007, FR-068) */
export const getResidentsByResidentIdPickupList = (params) => request('GET', '/residents/{residentId}/pickup-list', params)

/** Yêu cầu thay đổi quyền (lọc người cao tuổi, trạng thái); người đại diện thấy yêu cầu cần mình xác nhận (FR-013, FR-050) */
export const getRightChangeRequests = (params) => request('GET', '/right-change-requests', params)

/** Lập yêu cầu thay đổi quyền, danh sách đón, dấu được tự về, đặt/thôi người đại diện (FR-013, FR-014, FR-015, FR-016, FR-017, FR-018, BR-M10-07) */
export const postRightChangeRequests = (params) => request('POST', '/right-change-requests', params)

/** Xác nhận (người đại diện), duyệt (Quản lý viện, lý do), từ chối (lý do), hủy (người lập, lý do) (FR-005, FR-015, FR-016, FR-017, FR-020, BR-M10-07) */
export const postRightChangeRequestsByIdByCommand = (params) => request('POST', '/right-change-requests/{id}/{command}', params)

/** Ghi đón (chỉ trực tuyến); không đạt thì ghi lần chặn và trả PICKUP_BLOCKED (FR-021, FR-022, FR-023, FR-026, FR-027, FR-028, BR-M10-03) */
export const postResidentsByResidentIdPickups = (params) => request('POST', '/residents/{residentId}/pickups', params)

/** Bản ghi đón, lần chặn đón của người cao tuổi (nhân viên) (FR-023, FR-026) */
export const getResidentsByResidentIdPickupRecords = (params) => request('GET', '/residents/{residentId}/pickup-records', params)

/** Lập yêu cầu ngoại lệ đón cho người ngoài danh sách (FR-024, FR-025, BR-M10-03) */
export const postResidentsByResidentIdPickupExceptions = (params) => request('POST', '/residents/{residentId}/pickup-exceptions', params)

/** Xác nhận (người đại diện qua cổng), duyệt (Quản lý viện), từ chối, hủy (lý do) (FR-024) */
export const postPickupExceptionsByIdByCommand = (params) => request('POST', '/pickup-exceptions/{id}/{command}', params)

/** Khung thăm của một ngày và số chỗ còn (CFG-M10-04) (FR-030) */
export const getVisitSlots = (params) => request('GET', '/visit-slots', params)

/** Lượt thăm theo ngày, người cao tuổi; người thân chỉ thấy lượt của mình (FR-039, FR-046) */
export const getVisits = (params) => request('GET', '/visits', params)

/** Đăng ký thăm; đạt FR-030 thì Đã duyệt ngay và giữ chỗ, không đạt thì 422 kèm mọi lý do, không tạo lượt (FR-029, FR-030, FR-031, BR-M10-02) */
export const postVisits = (params) => request('POST', '/visits', params)

/** Hủy (người đăng ký trước giờ bắt đầu; Hành chính có lý do), vào (Hành chính, kiểm lại (c)(d), người thực tế), ra (giờ ra thực tế) (FR-033, FR-034, FR-035) */
export const postVisitsByIdByCommand = (params) => request('POST', '/visits/{id}/{command}', params)

/** Lượt ở lại theo người cao tuổi, trạng thái (FR-045) */
export const getFamilyStays = (params) => request('GET', '/family-stays', params)

/** Đăng ký ở lại (Hành chính); chặn chồng thời gian theo CFG-M10-10, khoanh vùng, trạng thái (FR-040, FR-041, BR-M10-04) */
export const postFamilyStays = (params) => request('POST', '/family-stays', params)

/** Lệnh trên lượt ở lại theo bảng FR-045 (FR-040, FR-041, FR-042, FR-043, FR-045, BR-M10-04) */
export const postFamilyStaysByIdByCommand = (params) => request('POST', '/family-stays/{id}/{command}', params)

/** Người cao tuổi mà người thân đang đăng nhập có quan hệ Hiệu lực, kèm quyền có tác dụng (FR-046, BR-M10-01) */
export const getFamilyPortalResidents = (params) => request('GET', '/family-portal/residents', params)

/** Thông tin chung: họ tên, ảnh, phòng, trạng thái, lượt vắng, lịch đến/về, lượt thăm và phản hồi của mình, thực đơn chung (bản tin bỏ theo Q-396) (FR-046, FR-047) */
export const getFamilyPortalResidentsByResidentIdGeneral = (params) => request('GET', '/family-portal/residents/{residentId}/general', params)

/** Phần sức khỏe (chỉ khi quyền xem sức khỏe có tác dụng); mỗi lần mở ghi nhật ký lượt xem (FR-046, FR-047, FR-048, BR-M10-01, DBR-03) */
export const getFamilyPortalResidentsByResidentIdHealthBySection = (params) => request('GET', '/family-portal/residents/{residentId}/health/{section}', params)

/** Hợp đồng, phụ lục, đặt cọc, tiến trình kết thúc lưu trú (chỉ người đại diện) (FR-046) */
export const getFamilyPortalResidentsByResidentIdContract = (params) => request('GET', '/family-portal/residents/{residentId}/contract', params)

/** Tra cứu nhật ký lượt xem sức khỏe (Quản lý viện) (FR-048) */
export const getHealthViewLogs = (params) => request('GET', '/health-view-logs', params)

/** Phản hồi (người phụ trách, Quản lý viện gồm danh sách quá hạn; người thân thấy phản hồi của mình) (FR-058, FR-063) */
export const getFeedback = (params) => request('GET', '/feedback', params)

/** Gửi phản hồi (người thân qua cổng; Hành chính ghi thay); đặt người phụ trách, mức, hạn (FR-058, FR-059, FR-061, FR-062, BR-M10-05) */
export const postFeedback = (params) => request('POST', '/feedback', params)

/** Chi tiết phản hồi; bản cho người gửi chỉ có phần trả lời mà họ được thấy (FR-058) (FR-058, FR-065) */
export const getFeedbackById = (params) => request('GET', '/feedback/{id}', params)

/** Lệnh trên phản hồi theo bảng FR-066 (FR-059, FR-060, FR-061, FR-062, FR-064, FR-065, FR-066, BR-M10-05) */
export const postFeedbackByIdByCommand = (params) => request('POST', '/feedback/{id}/{command}', params)

/** Mong muốn của người cao tuổi về khách thăm, người đón (Hiệu lực và lịch sử) kèm quyền trái mong muốn đang hiệu lực (bổ sung 2026-10-18) (FR-020a, FR-020b) */
export const getResidentsByResidentIdVisitPickupWishes = (params) => request('GET', '/residents/{residentId}/visit-pickup-wishes', params)

/** Ghi mong muốn (Hành chính, Trưởng tầng, Điều dưỡng có phạm vi); mong muốn Hiệu lực cùng người, cùng loại chuyển Được thay thế (FR-020a, BR-M01-14) */
export const postResidentsByResidentIdVisitPickupWishes = (params) => request('POST', '/residents/{residentId}/visit-pickup-wishes', params)

/** Lịch sinh hoạt hôm nay của chính người cao tuổi (hoạt động, lượt thăm, lịch đến/về bán trú) (FR-083, BR-M10-11) */
export const getResidentChannelToday = (params) => request('GET', '/resident-channel/today', params)

/** Thực đơn tuần đã công bố và suất ăn dự kiến của mình (món, món thay thế; không có chế độ ăn, dị ứng) (FR-083, FR-084, BR-M10-11) */
export const getResidentChannelMenu = (params) => request('GET', '/resident-channel/menu', params)

/** Buổi hoạt động sắp tới của tầng mình và toàn viện, kèm mình đã có tên chưa (FR-083, BR-M10-11) */
export const getResidentChannelActivities = (params) => request('GET', '/resident-channel/activities', params)

/** Lượt thăm Đã duyệt của mình: ngày, khung giờ, người thăm (FR-083, BR-M10-11) */
export const getResidentChannelVisits = (params) => request('GET', '/resident-channel/visits', params)

/** Đồ gửi của mình ở Đang giữ hoặc Đang được sử dụng: tên, trạng thái, ảnh (FR-083, BR-M10-11) */
export const getResidentChannelBelongings = (params) => request('GET', '/resident-channel/belongings', params)

/** Phản hồi của chính người cao tuổi (kể cả được ghi thay); trả lời nhóm sức khỏe chỉ phần chung (FR-058a, FR-086) */
export const getResidentChannelFeedback = (params) => request('GET', '/resident-channel/feedback', params)

/** Người cao tuổi gửi phản hồi (Góp ý, Hỏi đáp, Khiếu nại, Mong muốn) (FR-058, FR-058a, FR-058b, FR-059, FR-062) */
export const postResidentChannelFeedback = (params) => request('POST', '/resident-channel/feedback', params)

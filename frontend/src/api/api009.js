// File sinh bởi frontend/scripts/gen-api.mjs từ hợp đồng; không sửa tay.

// Nguồn: specs/009-notification-service/contracts (Elder Care – Feature 009 (dịch vụ thông báo))
import { request } from './client.js'

/** Hộp thông báo của tôi (Khẩn cấp chưa xác nhận lên đầu; lọc mức, chưa xem, loại sự kiện, người cao tuổi) (FR-033, FR-034) */
export const getMeNotifications = (params) => request('GET', '/me/notifications', params)

/** Mở thông báo (ghi thời điểm xem lần đầu; nội dung theo quyền hiện hành) (FR-034, FR-044, BR-M13-05, BR-M10-06) */
export const getMeNotificationsById = (params) => request('GET', '/me/notifications/{id}', params)

/** Ẩn thông báo đã xem khỏi danh sách của tôi (bản ghi vẫn còn) (FR-036) */
export const postMeNotificationsByIdHide = (params) => request('POST', '/me/notifications/{id}/hide', params)

/** Luồng SSE thông báo mới cho web (nhân viên và cổng người thân) (FR-016, FR-017) */
export const getNotificationsStream = (params) => request('GET', '/notifications/stream', params)

/** Đăng ký hoặc cập nhật token FCM của thiết bị đang đăng nhập (app nhân viên, app người thân) (FR-016) */
export const postPushDevices = (params) => request('POST', '/push-devices', params)

/** Xác nhận đã nhận thông báo Khẩn cấp (trực tuyến); dừng lượt nhắc gọi khi mọi thông báo của lượt đã xác nhận (FR-026, FR-026a, FR-030, FR-044, BR-M13-02) */
export const postNotificationsByIdAcknowledge = (params) => request('POST', '/notifications/{id}/acknowledge', params)

/** Xác nhận và tiếp nhận cảnh báo/sự cố nguồn trong một thao tác (hai bản ghi riêng) (FR-026a) */
export const postNotificationsByIdAcknowledgeAndTakeCharge = (params) => request('POST', '/notifications/{id}/acknowledge-and-take-charge', params)

/** Lượt nhắc gọi tôi đang được nhắc hoặc có quyền ghi kết quả (Người phụ trách ca, Trưởng tầng, Quản lý viện) (FR-027, FR-029) */
export const getMeCallReminders = (params) => request('GET', '/me/call-reminders', params)

/** Chi tiết lượt nhắc gọi (số điện thoại hiện hành lúc mở, nội dung cần báo, thông báo gộp, lịch sử chuyển nhắc, kết quả) (FR-027, FR-027a, FR-030a) */
export const getCallRemindersById = (params) => request('GET', '/call-reminders/{id}', params)

/** Ghi kết quả gọi (người có quyền gọi; trực tuyến); sau "Không liên lạc được" MAY ghi lần gọi bổ sung (FR-029, FR-029a, FR-030, FR-031, BR-M13-02) */
export const postCallRemindersByIdResults = (params) => request('POST', '/call-reminders/{id}/results', params)

/** Danh sách "cần liên hệ trực tiếp" (Hành chính thực hiện; Quản lý viện xem) (FR-021, FR-038) */
export const getDirectContacts = (params) => request('GET', '/direct-contacts', params)

/** Ghi kết quả liên hệ (Hành chính); Không liên hệ được bắt buộc lý do (FR-021, FR-031) */
export const postDirectContactsByIdResults = (params) => request('POST', '/direct-contacts/{id}/results', params)

/** Lịch sử thông báo (Quản lý viện toàn viện; Trưởng tầng tầng được giao) (FR-037, BR-M13-05) */
export const getNotificationHistory = (params) => request('GET', '/notification-history', params)

/** Danh sách lỗi gửi (Quản lý viện) (FR-038, FR-003, FR-011, FR-020) */
export const getDeliveryErrors = (params) => request('GET', '/delivery-errors', params)

/** Kênh thông báo (FR-016, FR-039) */
export const getNotificationChannels = (params) => request('GET', '/notification-channels', params)

/** Bật/tắt kênh, gắn kênh khác vào mức (Quản lý viện; lý do; nhật ký) (FR-016, FR-040) */
export const putNotificationChannelsByCode = (params) => request('PUT', '/notification-channels/{code}', params)

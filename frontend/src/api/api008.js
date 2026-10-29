// File sinh bởi frontend/scripts/gen-api.mjs từ hợp đồng; không sửa tay.

// Nguồn: specs/008-staff-shift-handover/contracts (Elder Care – Feature 008 (nhân sự, ca trực, bàn giao))
import { request } from './client.js'

/** Danh sách nhân viên (Quản lý viện toàn bộ; Trưởng tầng nhóm công khai; nhân viên chỉ người cùng ca) (FR-001, FR-052) */
export const getStaff = (params) => request('GET', '/staff', params)

/** Tạo hồ sơ nhân viên (Quản lý viện); trạng thái Đang làm việc (FR-001, FR-003) */
export const postStaff = (params) => request('POST', '/staff', params)

/** Tải file mẫu import (.xlsx) (bổ sung 2026-10-22, Q-382) (FR-001b, BR-M09-18) */
export const getStaffImportTemplate = (params) => request('GET', '/staff/import/template', params)

/** Tải file lên, kiểm tra từng dòng, trả xem trước; không ghi dữ liệu (Quản lý viện) (FR-001b, FR-001c, BR-M09-18) */
export const postStaffImportPreview = (params) => request('POST', '/staff/import/preview', params)

/** Gửi lại cùng file để tạo mọi hồ sơ trong một lần; còn dòng lỗi thì không tạo gì (FR-001b, FR-001c, BR-M09-18) */
export const postStaffImportConfirm = (params) => request('POST', '/staff/import/confirm', params)

/** Hồ sơ nhân viên (giới hạn trường FR-052) (FR-001, FR-052) */
export const getStaffById = (params) => request('GET', '/staff/{id}', params)

/** Sửa thông tin mô tả (Quản lý viện); không có DELETE (FR-001) */
export const putStaffById = (params) => request('PUT', '/staff/{id}', params)

/** Cho nghỉ việc (Quản lý viện; lý do; hiệu lực không sớm hơn hiện tại) (FR-003, FR-004, FR-001a, BR-M09-04) */
export const postStaffByIdTerminateEmployment = (params) => request('POST', '/staff/{id}/terminate-employment', params)

/** Nhận lại làm việc (Quản lý viện; lý do); tài khoản không tự mở (FR-003) */
export const postStaffByIdRehire = (params) => request('POST', '/staff/{id}/rehire', params)

/** Đổi bộ phận (Quản lý viện; lý do; người đang là Phụ trách bộ phận phải có người thay) (FR-001a) */
export const postStaffByIdChangeDepartment = (params) => request('POST', '/staff/{id}/change-department', params)

/** Danh mục bộ phận và Phụ trách bộ phận (FR-001a) */
export const getDepartments = (params) => request('GET', '/departments', params)

/** Thêm bộ phận (Quản lý viện); chưa có phụ trách thì Quản lý viện được nhắc (FR-001a) */
export const postDepartments = (params) => request('POST', '/departments', params)

/** Chỉ định / đổi Phụ trách bộ phận (Quản lý viện; người mới trong cùng lệnh) (FR-001a) */
export const postDepartmentsByIdAssignLead = (params) => request('POST', '/departments/{id}/assign-lead', params)

/** Danh mục loại giấy phép/đào tạo (FR-005) */
export const getCredentialTypes = (params) => request('GET', '/credential-types', params)

/** Thêm loại (Quản lý viện) (FR-005) */
export const postCredentialTypes = (params) => request('POST', '/credential-types', params)

/** Danh mục phạm vi hành nghề (dùng chung với 002, 007) (FR-006) */
export const getPracticeScopes = (params) => request('GET', '/practice-scopes', params)

/** Giấy phép, đào tạo của nhân viên và lịch sử, kèm tình trạng hiệu lực suy ra (FR-006, FR-007, FR-052) */
export const getStaffByIdCredentials = (params) => request('GET', '/staff/{id}/credentials', params)

/** Thêm giấy phép/đào tạo (Quản lý viện) (FR-006) */
export const postStaffByIdCredentials = (params) => request('POST', '/staff/{id}/credentials', params)

/** Gia hạn (bản mới trỏ bản cũ; cảnh báo sắp hết hạn của bản cũ đóng) (FR-006, FR-008) */
export const postCredentialsByIdRenew = (params) => request('POST', '/credentials/{id}/renew', params)

/** Ghi nhận thu hồi (lý do, ngày hiệu lực); tại mốc kết thúc phân công cần loại này (FR-009) (FR-006, FR-009, BR-M09-01) */
export const postCredentialsByIdRevoke = (params) => request('POST', '/credentials/{id}/revoke', params)

/** Sửa sai (lý do; nhật ký trước/sau; ngày hết hạn sớm lên áp FR-009) (FR-006, FR-009) */
export const postCredentialsByIdCorrect = (params) => request('POST', '/credentials/{id}/correct', params)

/** Mẫu ca (FR-011) */
export const getShiftTemplates = (params) => request('GET', '/shift-templates', params)

/** Thêm mẫu ca (Quản lý viện; ca qua ngày được; dài hơn 0, không quá 24 giờ) (FR-011) */
export const postShiftTemplates = (params) => request('POST', '/shift-templates', params)

/** Sửa mẫu ca (không ảnh hưởng ca đã tạo) (FR-011) */
export const putShiftTemplatesById = (params) => request('PUT', '/shift-templates/{id}', params)

/** Lịch ca theo tháng, phạm vi (Trưởng tầng tầng được giao; Quản lý viện; nhân viên chỉ ca của mình qua /me/shifts) (FR-012, FR-033, FR-054) */
export const getShiftSchedules = (params) => request('GET', '/shift-schedules', params)

/** Lập lịch Nháp cho tháng, phạm vi (lịch toàn viện chỉ Quản lý viện) (FR-012, FR-016) */
export const postShiftSchedules = (params) => request('POST', '/shift-schedules', params)

/** Tạo ca trong lịch Nháp (từ mẫu; giờ chỉnh riêng được) (FR-012) */
export const postShiftSchedulesByIdShifts = (params) => request('POST', '/shift-schedules/{id}/shifts', params)

/** Chi tiết ca (nhân viên, người phụ trách, Bác sĩ trực, tỷ lệ phục vụ, allowedCommands[]) (FR-012, FR-023, FR-034) */
export const getShiftsById = (params) => request('GET', '/shifts/{id}', params)

/** Sửa ca, xếp/gỡ nhân viên, chỉ định người phụ trách, Bác sĩ trực (chỉ khi lịch Nháp) (FR-012, FR-013, FR-014, FR-015, FR-002, FR-010, BR-M09-03) */
export const putShiftsById = (params) => request('PUT', '/shifts/{id}', params)

/** Xóa ca (chỉ lịch Nháp; phân công Nháp xóa cùng) (FR-012, FR-025) */
export const deleteShiftsById = (params) => request('DELETE', '/shifts/{id}', params)

/** Điều kiện chặn và cảnh báo không chặn trước khi công bố (FR-014, FR-034, FR-010; nghỉ theo ngày của 015) (FR-016) */
export const getShiftSchedulesByIdPublicationPreview = (params) => request('GET', '/shift-schedules/{id}/publication-preview', params)

/** Công bố lịch (Trưởng tầng được giao; Quản lý viện; lịch toàn viện chỉ Quản lý viện) (FR-016, FR-027, FR-034, FR-055, BR-M09-03) */
export const postShiftSchedulesByIdPublish = (params) => request('POST', '/shift-schedules/{id}/publish', params)

/** Bổ sung nhân viên vào ca đã công bố hoặc đang diễn ra (Trưởng tầng của tầng, Quản lý viện; lý do) (FR-018, FR-013, FR-014) */
export const postShiftsByIdAddStaff = (params) => request('POST', '/shifts/{id}/add-staff', params)

/** Chuyển người phụ trách ca (Trưởng tầng của tầng, Quản lý viện; lý do); bàn giao đang dở chuyển theo (FR-019) */
export const postShiftsByIdChangeLead = (params) => request('POST', '/shifts/{id}/change-lead', params)

/** Ghi nhận vắng ca (Trưởng tầng; Người phụ trách ca của chính ca đó) (FR-020, BR-M04-13) */
export const postShiftsByIdRecordAbsence = (params) => request('POST', '/shifts/{id}/record-absence', params)

/** Hủy ca đã công bố chưa bắt đầu (Quản lý viện; lý do) (FR-021) */
export const postShiftsByIdCancel = (params) => request('POST', '/shifts/{id}/cancel', params)

/** Ca và phân công của chính mình, người cùng ca, người phụ trách (mọi nhân viên có ca) (FR-033, FR-054) */
export const getMeShifts = (params) => request('GET', '/me/shifts', params)

/** Bác sĩ trực tại thời điểm (nhiều người được) (FR-023) */
export const getOnDutyDoctors = (params) => request('GET', '/on-duty-doctors', params)

/** Giao Trưởng tầng theo tầng và thời gian (Quản lý viện; Trưởng tầng xem) (FR-024) */
export const getFloorManagerAssignments = (params) => request('GET', '/floor-manager-assignments', params)

/** Giao Trưởng tầng (Quản lý viện); chồng thời gian cùng tầng bị chặn (409 FLOOR_MANAGER_ASSIGNMENT_OVERLAP) (FR-024) */
export const postFloorManagerAssignments = (params) => request('POST', '/floor-manager-assignments', params)

/** Kết thúc hoặc rút ngắn giao (Quản lý viện; lý do) (FR-024) */
export const postFloorManagerAssignmentsByIdEnd = (params) => request('POST', '/floor-manager-assignments/{id}/end', params)

/** Phân công của ca, nhân viên chính theo người cao tuổi, người chưa có Điều dưỡng phụ trách / nhân viên chăm sóc chính (FR-025, FR-028, FR-029) */
export const getShiftsByIdAssignments = (params) => request('GET', '/shifts/{id}/assignments', params)

/** Lập phân công (Trưởng tầng trong phạm vi, Quản lý viện) (FR-025, FR-027, FR-028, FR-028a, FR-030, FR-031, BR-M09-01) */
export const postShiftsByIdAssignments = (params) => request('POST', '/shifts/{id}/assignments', params)

/** Kiểm tra phân công và cảnh báo chứng chỉ trước khi lưu (FR-025, FR-028, FR-028a, FR-030, FR-031) */
export const postShiftsByIdAssignmentsPreview = (params) => request('POST', '/shifts/{id}/assignments/preview', params)

/** Xóa phân công Nháp (chỉ khi lịch Nháp) (FR-025) */
export const deleteAssignmentsById = (params) => request('DELETE', '/assignments/{id}', params)

/** Hủy phân công Tương lai (lý do) (FR-027) */
export const postAssignmentsByIdCancel = (params) => request('POST', '/assignments/{id}/cancel', params)

/** Kết thúc phân công Hiệu lực (Trưởng tầng, Người phụ trách ca trong ca, Quản lý viện; lý do); công việc, liều gán lại hoặc thành chung (FR-027) */
export const postAssignmentsByIdEnd = (params) => request('POST', '/assignments/{id}/end', params)

/** Sao chép phân công từ ca nguồn cùng phạm vi (Trưởng tầng); trả danh sách không sao chép kèm lý do (FR-032) */
export const postShiftsByIdCopyAssignments = (params) => request('POST', '/shifts/{id}/copy-assignments', params)

/** Các lần tính tỷ lệ phục vụ của ca (FR-034, FR-037, BR-M09-02) */
export const getShiftsByIdStaffingRatios = (params) => request('GET', '/shifts/{id}/staffing-ratios', params)

/** Tìm bàn giao của ca trong phạm vi được xem; chưa lập trả 404 (FR-038, FR-047) */
export const getShiftsByIdHandovers = (params) => request('GET', '/shifts/{id}/handovers', params)

/** Lập bàn giao (Bản nháp) từ mốc CFG-M09-04 trước kết ca; không có mục lập sẵn (FR-038, FR-039, FR-042, BR-M09-06) */
export const postShiftsByIdHandovers = (params) => request('POST', '/shifts/{id}/handovers', params)

/** Bàn giao (mục với trạng thái lúc chốt và hiện tại, phát sinh sau khi lập, ý kiến; giới hạn trường FR-047, FR-050) (FR-040, FR-045, FR-047, FR-050) */
export const getHandoversById = (params) => request('GET', '/handovers/{id}', params)

/** Danh sách bản ghi nguồn đang mở theo nhóm (a) → (g) để tham khảo; afterPreparation = true chỉ lấy phát sinh sau thời điểm Đã lập (FR-039, FR-045) */
export const getHandoversByIdSourceRecords = (params) => request('GET', '/handovers/{id}/source-records', params)

/** Thêm mục (Bản nháp; hoặc phần "Phát sinh sau khi lập" khi Đã lập / Có ý kiến) (FR-040, FR-041, FR-051) */
export const postHandoversByIdItems = (params) => request('POST', '/handovers/{id}/items', params)

/** Sửa mục, ghi chú, đánh dấu đã xử lý (chỉ Bản nháp) (FR-040) */
export const putHandoversByIdItemsByItemId = (params) => request('PUT', '/handovers/{id}/items/{itemId}', params)

/** Bỏ mục (chỉ Bản nháp; nhóm chỉ số vượt ngưỡng và biến động người cao tuổi không bỏ được, Q-88) (FR-040) */
export const deleteHandoversByIdItemsByItemId = (params) => request('DELETE', '/handovers/{id}/items/{itemId}', params)

/** Ghi nhận định chung và theo người cao tuổi (chỉ Bản nháp); nhận định chung có nhắc không ghi thông tin sức khỏe từng người (FR-040, FR-051) */
export const putHandoversByIdAssessments = (params) => request('PUT', '/handovers/{id}/assessments', params)

/** Hoàn tất bàn giao (chốt nội dung); thiếu ghi chú mục nghiêm trọng 409 CRITICAL_ITEM_NOTE_REQUIRED; bắt buộc trực tuyến (FR-041, FR-042, FR-022, FR-060, BR-M09-06, BR-M09-07) */
export const postHandoversByIdComplete = (params) => request('POST', '/handovers/{id}/complete', params)

/** Xác nhận bàn giao (người phụ trách ca sau trong phạm vi ca sau; Trưởng tầng) (FR-042, FR-044, FR-060, BR-M09-08, BR-M05-05) */
export const postHandoversByIdConfirm = (params) => request('POST', '/handovers/{id}/confirm', params)

/** Gửi ý kiến (nội dung bắt buộc) (FR-042) */
export const postHandoversByIdComments = (params) => request('POST', '/handovers/{id}/comments', params)

/** Bổ sung và gửi lại (người bàn giao trong phạm vi ca; Trưởng tầng); phản hồi cho từng ý kiến (FR-041, FR-042) */
export const postHandoversByIdSupplementAndResubmit = (params) => request('POST', '/handovers/{id}/supplement-and-resubmit', params)

/** Các lần dùng quyền theo nhiệm vụ hoặc thay thế (căn cứ khác "vai trò"), theo tầng và thời gian (Quản lý viện) (FR-056) */
export const getDutyBasisAuditLogs = (params) => request('GET', '/duty-basis-audit-logs', params)

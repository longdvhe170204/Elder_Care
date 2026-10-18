// DỮ LIỆU MINH HỌA cho màn mẫu của giai đoạn thiết kế giao diện.
// Không phải dữ liệu thật, không phải quy tắc nghiệp vụ. Xóa khi màn hình lấy dữ liệu từ API (000 T010).
// Tên trạng thái lấy đúng theo docs/business/nghiep-vu.md: người cao tuổi (5.5), giường (7.2),
// công việc (8.x), mức chăm sóc (mục 4), mức cảnh báo (Nhẹ, Trung bình, Khẩn cấp).

export const DEMO_SCOPE = { floor: 'Tầng 2', shift: 'Ca ngày', updatedAt: '10:05' }

export const DEMO_ESCALATED_ALERT = {
  room: '202-A',
  resident: '[Họ tên NCT 3]',
  level: 'Trung bình',
  note: 'Đã leo thang tối đa, chưa có người tiếp nhận',
}

export const DEMO_INDICATORS = [
  { label: 'Đang lưu trú', value: 38 },
  { label: 'Công việc hôm nay', value: 40 },
  { label: 'Quá hạn', value: 3, detail: '1 bắt buộc', urgent: true },
  { label: 'Cảnh báo đang mở', value: 2 },
  { label: 'Sự cố đang mở', value: 1 },
  { label: 'Bàn giao chưa xác nhận', value: 1 },
]

// Công việc trong ca, nhóm theo khung giờ ở Tổng quan. "[Công việc chuyên môn minh họa]" là công việc chuyên môn (8.5),
// tên thật lấy từ danh mục. Giờ hiện tại của bản mẫu là DEMO_SCOPE.updatedAt.
export const DEMO_TASKS = [
  { time: '07:30', title: 'Đo chỉ số sức khỏe', resident: '[Họ tên NCT 1]', room: '201-A', assignee: '[Điều dưỡng 1]', status: 'Hoàn thành' },
  { time: '07:30', title: 'Hỗ trợ ăn bữa sáng', resident: '[Họ tên NCT 3]', room: '202-A', assignee: '[NV chăm sóc 2]', status: 'Hoàn thành' },
  { time: '07:45', title: 'Hỗ trợ vệ sinh cá nhân', resident: '[Họ tên NCT 2]', room: '201-B', assignee: '[NV chăm sóc 1]', status: 'Hoàn thành' },
  { time: '08:00', title: '[Công việc chuyên môn minh họa]', resident: '[Họ tên NCT 2]', room: '201-B', assignee: '[Bác sĩ 1]', status: 'Hoàn thành' },
  { time: '08:30', title: 'Đo chỉ số sức khỏe', resident: '[Họ tên NCT 3]', room: '202-A', assignee: '[Điều dưỡng 1]', status: 'Quá hạn', mandatory: true },
  { time: '09:00', title: 'Hỗ trợ vận động', resident: '[Họ tên NCT 5]', room: '—', assignee: '[NV chăm sóc 2]', status: 'Quá hạn' },
  { time: '09:30', title: 'Hỗ trợ uống nước', resident: '[Họ tên NCT 1]', room: '201-A', assignee: '[NV chăm sóc 1]', status: 'Đến hạn' },
  { time: '09:30', title: 'Thay tã', resident: '[Họ tên NCT 2]', room: '201-B', assignee: '[NV chăm sóc 1]', status: 'Đến hạn' },
  { time: '10:00', title: 'Đo chỉ số sức khỏe', resident: '[Họ tên NCT 1]', room: '201-A', assignee: '[Điều dưỡng 1]', status: 'Đến hạn' },
  { time: '10:00', title: '[Công việc chuyên môn minh họa]', resident: '[Họ tên NCT 3]', room: '202-A', assignee: '[Bác sĩ 1]', status: 'Đến hạn' },
  { time: '10:30', title: 'Hỗ trợ vận động', resident: '[Họ tên NCT 2]', room: '201-B', assignee: '[NV chăm sóc 2]', status: 'Chưa đến hạn' },
  { time: '11:00', title: 'Hỗ trợ ăn bữa trưa', resident: '[Họ tên NCT 3]', room: '202-A', assignee: null, status: 'Chưa đến hạn' },
  { time: '11:00', title: 'Hỗ trợ ăn bữa trưa', resident: '[Họ tên NCT 1]', room: '201-A', assignee: '[NV chăm sóc 1]', status: 'Chưa đến hạn' },
  { time: '11:00', title: 'Hỗ trợ ăn bữa trưa', resident: '[Họ tên NCT 2]', room: '201-B', assignee: '[NV chăm sóc 1]', status: 'Chưa đến hạn' },
]

export const DEMO_BEDS = [
  ...Array(30).fill('Đang sử dụng'),
  'Đang giữ chỗ',
  'Đang giữ chỗ',
  'Trống',
  'Trống',
  'Trống',
  'Chờ sẵn sàng',
  'Đang bảo trì',
  'Tạm ngừng sử dụng',
]

// Bàn giao ca (13.5): Có ý kiến nghĩa là quay lại người bàn giao bổ sung rồi gửi lại (Q-87)
export const DEMO_HANDOVER = {
  from: 'Ca đêm',
  to: 'Ca ngày',
  status: 'Có ý kiến',
  giver: '[Điều dưỡng 2]',
  receiver: '[Điều dưỡng 1]',
  openTasks: 2,
  openAlerts: 1,
  comment: '[Ý kiến minh họa: thiếu ghi chú cho cảnh báo phòng 202-A]',
}

export const DEMO_RESIDENTS = [
  { id: 1, code: 'NCT-0001', name: '[Họ tên NCT 1]', initials: 'N1', room: '201-A', careLevel: 'Thường xuyên', stayType: 'Nội trú dài hạn', status: 'Đang lưu trú', allergies: ['[Dị ứng minh họa]'], careNote: '[Lưu ý chăm sóc minh họa]', today: { tasks: 2, overdue: 0, doses: 2 }, careNeeds: ['Hỗ trợ vận động'], primaryCaregiver: '[NV chăm sóc 1]', preferences: [
    { group: '[Nhóm sở thích 1]', level: 'Thích', source: 'resident', recordedBy: '[NV chăm sóc 1]', recordedAt: '02/10/2026' },
    { group: '[Nhóm sở thích 2]', level: 'Không thích', source: 'resident', recordedBy: '[Điều dưỡng 1]', recordedAt: '05/10/2026' },
    { group: '[Nhóm sở thích 2]', level: 'Thích', source: 'family', relative: 'con gái, [Người thân 1]', recordedBy: '[Hành chính 1]', recordedAt: '01/10/2026' },
  ], riskChoices: [{ choice: '[Lựa chọn minh họa: vẫn uống trà thường mỗi chiều]', reviewOn: '17/11/2026' }], photo: null },
  { id: 2, code: 'NCT-0002', name: '[Họ tên NCT 2]', initials: 'N2', room: '201-B', careLevel: 'Đặc biệt', stayType: 'Nội trú dài hạn', status: 'Đang lưu trú', allergies: [], careNote: '[Lưu ý chăm sóc minh họa]', today: { tasks: 3, overdue: 1, doses: 3 }, careNeeds: ['Phục hồi sau tai biến', 'Theo dõi sức khỏe thường xuyên'], primaryCaregiver: '[NV chăm sóc 1]', preferences: [], photo: null },
  { id: 3, code: 'NCT-0003', name: '[Họ tên NCT 3]', initials: 'N3', room: '202-A', careLevel: 'Cơ bản', stayType: 'Nội trú ngắn ngày', status: 'Đang lưu trú', openAlert: true, allergies: ['[Dị ứng minh họa]'], careNote: '', today: { tasks: 1, overdue: 0, doses: 1 }, careNeeds: [], primaryCaregiver: '[Điều dưỡng 1]', preferences: [{ group: '[Nhóm sở thích 1]', level: 'Thích', source: 'staff', recordedBy: '[NV chăm sóc 2]', recordedAt: '08/10/2026' }], photo: null },
  { id: 4, code: 'NCT-0004', name: '[Họ tên NCT 4]', initials: 'N4', room: '203-A', careLevel: 'Thường xuyên', stayType: 'Nội trú dài hạn', status: 'Tạm vắng', allergies: [], careNote: '', today: { tasks: 0, overdue: 0, doses: 0 }, careNeeds: ['Hỗ trợ vận động'], primaryCaregiver: '[NV chăm sóc 2]', preferences: [], photo: null },
  { id: 5, code: 'NCT-0005', name: '[Họ tên NCT 5]', initials: 'N5', room: '—', careLevel: 'Cơ bản', stayType: 'Bán trú', status: 'Đang lưu trú', allergies: [], careNote: '', today: { tasks: 2, overdue: 0, doses: 0 }, careNeeds: [], primaryCaregiver: '[NV chăm sóc 2]', preferences: [], photo: null },
  { id: 6, code: 'NCT-0006', name: '[Họ tên NCT 6]', initials: 'N6', room: '206-A', careLevel: 'Đặc biệt', stayType: 'Nội trú dài hạn', status: 'Điều trị tại bệnh viện', allergies: [], careNote: '', today: { tasks: 0, overdue: 0, doses: 0 }, careNeeds: ['Theo dõi sức khỏe thường xuyên'], primaryCaregiver: '[Điều dưỡng 1]', preferences: [], photo: null },
  { id: 7, code: 'NCT-0007', name: '[Họ tên NCT 7]', initials: 'N7', room: '207-A', careLevel: 'Thường xuyên', stayType: 'Nội trú dài hạn', status: 'Đang tiếp nhận', allergies: [], careNote: '', today: { tasks: 0, overdue: 0, doses: 0 }, careNeeds: [], primaryCaregiver: null, preferences: [], photo: null },
]

// Checklist ca (bản mẫu màn 005 User Story 4). Giờ hiện tại thật lấy từ máy chủ (NFR-09);
// ở đây cố định để màn mẫu ổn định. initials chỉ dùng cho tên minh họa; tên thật lấy chữ cái của tên gọi. Loại kết quả theo 005 US4: ml, mức ăn, giá trị đo, số lượng.
export const DEMO_SHIFT = {
  name: 'Ca ngày',
  floor: 'Tầng 2',
  start: '06:00',
  end: '14:00',
  now: '09:40',
  residentsAssigned: 4,
}

export const DEMO_CARE_TASKS = [
  { id: 't1', from: '07:00', to: '08:00', title: 'Hỗ trợ uống nước', kind: 'water', importance: 'Bắt buộc', status: 'Quá hạn', bed: '201-A', resident: '[Họ tên NCT 1]', residentCode: 'NCT-0001', initials: 'N1', careNote: '[Lưu ý chăm sóc minh họa]', assignee: '[NV chăm sóc 1]' },
  { id: 't2', from: '08:00', to: '09:00', title: 'Hỗ trợ vận động', kind: 'simple', importance: 'Thường', status: 'Quá hạn', bed: '203-A', resident: '[Họ tên NCT 4]', residentCode: 'NCT-0004', initials: 'N4', carriedOver: true, assignee: '[NV chăm sóc 2]' },
  { id: 't3', from: '09:30', to: '10:30', title: 'Hỗ trợ ăn bữa phụ', kind: 'meal', importance: 'Thường', status: 'Đến hạn', bed: '201-A', resident: '[Họ tên NCT 1]', residentCode: 'NCT-0001', initials: 'N1', careNote: '[Lưu ý chăm sóc minh họa]', assignee: '[NV chăm sóc 1]' },
  { id: 't4', from: '09:00', to: '10:00', title: 'Đo huyết áp', kind: 'measure', unit: 'mmHg', importance: 'Bắt buộc', status: 'Đến hạn', bed: '202-A', resident: '[Họ tên NCT 3]', residentCode: 'NCT-0003', initials: 'N3', assignee: '[Điều dưỡng 1]' },
  { id: 't5', from: '09:30', to: '10:00', title: 'Thay tã', kind: 'quantity', unit: 'cái', importance: 'Thường', status: 'Đến hạn', bed: '203-A', resident: '[Họ tên NCT 4]', residentCode: 'NCT-0004', initials: 'N4', assignee: '[NV chăm sóc 2]' },
  { id: 't6', from: '09:00', to: '11:00', title: '[Việc chung của tầng, minh họa]', kind: 'simple', importance: 'Thường', status: 'Đến hạn', floorTask: true, assignee: null },
  { id: 't7', from: '11:00', to: '12:00', title: 'Hỗ trợ ăn bữa trưa', kind: 'meal', importance: 'Thường', status: 'Chưa đến hạn', bed: '201-B', resident: '[Họ tên NCT 2]', residentCode: 'NCT-0002', initials: 'N2', assignee: '[NV chăm sóc 1]' },
  { id: 't8', from: '11:00', to: '11:30', title: 'Đo huyết áp', kind: 'measure', unit: 'mmHg', importance: 'Thường', status: 'Chưa đến hạn', bed: '201-A', resident: '[Họ tên NCT 1]', residentCode: 'NCT-0001', initials: 'N1', assignee: '[Điều dưỡng 1]' },
  { id: 't9', from: '12:30', to: '13:30', title: 'Hỗ trợ uống nước', kind: 'water', importance: 'Thường', status: 'Chưa đến hạn', bed: '202-A', resident: '[Họ tên NCT 3]', residentCode: 'NCT-0003', initials: 'N3', assignee: '[NV chăm sóc 2]' },
  { id: 't10', from: '06:30', to: '07:00', title: 'Hỗ trợ vệ sinh cá nhân', kind: 'simple', importance: 'Thường', status: 'Hoàn thành', bed: '203-A', resident: '[Họ tên NCT 4]', residentCode: 'NCT-0004', initials: 'N4', result: 'Đã làm lúc 06:50', assignee: '[NV chăm sóc 2]' },
  { id: 't11', from: '07:00', to: '08:00', title: 'Hỗ trợ ăn bữa sáng', kind: 'meal', importance: 'Thường', status: 'Hoàn thành', bed: '202-A', resident: '[Họ tên NCT 3]', residentCode: 'NCT-0003', initials: 'N3', result: 'Phần lớn', assignee: '[NV chăm sóc 1]' },
  { id: 't12', from: '07:30', to: '08:00', title: 'Hỗ trợ uống nước', kind: 'water', importance: 'Thường', status: 'Không thực hiện', bed: '201-B', resident: '[Họ tên NCT 2]', residentCode: 'NCT-0002', initials: 'N2', result: 'Người cao tuổi từ chối', assignee: '[NV chăm sóc 1]' },
]

// Loại lý do Không thực hiện (Q-350): "Người cao tuổi từ chối" luôn có; các loại khác là danh mục nhóm 1.
export const DEMO_NOT_DONE_REASONS = ['Người cao tuổi từ chối', '[Loại lý do minh họa 1]', '[Loại lý do minh họa 2]']

export const DEMO_MEAL_LEVELS = ['Ăn hết', 'Phần lớn', 'Một phần', 'Không ăn', 'Bỏ bữa']

// Lịch sử ý kiến (DESIGN.md 2.2, màn A): mục cũ của sở thích, giữ nguyên nội dung, ghi ngày bị thay (Q-347, Q-363).
export const DEMO_PREFERENCE_HISTORY = {
  'NCT-0001': [
    { group: '[Nhóm sở thích 2]', level: 'Thích', source: 'resident', recordedBy: '[NV chăm sóc 1]', recordedAt: '12/09/2026', replacedAt: '05/10/2026' },
  ],
}

// Ý kiến về việc vào ở (6.1, Q-356) và điều kiện của Hoàn tất tiếp nhận (5.6, 004), màn D.
export const DEMO_ADMISSION = {
  'NCT-0007': {
    opinion: { value: 'Không đồng ý', source: 'resident', recordedBy: '[Hành chính 1]', recordedAt: '10/10/2026', note: '[Ghi chú minh họa]' },
    conditions: ['[Điều kiện tiếp nhận minh họa 1]', '[Điều kiện tiếp nhận minh họa 2]', '[Điều kiện tiếp nhận minh họa 3]'],
  },
}

// Sự cố hai phần (14.5, Q-352, Q-358), màn B. Thông báo do hệ thống gửi; bản ghi gửi theo BR-M10-06.
export const DEMO_INCIDENT = {
  code: 'SC-0012',
  resident: '[Họ tên NCT 1]',
  residentCode: 'NCT-0001',
  initials: 'N1',
  room: '201-A',
  general: { type: 'Ngã', level: 'Trung bình', at: '14:10, 12/10/2026', handled: '[Việc viện đã xử lý, minh họa]' },
  health: {
    description: '[Mô tả minh họa]',
    firstAid: '[Xử lý ban đầu minh họa]',
    progress: '[Diễn biến minh họa]',
    injury: '[Tổn thương minh họa]',
  },
  deliveries: [
    { relative: '[Người thân 1]', relation: 'Con gái', parts: 'Phần chung, phần sức khỏe', basis: 'Trong phạm vi đồng ý', at: '14:40' },
    { relative: '[Người thân 2]', relation: 'Con trai', parts: 'Phần chung', basis: 'Trong phạm vi đồng ý', at: '14:40' },
  ],
}

// Người thân, mong muốn về khách thăm và người đón (14.1, Q-351, Q-364), màn C.
export const DEMO_RELATIVES = [
  { name: '[Người thân 1]', relation: 'Con gái', representative: true, primaryContact: true, canPickUp: true },
  { name: '[Người thân 2]', relation: 'Con trai', canPickUp: true },
  { name: '[Người thân 3]', relation: 'Cháu', canPickUp: false },
]

export const DEMO_VISIT_WISHES = [
  { about: '[Người thân 3]', text: 'Không muốn [Người thân 3] đón', source: 'resident', recordedBy: '[Trưởng tầng 1]', recordedAt: '03/10/2026' },
  { about: '[Người thân 2]', text: 'Muốn [Người thân 2] đến thăm', source: 'resident', recordedBy: '[Điều dưỡng 1]', recordedAt: '03/10/2026' },
]

// Yêu cầu BR-M10-07 (vòng đời Q-118: Chờ xác nhận → Hiệu lực / Từ chối / Hủy).
export const DEMO_RELATIVE_REQUEST = {
  change: 'Thêm [Người thân 3] vào người được phép đón',
  about: '[Người thân 3]',
  requestedBy: '[Người thân 1], người đại diện',
  requestedAt: '14/10/2026',
  status: 'Chờ xác nhận',
}

// DỮ LIỆU MINH HỌA cho màn mẫu của giai đoạn thiết kế giao diện.
// Không phải dữ liệu thật, không phải quy tắc nghiệp vụ. Xóa khi màn hình lấy dữ liệu từ API (000 T010).
// Tên trạng thái lấy đúng theo docs/business/nghiep-vu.md: người cao tuổi (5.5), giường (7.2),
// công việc (8.x), mức chăm sóc (mục 4), mức cảnh báo (Nhẹ, Trung bình, Khẩn cấp).

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
    { relative: '[Người thân 3]', relation: 'Cháu', parts: 'Phần chung', basis: 'Trong phạm vi đồng ý', at: '14:40' },
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

// ---------------------------------------------------------------------------------------------
// Cổng người thân (DESIGN.md mục 2.2, docs/thiet-ke/man-hinh-theo-actor.md M37, P1 → P11).
// Người xem của bản mẫu: người đại diện, người liên hệ chính, có tên trong bản đồng ý và đủ quyền, để thấy mọi phần
// của cổng. Người thân thiếu quyền thì phần tương ứng ẩn đi (DESIGN.md 2.3). Quyền thật do backend trả.
export const DEMO_PORTAL_VIEWER = {
  name: '[Người thân 1]',
  relation: 'Con gái',
  representative: true,
  primaryContact: true,
  perms: { health: true, costs: true, visits: true, pickup: true, serviceChange: true },
}

// Tên quyền theo 14.1; dùng ở trang Tài khoản để người thân biết mình được xem, làm gì.
export const DEMO_PORTAL_PERMISSION_LABELS = [
  ['health', 'Xem sức khỏe'],
  ['costs', 'Xem chi phí'],
  ['visits', 'Đăng ký thăm'],
  ['pickup', 'Được phép đón'],
  ['serviceChange', 'Được yêu cầu thay đổi dịch vụ'],
]

// Lịch hôm nay (FR-046): dòng health chỉ hiện với người có quyền xem sức khỏe.
export const DEMO_PORTAL_TODAY = [
  { time: '07:30', text: 'Hỗ trợ ăn bữa sáng: Phần lớn', tag: 'Chăm sóc', health: true },
  { time: '08:00', text: 'Thuốc cữ sáng: đã dùng', tag: 'Thuốc', health: true },
  { time: '09:30', text: '[Hoạt động minh họa 1]', tag: 'Hoạt động' },
  { time: '15:00', text: 'Lượt thăm', tag: 'Lượt thăm' },
]

export const DEMO_PORTAL_HEALTH = {
  allergies: ['[Dị ứng minh họa]'],
  conditions: ['[Bệnh nền minh họa]'],
  indicators: [
    { name: 'Huyết áp', value: '[120/80] mmHg', at: '08:30 hôm nay' },
    { name: 'Nhịp tim', value: '[72] lần/phút', at: '08:30 hôm nay' },
    { name: 'Nhiệt độ', value: '[36,6] °C', at: '08:30 hôm nay' },
  ],
  weight: [
    { week: '22/09', value: '[52,4] kg' },
    { week: '29/09', value: '[52,1] kg' },
    { week: '06/10', value: '[52,0] kg' },
    { week: '13/10', value: '[51,8] kg' },
  ],
  medicines: [
    { name: '[Tên thuốc minh họa 1]', dose: '[liều minh họa]', times: 'Sáng, tối' },
    { name: '[Tên thuốc minh họa 2]', dose: '[liều minh họa]', times: 'Sáng' },
  ],
  eating: { meals: '[Phần lớn ở 17 trên 21 bữa]', water: '[1.200] ml mỗi ngày' },
  diet: '[Chế độ ăn riêng minh họa]',
  externalVisits: [{ date: '24/10/2026', place: '[Cơ sở khám bên ngoài minh họa]' }],
  updatedAt: '08:30 hôm nay',
}

// Bản tin (012 FR-051 → FR-057): Điều dưỡng lập, duyệt rồi gửi; nội dung chia phần theo quyền.
export const DEMO_PORTAL_BULLETINS = [
  {
    id: 'b2',
    period: '06/10 → 12/10/2026',
    approvedBy: '[Điều dưỡng 1]',
    sentAt: '13/10/2026, 09:00',
    general: {
      activities: 4,
      absentDays: 0,
      incidents: ['Ngã, mức Trung bình, 12/10'],
      comment: '[Nhận xét phần chung minh họa]',
    },
    health: {
      meals: '[Phần lớn ở 17 trên 21 bữa]',
      water: '[1.200] ml mỗi ngày',
      weight: '[52,0] kg, [giảm 0,1] kg so với tuần trước',
      indicators: '[Các chỉ số chính minh họa]',
      explanation: '[Giải thích của người duyệt về sự cố mức Trung bình, minh họa]',
    },
    costs: [
      { type: 'Phí lưu trú định kỳ', amount: '[7.000.000] đ' },
      { type: 'Vật phẩm tiêu hao', amount: '[350.000] đ' },
      { type: 'Thuốc', amount: '[210.000] đ' },
    ],
  },
  {
    id: 'b1',
    period: '29/09 → 05/10/2026',
    approvedBy: '[Điều dưỡng 1]',
    sentAt: '06/10/2026, 09:00',
    corrected: true,
    general: { activities: 3, absentDays: 0, incidents: [], comment: '[Nhận xét phần chung minh họa]' },
    health: {
      meals: '[Phần lớn ở 18 trên 21 bữa]',
      water: '[1.250] ml mỗi ngày',
      weight: '[52,1] kg',
      indicators: '[Các chỉ số chính minh họa]',
    },
    costs: [
      { type: 'Phí lưu trú định kỳ', amount: '[7.000.000] đ' },
      { type: 'Vật phẩm tiêu hao', amount: '[300.000] đ' },
    ],
  },
]

// Thăm (14.2, 012 FR-029 → FR-039): khung giờ CFG-M10-04, tối đa người mỗi lượt CFG-M10-06.
export const DEMO_PORTAL_VISIT_SLOTS = [
  { label: '09:00 → 11:00', left: 12 },
  { label: '15:00 → 17:00', left: 2 },
]
export const DEMO_PORTAL_VISIT_MAX_PEOPLE = 3
export const DEMO_PORTAL_VISITS = [
  { id: 'v1', when: '15:00 → 17:00, 18/10/2026', people: 2, status: 'Đã duyệt' },
  { id: 'v2', when: '09:00 → 11:00, 11/10/2026', people: 1, status: 'Đã ra' },
  { id: 'v4', when: '15:00 → 17:00, 05/10/2026', people: 1, status: 'Hủy', reason: 'Khu đang khoanh vùng' },
]

// Thông báo (009, Q-353): Khẩn cấp tới người liên hệ chính, nội dung tối thiểu (Q-96, Q-72).
export const DEMO_PORTAL_NOTIFICATIONS = [
  {
    id: 'n1',
    level: 'Khẩn cấp',
    text: 'Có tình huống khẩn cấp liên quan [Họ tên NCT 1], đề nghị liên hệ viện.',
    at: '14:05, 12/10/2026',
    needsAck: true,
  },
  { id: 'n2', text: 'Bản tin tuần 06/10 → 12/10/2026', at: '09:00, 13/10/2026' },
  { id: 'n3', text: 'Lượt thăm 18/10/2026 đã được duyệt', at: '10:12, 10/10/2026' },
  { id: 'n4', text: 'Lượt thăm 05/10/2026 bị hủy: khu đang khoanh vùng', at: '07:40, 05/10/2026' },
]

// Việc chờ người đại diện quyết định (012 FR-046a, FR-046b, FR-050).
export const DEMO_PORTAL_CONFIRMATIONS = [
  {
    id: 'c1',
    kind: 'Đề nghị mua hộ',
    text: '[Vật phẩm minh họa], số lượng 1, dự kiến [450.000] đ',
    detail: 'Lý do: [lý do minh họa]. Người yêu cầu: [Hành chính 1].',
    status: 'Chờ đồng ý',
    accept: 'Đồng ý',
  },
  {
    id: 'c2',
    kind: 'Xác nhận người nhận khác',
    text: 'Trả [Kính] cho [Họ tên người nhận minh họa]',
    detail: 'Người nhận không thuộc người được phép đón; xác nhận đúng người, đúng đồ, dùng một lần.',
    status: 'Chờ xác nhận',
    accept: 'Xác nhận',
  },
]

// Chi phí, số dư (010, 017): thuốc chỉ hiện mã vật phẩm khi người xem không có quyền xem sức khỏe (chú thích 16).
export const DEMO_PORTAL_COSTS = {
  balance: '[3.200.000] đ',
  periods: [
    {
      period: 'Tháng 10/2026',
      status: 'Tạm tính',
      lines: [
        { type: 'Phí lưu trú định kỳ', amount: '[7.000.000] đ' },
        { type: 'Vật phẩm tiêu hao', item: 'Tã', amount: '[350.000] đ' },
        { type: 'Thuốc', item: '[Mã vật phẩm minh họa]', medicine: '[Tên thuốc minh họa 1]', amount: '[210.000] đ' },
      ],
      total: '[7.560.000] đ',
    },
    {
      period: 'Tháng 09/2026',
      status: 'Đã chốt',
      lines: [
        { type: 'Phí lưu trú định kỳ', amount: '[7.000.000] đ' },
        { type: 'Dịch vụ', item: 'Đưa đi khám', amount: '[500.000] đ' },
        { type: 'Vật phẩm tiêu hao', item: 'Tã', amount: '[320.000] đ' },
      ],
      total: '[7.820.000] đ',
    },
  ],
}

// Phản hồi (14.7, 012 FR-058 → FR-066).
export const DEMO_PORTAL_FEEDBACK_TYPES = ['Góp ý', 'Khiếu nại', 'Hỏi đáp']
export const DEMO_PORTAL_FEEDBACK_GROUPS = ['Chăm sóc, sinh hoạt', 'Sức khỏe, thuốc', 'Ăn uống', 'Chi phí, hợp đồng', 'Khác']
export const DEMO_PORTAL_FEEDBACK = [
  {
    id: 'f1',
    type: 'Hỏi đáp',
    group: 'Ăn uống',
    text: '[Câu hỏi minh họa về bữa ăn]',
    sentAt: '08/10/2026',
    status: 'Đã phản hồi',
    reply: '[Nội dung trả lời minh họa]',
  },
]

// Đồ gửi (16, 013): chỉ người đại diện và người được phép đón thấy (012 FR-046b).
export const DEMO_PORTAL_DEPOSITS = [
  { name: 'Kính', qty: 1, status: 'Đang được người cao tuổi sử dụng', lastHandover: 'Giao sử dụng, 02/10/2026' },
  { name: 'Xe lăn', qty: 1, status: 'Đang giữ', lastHandover: 'Tiếp nhận, 01/09/2026' },
  { name: 'Quần áo', qty: 6, status: 'Đang giữ', lastHandover: 'Tiếp nhận, 01/09/2026' },
]

// Hợp đồng, dịch vụ (6.3, 6.6): chỉ người đại diện xem, không sửa (004 chú thích 7).
export const DEMO_PORTAL_CONTRACT = {
  code: '[HĐ-0001]',
  stayType: 'Nội trú dài hạn',
  from: '01/09/2026',
  to: '31/08/2027',
  status: 'Hiệu lực',
  careLevel: 'Thường xuyên',
  deposit: 'Đã đáp ứng',
  services: ['Chăm sóc thường xuyên', '[Dịch vụ minh họa]'],
  annexes: [{ code: '[PL-01]', text: '[Nội dung phụ lục minh họa]', date: '15/09/2026' }],
}

// ---------------------------------------------------------------------------------------------
// Phát thuốc (spec 006 User Story 2, 6; FR-018 → FR-033, FR-044). Giờ hiện tại của bản mẫu là DEMO_SHIFT.now
// (09:40). Cửa sổ mặc định ±30 phút (CFG-M07-01); Trễ sau cuối cửa sổ, Bỏ lỡ sau thêm 30 phút (CFG-M07-02).
// Tên thuốc là minh họa; lịch thuốc thật do hệ thống tính từ đơn Hiệu lực, không lập sẵn bản ghi liều (Q-306).
const FAMILY_LOTS = [
  { code: '[Lô A]', expiry: '12/2026', left: 18 },
  { code: '[Lô B]', expiry: '09/2026', left: 4, expired: true },
]

export const DEMO_DOSES = [
  { id: 'd1', time: '08:00', residentCode: 'NCT-0001', drug: '[Thuốc minh họa 1]', dose: '5 mg', route: 'uống', source: 'Viện', status: 'Đã dùng', result: 'Đã dùng lúc 08:05' },
  { id: 'd2', time: '08:00', residentCode: 'NCT-0002', drug: '[Thuốc minh họa 5]', dose: '1 viên', route: 'uống', source: 'Viện', status: 'Đã dùng', result: 'Đã dùng lúc 08:12' },
  { id: 'd3', time: '08:00', residentCode: 'NCT-0003', drug: '[Thuốc minh họa 6]', dose: '1 viên', route: 'uống', source: 'Viện', status: 'Từ chối', result: 'Từ chối: [lý do minh họa]' },
  { id: 'd4', time: '09:00', residentCode: 'NCT-0003', drug: '[Thuốc minh họa 1]', dose: '5 mg', route: 'uống', source: 'Viện', status: 'Trễ' },
  { id: 'd5', time: '10:00', residentCode: 'NCT-0001', drug: '[Thuốc minh họa 2]', dose: '1 viên', route: 'uống', source: 'Gia đình gửi', status: 'Đến giờ', lots: FAMILY_LOTS },
  { id: 'd6', time: '10:00', residentCode: 'NCT-0001', drug: '[Thuốc minh họa 3]', dose: '10 ml', route: 'uống', source: 'Viện', status: 'Đến giờ', refusedBefore: true },
  { id: 'd7', time: '10:00', residentCode: 'NCT-0002', drug: '[Thuốc kiểm soát đặc biệt minh họa]', dose: '1 ống', route: 'tiêm dưới da', source: 'Viện', status: 'Đến giờ', controlled: true },
  { id: 'd8', time: '10:00', residentCode: 'NCT-0005', drug: '[Thuốc minh họa 4]', dose: '1 viên', route: 'uống', source: 'Viện', status: 'Đến giờ', sharedFloor: true },
  { id: 'd9', time: '12:00', residentCode: 'NCT-0001', drug: '[Thuốc minh họa 1]', dose: '5 mg', route: 'uống', source: 'Viện', status: 'Chưa đến giờ' },
  { id: 'd10', time: '12:00', residentCode: 'NCT-0002', drug: '[Thuốc minh họa 5]', dose: '1 viên', route: 'uống', source: 'Viện', status: 'Chưa đến giờ' },
  { id: 'd11', time: '12:00', residentCode: 'NCT-0003', drug: '[Thuốc minh họa 6]', dose: '1 viên', route: 'uống', source: 'Viện', status: 'Chưa đến giờ' },
  { id: 'd12', time: '12:00', residentCode: 'NCT-0004', drug: '[Thuốc minh họa 7]', dose: '1 viên', route: 'uống', source: 'Viện', status: 'Tạm dừng', note: 'Người cao tuổi đang tạm vắng' },
  { id: 'd13', time: '12:00', residentCode: 'NCT-0006', drug: '[Thuốc minh họa 8]', dose: '1 viên', route: 'uống', source: 'Viện', status: 'Tạm dừng', note: 'Đang điều trị tại bệnh viện' },
]

// Thuốc khi cần (FR-024, FR-025): khoảng cách tối thiểu và số lần tối đa trong 24 giờ trượt
export const DEMO_PRN_ORDERS = [
  { id: 'p1', residentCode: 'NCT-0001', drug: '[Thuốc giảm đau minh họa]', dose: '500 mg', route: 'uống', minHours: 4, maxPer24h: 4, uses: ['05:30'], source: 'Viện' },
  { id: 'p2', residentCode: 'NCT-0003', drug: '[Thuốc hạ sốt minh họa]', dose: '500 mg', route: 'uống', minHours: 6, maxPer24h: 3, uses: ['07:10'], source: 'Viện' },
]

// ---------------------------------------------------------------------------------------------
// Bàn giao ca (spec 008 User Story 1; FR-038 → FR-047). Bản mẫu hiện như lúc 13:40, trong khoảng được lập
// bàn giao (CFG-M09-04, 30 phút trước kết ca 14:00). Hệ thống không lập sẵn mục; danh sách dưới là bản ghi nguồn
// đang mở để người bàn giao tự chọn (Q-306, Q-307). serious: mục nghiêm trọng phải có ghi chú (FR-041, Q-83);
// keep: chỉ số vượt ngưỡng, biến động người cao tuổi không bỏ được (Q-88).
export const DEMO_HANDOVER_SCOPE = { floor: 'Tầng 2', from: 'Ca ngày', to: 'Ca chiều', end: '14:00', now: '13:40', giver: '[Điều dưỡng 1]', receiver: 'Người phụ trách ca chiều' }

export const DEMO_HANDOVER_SOURCE_GROUPS = [
  { key: 'tasks', label: 'Công việc chưa hoàn thành' },
  { key: 'doses', label: 'Liều Trễ, Bỏ lỡ, Từ chối' },
  { key: 'vitals', label: 'Chỉ số vượt ngưỡng trong ca' },
  { key: 'alerts', label: 'Cảnh báo, sự cố đang mở' },
  { key: 'changes', label: 'Người cao tuổi mới nhập, trở về, bắt đầu vắng' },
  { key: 'beds', label: 'Giường chờ sẵn sàng quá hạn' },
  { key: 'deposits', label: 'Đồ gửi có giá trị nhân viên trong ca đang giữ' },
]

export const DEMO_HANDOVER_SOURCES = [
  { id: 's1', group: 'tasks', resident: '[Họ tên NCT 1]', text: 'Hỗ trợ uống nước, Bắt buộc', status: 'Quá hạn', serious: true, laterStatus: 'Hoàn thành trễ' },
  { id: 's2', group: 'tasks', resident: '[Họ tên NCT 4]', text: 'Hỗ trợ vận động', status: 'Quá hạn', currentStatus: 'Hoàn thành' },
  { id: 's3', group: 'doses', resident: '[Họ tên NCT 3]', text: '[Thuốc minh họa 1], 09:00', status: 'Bỏ lỡ', serious: true },
  { id: 's4', group: 'doses', resident: '[Họ tên NCT 3]', text: '[Thuốc minh họa 6], 08:00', status: 'Từ chối', serious: true },
  { id: 's5', group: 'vitals', resident: '[Họ tên NCT 3]', text: 'Huyết áp [180/100] mmHg lúc 08:30, ngưỡng nguy hiểm', status: 'Vượt ngưỡng', keep: true },
  { id: 's6', group: 'alerts', resident: '[Họ tên NCT 3]', text: 'Cảnh báo huyết áp, mức Trung bình', status: 'Leo thang', serious: true },
  { id: 's7', group: 'alerts', resident: '[Họ tên NCT 1]', text: 'Sự cố SC-0012, ngã, mức Trung bình', status: 'Đang xử lý', serious: true },
  { id: 's8', group: 'changes', resident: '[Họ tên NCT 4]', text: 'Bắt đầu tạm vắng lúc 10:00', status: 'Tạm vắng', keep: true },
  { id: 's9', group: 'beds', resident: null, text: 'Giường 208-C', status: 'Chờ sẵn sàng' },
]

// Bản nháp người bàn giao đã ghi trước đó (người lập tự thêm, không phải hệ thống)
export const DEMO_HANDOVER_DRAFT_ITEMS = [
  { sourceId: 's3', note: '' },
  { sourceId: 's8', note: '[Ghi chú minh họa: người thân đón, dự kiến về 18:00]' },
  { sourceId: 's2', note: '' },
]

// Phát sinh sau khi lập (chỉ hiển thị cho người xác nhận, FR-045)
export const DEMO_HANDOVER_AFTER_LOCK = [
  { id: 'n1', resident: '[Họ tên NCT 2]', text: 'Cảnh báo nhiệt độ, mức Nhẹ, 13:52', status: 'Mới' },
]

export const DEMO_HANDOVER_HISTORY = [
  { id: 'h1', from: 'Ca đêm', to: 'Ca ngày', date: '18/10/2026', giver: '[Điều dưỡng 2]', receiver: '[Điều dưỡng 1]', status: 'Có ý kiến' },
  { id: 'h2', from: 'Ca chiều', to: 'Ca đêm', date: '17/10/2026', giver: '[Điều dưỡng 3]', receiver: '[Điều dưỡng 2]', status: 'Đã xác nhận' },
]

// Cảnh báo (spec 007 User Story 3; FR-029 → FR-036). Mức Nhẹ, Trung bình, Khẩn cấp; leo thang theo hạn tiếp nhận.
export const DEMO_ALERTS = [
  {
    id: 'a1',
    residentCode: 'NCT-0003',
    level: 'Trung bình',
    text: 'Huyết áp vượt ngưỡng nguy hiểm',
    source: 'Chỉ số đo lúc 08:30',
    status: 'Leo thang',
    escalation: 2,
    maxEscalation: true,
    owner: '[Trưởng tầng 1]',
    due: 'quá hạn tiếp nhận 4 phút',
    merged: 3,
    history: ['08:31 tạo, giao [Điều dưỡng 1]', '08:46 leo thang cấp 1, giao [Trưởng tầng 1]', '09:16 leo thang cấp 2 (tối đa), báo Quản lý viện'],
  },
  {
    id: 'a2',
    residentCode: 'NCT-0001',
    level: 'Trung bình',
    text: 'Từ chối [Thuốc minh họa 3] 2 lần liên tiếp',
    source: 'Phát thuốc',
    status: 'Đã tiếp nhận',
    owner: '[Điều dưỡng 1]',
    mine: true,
    merged: 1,
    history: ['09:45 tạo, giao [Điều dưỡng 1]', '09:50 [Điều dưỡng 1] tiếp nhận'],
  },
  {
    id: 'a3',
    residentCode: 'NCT-0003',
    level: 'Trung bình',
    text: 'Công việc Bắt buộc quá hạn: Đo chỉ số sức khỏe',
    source: 'Checklist ca',
    status: 'Mới',
    owner: '[Điều dưỡng 1]',
    mine: true,
    due: 'còn 12 phút để tiếp nhận',
    merged: 1,
    fromMandatoryTask: true,
    history: ['09:30 tạo, giao [Điều dưỡng 1]'],
  },
  {
    id: 'a4',
    residentCode: 'NCT-0002',
    level: 'Nhẹ',
    text: 'Nhiệt độ vượt ngưỡng cảnh báo',
    source: 'Chỉ số đo lúc 07:40',
    status: 'Đang xử lý',
    owner: '[Điều dưỡng 1]',
    mine: true,
    merged: 1,
    history: ['07:41 tạo, giao [Điều dưỡng 1]', '07:50 tiếp nhận', '07:52 bắt đầu xử lý'],
  },
  {
    id: 'a5',
    residentCode: 'NCT-0005',
    level: 'Nhẹ',
    text: 'Lượng nước thấp trong ca',
    source: 'Checklist ca',
    status: 'Đã đóng',
    owner: '[Điều dưỡng 1]',
    merged: 1,
    result: '[Kết quả xử lý minh họa]',
    history: ['06:40 tạo', '06:45 tiếp nhận', '07:10 đóng'],
  },
]

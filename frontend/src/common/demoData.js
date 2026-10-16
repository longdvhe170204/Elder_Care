// DỮ LIỆU MINH HỌA cho màn mẫu của giai đoạn thiết kế giao diện.
// Không phải dữ liệu thật, không phải quy tắc nghiệp vụ. Xóa khi màn hình lấy dữ liệu từ API (000 T010).
// Tên trạng thái lấy đúng theo docs/business/nghiep-vu.md: người cao tuổi (5.5), giường (7.2),
// công việc (8.x), mức chăm sóc (mục 4), mức cảnh báo (Nhẹ, Trung bình, Khẩn cấp).

export const DEMO_SCOPE = { floor: 'Tầng 1', shift: 'Ca ngày', updatedAt: '10:05' }

export const DEMO_ESCALATED_ALERT = {
  room: '104-B',
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

export const DEMO_TASKS = [
  { time: '07:45', title: 'Hỗ trợ vệ sinh cá nhân', room: '101-B', status: 'Hoàn thành' },
  { time: '08:30', title: 'Đo chỉ số sức khỏe', room: '102-A', status: 'Quá hạn', mandatory: true },
  { time: '09:00', title: 'Hỗ trợ vận động', room: '103-A', status: 'Quá hạn' },
  { time: '10:00', title: 'Đo chỉ số sức khỏe', room: '105-C', status: 'Đến hạn' },
  { time: '10:30', title: 'Hỗ trợ vận động', room: '103-A', status: 'Chưa đến hạn' },
  { time: '11:00', title: 'Hỗ trợ bữa trưa', room: '102-A', status: 'Chưa đến hạn' },
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

export const DEMO_HANDOVER = { from: 'Ca đêm', to: 'Ca ngày', status: 'Có ý kiến' }

export const DEMO_RESIDENTS = [
  { id: 1, code: 'NCT-0001', name: '[Họ tên NCT 1]', room: '101-B', careLevel: 'Thường xuyên', stayType: 'Nội trú dài hạn', status: 'Đang lưu trú', allergies: ['[Dị ứng minh họa]'], careNote: '[Lưu ý chăm sóc minh họa]', today: { tasks: 2, overdue: 0, doses: 2 } },
  { id: 2, code: 'NCT-0002', name: '[Họ tên NCT 2]', room: '102-A', careLevel: 'Đặc biệt', stayType: 'Nội trú dài hạn', status: 'Đang lưu trú', allergies: [], careNote: '[Lưu ý chăm sóc minh họa]', today: { tasks: 3, overdue: 1, doses: 3 } },
  { id: 3, code: 'NCT-0003', name: '[Họ tên NCT 3]', room: '104-B', careLevel: 'Cơ bản', stayType: 'Nội trú ngắn ngày', status: 'Đang lưu trú', openAlert: true, allergies: ['[Dị ứng minh họa]'], careNote: '', today: { tasks: 1, overdue: 0, doses: 1 } },
  { id: 4, code: 'NCT-0004', name: '[Họ tên NCT 4]', room: '105-C', careLevel: 'Thường xuyên', stayType: 'Nội trú dài hạn', status: 'Tạm vắng', allergies: [], careNote: '', today: { tasks: 0, overdue: 0, doses: 0 } },
  { id: 5, code: 'NCT-0005', name: '[Họ tên NCT 5]', room: '—', careLevel: 'Cơ bản', stayType: 'Bán trú', status: 'Đang lưu trú', allergies: [], careNote: '', today: { tasks: 2, overdue: 0, doses: 0 } },
  { id: 6, code: 'NCT-0006', name: '[Họ tên NCT 6]', room: '106-A', careLevel: 'Đặc biệt', stayType: 'Nội trú dài hạn', status: 'Điều trị tại bệnh viện', allergies: [], careNote: '', today: { tasks: 0, overdue: 0, doses: 0 } },
  { id: 7, code: 'NCT-0007', name: '[Họ tên NCT 7]', room: '107-A', careLevel: 'Thường xuyên', stayType: 'Nội trú dài hạn', status: 'Đang tiếp nhận', allergies: [], careNote: '', today: { tasks: 0, overdue: 0, doses: 0 } },
]

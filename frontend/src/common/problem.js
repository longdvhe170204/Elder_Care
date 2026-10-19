import { ApiError } from '@/api'

// Lỗi lệnh theo Problem Details (RFC 9457, research R-03 của 000; 000 T099). Backend đặt `title` là tên mã HTTP
// tiếng Anh, nội dung tiếng Việt nằm ở `detail`; màn hiển thị tiêu đề theo mã trạng thái (NFR-11, DESIGN.md 8).

const TITLES = {
  0: 'Không kết nối được máy chủ',
  400: 'Yêu cầu không hợp lệ',
  401: 'Phiên đăng nhập đã hết',
  403: 'Không có quyền thực hiện',
  404: 'Không tìm thấy',
  409: 'Chưa thực hiện được',
  412: 'Dữ liệu đã thay đổi',
  422: 'Giá trị không hợp lệ',
}

const DEFAULT_DETAILS = {
  0: 'Kiểm tra kết nối mạng rồi thử lại.',
  401: 'Đăng nhập lại để tiếp tục.',
  412: 'Người khác vừa lưu thay đổi. Xem giá trị mới rồi thực hiện lại.',
  500: 'Thử lại sau ít phút. Nếu vẫn lỗi, báo cho quản trị hệ thống.',
}

export function problemOf(error) {
  const status = error instanceof ApiError ? error.status : 0
  const body = (error instanceof ApiError && error.problem) || {}
  const group = status >= 500 ? 500 : status
  return {
    status,
    errorCode: body.errorCode ?? null,
    title: TITLES[group] ?? (group === 500 ? 'Máy chủ gặp lỗi' : 'Không thực hiện được'),
    detail: body.detail || DEFAULT_DETAILS[group] || '',
    unmetConditions: body.unmetConditions ?? [],
    currentValue: body.currentValue,
  }
}

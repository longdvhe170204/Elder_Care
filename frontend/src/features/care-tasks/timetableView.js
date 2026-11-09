import { careTime, STATUS } from './careChecklistView'

// T058 (FR-048): chuyển TimetableItem của api-005 sang dòng hiển thị; mã của nguồn khác giữ nguyên văn.
const SOURCE = {
  CARE_TASK: 'Công việc',
  MEDICATION_DOSE: 'Liều thuốc',
  ACTIVITY_SESSION: 'Buổi hoạt động',
  VISIT: 'Lịch thăm',
  DAY_CARE_ARRIVAL_DEPARTURE: 'Lịch đến/về',
}
const ATTENDANCE = {
  NOT_ARRIVED: 'Chưa đến',
  PRESENT: 'Có mặt',
  LEFT: 'Đã về',
  REPORTED_ABSENT: 'Vắng có báo',
  UNREPORTED_ABSENT: 'Vắng không báo',
  HOLIDAY_CANCELLED: 'Đã hủy do ngày nghỉ',
}

function statusText(item) {
  if (item.source === 'CARE_TASK') return STATUS[item.status] ?? item.status
  if (item.source === 'DAY_CARE_ARRIVAL_DEPARTURE') return ATTENDANCE[item.status] ?? item.status
  return item.status ?? ''
}

export function timetableView(items, now) {
  const rows = [...items]
    .sort((a, b) => new Date(a.at) - new Date(b.at))
    .map((item, index) => ({
      key: `${item.at}-${item.source}-${index}`,
      time: careTime(item.at),
      source: SOURCE[item.source] ?? item.source,
      description: item.description ?? '',
      status: statusText(item),
      past: now ? new Date(item.at) < now : false,
    }))
  const nowIndex = now ? rows.findIndex((r) => !r.past) : -1
  return { rows, nowIndex: now ? (nowIndex === -1 ? rows.length : nowIndex) : -1 }
}

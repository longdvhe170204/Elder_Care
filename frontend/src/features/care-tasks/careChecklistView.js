import { formatDate, formatDateTime } from '@/common/dateTime'

export const careTime = (iso) => (iso ? formatDateTime(iso).slice(-5) : '')
export const STATUS = {
  NOT_YET_DUE: 'Chưa đến hạn',
  DUE: 'Đến hạn',
  OVERDUE: 'Quá hạn',
  COMPLETED: 'Hoàn thành',
  COMPLETED_LATE: 'Hoàn thành trễ',
  NOT_PERFORMED: 'Không thực hiện',
  CANCELLED: 'Hủy',
}
const IMPORTANCE = { NORMAL: 'Thường', IMPORTANT: 'Quan trọng', MANDATORY: 'Bắt buộc' }
const SOURCE = {
  CARE_PLAN_ITEM: 'Kế hoạch chăm sóc',
  MEASUREMENT: 'Lịch đo',
  MEAL: 'Bữa ăn',
  ACTIVITY_SESSION: 'Buổi hoạt động',
  POST_FALL_MONITORING: 'Theo dõi sau ngã',
  CONTACT_MONITORING: 'Theo dõi tiếp xúc',
  WATER_INTAKE_ALERT: 'Cảnh báo uống nước',
  SHIFT_START_CHECK: 'Kiểm tra đầu ca',
  SUPERVISION: 'Giám sát',
  AD_HOC: 'Phát sinh',
  AD_HOC_RECORDING: 'Ghi nhận phát sinh',
  REWORK: 'Làm lại',
}
const CARRY = { FROM_PREVIOUS_SHIFT: 'tồn từ ca trước', AWAITING_HANDOVER_CONFIRMATION: 'tồn, chờ xác nhận' }

// Adapter dữ liệu thật; không ghép tên loại với danh mục hiện tại để đoán loại kết quả snapshot.
export function careChecklistView(data) {
  const choices = new Map((data.riskAgreements ?? []).map((r) => [r.residentId, r.agreements]))
  const tasks = data.tasks.map((t) => ({
    id: t.id,
    nctId: t.residentId,
    resident: t.residentCard?.fullName ?? 'Người cao tuổi',
    photo: t.residentCard?.photo,
    bed: t.residentCard?.roomBed,
    careNote: t.residentCard?.careNotes,
    location: t.location,
    floorTask: t.shared,
    title: t.taskType,
    content: t.content,
    source: SOURCE[t.basis?.type],
    carry: CARRY[t.carryover],
    plannedAt: careTime(t.plannedAt),
    from: careTime(t.windowFrom),
    to: careTime(t.windowTo),
    importance: IMPORTANCE[t.importance],
    assignee: t.performer,
    status: STATUS[t.status] ?? t.status,
    awaitingRemeasure: t.awaitingRemeasurementConfirmation,
    commands: t.allowedCommands ?? [],
    riskChoices: (choices.get(t.residentId) ?? []).map((a) => ({
      choice: a.choice,
      reviewOn: formatDate(a.reviewDate),
    })),
    raw: t,
  }))
  tasks.sort(
    (a, b) =>
      Number(b.raw.status === 'OVERDUE') - Number(a.raw.status === 'OVERDUE') ||
      (a.raw.plannedAt ?? '').localeCompare(b.raw.plannedAt ?? '') ||
      a.id - b.id,
  )
  return {
    now: careTime(data.loadedAt),
    tasks,
    reasons: (data.nonPerformanceReasonTypes ?? [])
      .filter((r) => r.status === 'ACTIVE')
      .sort((a, b) => Number(b.fixed) - Number(a.fixed) || a.id - b.id),
  }
}

export const DONE = ['Hoàn thành', 'Hoàn thành trễ', 'Không thực hiện', 'Hủy']

// Nguồn của công việc (005 FR-030, canCu) và dấu tồn (dauTon, FR-047, FR-047a)
export function sourceText(task) {
  return [task.source, task.carry].filter(Boolean).join(', ')
}

// Khung thời gian, kèm thời điểm dự kiến khi khác đầu khung (FR-030 hiện cả hai)
export function timeText(task) {
  return task.plannedAt && task.plannedAt !== task.from ? `${task.from}–${task.to}, dự kiến ${task.plannedAt}` : `${task.from}–${task.to}`
}

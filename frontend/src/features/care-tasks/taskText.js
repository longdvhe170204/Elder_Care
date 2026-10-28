export const DONE = ['Hoàn thành', 'Hoàn thành trễ', 'Không thực hiện', 'Hủy']

// Nguồn của công việc (005 FR-030) và dấu tồn từ ca trước (FR-047)
export function sourceText(task) {
  return [task.source, task.carriedOver && 'tồn từ ca trước'].filter(Boolean).join(', ')
}

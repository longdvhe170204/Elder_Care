import { beforeEach, afterEach, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, within } from '@testing-library/react'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { json, mockRoutes } from '@/test/fetchRoutes'
import { CareChecklistPage } from './CareChecklistPage'

// Lượt rà màn mẫu Checklist ca (team-plan mục 8, DESIGN.md 7.2 shift-timeline, task-block; 9.2 điểm 8)
let api
const task = (id, status, at, careNotes = 'Lưu ý ngắn') => ({
  id,
  residentId: 77,
  residentCard: { fullName: '[Người kiểm thử]', roomBed: 'Phòng kiểm thử', careNotes },
  taskType: `Công việc ${id}`,
  basis: { type: 'AD_HOC' },
  plannedAt: at,
  windowFrom: at,
  windowTo: '2026-11-01T04:00:00Z',
  importance: 'NORMAL',
  performer: '[Nhân viên kiểm thử]',
  shared: id === 5,
  status,
  allowedCommands: [],
})
const work = [
  task(7, 'COMPLETED', '2026-11-01T01:00:00Z'),
  task(6, 'OVERDUE', '2026-11-01T02:00:00Z'),
  task(5, 'DUE', '2026-11-01T03:00:00Z', 'Lưu ý rất dài '.repeat(30)),
]
let clamped
beforeEach(() => {
  clamped = vi.spyOn(HTMLElement.prototype, 'scrollHeight', 'get').mockImplementation(function () {
    return this.textContent.length > 200 ? 120 : 20
  })
  vi.spyOn(HTMLElement.prototype, 'clientHeight', 'get').mockReturnValue(40)
  api = mockRoutes()
  api.on('GET', '/me/permissions', json(200, [{ function: 'TASK_CHECKLIST', operation: 'VIEW' }]))
  api.on(
    'GET',
    '/shifts/9',
    json(200, { id: 9, startsAt: '2026-11-01T00:00:00Z', endsAt: '2026-11-01T08:00:00Z', status: 'IN_PROGRESS', staff: [] }),
  )
  api.on(
    'GET',
    '/checklist',
    json(200, {
      shiftId: 9,
      loadedAt: '2026-11-01T03:30:00Z',
      tasks: work,
      riskAgreements: [],
      nonPerformanceReasonTypes: [],
      medicationDoses: [],
      openAlerts: [],
      handoverTasks: {},
    }),
  )
})
afterEach(() => {
  clamped.mockRestore()
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})
const view = () =>
  render(
    <PermissionsProvider>
      <CareChecklistPage shiftId={9} />
    </PermissionsProvider>,
  )

it('loc_khongCoTabViecChungCuaTang', async () => {
  view()
  await screen.findByText('Công việc 5')
  const filters = screen.getByRole('group', { name: 'Lọc công việc' })
  expect(within(filters).queryByText(/Việc chung của tầng/)).toBeNull()
  expect(within(filters).getByText(/Đã xong/)).toBeInTheDocument()
})

it('gioChiCoViecDaXong_thuGonVaMoLaiDuoc', async () => {
  view()
  await screen.findByText('Công việc 5')
  expect(screen.queryByText('Công việc 7')).toBeNull()
  const toggle = screen.getByRole('button', { name: /08:00\s*1 việc đã xong/ })
  expect(toggle).toHaveAttribute('aria-expanded', 'false')
  fireEvent.click(toggle)
  expect(toggle).toHaveAttribute('aria-expanded', 'true')
  expect(screen.getByText('Công việc 7')).toBeInTheDocument()
})

it('nhomQuaHan_khongLapNhanTrongKhoi_vaThangCotGio', async () => {
  view()
  await screen.findByText('Công việc 5')
  const overdue = screen.getByRole('region', { name: 'Công việc Quá hạn' })
  expect(within(overdue).getByRole('heading')).toHaveTextContent('Quá hạn (1)')
  const block = within(overdue).getByRole('button', { name: /Công việc 6/ })
  expect(block.textContent).not.toMatch(/Quá hạn/)
  // Lưới khối của nhóm Quá hạn và của dòng thời gian cùng thụt sau cột giờ
  const grid = block.parentElement
  expect(grid.className).toContain('items-start')
  expect(grid.previousElementSibling?.className).toContain('w-13')
})

it('luuYBiCat_hienXemDu', async () => {
  view()
  const longBlock = (await screen.findByText('Công việc 5')).closest('button')
  expect(within(longBlock).getByText('Xem đủ')).toBeInTheDocument()
  const overdueBlock = screen.getByText('Công việc 6').closest('button')
  expect(within(overdueBlock).queryByText('Xem đủ')).toBeNull()
})

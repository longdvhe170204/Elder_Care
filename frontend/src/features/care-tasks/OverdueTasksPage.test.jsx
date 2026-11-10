import { beforeEach, afterEach, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { OverdueTasksPage } from './OverdueTasksPage'
import { NAV_GROUPS } from '@/common/layout/navigation'

let api
it('FR_045_menuQuaHanDungKhoaQuyen', () => {
  expect(NAV_GROUPS.flatMap((g) => g.items).find((i) => i.key === 'overdue-care-tasks')?.functions).toEqual([
    'OVERDUE_TASK_HANDLING',
  ])
})
beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/me/permissions', json(200, [{ function: 'OVERDUE_TASK_HANDLING', operation: 'VIEW' }]))
})
afterEach(() => vi.unstubAllGlobals())

it('FR_045_hienDanhSachQuaHanTuApiVaTaiLai', async () => {
  api.on(
    'GET',
    '/care-tasks/overdue',
    json(200, [
      {
        id: 7,
        residentId: 9,
        residentCard: { fullName: '[Người kiểm thử]' },
        taskType: 'Hỗ trợ uống nước',
        plannedAt: '2026-11-02T03:00:00Z',
        windowFrom: '2026-11-02T02:45:00Z',
        windowTo: '2026-11-02T03:15:00Z',
        status: 'OVERDUE',
        importance: 'IMPORTANT',
        basis: { type: 'MEAL' },
        shared: true,
        carryover: 'AWAITING_CONFIRMATION',
        reminderCount: 2,
        allowedCommands: [],
      },
    ]),
  )
  render(
    <PermissionsProvider>
      <OverdueTasksPage />
    </PermissionsProvider>,
  )
  expect(await screen.findByText('Hỗ trợ uống nước')).toBeInTheDocument()
  expect(screen.getByText(/\[Người kiểm thử\]/)).toBeInTheDocument()
  expect(screen.getByText(/Đã nhắc và báo: 2/)).toBeInTheDocument()
  expect(screen.getByText('Dự kiến: 02/11/2026 10:00')).toBeInTheDocument()
  expect(screen.getByText(/tồn, chờ xác nhận/)).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Ghi nhận hoàn thành' })).not.toBeInTheDocument()
  api.on('GET', '/care-tasks/overdue', json(200, []))
  fireEvent.click(screen.getByRole('button', { name: 'Tải lại' }))
  expect(await screen.findByText('Không có công việc quá hạn')).toBeInTheDocument()
})

it('FR_045_403HienKhongCoQuyenVaKhongGiuDuLieuCu', async () => {
  api.on('GET', '/care-tasks/overdue', problem(403, 'PERMISSION_DENIED'))
  render(
    <PermissionsProvider>
      <OverdueTasksPage />
    </PermissionsProvider>,
  )
  expect(await screen.findByText(/không có quyền/i)).toBeInTheDocument()
})

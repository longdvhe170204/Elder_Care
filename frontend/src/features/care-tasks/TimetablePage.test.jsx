import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { NAV_GROUPS } from '@/common/layout/navigation'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { TimetablePage } from './TimetablePage'
import { timetableView } from './timetableView'

let api
beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/me/permissions', json(200, [{ function: 'CARE_PLAN', operation: 'VIEW' }]))
  api.on(
    'GET',
    '/residents',
    json(200, { items: [{ id: 9, profileCode: 'NCT-9', personalInfo: { fullName: '[Người kiểm thử]' } }] }),
  )
})
afterEach(() => vi.unstubAllGlobals())

it('FR_048_menuThoiKhoaBieuChoTruongTangVaDieuDuong', () => {
  const item = NAV_GROUPS.flatMap((g) => g.items).find((i) => i.key === 'care-timetable')
  expect(item.functions).toEqual(['CARE_PLAN'])
  expect(item.roles).toHaveLength(2)
})

it('FR_048_sapTheoGioGhiNguonVaTrangThaiKhongCoNutSua', async () => {
  api.on(
    'GET',
    '/residents/9/timetable',
    json(200, [
      { at: '2026-10-01T02:30:00Z', source: 'ACTIVITY_SESSION', description: 'Vận động', status: 'REGISTERED' },
      { at: '2026-10-01T00:00:00Z', source: 'CARE_TASK', description: 'Hỗ trợ vệ sinh', status: 'NOT_YET_DUE' },
      { at: '2026-10-01T09:30:00Z', source: 'DAY_CARE_ARRIVAL_DEPARTURE', description: 'Giờ về', status: 'PRESENT' },
    ]),
  )
  render(
    <PermissionsProvider>
      <TimetablePage initialDate="2026-10-01" />
    </PermissionsProvider>,
  )
  await screen.findByRole('option', { name: /Người kiểm thử/ })
  fireEvent.change(await screen.findByLabelText('Người cao tuổi'), { target: { value: '9' } })
  const list = await screen.findByRole('list', { name: 'Các mục trong ngày' })
  const rows = within(list).getAllByRole('listitem')
  expect(rows.map((r) => r.textContent)).toEqual([
    '07:00Công việcHỗ trợ vệ sinhChưa đến hạn',
    '09:30Buổi hoạt độngVận độngREGISTERED',
    '16:30Lịch đến/vềGiờ vềCó mặt',
  ])
  expect(screen.getByRole('heading', { name: 'Thời khóa biểu của [Người kiểm thử], 01/10/2026' })).toBeInTheDocument()
  expect(screen.queryByRole('button')).not.toBeInTheDocument()
})

it('FR_048_khongCoMucThiBaoTrong', async () => {
  api.on('GET', '/residents/9/timetable', json(200, []))
  render(
    <PermissionsProvider>
      <TimetablePage initialDate="2026-10-01" />
    </PermissionsProvider>,
  )
  await screen.findByRole('option', { name: /Người kiểm thử/ })
  fireEvent.change(await screen.findByLabelText('Người cao tuổi'), { target: { value: '9' } })
  expect(await screen.findByText('Không có mục nào trong ngày')).toBeInTheDocument()
})

it('FR_048_403HienKhongCoQuyen', async () => {
  api.on('GET', '/residents/9/timetable', problem(403, 'PERMISSION_DENIED'))
  render(
    <PermissionsProvider>
      <TimetablePage initialDate="2026-10-01" />
    </PermissionsProvider>,
  )
  await screen.findByRole('option', { name: /Người kiểm thử/ })
  fireEvent.change(await screen.findByLabelText('Người cao tuổi'), { target: { value: '9' } })
  expect(await screen.findByText(/không có quyền/i)).toBeInTheDocument()
})

it('FR_048_vachBayGioDatTruocMucChuaToi', () => {
  const view = timetableView(
    [
      { at: '2026-10-01T00:00:00Z', source: 'CARE_TASK', status: 'COMPLETED' },
      { at: '2026-10-01T05:00:00Z', source: 'CARE_TASK', status: 'NOT_YET_DUE' },
    ],
    new Date('2026-10-01T03:00:00Z'),
  )
  expect(view.nowIndex).toBe(1)
  expect(view.rows[0].status).toBe('Hoàn thành')
})

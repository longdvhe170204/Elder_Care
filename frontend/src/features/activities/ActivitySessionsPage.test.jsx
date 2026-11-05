import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { NAV_GROUPS } from '@/common/layout/navigation'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { ActivitySessionsPage } from './ActivitySessionsPage'

let api

const SESSIONS = [
  {
    id: 11,
    activityId: 3,
    startsAt: '2026-11-03T01:00:00Z',
    endsAt: '2026-11-03T01:45:00Z',
    status: 'SCHEDULED',
    registrationCount: 3,
    isTrip: false,
  },
  {
    id: 12,
    activityId: 3,
    startsAt: '2026-11-04T01:00:00Z',
    endsAt: '2026-11-04T01:45:00Z',
    status: 'CANCELLED',
    registrationCount: 0,
    isTrip: false,
  },
]

const DETAIL = {
  id: 11,
  activityId: 3,
  startsAt: '2026-11-03T01:00:00Z',
  endsAt: '2026-11-03T01:45:00Z',
  status: 'SCHEDULED',
  incompleteAttendance: false,
  floorAreaId: 5,
  cancelReason: null,
  version: 0,
  registrations: [{ id: 91, residentId: 701, source: 'REGULAR', status: 'REGISTERED' }],
}

function permissions(ops) {
  api.on(
    'GET',
    '/me/permissions',
    json(
      200,
      ops.map((operation) => ({ function: 'ACTIVITY_OUTING', operation })),
    ),
  )
}

function renderPage() {
  return render(
    <PermissionsProvider>
      <ActivitySessionsPage floorAreaId={5} initialDate="2026-11-05" />
    </PermissionsProvider>,
  )
}

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/activities', json(200, [{ id: 3, name: 'Thể dục buổi sáng', status: 'ACTIVE' }]))
  api.on('GET', '/activity-sessions', json(200, SESSIONS))
  api.on('GET', '/activity-sessions/11', json(200, DETAIL))
})
afterEach(() => vi.unstubAllGlobals())

it('FR_011_menuHoatDongDungKhoaQuyen', () => {
  const item = NAV_GROUPS.flatMap((g) => g.items).find((i) => i.key === 'activity-sessions')
  expect(item?.functions).toEqual(['ACTIVITY_OUTING'])
})

it('FR_011_bangBuoiTrongTuanCoTenHoatDongTrangThaiVaSoDem', async () => {
  permissions(['VIEW'])
  renderPage()
  expect(await screen.findByRole('heading', { name: 'Buổi hoạt động' })).toBeInTheDocument()
  expect(screen.getByText('Tuần 02/11/2026 → 08/11/2026')).toBeInTheDocument()
  const rows = await screen.findAllByRole('row')
  expect(within(rows[1]).getByText('Thể dục buổi sáng')).toBeInTheDocument()
  expect(within(rows[1]).getByText('03/11/2026 08:00')).toBeInTheDocument()
  expect(within(rows[1]).getByText('Đã lên lịch')).toBeInTheDocument()
  expect(within(rows[1]).getByText('3')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: /Đã hủy 1/ })).toBeInTheDocument()
  const query = new URL(api.calls('GET', '/activity-sessions')[0][0], 'http://localhost').searchParams
  expect(query.get('from')).toBe('2026-11-02')
  expect(query.get('to')).toBe('2026-11-08')
  expect(query.get('floorAreaId')).toBe('5')
  // Chỉ xem: không có nút lệnh
  expect(screen.queryByRole('button', { name: 'Tạo buổi từ mẫu' })).not.toBeInTheDocument()
})

it('FR_011_locTheoTrangThai', async () => {
  permissions(['VIEW'])
  renderPage()
  await screen.findAllByText('Thể dục buổi sáng')
  fireEvent.click(screen.getByRole('button', { name: /Đã hủy 1/ }))
  const rows = screen.getAllByRole('row')
  expect(rows).toHaveLength(2)
  expect(within(rows[1]).getByText('Đã hủy')).toBeInTheDocument()
})

it('FR_013_huyBuoiCanLyDoVaGuiLenh', async () => {
  permissions(['VIEW', 'CREATE', 'UPDATE'])
  api.on('POST', '/activity-sessions/11/cancel', json(200, {}))
  renderPage()
  fireEvent.click(await screen.findByRole('button', { name: /Chi tiết buổi Thể dục buổi sáng 03\/11\/2026 08:00/ }))
  expect(await screen.findByText('701')).toBeInTheDocument()
  expect(screen.getByText('Thường xuyên')).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: 'Hủy buổi' }))
  const confirm = screen.getByRole('button', { name: 'Xác nhận hủy buổi' })
  expect(confirm).toBeDisabled()
  fireEvent.change(screen.getByLabelText(/Lý do hủy/), { target: { value: 'Ngày lễ' } })
  fireEvent.click(confirm)
  await screen.findByText('Đã hủy buổi')
  const [, init] = api.calls('POST', '/activity-sessions/11/cancel')[0]
  expect(JSON.parse(init.body)).toEqual({ reason: 'Ngày lễ' })
  expect(init.headers['Idempotency-Key']).toBeTruthy()
})

it('FR_012_doiBuoiGuiGioMoiKemLyDo', async () => {
  permissions(['VIEW', 'UPDATE'])
  api.on('POST', '/activity-sessions/11/reschedule', json(200, {}))
  renderPage()
  fireEvent.click(await screen.findByRole('button', { name: /Chi tiết buổi Thể dục buổi sáng 03\/11\/2026 08:00/ }))
  fireEvent.click(await screen.findByRole('button', { name: 'Dời buổi' }))
  fireEvent.change(screen.getByLabelText(/^Bắt đầu mới/), { target: { value: '2026-11-03T09:00' } })
  fireEvent.change(screen.getByLabelText(/^Kết thúc mới/), { target: { value: '2026-11-03T09:45' } })
  fireEvent.change(screen.getByLabelText(/Lý do dời/), { target: { value: 'Đổi phòng' } })
  fireEvent.click(screen.getByRole('button', { name: 'Lưu giờ mới' }))
  await screen.findByText('Đã dời buổi')
  const [, init] = api.calls('POST', '/activity-sessions/11/reschedule')[0]
  expect(JSON.parse(init.body)).toEqual({
    reason: 'Đổi phòng',
    startsAt: '2026-11-03T09:00:00+07:00',
    endsAt: '2026-11-03T09:45:00+07:00',
  })
})

it('FR_006_taoBuoiTuMauChoKhoangNgay', async () => {
  permissions(['VIEW', 'CREATE'])
  api.on('POST', '/activities/3/create-sessions-from-template', json(200, {}))
  renderPage()
  fireEvent.click(await screen.findByRole('button', { name: 'Tạo buổi từ mẫu' }))
  fireEvent.change(screen.getByLabelText(/^Hoạt động/), { target: { value: '3' } })
  fireEvent.change(screen.getByLabelText(/^Từ ngày/), { target: { value: '2026-11-09' } })
  fireEvent.change(screen.getByLabelText(/^Đến ngày/), { target: { value: '2026-11-15' } })
  fireEvent.click(screen.getByRole('button', { name: 'Tạo các buổi còn thiếu' }))
  await screen.findByText('Đã tạo buổi từ mẫu')
  const [, init] = api.calls('POST', '/activities/3/create-sessions-from-template')[0]
  expect(JSON.parse(init.body)).toEqual({ fromDate: '2026-11-09', toDate: '2026-11-15' })
})

it('FR_006_loiTuBackendHienTaiKhung', async () => {
  permissions(['VIEW', 'CREATE'])
  api.on(
    'POST',
    '/activities/3/create-sessions-from-template',
    problem(409, 'AFTER_TEMPLATE_END_DATE', { detail: 'Khoảng ngày sau ngày kết thúc của mẫu' }),
  )
  renderPage()
  fireEvent.click(await screen.findByRole('button', { name: 'Tạo buổi từ mẫu' }))
  fireEvent.change(screen.getByLabelText(/^Hoạt động/), { target: { value: '3' } })
  fireEvent.change(screen.getByLabelText(/^Từ ngày/), { target: { value: '2027-01-01' } })
  fireEvent.change(screen.getByLabelText(/^Đến ngày/), { target: { value: '2027-01-07' } })
  fireEvent.click(screen.getByRole('button', { name: 'Tạo các buổi còn thiếu' }))
  expect(await screen.findByText('Khoảng ngày sau ngày kết thúc của mẫu')).toBeInTheDocument()
})

it('FR_011_403HienKhongCoQuyen', async () => {
  permissions(['VIEW'])
  api.on('GET', '/activity-sessions', problem(403, 'PERMISSION_DENIED'))
  renderPage()
  expect(await screen.findByText(/không có quyền/i)).toBeInTheDocument()
})

import { beforeEach, afterEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { ActivitySessionsPage } from './ActivitySessionsPage'

let api
const TRIP = {
  id: 41,
  status: 'SCHEDULED',
  version: 1,
  destination: 'Chùa',
  plannedDepartureAt: '2026-11-05T00:00:00Z',
  plannedReturnAt: '2026-11-05T06:00:00Z',
  transport: 'Xe thuê',
  facilityVehicle: false,
  leaderId: 21,
  companionIds: [22],
  participants: [{ id: 91, residentId: 701, status: 'ELIGIBLE', reason: null }],
}

beforeEach(() => {
  api = mockRoutes()
  api.on(
    'GET',
    '/me/permissions',
    json(
      200,
      ['VIEW', 'CREATE', 'UPDATE'].map((operation) => ({ function: 'ACTIVITY_OUTING', operation })),
    ),
  )
  api.on('GET', '/activities', json(200, [{ id: 3, name: 'Tham quan', status: 'ACTIVE' }]))
  api.on(
    'GET',
    '/activity-sessions',
    json(200, [
      {
        id: 41,
        activityId: 3,
        startsAt: TRIP.plannedDepartureAt,
        endsAt: TRIP.plannedReturnAt,
        status: 'SCHEDULED',
        isTrip: true,
        registrationCount: 1,
      },
    ]),
  )
  api.on('GET', '/trips/41', json(200, TRIP))
  api.on('GET', '/residents', json(200, { items: [{ id: 701, fullName: 'Nguyễn Thị Lan' }] }))
})
afterEach(() => vi.unstubAllGlobals())
function page() {
  render(
    <PermissionsProvider>
      <ActivitySessionsPage floorAreaId={5} initialDate="2026-11-05" />
    </PermissionsProvider>,
  )
}
async function open() {
  page()
  fireEvent.click(await screen.findByRole('button', { name: /Chi tiết buổi Tham quan/ }))
  await screen.findByText('Điểm đến: Chùa')
}

it('FR_039_chuyenDungEndpointChuyenVaKhongCoDiemDanhBuoiTrongVien', async () => {
  await open()
  expect(screen.getByText('Được đi')).toBeInTheDocument()
  expect(screen.getByText('Nguyễn Thị Lan')).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Lưu điểm danh' })).not.toBeInTheDocument()
  expect(api.calls('GET', '/trips/41')).toHaveLength(1)
})

it('FR_035_phanCongGuiMaNhanVienVaDanhSachNguoiDiCung', async () => {
  api.on('POST', '/trips/41/assignment', json(200, {}))
  await open()
  fireEvent.change(screen.getByLabelText(/Mã trưởng đoàn/), { target: { value: '31' } })
  fireEvent.change(screen.getByLabelText(/Mã người đi cùng/), { target: { value: '32, 33' } })
  fireEvent.click(screen.getByRole('button', { name: 'Lưu phân công' }))
  await screen.findByText('Đã lưu phân công chuyến')
  expect(JSON.parse(api.calls('POST', '/trips/41/assignment')[0][1].body)).toEqual({
    leaderId: 31,
    companionIds: [32, 33],
  })
})

it('FR_040_roiVienCoBuocXacNhanVaGiuLuaChonKhiBiChan', async () => {
  api.on(
    'POST',
    '/trips/41/depart',
    problem(409, 'MEDICATION_NOT_HANDLED', { detail: 'Chưa chuẩn bị thuốc mang theo' }),
  )
  await open()
  fireEvent.click(screen.getByRole('checkbox', { name: 'Chọn Nguyễn Thị Lan' }))
  fireEvent.click(screen.getByRole('button', { name: 'Điểm danh rời viện' }))
  fireEvent.click(screen.getByRole('button', { name: 'Xác nhận rời viện' }))
  expect(await screen.findByText('Chưa chuẩn bị thuốc mang theo')).toBeInTheDocument()
  expect(screen.getByRole('checkbox', { name: 'Chọn Nguyễn Thị Lan' })).toBeChecked()
  fireEvent.click(screen.getByRole('button', { name: 'Điểm danh rời viện' }))
  fireEvent.click(screen.getByRole('button', { name: 'Xác nhận rời viện' }))
  await screen.findByText('Chưa chuẩn bị thuốc mang theo')
  const calls = api.calls('POST', '/trips/41/depart')
  expect(calls).toHaveLength(2)
  expect(calls[0][1].headers['Idempotency-Key']).toBe(calls[1][1].headers['Idempotency-Key'])
})

it('FR_034_taoBuoiLeCoThongTinChuyenVaMuiGioVietNam', async () => {
  api.on('POST', '/activity-sessions', json(201, {}))
  page()
  fireEvent.click(await screen.findByRole('button', { name: 'Tạo buổi lẻ' }))
  fireEvent.change(screen.getByLabelText(/^Hoạt động/), { target: { value: '3' } })
  fireEvent.change(screen.getByLabelText(/^Bắt đầu/), { target: { value: '2026-11-06T07:00' } })
  fireEvent.change(screen.getByLabelText(/^Kết thúc/), { target: { value: '2026-11-06T13:00' } })
  fireEvent.click(screen.getByRole('checkbox', { name: 'Chuyến đi ngoài viện' }))
  fireEvent.change(screen.getByLabelText(/^Điểm đến/), { target: { value: 'Chùa' } })
  fireEvent.change(screen.getByLabelText(/^Giờ rời dự kiến/), { target: { value: '2026-11-06T07:00' } })
  fireEvent.change(screen.getByLabelText(/^Giờ về dự kiến/), { target: { value: '2026-11-06T13:00' } })
  fireEvent.change(screen.getByLabelText(/^Phương tiện/), { target: { value: 'Xe thuê' } })
  fireEvent.click(screen.getAllByRole('button', { name: 'Tạo buổi lẻ' }).find((button) => button.type === 'submit'))
  await screen.findByText('Đã tạo buổi lẻ')
  expect(JSON.parse(api.calls('POST', '/activity-sessions')[0][1].body).trip).toEqual({
    destination: 'Chùa',
    plannedDepartureAt: '2026-11-06T07:00:00+07:00',
    plannedReturnAt: '2026-11-06T13:00:00+07:00',
    transport: 'Xe thuê',
    facilityVehicle: false,
  })
})

it('FR_015_chuyenDaLenLichVanChoDangKyVaXacNhanCanhBao', async () => {
  api.on('GET', '/trips/41', json(200, { ...TRIP, participants: [] }))
  api.on('POST', '/activity-sessions/41/registrations', (_url, init) => {
    const body = JSON.parse(init.body)
    return body.acknowledgeWarnings
      ? json(201, {})
      : problem(409, 'WARNING_ACKNOWLEDGEMENT_REQUIRED', {
          unmetConditions: [{ code: 'CARE_NOTE', description: 'Cần xem lưu ý chăm sóc' }],
        })
  })
  await open()
  fireEvent.change(screen.getByLabelText(/^Người cao tuổi/), { target: { value: '701' } })
  fireEvent.click(screen.getByRole('button', { name: 'Đăng ký' }))
  await screen.findByText('Cần xem lưu ý chăm sóc')
  fireEvent.click(screen.getByRole('button', { name: 'Xác nhận cảnh báo và đăng ký' }))
  await screen.findByText('Đã đăng ký người cao tuổi')
  const calls = api.calls('POST', '/activity-sessions/41/registrations')
  expect(JSON.parse(calls[1][1].body)).toEqual({ residentId: 701, acknowledgeWarnings: true })
  expect(calls[0][1].headers['Idempotency-Key']).not.toBe(calls[1][1].headers['Idempotency-Key'])
})

it('FR_039_trangThaiNguoiThamGiaTheoGlossary', async () => {
  api.on(
    'GET',
    '/trips/41',
    json(200, {
      ...TRIP,
      status: 'IN_PROGRESS',
      participants: [
        { ...TRIP.participants[0], status: 'MISSING_ON_RETURN' },
        { id: 92, residentId: 702, status: 'LEFT_GROUP' },
      ],
    }),
  )
  await open()
  expect(screen.getByText('Thiếu khi về')).toBeInTheDocument()
  expect(screen.getByText('Rời đoàn')).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Lưu phân công' })).not.toBeInTheDocument()
})

it('FR_019_huyDangKyChuyenCanLyDo', async () => {
  api.on('POST', '/activity-registrations/91/cancel', json(200, {}))
  await open()
  fireEvent.click(screen.getByRole('button', { name: 'Hủy đăng ký của Nguyễn Thị Lan' }))
  fireEvent.change(screen.getByLabelText(/^Lý do hủy đăng ký/), { target: { value: 'Người cao tuổi đổi ý' } })
  fireEvent.click(screen.getByRole('button', { name: 'Xác nhận hủy đăng ký' }))
  await screen.findByText('Đã hủy đăng ký')
  expect(JSON.parse(api.calls('POST', '/activity-registrations/91/cancel')[0][1].body)).toEqual({
    reason: 'Người cao tuổi đổi ý',
  })
})

it('FR_012_doiGioChuyenGuiLenhBuoiCoLyDo', async () => {
  api.on('POST', '/activity-sessions/41/reschedule', json(200, {}))
  await open()
  fireEvent.click(screen.getByRole('button', { name: 'Dời chuyến' }))
  fireEvent.change(screen.getByLabelText(/^Bắt đầu mới/), { target: { value: '2026-11-06T07:00' } })
  fireEvent.change(screen.getByLabelText(/^Kết thúc mới/), { target: { value: '2026-11-06T13:00' } })
  fireEvent.change(screen.getByLabelText(/^Lý do dời chuyến/), { target: { value: 'Dời sang ngày mai' } })
  fireEvent.click(screen.getByRole('button', { name: 'Xác nhận dời chuyến' }))
  await screen.findByText('Đã dời chuyến')
  expect(JSON.parse(api.calls('POST', '/activity-sessions/41/reschedule')[0][1].body)).toEqual({
    startsAt: '2026-11-06T07:00:00+07:00',
    endsAt: '2026-11-06T13:00:00+07:00',
    reason: 'Dời sang ngày mai',
  })
})

it('FR_013_huyChuyenCanXacNhanVaLyDo', async () => {
  api.on('POST', '/activity-sessions/41/cancel', json(200, {}))
  await open()
  fireEvent.click(screen.getByRole('button', { name: 'Hủy chuyến' }))
  fireEvent.change(screen.getByLabelText(/^Lý do hủy chuyến/), { target: { value: 'Thời tiết không phù hợp' } })
  fireEvent.click(screen.getByRole('button', { name: 'Xác nhận hủy chuyến' }))
  await screen.findByText('Đã hủy chuyến')
  expect(JSON.parse(api.calls('POST', '/activity-sessions/41/cancel')[0][1].body)).toEqual({
    reason: 'Thời tiết không phù hợp',
  })
})

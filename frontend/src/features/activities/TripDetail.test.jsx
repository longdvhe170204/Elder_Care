import { beforeEach, afterEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, within } from '@testing-library/react'
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
  // Chờ dữ liệu tải bằng truy vấn rẻ (aria-label): *ByRole tính lại cây trợ năng ở mỗi lần DOM đổi trong lúc tải, khi
  // chạy toàn kho (nhiều worker) ca đầu file vượt 1000 ms mặc định (log 014/20261107-web-full-durations.json).
  // Ca đầu file còn tải module lúc chờ (1027 ms ngày 10/11, log 014/20261110-web-full-durations.json): chờ 4000 ms
  // như StaffPage.test.jsx.
  fireEvent.click(await screen.findByLabelText(/Chi tiết buổi Tham quan/, {}, { timeout: 4000 }))
  await screen.findByText('Điểm đến: Chùa', {}, { timeout: 4000 })
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

it('FR_036_danhGiaDatKhongDatMoSheetKhongDatBatBuocLyDo', async () => {
  api.on(
    'GET',
    '/trips/41',
    json(200, { ...TRIP, participants: [{ id: 91, residentId: 701, status: 'REGISTERED', reason: null }] }),
  )
  api.on('POST', '/trips/41/eligibility-assessments', json(200, {}))
  await open()
  fireEvent.click(screen.getByRole('button', { name: 'Đánh giá Nguyễn Thị Lan' }))
  const sheet = await screen.findByRole('dialog', { name: 'Đánh giá khả năng tham gia' })
  expect(within(sheet).getByText(/Chưa có dữ liệu thuốc, sức khỏe/)).toBeInTheDocument()
  fireEvent.click(within(sheet).getByRole('radio', { name: 'Không đạt' }))
  expect(within(sheet).getByRole('button', { name: 'Lưu đánh giá' })).toBeDisabled()
  fireEvent.change(within(sheet).getByLabelText(/^Lý do không đạt/), { target: { value: 'Đang sốt' } })
  fireEvent.click(within(sheet).getByRole('button', { name: 'Lưu đánh giá' }))
  await screen.findByText('Đã lưu đánh giá khả năng tham gia')
  expect(JSON.parse(api.calls('POST', '/trips/41/eligibility-assessments')[0][1].body)).toEqual({
    residentId: 701,
    result: 'FAIL',
    reason: 'Đang sốt',
  })
})

const OUT = {
  ...TRIP,
  status: 'IN_PROGRESS',
  plannedReturnAt: '2026-11-05T04:30:00Z',
  companionIds: [22, 23],
  participants: [
    { id: 91, residentId: 701, status: 'DEPARTED', reason: null },
    { id: 92, residentId: 702, status: 'RETURNED', reason: null },
  ],
}

it('FR_044_diemDanhVeChiChonNguoiDaRoiVienVaBaoQuaGioVe', async () => {
  vi.useFakeTimers({ toFake: ['Date'], now: new Date('2026-11-05T05:00:00Z') })
  api.on('GET', '/trips/41', json(200, OUT))
  api.on('POST', '/trips/41/return', json(200, {}))
  await open()
  expect(screen.getByText('Quá giờ về dự kiến')).toBeInTheDocument()
  expect(screen.queryByRole('checkbox', { name: 'Chọn về Trần Văn Minh' })).not.toBeInTheDocument()
  fireEvent.click(screen.getByRole('checkbox', { name: 'Chọn về Nguyễn Thị Lan' }))
  fireEvent.click(screen.getByRole('button', { name: 'Điểm danh về (1)' }))
  await screen.findByText('Đã điểm danh về')
  expect(JSON.parse(api.calls('POST', '/trips/41/return')[0][1].body)).toEqual({ residentIds: [701] })
  vi.useRealTimers()
})

it('FR_043_FR_035a_giaHanVaChuyenTruongDoanCanLyDo', async () => {
  api.on('GET', '/trips/41', json(200, OUT))
  api.on('POST', '/trips/41/extend', json(200, {}))
  api.on('POST', '/trips/41/transfer-leader', json(200, {}))
  await open()
  fireEvent.click(screen.getByRole('button', { name: 'Gia hạn giờ về' }))
  let sheet = await screen.findByRole('dialog', { name: 'Gia hạn giờ về' })
  fireEvent.change(within(sheet).getByLabelText(/^Giờ về mới/), { target: { value: '2026-11-05T13:30' } })
  expect(within(sheet).getByRole('button', { name: 'Lưu gia hạn' })).toBeDisabled()
  fireEvent.change(within(sheet).getByLabelText(/^Lý do gia hạn/), { target: { value: 'Kẹt xe' } })
  fireEvent.click(within(sheet).getByRole('button', { name: 'Lưu gia hạn' }))
  await screen.findByText('Đã gia hạn giờ về')
  // Lệnh xong thì màn tải lại danh sách và đóng khung (hành vi sẵn có); mở lại chuyến cho lệnh tiếp theo.
  fireEvent.click(await screen.findByLabelText(/Chi tiết buổi Tham quan/))
  await screen.findByText('Điểm đến: Chùa')
  expect(JSON.parse(api.calls('POST', '/trips/41/extend')[0][1].body)).toEqual({
    newPlannedReturnAt: '2026-11-05T13:30:00+07:00',
    reason: 'Kẹt xe',
  })
  fireEvent.click(screen.getByRole('button', { name: 'Chuyển trưởng đoàn' }))
  sheet = await screen.findByRole('dialog', { name: 'Chuyển trưởng đoàn' })
  fireEvent.change(within(sheet).getByLabelText(/^Người nhận/), { target: { value: '23' } })
  fireEvent.change(within(sheet).getByLabelText(/^Lý do chuyển/), { target: { value: 'Trưởng đoàn mệt' } })
  fireEvent.click(within(sheet).getByRole('button', { name: 'Xác nhận chuyển trưởng đoàn' }))
  await screen.findByText('Đã chuyển trưởng đoàn')
  expect(JSON.parse(api.calls('POST', '/trips/41/transfer-leader')[0][1].body)).toEqual({
    newLeaderId: 23,
    reason: 'Trưởng đoàn mệt',
  })
})

it('FR_047_ketThucDiemDanhVeNeuNguoiSeThanhThieuKhiVe', async () => {
  api.on('GET', '/trips/41', json(200, OUT))
  api.on('POST', '/trips/41/finish-return', json(200, {}))
  await open()
  fireEvent.click(screen.getByRole('button', { name: 'Kết thúc điểm danh về' }))
  const dialog = await screen.findByRole('dialog', { name: 'Kết thúc điểm danh về' })
  expect(within(dialog).getByText(/Nguyễn Thị Lan sẽ chuyển sang Thiếu khi về/)).toBeInTheDocument()
  fireEvent.click(within(dialog).getByRole('button', { name: 'Xác nhận kết thúc' }))
  await screen.findByText('Đã kết thúc điểm danh về')
  expect(api.calls('POST', '/trips/41/finish-return')).toHaveLength(1)
})

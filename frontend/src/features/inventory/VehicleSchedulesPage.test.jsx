import { beforeEach, describe, expect, it } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { VehicleSchedulesPage } from './VehicleSchedulesPage'

// 019 T022 màn web (US6; FR-017, FR-018; BR-M03-16; bố cục duyệt 2026-11-09 mục (8)): lịch xe theo tuần, lọc trạng thái
// kèm số đếm; khung chi tiết có lệnh theo trạng thái và vai trò (api-019 chưa có allowedCommands); đặt lịch bằng Sheet.

let api

const BOOKED = {
  id: 61,
  vehicleId: 12,
  plannedDepartureAt: '2026-11-11T01:00:00Z',
  plannedReturnAt: '2026-11-11T04:00:00Z',
  purpose: 'MEDICAL_VISIT',
  tripId: null,
  residentId: 5,
  driverStaffId: 21,
  bookedByStaffId: 30,
  status: 'BOOKED',
  departedAt: null,
  returnedAt: null,
  cancellationReason: null,
}
const IN_USE = {
  ...BOOKED,
  id: 62,
  plannedDepartureAt: '2026-11-12T01:00:00Z',
  plannedReturnAt: '2026-11-12T09:00:00Z',
  purpose: 'TRIP',
  tripId: 7,
  residentId: null,
  status: 'IN_USE',
  departedAt: '2026-11-12T01:05:00Z',
}
const CANCELLED = {
  ...BOOKED,
  id: 63,
  plannedDepartureAt: '2026-11-14T01:00:00Z',
  plannedReturnAt: '2026-11-14T03:00:00Z',
  purpose: 'OTHER',
  residentId: null,
  status: 'CANCELLED',
  cancellationReason: 'Đổi lịch khám',
}

const VAN = { id: 12, code: 'XE-01', typeId: 1, name: 'Xe 16 chỗ', departmentId: 3, status: 'READY', flags: [] }

function permissions(list) {
  api.on(
    'GET',
    '/me/permissions',
    json(
      200,
      list.map(([fn, operation]) => ({ function: fn, operation })),
    ),
  )
}

const BOOKER = [
  ['ASSET_VEHICLE', 'VIEW'],
  ['ASSET_VEHICLE', 'CREATE'],
  ['ASSET_VEHICLE', 'UPDATE'],
]

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/vehicle-schedules', json(200, [BOOKED, IN_USE, CANCELLED]))
  api.on('GET', '/asset-types', json(200, [{ id: 1, code: 'VEHICLE', name: 'Xe', status: 'ACTIVE' }]))
  api.on('GET', '/assets', json(200, [VAN]))
  api.on('GET', '/staff', json(200, [{ id: 21, fullName: 'Trần Văn Bình', position: 'Lái xe' }]))
  api.on(
    'GET',
    '/residents/5',
    json(200, { id: 5, profileCode: 'NCT-0005', personalInfo: { fullName: 'Nguyễn Thị Lan' }, status: 'IN_RESIDENCE' }),
  )
})

function renderPage(role) {
  render(
    <PermissionsProvider>
      <VehicleSchedulesPage role={role} initialDate="2026-11-10" />
    </PermissionsProvider>,
  )
  return screen.findByRole('table', { name: 'Lịch xe' })
}

function openSchedule(table, name) {
  fireEvent.click(within(table).getByRole('button', { name }))
  // Dưới 1280px (jsdom không có matchMedia) khung chi tiết là Sheet
  return screen.findByRole('dialog')
}

describe('Lịch xe', () => {
  it('FR_017_lichTheoTuan_tenXe_nguoiLai_mucDich_locTrangThai', async () => {
    permissions([['ASSET_VEHICLE', 'VIEW']])
    const table = await renderPage('KITCHEN_STAFF')

    const [, init] = api.calls('GET', '/vehicle-schedules')[0]
    const url = new URL(api.calls('GET', '/vehicle-schedules')[0][0], 'http://localhost')
    expect(init.method ?? 'GET').toBe('GET')
    expect(url.searchParams.get('from')).toBe('2026-11-09T00:00:00+07:00')
    expect(url.searchParams.get('to')).toBe('2026-11-16T00:00:00+07:00')

    expect(screen.getByRole('button', { name: 'Tất cả 3' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Đang dùng 1' })).toBeInTheDocument()
    const row = await within(table).findByRole('row', { name: /11\/11\/2026 08:00/ })
    expect(await within(row).findByText('XE-01, Xe 16 chỗ')).toBeInTheDocument()
    expect(await within(row).findByText('Trần Văn Bình')).toBeInTheDocument()
    expect(row).toHaveTextContent('Đưa đi khám')
    expect(await within(row).findByText('Nguyễn Thị Lan')).toBeInTheDocument()
    expect(row).toHaveTextContent('Đã đặt')
    expect(within(table).getByRole('row', { name: /12\/11\/2026/ })).toHaveTextContent('Chuyến đi #7')
    expect(screen.queryByRole('button', { name: 'Đặt lịch xe' })).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Tuần sau' }))
    await waitFor(() => expect(api.calls('GET', '/vehicle-schedules')).toHaveLength(2))
    const next = new URL(api.calls('GET', '/vehicle-schedules')[1][0], 'http://localhost')
    expect(next.searchParams.get('from')).toBe('2026-11-16T00:00:00+07:00')
  })

  it('FR_018_huyLich_batBuocLyDo_ghiXuatPhat', async () => {
    permissions(BOOKER)
    const table = await renderPage('OFFICE_STAFF')
    api.on('POST', '/vehicle-schedules/61/cancel', json(200, {}))
    const panel = await openSchedule(table, 'Lịch xe 11/11/2026 08:00')
    expect(within(panel).getByRole('button', { name: 'Ghi xuất phát' })).toBeInTheDocument()
    expect(within(panel).queryByRole('button', { name: 'Ghi trả xe' })).not.toBeInTheDocument()

    fireEvent.click(within(panel).getByRole('button', { name: 'Hủy lịch…' }))
    fireEvent.click(within(panel).getByRole('button', { name: 'Xác nhận hủy' }))
    expect(await within(panel).findByText('Nhập lý do')).toBeInTheDocument()
    expect(api.calls('POST', '/vehicle-schedules/61/cancel')).toHaveLength(0)
    fireEvent.change(within(panel).getByLabelText('Lý do'), { target: { value: 'Đổi lịch khám' } })
    fireEvent.click(within(panel).getByRole('button', { name: 'Xác nhận hủy' }))
    await waitFor(() => expect(api.calls('POST', '/vehicle-schedules/61/cancel')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/vehicle-schedules/61/cancel')[0][1].body)).toEqual({
      reason: 'Đổi lịch khám',
    })
    expect(await screen.findByText('Đã hủy lịch xe')).toBeInTheDocument()
  })

  it('BR_M03_16_doiGio_chongLich_hien409', async () => {
    permissions(BOOKER)
    const table = await renderPage('FLOOR_MANAGER')
    api.on(
      'POST',
      '/vehicle-schedules/61/reschedule',
      problem(409, 'VEHICLE_SCHEDULE_OVERLAP', {
        detail: 'Không dời được lịch',
        unmetConditions: [{ code: 'VEHICLE_SCHEDULE_OVERLAP', description: 'Chồng giờ với lịch #64 của cùng xe' }],
      }),
    )
    const panel = await openSchedule(table, 'Lịch xe 11/11/2026 08:00')
    fireEvent.click(within(panel).getByRole('button', { name: 'Dời giờ…' }))
    fireEvent.change(within(panel).getByLabelText('Giờ đi mới'), { target: { value: '2026-11-11T09:00' } })
    fireEvent.change(within(panel).getByLabelText('Giờ về mới'), { target: { value: '2026-11-11T08:00' } })
    fireEvent.click(within(panel).getByRole('button', { name: 'Lưu giờ mới' }))
    expect(await within(panel).findByText('Giờ về phải sau giờ đi')).toBeInTheDocument()
    fireEvent.change(within(panel).getByLabelText('Giờ về mới'), { target: { value: '2026-11-11T12:00' } })
    fireEvent.click(within(panel).getByRole('button', { name: 'Lưu giờ mới' }))
    expect(await within(panel).findByText('Chồng giờ với lịch #64 của cùng xe')).toBeInTheDocument()
    expect(JSON.parse(api.calls('POST', '/vehicle-schedules/61/reschedule')[0][1].body)).toEqual({
      plannedDepartureAt: '2026-11-11T09:00:00+07:00',
      plannedReturnAt: '2026-11-11T12:00:00+07:00',
    })
    expect(within(panel).getByLabelText('Giờ đi mới')).toHaveValue('2026-11-11T09:00')
  })

  it('FR_017_datLich_chonXe_nguoiLai_mucDich', async () => {
    permissions(BOOKER)
    api.on(
      'GET',
      '/residents',
      json(200, {
        items: [
          { id: 5, profileCode: 'NCT-0005', personalInfo: { fullName: 'Nguyễn Thị Lan' }, status: 'IN_RESIDENCE' },
        ],
        nextCursor: null,
      }),
    )
    api.on('POST', '/vehicle-schedules', () => new Response(null, { status: 201 }))
    await renderPage('NURSING_HOME_MANAGER')
    fireEvent.click(screen.getByRole('button', { name: 'Đặt lịch xe' }))
    const sheet = await screen.findByRole('dialog', { name: 'Đặt lịch xe' })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Lưu lịch xe' }))
    expect(await within(sheet).findByText('Chọn xe')).toBeInTheDocument()
    expect(within(sheet).getByText('Chọn mục đích')).toBeInTheDocument()
    expect(within(sheet).getByText('Chọn người lái')).toBeInTheDocument()

    await within(sheet).findByRole('option', { name: 'XE-01, Xe 16 chỗ' })
    fireEvent.change(within(sheet).getByLabelText('Xe'), { target: { value: '12' } })
    fireEvent.change(within(sheet).getByLabelText('Giờ đi dự kiến'), { target: { value: '2026-11-13T08:00' } })
    fireEvent.change(within(sheet).getByLabelText('Giờ về dự kiến'), { target: { value: '2026-11-13T11:30' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Đưa đi khám' }))
    await within(sheet).findByRole('option', { name: /Nguyễn Thị Lan/ })
    fireEvent.change(within(sheet).getByLabelText('Người cao tuổi (không bắt buộc)'), { target: { value: '5' } })
    await within(sheet).findByRole('option', { name: 'Trần Văn Bình' })
    fireEvent.change(within(sheet).getByLabelText('Người lái'), { target: { value: '21' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Lưu lịch xe' }))
    await waitFor(() => expect(api.calls('POST', '/vehicle-schedules')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/vehicle-schedules')[0][1].body)).toEqual({
      vehicleId: 12,
      plannedDepartureAt: '2026-11-13T08:00:00+07:00',
      plannedReturnAt: '2026-11-13T11:30:00+07:00',
      purpose: 'MEDICAL_VISIT',
      residentId: 5,
      driverStaffId: 21,
    })
  })

  it('FR_017_khongCoQuyenXem_hien403', async () => {
    permissions([])
    api.on('GET', '/vehicle-schedules', problem(403, 'PERMISSION_DENIED'))
    render(
      <PermissionsProvider>
        <VehicleSchedulesPage role="CAREGIVER" initialDate="2026-11-10" />
      </PermissionsProvider>,
    )
    expect(await screen.findByText(/không có quyền/i)).toBeInTheDocument()
  })
})

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { VisitsPage } from './VisitsPage'

// 012 T025 (US4; FR-029 → FR-039, BR-M10-02): màn "Đăng ký thăm" của nhân viên. Khung trong ngày và số chỗ còn; bảng
// lượt thăm của người cao tuổi; Hành chính đăng ký tại quầy cho người thân có quan hệ Hiệu lực (không đạt thì hiện mọi lý
// do, không tạo lượt), ghi vào với người thực tế, ghi ra, hủy kèm lý do. Nút theo /me/permissions.

let api

const MINH = {
  id: 11,
  familyMember: { id: 31, fullName: 'Trần Văn Minh', phoneNumber: '0901000111', version: 0 },
  relationship: 'CHILD',
  representative: true,
  primaryContact: true,
  contactOrder: 1,
  rights: { REGISTER_VISITS: { enabled: true, effective: true } },
  status: 'ACTIVE',
}

const APPROVED = {
  id: 81,
  residentId: 5,
  familyMemberId: 31,
  date: '2026-11-06',
  startTime: '09:00',
  endTime: '11:00',
  companions: [{ fullName: 'Trần Hoa', relationship: 'Cháu' }],
  visitorCount: 2,
  status: 'APPROVED',
}

const INSIDE = {
  ...APPROVED,
  id: 82,
  startTime: '15:00',
  endTime: '17:00',
  status: 'CHECKED_IN',
  companions: [],
  visitorCount: 1,
  checkedInAt: '2026-11-06T08:10:00Z',
}

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

beforeEach(() => {
  api = mockRoutes()
  api.on(
    'GET',
    '/residents',
    json(200, {
      items: [{ id: 5, profileCode: 'NCT-0005', personalInfo: { fullName: 'Nguyễn Thị Lan' }, status: 'IN_RESIDENCE' }],
      nextCursor: null,
    }),
  )
  api.on('GET', '/residents/5/relationships', json(200, [MINH]))
  api.on(
    'GET',
    '/visit-slots',
    json(200, [
      { startTime: '09:00', endTime: '11:00', capacity: 20, seatsLeft: 18 },
      { startTime: '15:00', endTime: '17:00', capacity: 20, seatsLeft: 19 },
    ]),
  )
  api.on('GET', '/visits', json(200, [APPROVED, INSIDE]))
})

afterEach(() => {
  vi.unstubAllGlobals()
})

async function openResident() {
  render(
    <PermissionsProvider>
      <VisitsPage />
    </PermissionsProvider>,
  )
  fireEvent.click(await screen.findByRole('button', { name: /Nguyễn Thị Lan/ }))
  return screen.findByRole('table', { name: 'Lượt thăm' })
}

describe('Đăng ký thăm', () => {
  it('FR_039_khungConCho_vaBangLuotTham_theoNgay', async () => {
    permissions([['VISIT_REGISTRATION', 'VIEW']])
    const table = await openResident()

    expect(screen.getByRole('list', { name: 'Khung thăm trong ngày' })).toHaveTextContent('09:00–11:00, còn 18 chỗ')
    const row = within(table).getByRole('row', { name: /09:00–11:00/ })
    expect(within(row).getByText('Trần Văn Minh')).toBeInTheDocument()
    expect(within(row).getByText('Đã duyệt')).toBeInTheDocument()
    expect(within(table).getByRole('row', { name: /15:00–17:00/ })).toHaveTextContent('Đã vào')
    const query = new URL(api.calls('GET', '/visits')[0][0], 'http://x').searchParams
    expect(query.get('residentId')).toBe('5')
    expect(screen.queryByRole('button', { name: 'Đăng ký tại quầy' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Ghi vào/ })).not.toBeInTheDocument()
  })

  it('FR_029_dangKyTaiQuay_khongDat_hienMoiLyDo', async () => {
    permissions([['VISIT_REGISTRATION', 'CREATE']])
    await openResident()
    api.on(
      'POST',
      '/visits',
      problem(422, 'VISIT_SLOT_FULL', {
        unmetConditions: [
          { code: 'VISIT_SLOT_FULL', description: 'Khung giờ đã đủ sức chứa' },
          { code: 'TOO_MANY_VISITORS', description: 'Vượt số người tối đa mỗi lượt thăm' },
        ],
      }),
    )

    fireEvent.click(screen.getByRole('button', { name: 'Đăng ký tại quầy' }))
    const sheet = await screen.findByRole('dialog', { name: 'Đăng ký thăm tại quầy cho Nguyễn Thị Lan' })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Đăng ký thăm' }))
    expect(await within(sheet).findByText('Chọn người thân')).toBeInTheDocument()
    expect(within(sheet).getByText('Chọn khung giờ')).toBeInTheDocument()
    expect(api.calls('POST', '/visits')).toHaveLength(0)

    fireEvent.change(within(sheet).getByLabelText('Người thân'), { target: { value: '31' } })
    fireEvent.click(within(sheet).getByRole('radio', { name: /09:00–11:00/ }))
    fireEvent.click(within(sheet).getByRole('button', { name: 'Thêm người đi cùng' }))
    fireEvent.change(within(sheet).getByLabelText('Họ tên người đi cùng 1'), { target: { value: 'Trần Hoa' } })
    fireEvent.change(within(sheet).getByLabelText('Quan hệ người đi cùng 1'), { target: { value: 'Cháu' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Đăng ký thăm' }))

    await waitFor(() => expect(api.calls('POST', '/visits')).toHaveLength(1))
    const [, init] = api.calls('POST', '/visits')[0]
    expect(JSON.parse(init.body)).toEqual({
      residentId: 5,
      familyMemberId: 31,
      date: expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/),
      startTime: '09:00',
      endTime: '11:00',
      companions: [{ fullName: 'Trần Hoa', relationship: 'Cháu' }],
    })
    expect(init.headers['Idempotency-Key']).toMatch(/^[0-9a-f-]{36}$/)
    expect(await within(sheet).findByText('Khung giờ đã đủ sức chứa')).toBeInTheDocument()
    expect(within(sheet).getByText('Vượt số người tối đa mỗi lượt thăm')).toBeInTheDocument()
  })

  it('FR_033_ghiVao_voiNguoiThucTe', async () => {
    permissions([
      ['VISIT_REGISTRATION', 'VIEW'],
      ['VISIT_REGISTRATION', 'UPDATE'],
    ])
    const table = await openResident()
    api.on('POST', '/visits/81/check-in', json(200, { id: 81 }))

    fireEvent.click(within(table).getByRole('button', { name: 'Ghi vào lượt 09:00–11:00 của Trần Văn Minh' }))
    const sheet = await screen.findByRole('dialog', { name: 'Ghi vào' })
    expect(within(sheet).getByRole('checkbox', { name: 'Trần Hoa' })).not.toBeChecked()
    fireEvent.click(within(sheet).getByRole('button', { name: 'Thêm người vào' }))
    fireEvent.change(within(sheet).getByLabelText('Họ tên người thêm 1'), { target: { value: 'Ngô Bình' } })
    fireEvent.change(within(sheet).getByLabelText('Quan hệ người thêm 1'), { target: { value: 'Bạn' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Xác nhận ghi vào' }))

    await waitFor(() => expect(api.calls('POST', '/visits/81/check-in')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/visits/81/check-in')[0][1].body)).toEqual({
      actualVisitors: [{ fullName: 'Ngô Bình', relationship: 'Bạn' }],
    })
    expect(await screen.findByText('Đã ghi vào')).toBeInTheDocument()
  })

  it('FR_034_ghiRa_vaFR_035_huyBatBuocLyDo', async () => {
    permissions([['VISIT_REGISTRATION', 'UPDATE']])
    const table = await openResident()
    api.on('POST', '/visits/82/check-out', json(200, { id: 82 }))
    api.on('POST', '/visits/81/cancel', problem(409, 'START_TIME_PASSED', { detail: 'Đã qua giờ bắt đầu khung thăm' }))

    fireEvent.click(within(table).getByRole('button', { name: 'Ghi ra lượt 15:00–17:00 của Trần Văn Minh' }))
    await waitFor(() => expect(api.calls('POST', '/visits/82/check-out')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/visits/82/check-out')[0][1].body)).toEqual({})

    fireEvent.click(within(table).getByRole('button', { name: 'Hủy lượt 09:00–11:00 của Trần Văn Minh' }))
    const sheet = await screen.findByRole('dialog', { name: 'Hủy lượt thăm' })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Xác nhận hủy' }))
    expect(await within(sheet).findByText('Nhập lý do')).toBeInTheDocument()
    fireEvent.change(within(sheet).getByLabelText('Lý do'), { target: { value: 'Gia đình báo không đến' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Xác nhận hủy' }))
    expect(await within(sheet).findByText('Đã qua giờ bắt đầu khung thăm')).toBeInTheDocument()
    expect(JSON.parse(api.calls('POST', '/visits/81/cancel')[0][1].body)).toEqual({ reason: 'Gia đình báo không đến' })
  })

  it('khongCoQuyenXem_hienKhongCoQuyen', async () => {
    permissions([])
    api.on('GET', '/visits', problem(403, 'PERMISSION_DENIED'))
    render(
      <PermissionsProvider>
        <VisitsPage />
      </PermissionsProvider>,
    )
    fireEvent.click(await screen.findByRole('button', { name: /Nguyễn Thị Lan/ }))
    expect(await screen.findByText('Bạn không có quyền xem đăng ký thăm')).toBeInTheDocument()
  })
})

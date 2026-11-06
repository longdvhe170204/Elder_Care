import { beforeEach, describe, expect, it } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { FamilyStaysPage } from './FamilyStaysPage'

// 012 T035 màn web (US8; FR-040 → FR-045; bố cục duyệt 2026-11-05, DESIGN.md 6.8.7): bảng lượt ở lại lọc theo trạng
// thái kèm số đếm; khung chi tiết có lệnh theo trạng thái và vai trò (Phụ lục 27 chú thích 20; hợp đồng không trả lệnh
// được phép); xác nhận kèm ý kiến người cùng phòng; Hành chính đăng ký cho người thân có quan hệ Hiệu lực.

let api

const PENDING = {
  id: 71,
  familyMemberId: 31,
  residentId: 5,
  expectedStartAt: '2026-11-01T11:00:00Z',
  expectedEndAt: '2026-11-03T01:00:00Z',
  location: 'PHONG_NCT',
  reason: 'Chăm sóc sau mổ',
  mealRegistered: true,
  status: 'PENDING_CONFIRMATION',
  nightCount: 0,
}

const STAYING = {
  ...PENDING,
  id: 72,
  familyMemberId: 32,
  status: 'STAYING',
  actualStartAt: '2026-10-30T11:10:00Z',
  mealRegistered: false,
  nightCount: 2,
}

const MINH = {
  id: 11,
  familyMember: { id: 31, fullName: 'Trần Văn Minh', phoneNumber: '0901000111', version: 0 },
  relationship: 'CHILD',
  status: 'ACTIVE',
}
const HOA = { ...MINH, id: 12, familyMember: { ...MINH.familyMember, id: 32, fullName: 'Lê Thu Hoa' } }

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

const OFFICE = [
  ['FAMILY_STAY', 'VIEW'],
  ['FAMILY_STAY', 'CREATE'],
  ['FAMILY_STAY', 'UPDATE'],
]

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/family-stays', json(200, [PENDING, STAYING]))
  api.on(
    'GET',
    '/residents/5',
    json(200, { id: 5, profileCode: 'NCT-0005', personalInfo: { fullName: 'Nguyễn Thị Lan' }, status: 'IN_RESIDENCE' }),
  )
  api.on('GET', '/residents/5/relationships', json(200, [MINH, HOA]))
  api.on(
    'GET',
    '/residents',
    json(200, {
      items: [{ id: 5, profileCode: 'NCT-0005', personalInfo: { fullName: 'Nguyễn Thị Lan' }, status: 'IN_RESIDENCE' }],
      nextCursor: null,
    }),
  )
})

function renderPage(role) {
  render(
    <PermissionsProvider>
      <FamilyStaysPage role={role} />
    </PermissionsProvider>,
  )
  return screen.findByRole('table', { name: 'Lượt ở lại' })
}

describe('Người thân ở lại', () => {
  it('FR_045_locTheoTrangThai_kemSoDem_tenNguoiThan', async () => {
    permissions([['FAMILY_STAY', 'VIEW']])
    const table = await renderPage('NURSING_HOME_MANAGER')

    expect(screen.getByRole('button', { name: 'Chờ xác nhận 1' })).toBeInTheDocument()
    const row = await within(table).findByRole('row', { name: /Trần Văn Minh/ })
    expect(row).toHaveTextContent('Nguyễn Thị Lan')
    expect(row).toHaveTextContent('Chờ xác nhận')
    expect(row).toHaveTextContent('Phòng của người cao tuổi, có đăng ký ăn')
    expect(screen.queryByRole('button', { name: 'Đăng ký ở lại' })).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Đang ở lại 1' }))
    const staying = await within(screen.getByRole('table', { name: 'Lượt ở lại' })).findByRole('row', {
      name: /Lê Thu Hoa/,
    })
    expect(staying).toHaveTextContent('Đang ở lại')
    expect(within(staying).getByText('2')).toBeInTheDocument()
  })

  it('FR_040_truongTangXacNhan_kemYKienNguoiCungPhong', async () => {
    permissions([
      ['FAMILY_STAY', 'VIEW'],
      ['FAMILY_STAY', 'UPDATE'],
    ])
    const table = await renderPage('FLOOR_MANAGER')
    api.on(
      'POST',
      '/family-stays/71/confirm',
      problem(409, 'ROOMMATE_OPINION_REQUIRED', {
        detail: 'Phòng có từ hai giường đang dùng: ghi đã hỏi ý kiến người cùng phòng và kết quả',
      }),
    )

    fireEvent.click(await within(table).findByRole('button', { name: /Trần Văn Minh/ }))
    const detail = await screen.findByRole('dialog', { name: 'Trần Văn Minh ở lại với Nguyễn Thị Lan' })
    expect(within(detail).queryByRole('button', { name: 'Hủy lượt…' })).not.toBeInTheDocument()
    fireEvent.click(within(detail).getByRole('button', { name: 'Xác nhận lượt ở lại' }))
    expect(await within(detail).findByText(/ghi đã hỏi ý kiến người cùng phòng/)).toBeInTheDocument()
    expect(JSON.parse(api.calls('POST', '/family-stays/71/confirm')[0][1].body)).toEqual({})

    api.on('POST', '/family-stays/71/confirm', json(200, { id: 71 }))
    fireEvent.change(within(detail).getByLabelText('Người được hỏi'), { target: { value: 'Võ Thị Hòa' } })
    fireEvent.click(within(detail).getByRole('button', { name: 'Đồng ý' }))
    fireEvent.click(within(detail).getByRole('button', { name: 'Xác nhận lượt ở lại' }))

    await waitFor(() => expect(api.calls('POST', '/family-stays/71/confirm')).toHaveLength(2))
    expect(JSON.parse(api.calls('POST', '/family-stays/71/confirm')[1][1].body)).toEqual({
      roommateOpinion: { askedPerson: 'Võ Thị Hòa', result: 'AGREED' },
    })
    expect(await screen.findByText('Đã xác nhận lượt ở lại')).toBeInTheDocument()
  })

  it('FR_045_tuChoi_batBuocLyDo', async () => {
    permissions([['FAMILY_STAY', 'VIEW']])
    const table = await renderPage('NURSING_HOME_MANAGER')
    api.on('POST', '/family-stays/71/reject', json(200, { id: 71 }))

    fireEvent.click(await within(table).findByRole('button', { name: /Trần Văn Minh/ }))
    const detail = await screen.findByRole('dialog', { name: 'Trần Văn Minh ở lại với Nguyễn Thị Lan' })
    fireEvent.click(within(detail).getByRole('button', { name: 'Từ chối…' }))
    fireEvent.click(within(detail).getByRole('button', { name: 'Xác nhận từ chối' }))
    expect(await within(detail).findByText('Nhập lý do')).toBeInTheDocument()
    expect(api.calls('POST', '/family-stays/71/reject')).toHaveLength(0)

    fireEvent.change(within(detail).getByLabelText('Lý do'), { target: { value: 'Người cùng phòng không đồng ý' } })
    fireEvent.click(within(detail).getByRole('button', { name: 'Xác nhận từ chối' }))
    await waitFor(() => expect(api.calls('POST', '/family-stays/71/reject')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/family-stays/71/reject')[0][1].body)).toEqual({
      reason: 'Người cùng phòng không đồng ý',
    })
  })

  it('FR_040_hanhChinhDangKy_choNguoiThanCoQuanHeHieuLuc', async () => {
    permissions(OFFICE)
    await renderPage('OFFICE_STAFF')
    api.on('POST', '/family-stays', json(201, { ...PENDING, id: 73 }))

    fireEvent.click(await screen.findByRole('button', { name: 'Đăng ký ở lại' }))
    const sheet = await screen.findByRole('dialog', { name: 'Đăng ký người thân ở lại' })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Lưu đăng ký' }))
    expect(await within(sheet).findByText('Chọn người cao tuổi')).toBeInTheDocument()
    expect(api.calls('POST', '/family-stays')).toHaveLength(0)

    fireEvent.change(within(sheet).getByLabelText('Người cao tuổi'), { target: { value: '5' } })
    await within(sheet).findByRole('option', { name: 'Trần Văn Minh' })
    fireEvent.change(within(sheet).getByLabelText('Người thân ở lại'), { target: { value: '31' } })
    fireEvent.change(within(sheet).getByLabelText('Bắt đầu dự kiến'), { target: { value: '2026-11-01T18:00' } })
    fireEvent.change(within(sheet).getByLabelText('Kết thúc dự kiến'), { target: { value: '2026-11-03T08:00' } })
    fireEvent.change(within(sheet).getByLabelText('Lý do'), { target: { value: 'Chăm sóc sau mổ' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Có' }))
    fireEvent.click(within(sheet).getByRole('button', { name: 'Lưu đăng ký' }))

    await waitFor(() => expect(api.calls('POST', '/family-stays')).toHaveLength(1))
    const [, init] = api.calls('POST', '/family-stays')[0]
    expect(JSON.parse(init.body)).toEqual({
      familyMemberId: 31,
      residentId: 5,
      expectedStartAt: '2026-11-01T18:00:00+07:00',
      expectedEndAt: '2026-11-03T08:00:00+07:00',
      location: 'PHONG_NCT',
      reason: 'Chăm sóc sau mổ',
      mealRegistered: true,
    })
    expect(init.headers['Idempotency-Key']).toMatch(/^[0-9a-f-]{36}$/)
    expect(await screen.findByText('Đã đăng ký lượt ở lại, chờ Trưởng tầng xác nhận')).toBeInTheDocument()
  })

  it('FR_045_hanhChinh_giaHanCanLyDo_ketThuc', async () => {
    permissions(OFFICE)
    await renderPage('OFFICE_STAFF')
    api.on('POST', '/family-stays/72/extend', json(200, { id: 72 }))
    api.on('POST', '/family-stays/72/end', json(200, { id: 72 }))

    fireEvent.click(screen.getByRole('button', { name: 'Đang ở lại 1' }))
    fireEvent.click(
      await within(screen.getByRole('table', { name: 'Lượt ở lại' })).findByRole('button', { name: /Lê Thu Hoa/ }),
    )
    const detail = await screen.findByRole('dialog', { name: 'Lê Thu Hoa ở lại với Nguyễn Thị Lan' })
    expect(within(detail).queryByRole('button', { name: 'Xác nhận lượt ở lại' })).not.toBeInTheDocument()

    fireEvent.click(within(detail).getByRole('button', { name: 'Gia hạn…' }))
    fireEvent.change(within(detail).getByLabelText('Kết thúc dự kiến mới'), { target: { value: '2026-11-04T08:00' } })
    fireEvent.click(within(detail).getByRole('button', { name: 'Lưu gia hạn' }))
    expect(await within(detail).findByText('Nhập lý do')).toBeInTheDocument()
    fireEvent.change(within(detail).getByLabelText('Lý do'), { target: { value: 'Chưa xuất viện' } })
    fireEvent.click(within(detail).getByRole('button', { name: 'Lưu gia hạn' }))
    await waitFor(() => expect(api.calls('POST', '/family-stays/72/extend')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/family-stays/72/extend')[0][1].body)).toEqual({
      newExpectedEndAt: '2026-11-04T08:00:00+07:00',
      reason: 'Chưa xuất viện',
    })

    fireEvent.click(within(detail).getByRole('button', { name: 'Kết thúc ở lại' }))
    await waitFor(() => expect(api.calls('POST', '/family-stays/72/end')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/family-stays/72/end')[0][1].body)).toEqual({})
  })

  it('khongCoQuyenXem_hienKhongCoQuyen', async () => {
    permissions([])
    api.on('GET', '/family-stays', problem(403, 'PERMISSION_DENIED'))
    render(
      <PermissionsProvider>
        <FamilyStaysPage role="DOCTOR" />
      </PermissionsProvider>,
    )
    expect(await screen.findByText(/không có quyền/i)).toBeInTheDocument()
  })
})

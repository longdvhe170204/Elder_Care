import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { PickupPage } from './PickupPage'

// 012 T021 (US2; FR-021 → FR-028, BR-M10-03): màn "Quy trình đón" cho nhân viên có quyền lệnh nguồn (Phụ lục 27 dòng
// "Quy trình đón, ngoại lệ đón"). Ghi đón chỉ trực tuyến; không đạt thì backend ghi lần chặn và trả PICKUP_BLOCKED kèm
// blockReason; ngoại lệ đón lập tại quầy, Quản lý viện duyệt kèm lý do. Mong muốn về người đón hiện kèm nhãn người nói
// (DESIGN.md 2.2).

let api

const HA = { familyMemberId: 32, fullName: 'Trần Thu Hà', phoneNumber: '0901000222', relationship: 'GRANDCHILD' }

const RECORDS = [
  {
    id: 51,
    type: 'TEMPORARY_ABSENCE',
    familyMemberId: 32,
    basis: 'PICKUP_LIST',
    at: '2026-11-05T02:00:00Z',
    status: 'USED',
    expiresAt: '2026-11-05T02:30:00Z',
  },
  {
    id: 52,
    type: 'DAY_CARE_DEPARTURE',
    familyMemberId: null,
    basis: 'MANAGER_EXCEPTION',
    at: '2026-11-05T09:00:00Z',
    status: 'UNUSED',
    expiresAt: '2026-11-05T09:30:00Z',
  },
]

const WISHES = {
  effective: [
    {
      id: 92,
      relationshipId: 12,
      fullName: 'Trần Văn Minh',
      type: 'PICKUP',
      wanted: false,
      source: { type: 'RESIDENT', familyMemberId: null },
      recordedBy: 'Lê Thị Mai',
      at: '2026-11-02T03:00:00Z',
      status: 'ACTIVE',
      supersededById: null,
    },
    {
      id: 93,
      relationshipId: 12,
      fullName: 'Trần Văn Minh',
      type: 'VISIT',
      wanted: true,
      source: { type: 'RESIDENT', familyMemberId: null },
      recordedBy: 'Lê Thị Mai',
      at: '2026-11-02T03:00:00Z',
      status: 'ACTIVE',
      supersededById: null,
    },
  ],
  history: [],
  conflictingRights: [],
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
  api.on('GET', '/residents/5/pickup-list', json(200, [HA]))
  api.on('GET', '/residents/5/pickup-records', json(200, RECORDS))
  api.on('GET', '/residents/5/visit-pickup-wishes', json(200, WISHES))
  api.on('GET', '/staff', json(200, [{ id: 9003, fullName: 'Lê Thị Mai', position: 'Hành chính' }]))
})

afterEach(() => {
  vi.unstubAllGlobals()
})

async function openResident() {
  render(
    <PermissionsProvider>
      <PickupPage />
    </PermissionsProvider>,
  )
  fireEvent.click(await screen.findByRole('button', { name: /Nguyễn Thị Lan/ }))
  return screen.findByRole('table', { name: 'Bản ghi đón' })
}

async function openRecordSheet() {
  fireEvent.click(await screen.findByRole('button', { name: 'Ghi đón' }))
  return screen.findByRole('dialog', { name: 'Ghi đón cho Nguyễn Thị Lan' })
}

describe('Quy trình đón', () => {
  it('FR_026_bangBanGhiDon_loai_canCu_trangThai', async () => {
    permissions([['PICKUP', 'VIEW']])
    const table = await openResident()

    const used = within(table).getByRole('row', { name: /Tạm vắng/ })
    expect(within(used).getByText('Trần Thu Hà')).toBeInTheDocument()
    expect(within(used).getByText('Trong danh sách')).toBeInTheDocument()
    expect(within(used).getByText('Đã dùng')).toBeInTheDocument()
    const exception = within(table).getByRole('row', { name: /Bán trú về/ })
    expect(within(exception).getByText('Người đón theo ngoại lệ')).toBeInTheDocument()
    expect(within(exception).getByText('Ngoại lệ do Quản lý viện duyệt')).toBeInTheDocument()
    expect(within(exception).getByText('Chưa dùng')).toBeInTheDocument()

    // Danh sách được phép đón và mong muốn về người đón có nhãn người nói; không có quyền tạo thì không có nút lệnh
    expect(screen.getByRole('list', { name: 'Người được phép đón' })).toHaveTextContent('Trần Thu Hà')
    expect(screen.getByText('Không muốn Trần Văn Minh đón')).toBeInTheDocument()
    expect(screen.queryByText('Muốn Trần Văn Minh đến thăm')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Ghi đón' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Lập yêu cầu ngoại lệ' })).not.toBeInTheDocument()
  })

  it('FR_021_ghiDonTrongDanhSach_guiDungThan', async () => {
    permissions([
      ['PICKUP', 'VIEW'],
      ['PICKUP', 'CREATE'],
    ])
    await openResident()
    const sheet = await openRecordSheet()
    api.on('POST', '/residents/5/pickups', json(201, { ...RECORDS[0], id: 53, status: 'UNUSED' }))

    fireEvent.click(within(sheet).getByRole('button', { name: 'Ghi bản ghi đón' }))
    expect(await within(sheet).findByText('Chọn loại đón')).toBeInTheDocument()
    expect(within(sheet).getByText('Chọn người đón')).toBeInTheDocument()
    expect(within(sheet).getByText('Nhập số giấy tờ xuất trình')).toBeInTheDocument()
    expect(within(sheet).getByText('Chọn nhân viên bàn giao')).toBeInTheDocument()
    expect(api.calls('POST', '/residents/5/pickups')).toHaveLength(0)

    fireEvent.click(within(sheet).getByRole('radio', { name: 'Tạm vắng' }))
    fireEvent.click(within(sheet).getByRole('radio', { name: /Trần Thu Hà/ }))
    fireEvent.change(within(sheet).getByLabelText('Loại giấy tờ xuất trình'), { target: { value: 'CCCD' } })
    fireEvent.change(within(sheet).getByLabelText('Số giấy tờ xuất trình'), { target: { value: ' 001234567890 ' } })
    fireEvent.change(await within(sheet).findByLabelText('Nhân viên bàn giao'), { target: { value: '9003' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Ghi bản ghi đón' }))

    await waitFor(() => expect(api.calls('POST', '/residents/5/pickups')).toHaveLength(1))
    const [, init] = api.calls('POST', '/residents/5/pickups')[0]
    expect(JSON.parse(init.body)).toEqual({
      type: 'TEMPORARY_ABSENCE',
      familyMemberId: 32,
      presentedIdDocument: { type: 'CCCD', number: '001234567890' },
      handoverStaffId: 9003,
    })
    expect(init.headers['Idempotency-Key']).toMatch(/^[0-9a-f-]{36}$/)
    expect(await screen.findByText('Đã ghi đón')).toBeInTheDocument()
  })

  it('FR_021_tuVe_khongCoNguoiDonVaGiayTo', async () => {
    permissions([['PICKUP', 'CREATE']])
    await openResident()
    const sheet = await openRecordSheet()
    api.on('POST', '/residents/5/pickups', json(201, { ...RECORDS[0], id: 54, type: 'SELF_RETURN' }))

    fireEvent.click(within(sheet).getByRole('radio', { name: 'Tự về' }))
    expect(within(sheet).queryByRole('group', { name: 'Người đón' })).not.toBeInTheDocument()
    expect(within(sheet).queryByLabelText('Số giấy tờ xuất trình')).not.toBeInTheDocument()
    fireEvent.change(await within(sheet).findByLabelText('Nhân viên tiễn'), { target: { value: '9003' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Ghi bản ghi đón' }))

    await waitFor(() => expect(api.calls('POST', '/residents/5/pickups')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/residents/5/pickups')[0][1].body)).toEqual({
      type: 'SELF_RETURN',
      handoverStaffId: 9003,
    })
  })

  it('FR_023_biChan_hienLyDoVaLanChanDaGhi', async () => {
    permissions([['PICKUP', 'CREATE']])
    await openResident()
    const sheet = await openRecordSheet()
    api.on(
      'POST',
      '/residents/5/pickups',
      problem(409, 'PICKUP_BLOCKED', { detail: 'Lượt đón bị chặn', blockReason: 'ID_DOCUMENT_MISMATCH' }),
    )

    fireEvent.click(within(sheet).getByRole('radio', { name: 'Đi chơi với gia đình' }))
    fireEvent.click(within(sheet).getByRole('radio', { name: /Trần Thu Hà/ }))
    fireEvent.change(within(sheet).getByLabelText('Số giấy tờ xuất trình'), { target: { value: '999' } })
    fireEvent.change(await within(sheet).findByLabelText('Nhân viên bàn giao'), { target: { value: '9003' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Ghi bản ghi đón' }))

    expect(await within(sheet).findByText('Giấy tờ xuất trình không khớp số đã đăng ký.')).toBeInTheDocument()
    expect(within(sheet).getByText(/Đã ghi lần chặn đón/)).toBeInTheDocument()
    // Giữ giá trị đã nhập để sửa và ghi lại (DESIGN.md 9.2 điểm 4)
    expect(within(sheet).getByLabelText('Số giấy tờ xuất trình')).toHaveValue('999')
  })

  it('FR_021_nguoiDonTheoNgoaiLe_guiMaNgoaiLe', async () => {
    permissions([['PICKUP', 'CREATE']])
    await openResident()
    const sheet = await openRecordSheet()
    api.on('POST', '/residents/5/pickups', json(201, { ...RECORDS[1], id: 55 }))

    fireEvent.click(within(sheet).getByRole('radio', { name: 'Bán trú về' }))
    fireEvent.click(within(sheet).getByRole('radio', { name: 'Người đón theo ngoại lệ' }))
    fireEvent.click(within(sheet).getByRole('button', { name: 'Ghi bản ghi đón' }))
    expect(await within(sheet).findByText('Nhập mã yêu cầu ngoại lệ')).toBeInTheDocument()

    fireEvent.change(within(sheet).getByLabelText('Mã yêu cầu ngoại lệ'), { target: { value: '81' } })
    fireEvent.change(within(sheet).getByLabelText('Số giấy tờ xuất trình'), { target: { value: '0011' } })
    fireEvent.change(await within(sheet).findByLabelText('Nhân viên bàn giao'), { target: { value: '9003' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Ghi bản ghi đón' }))

    await waitFor(() => expect(api.calls('POST', '/residents/5/pickups')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/residents/5/pickups')[0][1].body)).toEqual({
      type: 'DAY_CARE_DEPARTURE',
      exceptionId: 81,
      presentedIdDocument: { number: '0011' },
      handoverStaffId: 9003,
    })
  })

  it('FR_024_lapYeuCauNgoaiLe_batBuocDuTruong_vaHienMa', async () => {
    permissions([['PICKUP', 'CREATE']])
    await openResident()
    api.on('POST', '/residents/5/pickup-exceptions', json(201, { id: 81 }))

    fireEvent.click(screen.getByRole('button', { name: 'Lập yêu cầu ngoại lệ' }))
    const sheet = await screen.findByRole('dialog', { name: 'Lập yêu cầu ngoại lệ đón' })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Gửi yêu cầu ngoại lệ' }))
    expect(await within(sheet).findByText('Nhập họ tên người đón')).toBeInTheDocument()
    expect(within(sheet).getByText('Nhập lý do')).toBeInTheDocument()
    expect(api.calls('POST', '/residents/5/pickup-exceptions')).toHaveLength(0)

    fireEvent.change(within(sheet).getByLabelText('Họ tên người đón'), { target: { value: 'Phạm Văn Nam' } })
    fireEvent.change(within(sheet).getByLabelText('Số giấy tờ'), { target: { value: '0011' } })
    fireEvent.change(within(sheet).getByLabelText('Quan hệ với người cao tuổi'), { target: { value: 'Hàng xóm' } })
    fireEvent.change(within(sheet).getByLabelText('Lý do'), { target: { value: 'Con gái ốm, nhờ hàng xóm đón' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Gửi yêu cầu ngoại lệ' }))

    await waitFor(() => expect(api.calls('POST', '/residents/5/pickup-exceptions')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/residents/5/pickup-exceptions')[0][1].body)).toEqual({
      fullName: 'Phạm Văn Nam',
      idDocumentNumber: '0011',
      relationship: 'Hàng xóm',
      reason: 'Con gái ốm, nhờ hàng xóm đón',
    })
    expect(await screen.findByText(/Đã lập yêu cầu ngoại lệ, mã 81/)).toBeInTheDocument()
  })

  it('FR_024_quanLyDuyetNgoaiLe_batBuocLyDo', async () => {
    permissions([
      ['PICKUP', 'VIEW'],
      ['PICKUP', 'APPROVE'],
    ])
    await openResident()
    api.on('POST', '/pickup-exceptions/81/approve', json(200, { id: 81 }))

    const form = screen.getByRole('form', { name: 'Xử lý yêu cầu ngoại lệ' })
    fireEvent.change(within(form).getByLabelText('Mã yêu cầu ngoại lệ'), { target: { value: '81' } })
    fireEvent.click(within(form).getByRole('radio', { name: 'Duyệt' }))
    fireEvent.click(within(form).getByRole('button', { name: 'Thực hiện lệnh' }))
    expect(await within(form).findByText('Nhập lý do')).toBeInTheDocument()
    expect(api.calls('POST', '/pickup-exceptions/81/approve')).toHaveLength(0)

    fireEvent.change(within(form).getByLabelText('Lý do'), { target: { value: 'Đã gọi xác minh với con gái' } })
    fireEvent.click(within(form).getByRole('button', { name: 'Thực hiện lệnh' }))

    await waitFor(() => expect(api.calls('POST', '/pickup-exceptions/81/approve')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/pickup-exceptions/81/approve')[0][1].body)).toEqual({
      reason: 'Đã gọi xác minh với con gái',
    })
    expect(await screen.findByText('Đã duyệt yêu cầu ngoại lệ 81')).toBeInTheDocument()
  })

  it('khongCoQuyenXem_hienKhongCoQuyen', async () => {
    permissions([])
    api.on('GET', '/residents/5/pickup-records', problem(403, 'PERMISSION_DENIED'))
    render(
      <PermissionsProvider>
        <PickupPage />
      </PermissionsProvider>,
    )
    fireEvent.click(await screen.findByRole('button', { name: /Nguyễn Thị Lan/ }))
    expect(await screen.findByText('Bạn không có quyền xem quy trình đón')).toBeInTheDocument()
  })
})

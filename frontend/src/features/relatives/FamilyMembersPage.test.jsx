import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { FamilyMembersPage } from './FamilyMembersPage'

// 012 T013 (US1; FR-001 → FR-012, DBR-02): màn hồ sơ người thân của Hành chính. Chọn người cao tuổi, bảng quan hệ có vai
// trò và quyền; khung chi tiết có lệnh kèm lý do; lỗi 409 hiện tại khung đang thao tác.

let api

beforeEach(() => {
  api = mockRoutes()
  api.on(
    'GET',
    '/me/permissions',
    json(200, [
      { function: 'FAMILY_MEMBER_PERMISSION', operation: 'CREATE' },
      { function: 'FAMILY_MEMBER_PERMISSION', operation: 'UPDATE' },
    ]),
  )
  api.on(
    'GET',
    '/residents',
    json(200, {
      items: [{ id: 5, profileCode: 'NCT-0005', personalInfo: { fullName: 'Nguyễn Thị Lan' }, status: 'IN_RESIDENCE' }],
      nextCursor: null,
    }),
  )
})

afterEach(() => {
  vi.unstubAllGlobals()
})

const MINH = {
  id: 11,
  familyMember: {
    id: 31,
    fullName: 'Trần Văn Minh',
    phoneNumber: '0901000111',
    idDocumentType: 'CCCD',
    idDocumentNumber: '079000000001',
    version: 0,
  },
  relationship: 'CHILD',
  representative: true,
  primaryContact: true,
  contactOrder: 1,
  rights: {
    VIEW_HEALTH: { enabled: true, effective: false },
    RECEIVE_URGENT: { enabled: true, effective: true },
    REQUEST_SERVICES: { enabled: true, effective: true },
  },
  status: 'ACTIVE',
}

const HA = {
  id: 12,
  familyMember: { id: 32, fullName: 'Trần Thu Hà', phoneNumber: '0901000222', version: 0 },
  relationship: 'GRANDCHILD',
  representative: false,
  primaryContact: false,
  contactOrder: 2,
  rights: {},
  status: 'ACTIVE',
}

async function openResident() {
  api.on('GET', '/residents/5/relationships', json(200, [MINH, HA]))
  render(
    <PermissionsProvider>
      <FamilyMembersPage role="OFFICE_STAFF" onNavigate={() => {}} />
    </PermissionsProvider>,
  )
  fireEvent.click(await screen.findByRole('button', { name: /Nguyễn Thị Lan/ }))
  return screen.findByRole('table')
}

describe('FamilyMembersPage', () => {
  it('FR_002_bangQuanHe_coVaiTro_vaQuyenKhongCoTacDung', async () => {
    const table = await openResident()

    const minh = within(table).getByRole('row', { name: /Trần Văn Minh/ })
    expect(within(minh).getByText('Người đại diện, Người liên hệ chính')).toBeInTheDocument()
    expect(within(minh).getByText('Con')).toBeInTheDocument()
    fireEvent.click(within(minh).getByRole('button', { name: /Trần Văn Minh/ }))

    const detail = await screen.findByRole('heading', { name: 'Trần Văn Minh' })
    expect(detail).toBeInTheDocument()
    expect(screen.getByText('Có, không có tác dụng')).toBeInTheDocument()
  })

  it('FR_006_ketThucQuanHe_batBuocLyDo_va409HienTaiKhung', async () => {
    const table = await openResident()
    fireEvent.click(within(table).getByRole('button', { name: /Trần Văn Minh/ }))
    api.on('POST', '/relationships/11/end', problem(409, 'DBR_02_VIOLATED', { detail: 'vi phạm' }))

    fireEvent.click(await screen.findByRole('button', { name: 'Kết thúc quan hệ' }))
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận kết thúc quan hệ' }))
    expect(await screen.findByText('Nhập lý do')).toBeInTheDocument()
    expect(api.calls('POST', '/relationships/11/end')).toHaveLength(0)

    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Gia đình báo' } })
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận kết thúc quan hệ' }))

    expect(await screen.findByText(/đúng một người liên hệ chính và ít nhất một người đại diện/)).toBeInTheDocument()
    const [, init] = api.calls('POST', '/relationships/11/end')[0]
    expect(JSON.parse(init.body)).toEqual({ reason: 'Gia đình báo' })
    expect(init.headers['Idempotency-Key']).toMatch(/^[0-9a-f-]{36}$/)
  })

  it('FR_004_doiLienHeChinh_thanhCong_taiLaiBang', async () => {
    const table = await openResident()
    fireEvent.click(within(table).getByRole('button', { name: /Trần Thu Hà/ }))
    api.on('POST', '/relationships/12/change-primary-contact', json(200, { id: 12 }))

    fireEvent.click(await screen.findByRole('button', { name: 'Đổi người liên hệ chính' }))
    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Minh đi công tác' } })
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận đổi người liên hệ chính' }))

    await waitFor(() => expect(api.calls('GET', '/residents/5/relationships')).toHaveLength(2))
    expect(await screen.findByText('Đã đổi người liên hệ chính')).toBeInTheDocument()
  })

  it('FR_005_datNguoiDaiDienDauTien_chiKhiChuaCoNguoiDaiDien', async () => {
    const table = await openResident()
    fireEvent.click(within(table).getByRole('button', { name: /Trần Thu Hà/ }))
    await screen.findByRole('heading', { name: 'Trần Thu Hà' })
    expect(screen.queryByRole('button', { name: 'Đặt người đại diện đầu tiên' })).not.toBeInTheDocument()
  })
})

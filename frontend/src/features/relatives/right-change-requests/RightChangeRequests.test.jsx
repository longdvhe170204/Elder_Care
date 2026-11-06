import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { FamilyMembersPage } from '../FamilyMembersPage'

// 012 T017 (US3; FR-013 → FR-020c, BR-M10-07): trên màn "Người thân và quyền", khối yêu cầu thay đổi quyền (lập tại
// quầy, ghi nhận bản ký, Quản lý viện duyệt kèm nội dung đã trao đổi khi trái mong muốn) và khối mong muốn của người cao
// tuổi có nhãn người nói, cảnh báo quyền trái mong muốn (DESIGN.md 2.2, 7.2 visit-wishes).

let api

const MINH = {
  id: 11,
  familyMember: { id: 31, fullName: 'Trần Văn Minh', phoneNumber: '0901000111', version: 0 },
  relationship: 'CHILD',
  representative: true,
  primaryContact: true,
  contactOrder: 1,
  rights: { REQUEST_SERVICES: { enabled: true, effective: true } },
  status: 'ACTIVE',
}

const HA = {
  id: 12,
  familyMember: { id: 32, fullName: 'Trần Thu Hà', phoneNumber: '0901000222', version: 0 },
  relationship: 'GRANDCHILD',
  representative: false,
  primaryContact: false,
  contactOrder: 2,
  rights: { PICKUP_ALLOWED: { enabled: true, effective: true } },
  status: 'ACTIVE',
}

const REMOVE_HA = {
  id: 71,
  residentId: 5,
  type: 'RIGHT',
  relationshipId: 12,
  right: 'PICKUP_ALLOWED',
  newValue: false,
  reason: 'Hà chuyển công tác',
  status: 'PENDING_CONFIRMATION',
  source: 'COUNTER',
  appliedOnCreation: true,
  conflictingWishId: null,
}

const VISIT_HA = {
  id: 72,
  residentId: 5,
  type: 'RIGHT',
  relationshipId: 12,
  right: 'REGISTER_VISITS',
  newValue: true,
  reason: 'Hà xin đăng ký thăm',
  status: 'PENDING_CONFIRMATION',
  source: 'COUNTER',
  appliedOnCreation: false,
  conflictingWishId: 91,
}

const WISHES = {
  effective: [
    {
      id: 91,
      relationshipId: 12,
      fullName: 'Trần Thu Hà',
      type: 'VISIT',
      wanted: false,
      source: { type: 'RESIDENT', familyMemberId: null },
      recordedBy: 'Lê Thị Mai',
      at: '2026-11-02T03:00:00Z',
      status: 'ACTIVE',
      supersededById: null,
    },
  ],
  history: [],
  conflictingRights: [{ relationshipId: 12, right: 'PICKUP_ALLOWED', wishId: 92 }],
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
  api.on('GET', '/residents/5/relationships', json(200, [MINH, HA]))
  api.on('GET', '/right-change-requests', json(200, [REMOVE_HA, VISIT_HA]))
  api.on('GET', '/residents/5/visit-pickup-wishes', json(200, WISHES))
})

afterEach(() => {
  vi.unstubAllGlobals()
})

async function openResident() {
  render(
    <PermissionsProvider>
      <FamilyMembersPage role="OFFICE_STAFF" onNavigate={() => {}} />
    </PermissionsProvider>,
  )
  fireEvent.click(await screen.findByRole('button', { name: /Nguyễn Thị Lan/ }))
  return screen.findByRole('table', { name: 'Yêu cầu thay đổi quyền' })
}

describe('Yêu cầu thay đổi quyền', () => {
  it('FR_017_bangYeuCau_noiDung_trangThai_vaDaApNgay', async () => {
    permissions([['FAMILY_MEMBER_PERMISSION', 'VIEW']])
    const table = await openResident()

    const row = within(table).getByRole('row', { name: /Tắt quyền Được phép đón cho Trần Thu Hà/ })
    expect(within(row).getByText('Chờ xác nhận')).toBeInTheDocument()
    expect(within(row).getByText('Đã áp dụng khi lập')).toBeInTheDocument()
    const query = new URL(api.calls('GET', '/right-change-requests')[0][0], 'http://x').searchParams
    expect(query.get('residentId')).toBe('5')
    expect(screen.queryByRole('button', { name: 'Lập yêu cầu' })).not.toBeInTheDocument()
  })

  it('FR_013_lapYeuCauTaiQuay_guiDungThan', async () => {
    permissions([['FAMILY_MEMBER_PERMISSION', 'CREATE']])
    await openResident()
    api.on('POST', '/right-change-requests', json(201, { ...REMOVE_HA, id: 73 }))

    fireEvent.click(screen.getByRole('button', { name: 'Lập yêu cầu' }))
    fireEvent.change(await screen.findByLabelText('Người thân'), { target: { value: '11' } })
    fireEvent.change(screen.getByLabelText('Quyền'), { target: { value: 'VIEW_COSTS' } })
    fireEvent.click(screen.getByRole('radio', { name: 'Bật' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lập yêu cầu thay đổi quyền' }))
    expect(await screen.findByText('Nhập lý do')).toBeInTheDocument()
    expect(api.calls('POST', '/right-change-requests')).toHaveLength(0)

    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Minh lo chi phí' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lập yêu cầu thay đổi quyền' }))

    await waitFor(() => expect(api.calls('POST', '/right-change-requests')).toHaveLength(1))
    const [, init] = api.calls('POST', '/right-change-requests')[0]
    expect(JSON.parse(init.body)).toEqual({
      residentId: 5,
      type: 'RIGHT',
      relationshipId: 11,
      right: 'VIEW_COSTS',
      newValue: true,
      withSignedForm: false,
      reason: 'Minh lo chi phí',
    })
    expect(init.headers['Idempotency-Key']).toMatch(/^[0-9a-f-]{36}$/)
    expect(await screen.findByText('Đã lập yêu cầu')).toBeInTheDocument()
  })

  it('FR_020b_quanLyDuyetYeuCauTraiMongMuon_batBuocNoiDungDaTraoDoi', async () => {
    permissions([
      ['FAMILY_MEMBER_PERMISSION', 'VIEW'],
      ['FAMILY_MEMBER_PERMISSION', 'APPROVE'],
    ])
    const table = await openResident()
    fireEvent.click(within(table).getByRole('button', { name: /Bật quyền Đăng ký thăm cho Trần Thu Hà/ }))

    expect(await screen.findByText('Trái mong muốn của người cao tuổi')).toBeInTheDocument()
    api.on('POST', '/right-change-requests/72/approve', json(200, { ...VISIT_HA, status: 'EFFECTIVE' }))
    fireEvent.click(screen.getByRole('button', { name: 'Duyệt' }))
    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Bà đồng ý gặp lại cháu' } })
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận duyệt' }))
    expect(await screen.findByText('Ghi nội dung đã trao đổi với người cao tuổi')).toBeInTheDocument()
    expect(api.calls('POST', '/right-change-requests/72/approve')).toHaveLength(0)

    fireEvent.change(screen.getByLabelText('Nội dung đã trao đổi với người cao tuổi'), {
      target: { value: 'Bà nói muốn gặp Hà vào cuối tuần' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận duyệt' }))

    await waitFor(() => expect(api.calls('POST', '/right-change-requests/72/approve')).toHaveLength(1))
    const [, init] = api.calls('POST', '/right-change-requests/72/approve')[0]
    expect(JSON.parse(init.body)).toEqual({
      reason: 'Bà đồng ý gặp lại cháu',
      discussedWithResident: 'Bà nói muốn gặp Hà vào cuối tuần',
    })
  })

  it('FR_015b_ghiNhanBanKy_canNguoiKyVaBanScan', async () => {
    permissions([['FAMILY_MEMBER_PERMISSION', 'UPDATE']])
    const table = await openResident()
    fireEvent.click(within(table).getByRole('button', { name: /Tắt quyền Được phép đón cho Trần Thu Hà/ }))
    api.on('POST', '/files', json(201, { fileId: 501 }))
    api.on('POST', '/right-change-requests/71/confirm', problem(409, 'STATE_CHANGED', { detail: 'đã có kết quả' }))

    fireEvent.click(await screen.findByRole('button', { name: 'Ghi nhận bản ký' }))
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận theo bản ký' }))
    expect(await screen.findByText('Chọn người đại diện đã ký')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Người đại diện ký'), { target: { value: '31' } })
    fireEvent.change(screen.getByLabelText('Bản scan bản ký'), {
      target: { files: [new File(['x'], 'ban-ky.pdf', { type: 'application/pdf' })] },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận theo bản ký' }))

    expect(await screen.findByText(/Yêu cầu đã có kết quả/)).toBeInTheDocument()
    const [, init] = api.calls('POST', '/right-change-requests/71/confirm')[0]
    expect(JSON.parse(init.body)).toEqual({ signedByFamilyMemberId: 31, evidenceFileId: 501 })
  })
})

describe('Mong muốn của người cao tuổi', () => {
  it('FR_020a_hienNhanNguoiNoi_vaCanhBaoQuyenTraiMongMuon', async () => {
    permissions([['VISITOR_PREFERENCE', 'VIEW']])
    render(
      <PermissionsProvider>
        <FamilyMembersPage role="NURSING_HOME_MANAGER" onNavigate={() => {}} />
      </PermissionsProvider>,
    )
    fireEvent.click(await screen.findByRole('button', { name: /Nguyễn Thị Lan/ }))

    const block = await screen.findByRole('region', { name: 'Mong muốn của người cao tuổi' })
    expect(within(block).getByText('Không muốn Trần Thu Hà đến thăm')).toBeInTheDocument()
    expect(within(block).getByText('Theo lời người cao tuổi')).toBeInTheDocument()
    expect(within(block).getByText('Ghi bởi Lê Thị Mai, 02/11/2026')).toBeInTheDocument()
    expect(within(block).getByText(/Trần Thu Hà đang có quyền Được phép đón/)).toBeInTheDocument()
    expect(within(block).queryByRole('button', { name: 'Ghi mong muốn mới' })).not.toBeInTheDocument()
  })

  it('FR_020a_ghiMongMuonNguoiNgoai_canHoTenVaSoGiayTo', async () => {
    permissions([
      ['VISITOR_PREFERENCE', 'VIEW'],
      ['VISITOR_PREFERENCE', 'CREATE'],
    ])
    render(
      <PermissionsProvider>
        <FamilyMembersPage role="FLOOR_MANAGER" onNavigate={() => {}} />
      </PermissionsProvider>,
    )
    fireEvent.click(await screen.findByRole('button', { name: /Nguyễn Thị Lan/ }))
    api.on('POST', '/residents/5/visit-pickup-wishes', json(201, WISHES.effective[0]))

    fireEvent.click(await screen.findByRole('button', { name: 'Ghi mong muốn mới' }))
    fireEvent.change(await screen.findByLabelText('Người được nói tới'), { target: { value: 'outsider' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu mong muốn' }))
    expect(await screen.findByText('Nhập họ tên và số giấy tờ')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Họ tên'), { target: { value: 'Phạm Văn Tư' } })
    fireEvent.change(screen.getByLabelText('Số giấy tờ'), { target: { value: '079000000099' } })
    fireEvent.click(screen.getByRole('radio', { name: 'Đón' }))
    fireEvent.click(screen.getByRole('radio', { name: 'Không muốn' }))
    fireEvent.click(screen.getByRole('radio', { name: 'Người cao tuổi nói' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu mong muốn' }))

    await waitFor(() => expect(api.calls('POST', '/residents/5/visit-pickup-wishes')).toHaveLength(1))
    const [, init] = api.calls('POST', '/residents/5/visit-pickup-wishes')[0]
    expect(JSON.parse(init.body)).toEqual({
      fullName: 'Phạm Văn Tư',
      idDocumentNumber: '079000000099',
      type: 'PICKUP',
      wanted: false,
      source: { type: 'RESIDENT' },
    })
  })
})

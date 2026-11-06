import { beforeEach, describe, expect, it } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { ContractsPage } from './ContractsPage'

// 004 T038 (US3, US4; FR-019 → FR-025a, FR-028 → FR-031): danh sách hợp đồng lọc theo trạng thái, nút lệnh theo
// allowedCommands của hợp đồng, trường tài chính ẩn theo quyền (hiddenFields), gửi ký 409 hiện điều kiện chưa thỏa, ghi nhận
// đã ký cần ngày ký, người đại diện ký và bản scan; đặt cọc chỉ đọc (do 017 tính).

let api

const DRAFT = {
  id: 31,
  contractNumber: 'HD-031',
  residentId: 5,
  residenceType: 'LONG_TERM_RESIDENTIAL',
  startDate: '2026-11-10',
  careLevel: 'REGULAR',
  residencyFeeBillingPeriod: 'MONTH',
  status: 'DRAFT',
  allowedCommands: ['update', 'send-for-signature', 'cancel'],
  hiddenFields: [],
}
const PENDING = {
  ...DRAFT,
  id: 32,
  residentId: 6,
  contractNumber: 'HD-032',
  status: 'PENDING_SIGNATURE',
  allowedCommands: ['return-to-draft', 'record-signed', 'cancel'],
}
const ACTIVE = {
  ...DRAFT,
  id: 33,
  residentId: 7,
  contractNumber: 'HD-033',
  status: 'ACTIVE',
  allowedCommands: [],
  overdueSince: '2026-10-30',
  hiddenFields: ['depositAmount'],
}

function renderPage() {
  return render(
    <PermissionsProvider>
      <ContractsPage />
    </PermissionsProvider>,
  )
}

beforeEach(() => {
  api = mockRoutes()
  api.on(
    'GET',
    '/me/permissions',
    json(
      200,
      ['VIEW', 'CREATE', 'UPDATE'].map((operation) => ({ function: 'ADMISSION_CONTRACT_DEPOSIT', operation })),
    ),
  )
  api.on('GET', '/contracts', json(200, [DRAFT, PENDING, ACTIVE]))
  for (const [id, name] of [
    [5, 'Nguyễn Thị Lan'],
    [6, 'Lê Văn Hải'],
    [7, 'Phạm Thị Mai'],
  ])
    api.on('GET', `/residents/${id}`, json(200, { id, personalInfo: { fullName: name } }))
  api.on(
    'GET',
    '/residents/6/relationships',
    json(200, [
      { id: 1, status: 'ACTIVE', relationship: 'CHILD', familyMember: { id: 41, fullName: 'Trần Văn Minh' } },
    ]),
  )
  api.on(
    'GET',
    '/contracts/31/deposit',
    json(200, { notRequired: false, requiredAmount: 10000000, status: 'NOT_SATISFIED' }),
  )
  api.on('GET', '/contracts/32/deposit', json(200, { notRequired: true, status: 'NOT_REQUIRED' }))
  api.on('GET', '/contracts/33/deposit', json(200, { notRequired: false, status: 'SATISFIED' }))
  for (const id of [31, 32, 33]) api.on('GET', `/contracts/${id}/addenda`, json(200, []))
  api.on('GET', '/contracts/31/non-standard-terms', json(200, []))
  api.on('GET', '/services', json(200, []))
})

describe('ContractsPage', () => {
  it('FR_029_bangHopDong_locTheoTrangThai_vaDauQuaHan', async () => {
    renderPage()
    expect(await screen.findByRole('button', { name: 'Nguyễn Thị Lan' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Phạm Thị Mai' })).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Hiệu lực 1' }))
    expect(await screen.findByRole('button', { name: 'Phạm Thị Mai' })).toBeInTheDocument()
    expect(screen.getByText(/Quá hạn từ 30\/10\/2026/)).toBeInTheDocument()
  })

  it('FR_020_nutLenh_chiTheoAllowedCommands', async () => {
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Nguyễn Thị Lan' }))
    expect(await screen.findByRole('button', { name: 'Gửi ký' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Ghi nhận đã ký/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Gửi duyệt điều khoản/ })).not.toBeInTheDocument()
  })

  it('FR_030_datCoc_chiDoc_hienKhoanCanCocVaTrangThai', async () => {
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Nguyễn Thị Lan' }))
    expect(await screen.findByText(/10\.000\.000/)).toBeInTheDocument()
    expect(screen.getByText(/Chưa đặt cọc/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Thu cọc|Xác nhận đặt cọc/ })).not.toBeInTheDocument()
  })

  it('FR_028_truongTaiChinhBiAn_hienGhiChuTheoQuyen', async () => {
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Hiệu lực 1' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Phạm Thị Mai' }))
    expect(await screen.findByText(/Trường tài chính được ẩn theo quyền/)).toBeInTheDocument()
  })

  it('FR_025_guiKy409_hienDieuKienChuaThoa', async () => {
    api.on(
      'POST',
      '/contracts/31/send-for-signature',
      problem(409, 'CONDITION_NOT_MET', {
        unmetConditions: [{ code: 'CARE_SERVICE', description: 'Cần đúng một dịch vụ chăm sóc theo mức của hợp đồng' }],
      }),
    )
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Nguyễn Thị Lan' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Gửi ký' }))
    expect(await screen.findByText('Cần đúng một dịch vụ chăm sóc theo mức của hợp đồng')).toBeInTheDocument()
  })

  it('FR_021_ghiNhanDaKy_canNgayKyNguoiDaiDienVaBanScan', async () => {
    api.on('POST', '/files', json(201, { fileId: 77 }))
    api.on(
      'POST',
      '/contracts/32/record-signed',
      json(200, { contract: { ...PENDING, status: 'ACTIVE' }, warnings: [] }),
    )
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Chờ ký 1' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Lê Văn Hải' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Ghi nhận đã ký…' }))

    fireEvent.click(screen.getByRole('button', { name: 'Lưu đã ký' }))
    expect(await screen.findByText('Nhập ngày ký')).toBeInTheDocument()
    expect(screen.getByText('Chọn người đại diện ký')).toBeInTheDocument()
    expect(screen.getByText('Chọn bản scan hợp đồng')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Ngày ký'), { target: { value: '2026-11-09' } })
    fireEvent.change(await screen.findByLabelText('Người đại diện ký'), { target: { value: '41' } })
    fireEvent.change(screen.getByLabelText('Bản scan hợp đồng'), {
      target: { files: [new File(['x'], 'hd.pdf', { type: 'application/pdf' })] },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu đã ký' }))

    await waitFor(() => expect(api.calls('POST', '/contracts/32/record-signed')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/contracts/32/record-signed')[0][1].body)).toEqual({
      signedDate: '2026-11-09',
      signingRepresentativeId: 41,
      scanFileId: 77,
    })
  })

  it('FR_019_lapHopDongNhap_thieuTruong_baoTaiO', async () => {
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Lập hợp đồng' }))
    const dialog = screen.getByRole('dialog')
    fireEvent.click(within(dialog).getByRole('button', { name: 'Lưu hợp đồng nháp' }))
    expect(await within(dialog).findByText('Chọn người cao tuổi')).toBeInTheDocument()
    expect(within(dialog).getByText('Chọn loại lưu trú')).toBeInTheDocument()
    expect(api.calls('POST', '/contracts')).toHaveLength(0)
  })

  it('FR_028_403_hienKhongCoQuyenXem', async () => {
    api.on('GET', '/contracts', problem(403, 'PERMISSION_DENIED'))
    renderPage()
    expect(await screen.findByText(/Bạn không có quyền xem/)).toBeInTheDocument()
  })
})

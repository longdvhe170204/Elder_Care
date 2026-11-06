import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { RegistrationDetail } from './RegistrationDetail'

// Khung chi tiết theo người cao tuổi (018 T008, T010, T012, T014; bố cục K3, K7).

const PERMS = [
  { function: 'RESIDENCE_REGISTRATION', operation: 'VIEW' },
  { function: 'RESIDENCE_REGISTRATION', operation: 'CREATE' },
  { function: 'RESIDENCE_REGISTRATION', operation: 'UPDATE' },
]
const VIEW_ONLY = [{ function: 'RESIDENCE_REGISTRATION', operation: 'VIEW' }]

const toSubmit = {
  id: 7,
  residentId: 5,
  type: 'TEMPORARY_RESIDENCE_REGISTRATION',
  renewal: false,
  dueAt: '2026-11-10T16:59:59Z',
  status: 'TO_SUBMIT',
  localPermanentResident: false,
  allowedCommands: ['confirm-local-permanent-resident', 'record-submitted'],
  submissions: [],
}
const rejectedBefore = {
  id: 6,
  residentId: 5,
  type: 'STAY_NOTIFICATION',
  renewal: false,
  status: 'SUPERSEDED',
  localPermanentResident: true,
  basisNote: 'Sổ hộ khẩu cùng phường',
  allowedCommands: [],
  submissions: [
    {
      id: 31,
      submittedDate: '2026-10-01',
      channel: 'IN_PERSON',
      receivingAuthority: 'Công an phường X',
      dossierCode: 'HS-1',
      result: 'REJECTED',
      rejectionReason: 'Thiếu giấy tờ',
      recordedAt: '2026-10-01T03:00:00Z',
    },
  ],
}

function setup(perms, registrations, task) {
  const api = mockRoutes()
  api.on('GET', '/me/permissions', json(200, perms))
  api.on('GET', '/residents/5/residence-registrations', json(200, registrations))
  render(
    <PermissionsProvider>
      <RegistrationDetail
        residentId={5}
        fullName="Nguyễn Thị Lan"
        task={task}
        Title="h2"
        Description="p"
        onChanged={() => {}}
      />
    </PermissionsProvider>,
  )
  return api
}

describe('RegistrationDetail (018)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('K3_hienKhaiBaoHienHanh_nutLenhTheoAllowedCommands_vaKhaiBaoCuThuGon', async () => {
    setup(PERMS, [rejectedBefore, toSubmit])
    expect(await screen.findByRole('heading', { name: 'Nguyễn Thị Lan' })).toBeInTheDocument()
    expect(await screen.findByText('Đăng ký tạm trú')).toBeInTheDocument()
    expect(screen.getByText('Cần khai báo')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Xác nhận thường trú cùng xã/phường' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ghi đã nộp' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Ghi kết quả' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Ghi đã khai báo xóa' })).not.toBeInTheDocument()
    // Khai báo cũ (Được thay thế) thu gọn, không có nút lệnh
    expect(screen.getByRole('button', { name: /Khai báo cũ \(1\)/ })).toBeInTheDocument()
  })

  it('FR_009_chiCoQuyenXem_anMoiNutLenh_vaNutLap', async () => {
    setup(VIEW_ONLY, [toSubmit])
    expect(await screen.findByText('Cần khai báo')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Ghi đã nộp' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Lập khai báo' })).not.toBeInTheDocument()
  })

  it('K3_lichSuLanNop_hienNgayKenhCoQuanKetQuaVaLyDoTuChoi', async () => {
    setup(PERMS, [rejectedBefore])
    fireEvent.click(await screen.findByRole('button', { name: /Khai báo cũ \(1\)/ }))
    expect(await screen.findByText('Công an phường X')).toBeInTheDocument()
    expect(screen.getByText(/Trực tiếp/)).toBeInTheDocument()
    expect(screen.getByText('Bị từ chối')).toBeInTheDocument()
    expect(screen.getByText(/Thiếu giấy tờ/)).toBeInTheDocument()
    expect(screen.getByText(/HS-1/)).toBeInTheDocument()
    expect(screen.getByText(/Sổ hộ khẩu cùng phường/)).toBeInTheDocument()
  })

  it('FR_001_chuaCoKhaiBao_lapKhaiBao_goiPostRoiTaiLai', async () => {
    const api = setup(PERMS, [])
    api.on('POST', '/residents/5/residence-registrations', json(201, toSubmit))
    fireEvent.click(await screen.findByRole('button', { name: 'Lập khai báo' }))
    await waitFor(() => expect(api.calls('POST', '/residents/5/residence-registrations')).toHaveLength(1))
    const [, init] = api.calls('POST', '/residents/5/residence-registrations')[0]
    expect(init.headers['Idempotency-Key']).toBeTruthy()
    expect(JSON.parse(init.body)).toEqual({ renewal: false })
    expect(api.calls('GET', '/residents/5/residence-registrations').length).toBeGreaterThanOrEqual(2)
  })

  it('FR_006_coDangKyTamTruDaXacNhan_choLapKhaiBaoGiaHan', async () => {
    const confirmed = {
      ...toSubmit,
      id: 8,
      status: 'CONFIRMED',
      expiryDate: '2026-12-01',
      allowedCommands: [],
    }
    const api = setup(PERMS, [confirmed])
    api.on('POST', '/residents/5/residence-registrations', json(201, toSubmit))
    fireEvent.click(await screen.findByRole('button', { name: 'Lập khai báo gia hạn' }))
    await waitFor(() => expect(api.calls('POST', '/residents/5/residence-registrations')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/residents/5/residence-registrations')[0][1].body)).toEqual({ renewal: true })
  })

  it('FR_005_chiCoHetHan_chiHienLapKhaiBao_khongCoNutGiaHan', async () => {
    const expired = { ...toSubmit, id: 9, status: 'EXPIRED', allowedCommands: [], expiryDate: '2026-10-01' }
    setup(PERMS, [expired])
    expect(await screen.findByRole('button', { name: 'Lập khai báo' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Lập khai báo gia hạn' })).not.toBeInTheDocument()
  })

  it('FR_001_409_khongCoNghiaVu_hienAlertTrongKhung', async () => {
    const api = setup(PERMS, [])
    api.on(
      'POST',
      '/residents/5/residence-registrations',
      problem(409, 'NO_REGISTRATION_OBLIGATION', { detail: 'Người này chưa có nghĩa vụ khai báo' }),
    )
    fireEvent.click(await screen.findByRole('button', { name: 'Lập khai báo' }))
    expect(await screen.findByText('Người này chưa có nghĩa vụ khai báo')).toBeInTheDocument()
  })

  it('K7_hetHanVaKhaiBaoXoa_hienNutGhiKhaiBaoXoa_khiBackendChoPhep', async () => {
    const expired = { ...toSubmit, id: 9, status: 'EXPIRED', allowedCommands: ['record-deregistered'], expiryDate: '2026-10-01' }
    setup(PERMS, [expired])
    expect(await screen.findByText('Hết hạn')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Ghi đã khai báo xóa' }))
    const form = await screen.findByRole('form', { name: 'Ghi đã khai báo xóa' })
    expect(within(form).getByLabelText('Bằng chứng đã khai báo xóa')).toBeInTheDocument()
  })

  it('K3_ghiKetQua_chiHienKhiDaNop_moForm', async () => {
    const submitted = { ...toSubmit, status: 'SUBMITTED', allowedCommands: ['record-accepted', 'record-rejected'] }
    setup(PERMS, [submitted])
    fireEvent.click(await screen.findByRole('button', { name: 'Ghi kết quả' }))
    expect(await screen.findByRole('form', { name: 'Ghi kết quả' })).toBeInTheDocument()
  })
})

import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { RecipientConfirmations } from './RecipientConfirmations'

const PENDING = {
  id: 61,
  residentId: 7,
  recipientName: 'Lê Văn Nam',
  recipientIdDocument: 'CCCD 002',
  recipientIsResident: false,
  belongingIds: [90],
  status: 'PENDING_CONFIRMATION',
  method: 'PORTAL',
  expiresAt: null,
}
const BELONGINGS = [{ id: 90, description: 'Điện thoại Nokia', status: 'HELD', version: 1 }]

function setup(role = 'OFFICE_STAFF') {
  const api = mockRoutes()
  api.on('GET', '/me/permissions', json(200, [{ function: 'BELONGINGS', operation: 'UPDATE' }]))
  api.on('GET', '/recipient-confirmations', json(200, [PENDING]))
  render(
    <PermissionsProvider>
      <RecipientConfirmations residentId={7} belongings={BELONGINGS} role={role} />
    </PermissionsProvider>,
  )
  return api
}

describe('RecipientConfirmations (013 T019, FR-017 → FR-017b)', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('FR_017_danhSachTheoNguoiCaoTuoi_trangThai', async () => {
    const api = setup()
    expect(await screen.findByText('Lê Văn Nam')).toBeInTheDocument()
    expect(screen.getByText('Chờ xác nhận')).toBeInTheDocument()
    expect(api.calls('GET', '/recipient-confirmations')[0][0]).toContain('residentId=7')
  })

  it('FR_017_lapYeuCau_guiDoVaNguoiNhan', async () => {
    const api = setup()
    api.on('POST', '/recipient-confirmations', json(201, { id: 62 }))
    fireEvent.click(await screen.findByRole('button', { name: 'Lập xác nhận người nhận khác' }))
    fireEvent.change(screen.getByLabelText('Họ tên người nhận khác'), { target: { value: 'Phạm Thị Hồng' } })
    fireEvent.change(screen.getByLabelText('Giấy tờ của người nhận'), { target: { value: 'CCCD 003' } })
    fireEvent.click(screen.getByRole('checkbox', { name: 'Xác nhận cho Điện thoại Nokia' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu yêu cầu xác nhận' }))

    await waitFor(() => expect(api.calls('POST', '/recipient-confirmations')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/recipient-confirmations')[0][1].body)).toEqual({
      residentId: 7,
      recipientName: 'Phạm Thị Hồng',
      recipientIdDocument: 'CCCD 003',
      recipientIsResident: false,
      belongingIds: [90],
    })
  })

  it('FR_017a_ghiLanLienHe_lamBangChung', async () => {
    const api = setup()
    api.on('POST', '/recipient-confirmations/61/contact-evidence', json(201, {}))
    fireEvent.click(await screen.findByRole('button', { name: 'Ghi lần liên hệ cho Lê Văn Nam' }))
    fireEvent.change(screen.getByLabelText('Mã người đại diện đã liên hệ'), { target: { value: '33' } })
    fireEvent.change(screen.getByLabelText('Kênh liên hệ'), { target: { value: 'PHONE_CALL' } })
    fireEvent.change(screen.getByLabelText('Kết quả liên hệ'), { target: { value: 'Không nghe máy' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu lần liên hệ' }))

    await waitFor(() => expect(api.calls('POST', '/recipient-confirmations/61/contact-evidence')).toHaveLength(1))
    const body = JSON.parse(api.calls('POST', '/recipient-confirmations/61/contact-evidence')[0][1].body)
    expect(body).toMatchObject({ familyMemberId: 33, channel: 'PHONE_CALL', result: 'Không nghe máy' })
    expect(body.at).toMatch(/\+07:00$/)
  })

  it('FR_017a_quanLyVienDuyetThay_lyDoVaCanCu_409ChuaDuDieuKien', async () => {
    const api = setup('NURSING_HOME_MANAGER')
    api.on(
      'POST',
      '/recipient-confirmations/61/approve-on-behalf',
      problem(409, 'ON_BEHALF_APPROVAL_NOT_ELIGIBLE', { detail: 'Chưa đủ số lần liên hệ người đại diện.' }),
    )
    fireEvent.click(await screen.findByRole('button', { name: 'Duyệt thay cho Lê Văn Nam' }))
    fireEvent.change(screen.getByLabelText('Căn cứ duyệt thay'), { target: { value: 'REPRESENTATIVE_UNREACHABLE' } })
    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Đã gọi nhiều lần' } })
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận duyệt thay' }))

    expect(await screen.findByText('Chưa đủ số lần liên hệ người đại diện.')).toBeInTheDocument()
    expect(JSON.parse(api.calls('POST', '/recipient-confirmations/61/approve-on-behalf')[0][1].body)).toEqual({
      reason: 'Đã gọi nhiều lần',
      basis: 'REPRESENTATIVE_UNREACHABLE',
    })
  })

  it('FR_017b_hanhChinhHuy_batBuocLyDo', async () => {
    const api = setup()
    api.on('POST', '/recipient-confirmations/61/cancel', json(200, {}))
    fireEvent.click(await screen.findByRole('button', { name: 'Hủy yêu cầu của Lê Văn Nam' }))
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận hủy' }))
    expect(await screen.findByText('Nhập lý do.')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Gia đình tự đến nhận' } })
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận hủy' }))
    await waitFor(() => expect(api.calls('POST', '/recipient-confirmations/61/cancel')).toHaveLength(1))
    expect(screen.queryByRole('button', { name: 'Duyệt thay cho Lê Văn Nam' })).not.toBeInTheDocument()
  })
})

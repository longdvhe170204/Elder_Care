import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { IntakeForm } from './IntakeForm'

const TYPES = [
  { id: 1, name: 'Quần áo', hasCashAmount: false, requiresConsentForUse: true, valuable: false, status: 'ACTIVE' },
  { id: 2, name: 'Điện thoại', hasCashAmount: false, requiresConsentForUse: true, valuable: true, status: 'ACTIVE' },
  { id: 3, name: 'Tiền mặt', hasCashAmount: true, requiresConsentForUse: false, valuable: true, status: 'ACTIVE' },
]
const LEVELS = [
  { id: 11, name: 'Nguyên vẹn', order: 1, unusable: false, status: 'ACTIVE' },
  { id: 14, name: 'Không dùng được', order: 4, unusable: true, status: 'ACTIVE' },
]
const LOCATIONS = [{ id: 21, name: 'Kho tầng 2', status: 'ACTIVE' }]

function renderForm(onDone = vi.fn()) {
  render(<IntakeForm residentId={7} types={TYPES} levels={LEVELS} locations={LOCATIONS} onDone={onDone} />)
  return onDone
}

function fill({ type, description = 'Áo len', amount = '5' }) {
  fireEvent.change(screen.getByLabelText('Loại đồ'), { target: { value: String(type) } })
  fireEvent.change(screen.getByLabelText('Mô tả'), { target: { value: description } })
  const amountLabel = TYPES.find((t) => t.id === type).hasCashAmount ? 'Số tiền (đồng)' : 'Số lượng'
  fireEvent.change(screen.getByLabelText(amountLabel), { target: { value: amount } })
  fireEvent.change(screen.getByLabelText('Mức tình trạng'), { target: { value: '11' } })
  fireEvent.change(screen.getByLabelText('Người giao'), { target: { value: 'OTHER_PERSON' } })
  fireEvent.change(screen.getByLabelText('Họ tên người giao'), { target: { value: 'Trần Văn Bình' } })
  fireEvent.change(screen.getByLabelText('Giấy tờ người giao'), { target: { value: 'CCCD 001' } })
  fireEvent.change(screen.getByLabelText('Vị trí lưu giữ'), { target: { value: '21' } })
}

const photo = () => new File(['x'], 'anh.jpg', { type: 'image/jpeg' })

describe('IntakeForm (013 T011, US1)', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('FR_006_doCoGiaTriThieuAnh_baoTaiO_khongGui', async () => {
    const api = mockRoutes()
    renderForm()
    fill({ type: 2, description: 'Điện thoại Nokia', amount: '1' })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu tiếp nhận' }))

    expect(await screen.findByText('Đồ có giá trị bắt buộc có ít nhất một ảnh.')).toBeInTheDocument()
    expect(api.calls('POST', '/residents/7/belongings')).toHaveLength(0)
  })

  it('FR_005_tienMat_guiSoTien_kemAnh_vaIdempotencyKey', async () => {
    const api = mockRoutes()
    api.on('POST', '/files', json(201, { fileId: 501 }))
    api.on('POST', '/residents/7/belongings', json(201, { id: 90, status: 'HELD' }))
    const onDone = renderForm()
    fill({ type: 3, description: 'Tiền mặt gửi giữ', amount: '2000000' })
    fireEvent.change(screen.getByLabelText('Ảnh đồ gửi'), { target: { files: [photo()] } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu tiếp nhận' }))

    await waitFor(() => expect(onDone).toHaveBeenCalled())
    const [, init] = api.calls('POST', '/residents/7/belongings')[0]
    const body = JSON.parse(init.body)
    expect(body).toMatchObject({
      typeId: 3,
      description: 'Tiền mặt gửi giữ',
      cashAmount: 2000000,
      condition: { levelId: 11 },
      giver: { type: 'OTHER_PERSON', fullName: 'Trần Văn Bình', idDocument: 'CCCD 001' },
      storageLocationId: 21,
      photoFileIds: [501],
    })
    expect(body.quantity).toBeUndefined()
    expect(body.at).toMatch(/\+07:00$/)
    expect(init.headers['Idempotency-Key']).toBeTruthy()
  })

  it('BR_M12_06_giaoNgayLoaiCanDongY_batBuocBanKy', async () => {
    const api = mockRoutes()
    api.on('POST', '/files', json(201, { fileId: 777 }))
    api.on('POST', '/residents/7/belongings', json(201, { id: 91, status: 'IN_USE' }))
    const onDone = renderForm()
    fill({ type: 1 })
    fireEvent.click(screen.getByRole('checkbox', { name: 'Giao ngay cho người cao tuổi sử dụng' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu tiếp nhận' }))
    expect(
      await screen.findByText('Loại đồ này cần bản ký đồng ý của người đại diện khi giao ngay.'),
    ).toBeInTheDocument()
    expect(api.calls('POST', '/residents/7/belongings')).toHaveLength(0)

    fireEvent.change(screen.getByLabelText('Bản ký đồng ý cho tự giữ'), {
      target: { files: [new File(['p'], 'ky.pdf', { type: 'application/pdf' })] },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu tiếp nhận' }))
    await waitFor(() => expect(onDone).toHaveBeenCalled())
    const body = JSON.parse(api.calls('POST', '/residents/7/belongings')[0][1].body)
    expect(body).toMatchObject({ quantity: 5, giveForUseNow: true, consentSignedFormFileId: 777 })
  })

  it('DBR_22_409_hienDieuKienChuaThoa_giuGiaTriDaNhap', async () => {
    const api = mockRoutes()
    api.on(
      'POST',
      '/residents/7/belongings',
      problem(409, 'RESIDENT_FINAL_STATE', { detail: 'Người cao tuổi đã ở trạng thái cuối, không tiếp nhận đồ mới.' }),
    )
    renderForm()
    fill({ type: 1 })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu tiếp nhận' }))

    expect(await screen.findByText('Người cao tuổi đã ở trạng thái cuối, không tiếp nhận đồ mới.')).toBeInTheDocument()
    expect(screen.getByLabelText('Mô tả')).toHaveValue('Áo len')
  })

  it('FR_003_nguoiKhacBatBuocGiayTo_viTriBatBuocKhiDangGiu_khongChonNhanVien', async () => {
    const api = mockRoutes()
    renderForm()
    const givers = Array.from(screen.getByLabelText('Người giao').options).map((o) => o.textContent)
    expect(givers).not.toContain('Nhân viên')
    fill({ type: 1 })
    fireEvent.change(screen.getByLabelText('Giấy tờ người giao'), { target: { value: '' } })
    fireEvent.change(screen.getByLabelText('Vị trí lưu giữ'), { target: { value: '' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu tiếp nhận' }))

    expect(await screen.findByText('Nhập giấy tờ của người giao.')).toBeInTheDocument()
    expect(screen.getByText('Chọn vị trí lưu giữ.')).toBeInTheDocument()
    expect(api.calls('POST', '/residents/7/belongings')).toHaveLength(0)
  })
})

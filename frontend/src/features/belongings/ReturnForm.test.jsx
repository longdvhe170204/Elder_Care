import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { ReturnForm } from './ReturnForm'

const LEVELS = [{ id: 11, name: 'Nguyên vẹn', order: 1, unusable: false, status: 'ACTIVE' }]
const ITEMS = [
  { id: 90, description: 'Điện thoại Nokia', quantity: 1, status: 'HELD', version: 2 },
  { id: 92, description: 'Tiền mặt', cashAmount: 2000000, status: 'HELD', version: 1 },
]

function renderForm(onDone = vi.fn()) {
  render(<ReturnForm belongings={ITEMS} levels={LEVELS} onDone={onDone} />)
  return onDone
}

function pick(description, level = '11') {
  fireEvent.click(screen.getByRole('checkbox', { name: `Trả ${description}` }))
  fireEvent.change(screen.getByLabelText(`Mức tình trạng lúc trả của ${description}`), { target: { value: level } })
}

const pdf = () => new File(['k'], 'chu-ky.pdf', { type: 'application/pdf' })

describe('ReturnForm (013 T019, US3)', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('BR_M12_04_traNhieuDoChoNguoiThan_kemGiayToVaChuKy_traMotPhanTien', async () => {
    const api = mockRoutes()
    api.on('POST', '/files', json(201, { fileId: 700 }))
    api.on('POST', '/belongings/returns', json(201, { handovers: [], belongings: [] }))
    const onDone = renderForm()
    pick('Điện thoại Nokia')
    pick('Tiền mặt')
    fireEvent.change(screen.getByLabelText('Số trả của Tiền mặt (trống là toàn bộ)'), { target: { value: '500000' } })
    fireEvent.change(screen.getByLabelText('Người nhận'), { target: { value: 'FAMILY_MEMBER' } })
    fireEvent.change(screen.getByLabelText('Mã người thân'), { target: { value: '33' } })
    fireEvent.change(screen.getByLabelText('Họ tên người nhận'), { target: { value: 'Trần Văn Bình' } })
    fireEvent.change(screen.getByLabelText('Loại giấy tờ'), { target: { value: 'CCCD' } })
    fireEvent.change(screen.getByLabelText('Số giấy tờ'), { target: { value: '001' } })
    fireEvent.change(screen.getByLabelText('Bản ký của người nhận'), { target: { files: [pdf()] } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu trả đồ' }))

    await waitFor(() => expect(onDone).toHaveBeenCalled())
    const [, init] = api.calls('POST', '/belongings/returns')[0]
    expect(init.headers['Idempotency-Key']).toBeTruthy()
    expect(JSON.parse(init.body)).toEqual({
      items: [
        { belongingId: 90, version: 2, condition: { levelId: 11 } },
        { belongingId: 92, version: 1, quantity: 500000, condition: { levelId: 11 } },
      ],
      recipient: {
        type: 'FAMILY_MEMBER',
        familyMemberId: 33,
        fullName: 'Trần Văn Bình',
        presentedIdDocument: { type: 'CCCD', number: '001' },
      },
      signatureFileId: 700,
    })
  })

  it('FR_008_traDoCoGiaTri_batBuocAnhTinhTrang_vaGuiPhotoFileIds', async () => {
    const api = mockRoutes()
    api.on('POST', '/files', json(201, { fileId: 710 }))
    api.on('POST', '/belongings/returns', json(201, { handovers: [], belongings: [] }))
    const onDone = vi.fn()
    render(<ReturnForm belongings={ITEMS} levels={LEVELS} isValuable={(b) => b.id === 92} onDone={onDone} />)
    pick('Tiền mặt')
    fireEvent.change(screen.getByLabelText('Người nhận'), { target: { value: 'RESIDENT' } })
    fireEvent.change(screen.getByLabelText('Lý do không ký'), { target: { value: 'Run tay' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu trả đồ' }))
    expect(await screen.findByText('Chọn ảnh tình trạng của đồ có giá trị.')).toBeInTheDocument()
    expect(api.calls('POST', '/belongings/returns')).toHaveLength(0)

    const png = new File(['x'], 'tinh-trang.png', { type: 'image/png' })
    fireEvent.change(screen.getByLabelText('Ảnh tình trạng của Tiền mặt'), { target: { files: [png] } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu trả đồ' }))
    await waitFor(() => expect(onDone).toHaveBeenCalled())
    const body = JSON.parse(api.calls('POST', '/belongings/returns')[0][1].body)
    expect(body.items).toEqual([{ belongingId: 92, version: 1, condition: { levelId: 11 }, photoFileIds: [710] }])
  })

  it('FR_019_nguoiCaoTuoiKhongKy_batBuocLyDo_khongGuiChuKy', async () => {
    const api = mockRoutes()
    api.on('POST', '/belongings/returns', json(201, { handovers: [], belongings: [] }))
    const onDone = renderForm()
    pick('Điện thoại Nokia')
    fireEvent.change(screen.getByLabelText('Người nhận'), { target: { value: 'RESIDENT' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu trả đồ' }))
    expect(await screen.findByText('Chọn bản ký hoặc ghi lý do người cao tuổi không ký.')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Lý do không ký'), { target: { value: 'Run tay, không cầm bút được' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu trả đồ' }))
    await waitFor(() => expect(onDone).toHaveBeenCalled())
    const body = JSON.parse(api.calls('POST', '/belongings/returns')[0][1].body)
    expect(body.recipient).toEqual({ type: 'RESIDENT' })
    expect(body.noSignatureReason).toBe('Run tay, không cầm bút được')
    expect(body.signatureFileId).toBeUndefined()
  })

  it('FR_016_biChan_hienLyDo_vaGhiLanChan', async () => {
    const api = mockRoutes()
    api.on('POST', '/files', json(201, { fileId: 701 }))
    api.on(
      'POST',
      '/belongings/returns',
      problem(409, 'RETURN_BLOCKED', { detail: 'Người nhận không có quyền nhận đồ.' }),
    )
    const onDone = renderForm()
    pick('Điện thoại Nokia')
    fireEvent.change(screen.getByLabelText('Người nhận'), { target: { value: 'OTHER_PERSON' } })
    fireEvent.change(screen.getByLabelText('Họ tên người nhận'), { target: { value: 'Lê Văn Nam' } })
    fireEvent.change(screen.getByLabelText('Bản ký của người nhận'), { target: { files: [pdf()] } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu trả đồ' }))

    expect(await screen.findByText('Người nhận không có quyền nhận đồ.')).toBeInTheDocument()
    expect(screen.getByText('Lần chặn trả đã được ghi vào lịch sử của đồ.')).toBeInTheDocument()
    expect(onDone).toHaveBeenCalledWith(null)
  })
})

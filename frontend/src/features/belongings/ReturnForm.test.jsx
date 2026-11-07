import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { ReturnForm } from './ReturnForm'

const LEVELS = [{ id: 11, name: 'Nguyên vẹn', order: 1, unusable: false, status: 'ACTIVE' }]
const ITEMS = [
  { id: 90, description: 'Điện thoại Nokia', quantity: 1, status: 'HELD', version: 2 },
  { id: 92, description: 'Tiền mặt', cashAmount: 2000000, status: 'HELD', version: 1 },
]

function renderForm(onDone = vi.fn()) {
  render(<ReturnForm residentId={7} role="OFFICE_STAFF" belongings={ITEMS} levels={LEVELS} onDone={onDone} />)
  return onDone
}

function pick(description, level = '11') {
  const id = ITEMS.find((i) => i.description === description).id
  fireEvent.click(screen.getByRole('checkbox', { name: `Trả ${description}, mã ${id}` }))
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
    api.on('GET', '/recipient-confirmations', json(200, []))
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

// Chọn hoặc lập xác nhận người nhận khác ngay trong màn Trả đồ (FR-016a, FR-017, FR-018, FR-019, Q-253; BF-14)
describe('ReturnForm: xác nhận người nhận khác ngay trong màn trả', () => {
  afterEach(() => vi.unstubAllGlobals())

  const SWEATERS = [
    { id: 18, description: 'Áo len', quantity: 3, status: 'HELD', version: 1 },
    { id: 19, description: 'Áo len', quantity: 2, status: 'HELD', version: 1 },
    { id: 92, description: 'Tiền mặt', cashAmount: 2000000, status: 'HELD', version: 1 },
  ]
  const FUTURE = '2099-01-01T00:00:00Z'
  const conf = (o = {}) => ({
    id: 8,
    residentId: 7,
    recipientName: 'Lê Văn Nam',
    recipientIdDocument: 'CCCD 079000000102',
    recipientIsResident: false,
    belongingIds: [18],
    status: 'EFFECTIVE',
    method: 'SIGNED_FORM',
    expiresAt: FUTURE,
    ...o,
  })

  // list: mảng hoặc hàm trả mảng (đổi sau khi lập); POST /files trả mã tăng dần để phân biệt hai tệp
  function setupForm({ list = [], role = 'OFFICE_STAFF', onCreated = vi.fn() } = {}) {
    const api = mockRoutes()
    api.on('GET', '/recipient-confirmations', () => json(200, typeof list === 'function' ? list() : list))
    let fileId = 700
    api.on('POST', '/files', () => json(201, { fileId: fileId++ }))
    api.on('POST', '/belongings/returns', json(201, { handovers: [], belongings: [] }))
    const onDone = vi.fn()
    render(
      <ReturnForm
        residentId={7}
        residentName="Nguyễn Thị Lan"
        role={role}
        belongings={SWEATERS}
        levels={LEVELS}
        isValuable={(b) => b.id === 92}
        requiresConsent={(b) => b.id === 92}
        onConfirmationCreated={onCreated}
        onDone={onDone}
      />,
    )
    return { api, onDone, onCreated }
  }

  function pickId(id, description) {
    fireEvent.click(screen.getByRole('checkbox', { name: `Trả ${description}, mã ${id}` }))
    fireEvent.change(screen.getAllByLabelText(`Mức tình trạng lúc trả của ${description}`).at(-1), {
      target: { value: '11' },
    })
  }

  function otherPerson({ name = 'Lê Văn Nam', type = 'CCCD', number = '079000000102' } = {}) {
    fireEvent.change(screen.getByLabelText('Người nhận'), { target: { value: 'OTHER_PERSON' } })
    fireEvent.change(screen.getByLabelText('Họ tên người nhận'), { target: { value: name } })
    fireEvent.change(screen.getByLabelText('Loại giấy tờ'), { target: { value: type } })
    fireEvent.change(screen.getByLabelText('Số giấy tờ'), { target: { value: number } })
  }

  const signRecipient = () =>
    fireEvent.change(screen.getByLabelText('Bản ký của người nhận'), { target: { files: [pdf()] } })
  const returnBodies = (api) => api.calls('POST', '/belongings/returns').map(([, init]) => JSON.parse(init.body))

  it('FR_017_nguoiKhac_chonXacNhanKhopTuDanhSach_guiDungMa_khongGoMa', async () => {
    const { api, onDone } = setupForm({ list: [conf(), conf({ id: 9, recipientIdDocument: 'CCCD 111' })] })
    pickId(18, 'Áo len')
    otherPerson()
    fireEvent.click(await screen.findByRole('button', { name: 'Chọn xác nhận mã 8' }))
    expect(screen.queryByRole('button', { name: 'Chọn xác nhận mã 9' })).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Mã xác nhận người nhận khác')).not.toBeInTheDocument()
    expect(api.calls('GET', '/recipient-confirmations')[0][0]).toContain('status=EFFECTIVE%2CPENDING_CONFIRMATION')
    signRecipient()
    fireEvent.click(screen.getByRole('button', { name: 'Lưu trả đồ' }))

    await waitFor(() => expect(onDone).toHaveBeenCalledWith('Đã trả đồ.'))
    expect(returnBodies(api)[0]).toMatchObject({
      items: [{ belongingId: 18, version: 1, condition: { levelId: 11 } }],
      recipient: { type: 'OTHER_PERSON', fullName: 'Lê Văn Nam', presentedIdDocument: { type: 'CCCD', number: '079000000102' } },
      recipientConfirmationId: 8,
    })
  })

  it('FR_017_haiDoCungTen_chiXacNhanGomDungMaDoMoiHien', async () => {
    const { api } = setupForm({ list: [conf()] })
    pickId(19, 'Áo len')
    otherPerson()
    await waitFor(() => expect(api.calls('GET', '/recipient-confirmations')).toHaveLength(1))
    expect(await screen.findByText(/Chưa có xác nhận phù hợp/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Chọn xác nhận mã 8' })).not.toBeInTheDocument()
  })

  it('FR_017_traNhieuDo_xacNhanChiGomMotDo_khongKhop', async () => {
    setupForm({ list: [conf()] })
    pickId(18, 'Áo len')
    pickId(19, 'Áo len')
    otherPerson()
    expect(await screen.findByText(/Chưa có xác nhận phù hợp/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Chọn xác nhận mã 8' })).not.toBeInTheDocument()
  })

  it('FR_017_xacNhanGomNhieuDoHonSoDoDangTra_hienCanhBao', async () => {
    setupForm({ list: [conf({ belongingIds: [18, 19] })] })
    pickId(18, 'Áo len')
    otherPerson()
    fireEvent.click(await screen.findByRole('button', { name: 'Chọn xác nhận mã 8' }))
    expect(screen.getByText(/xác nhận chuyển Đã dùng/)).toHaveTextContent('mã 19')
  })

  it('FR_017_doiGiayToSauKhiChon_boChon_vanChoLuuDeBackendChanVaGhiLanChan', async () => {
    const { api } = setupForm({ list: [conf()] })
    pickId(18, 'Áo len')
    otherPerson()
    fireEvent.click(await screen.findByRole('button', { name: 'Chọn xác nhận mã 8' }))
    fireEvent.change(screen.getByLabelText('Số giấy tờ'), { target: { value: '999' } })
    expect(screen.queryByRole('button', { name: 'Chọn xác nhận mã 8' })).not.toBeInTheDocument()
    expect(screen.getByText(/Chưa chọn xác nhận người nhận khác phù hợp/)).toBeInTheDocument()
    signRecipient()
    fireEvent.click(screen.getByRole('button', { name: 'Lưu trả đồ' }))
    await waitFor(() => expect(api.calls('POST', '/belongings/returns')).toHaveLength(1))
    expect(returnBodies(api)[0].recipientConfirmationId).toBeUndefined()
  })

  it('FR_017_xacNhanQuaHan_khongHienDuTrangThaiHieuLuc', async () => {
    setupForm({ list: [conf({ expiresAt: '2020-01-01T00:00:00Z' })] })
    pickId(18, 'Áo len')
    otherPerson()
    expect(await screen.findByText(/Chưa có xác nhận phù hợp/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Chọn xác nhận mã 8' })).not.toBeInTheDocument()
  })

  it('FR_017_FR_019_lapNgayCoBanKy_dungLaiNguoiNhanVaDo_tuChonMaMoi_haiTepRieng', async () => {
    let created = false
    const { api, onCreated } = setupForm({ list: () => (created ? [conf({ id: 10 })] : []) })
    api.on('POST', '/recipient-confirmations', () => {
      created = true
      return json(201, { id: 10 })
    })
    pickId(18, 'Áo len')
    otherPerson()
    fireEvent.click(await screen.findByRole('button', { name: 'Lập xác nhận ngay' }))
    expect(screen.getByLabelText('Họ tên người nhận khác')).toHaveValue('Lê Văn Nam')
    expect(screen.getByLabelText('Giấy tờ của người nhận')).toHaveValue('CCCD 079000000102')
    fireEvent.change(screen.getByLabelText('Bản ký của người đại diện (không bắt buộc)'), {
      target: { files: [pdf()] },
    })
    fireEvent.change(screen.getByLabelText('Mã người đại diện đã ký'), { target: { value: '1' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lập xác nhận' }))

    expect(await screen.findByRole('button', { name: 'Chọn xác nhận mã 10' })).toHaveAttribute('aria-pressed', 'true')
    expect(JSON.parse(api.calls('POST', '/recipient-confirmations')[0][1].body)).toEqual({
      residentId: 7,
      recipientName: 'Lê Văn Nam',
      recipientIdDocument: 'CCCD 079000000102',
      recipientIsResident: false,
      belongingIds: [18],
      signedForm: { representativeId: 1, fileId: 700 },
    })
    expect(onCreated).toHaveBeenCalled()
    expect(api.calls('POST', '/belongings/returns')).toHaveLength(0)

    signRecipient()
    fireEvent.click(screen.getByRole('button', { name: 'Lưu trả đồ' }))
    await waitFor(() => expect(api.calls('POST', '/belongings/returns')).toHaveLength(1))
    expect(returnBodies(api)[0]).toMatchObject({ recipientConfirmationId: 10, signatureFileId: 701 })
  })

  it('FR_017_lapNgayKhongBanKy_choXacNhan_khongChonDuocDeTra', async () => {
    let created = false
    const pending = conf({ id: 11, status: 'PENDING_CONFIRMATION', method: 'PORTAL', expiresAt: null })
    const { api } = setupForm({ list: () => (created ? [pending] : []) })
    api.on('POST', '/recipient-confirmations', () => {
      created = true
      return json(201, { id: 11 })
    })
    pickId(18, 'Áo len')
    otherPerson()
    fireEvent.click(await screen.findByRole('button', { name: 'Lập xác nhận ngay' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lập xác nhận' }))

    expect(await screen.findByText(/Đang chờ xác nhận, chưa thể trả đồ/)).toHaveTextContent('mã 11')
    expect(screen.queryByRole('button', { name: 'Chọn xác nhận mã 11' })).not.toBeInTheDocument()
  })

  it('FR_017_lapXongTraLoi_thuLaiDungMaCu_khongLapLan2', async () => {
    let created = false
    const { api } = setupForm({ list: () => (created ? [conf({ id: 10 })] : []) })
    api.on('POST', '/recipient-confirmations', () => {
      created = true
      return json(201, { id: 10 })
    })
    let attempt = 0
    api.on('POST', '/belongings/returns', () =>
      attempt++ === 0
        ? problem(422, 'INVALID_VALUE', { detail: 'Thiếu thông tin người nhận.' })
        : json(201, { handovers: [], belongings: [] }),
    )
    pickId(18, 'Áo len')
    otherPerson()
    fireEvent.click(await screen.findByRole('button', { name: 'Lập xác nhận ngay' }))
    fireEvent.change(screen.getByLabelText('Bản ký của người đại diện (không bắt buộc)'), {
      target: { files: [pdf()] },
    })
    fireEvent.change(screen.getByLabelText('Mã người đại diện đã ký'), { target: { value: '1' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lập xác nhận' }))
    await screen.findByRole('button', { name: 'Chọn xác nhận mã 10' })
    signRecipient()
    fireEvent.click(screen.getByRole('button', { name: 'Lưu trả đồ' }))
    expect(await screen.findByText('Thiếu thông tin người nhận.')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Lưu trả đồ' }))

    await waitFor(() => expect(api.calls('POST', '/belongings/returns')).toHaveLength(2))
    expect(api.calls('POST', '/recipient-confirmations')).toHaveLength(1)
    expect(returnBodies(api).map((b) => b.recipientConfirmationId)).toEqual([10, 10])
  })

  it('Q_253_nguoiCaoTuoiNhanTienMat_chonXacNhanCuaNguoiCaoTuoi_guiMa', async () => {
    const lan = conf({ id: 12, recipientIsResident: true, recipientName: 'Nguyễn Thị Lan', recipientIdDocument: 'CCCD 001', belongingIds: [92] })
    const { api, onDone } = setupForm({ list: [conf(), lan] })
    pickId(92, 'Tiền mặt')
    fireEvent.change(screen.getByLabelText('Ảnh tình trạng của Tiền mặt'), {
      target: { files: [new File(['x'], 'tien.png', { type: 'image/png' })] },
    })
    fireEvent.change(screen.getByLabelText('Người nhận'), { target: { value: 'RESIDENT' } })
    fireEvent.click(await screen.findByRole('button', { name: 'Chọn xác nhận mã 12' }))
    expect(screen.queryByRole('button', { name: 'Chọn xác nhận mã 8' })).not.toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Lý do không ký'), { target: { value: 'Run tay' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu trả đồ' }))

    await waitFor(() => expect(onDone).toHaveBeenCalledWith('Đã trả đồ.'))
    expect(returnBodies(api)[0]).toMatchObject({ recipient: { type: 'RESIDENT' }, recipientConfirmationId: 12 })
  })

  it('Q_253_lapNgayChoChinhNguoiCaoTuoi_dienSanTen_nhapGiayTo', async () => {
    const { api } = setupForm({ list: [] })
    api.on('POST', '/recipient-confirmations', json(201, { id: 13 }))
    pickId(92, 'Tiền mặt')
    fireEvent.change(screen.getByLabelText('Người nhận'), { target: { value: 'RESIDENT' } })
    fireEvent.click(await screen.findByRole('button', { name: 'Lập xác nhận ngay' }))
    expect(screen.getByLabelText('Họ tên người nhận khác')).toHaveValue('Nguyễn Thị Lan')
    fireEvent.change(screen.getByLabelText('Giấy tờ của người nhận'), { target: { value: 'CCCD 001' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lập xác nhận' }))

    await waitFor(() => expect(api.calls('POST', '/recipient-confirmations')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/recipient-confirmations')[0][1].body)).toEqual({
      residentId: 7,
      recipientName: 'Nguyễn Thị Lan',
      recipientIdDocument: 'CCCD 001',
      recipientIsResident: true,
      belongingIds: [92],
    })
  })

  it('Q_253_nguoiCaoTuoiNhanDoThuong_khongHienKhoiXacNhan', async () => {
    const { api } = setupForm({ list: [conf()] })
    pickId(18, 'Áo len')
    fireEvent.change(screen.getByLabelText('Người nhận'), { target: { value: 'RESIDENT' } })
    expect(screen.queryByRole('region', { name: 'Chọn xác nhận người nhận khác' })).not.toBeInTheDocument()
    expect(api.calls('GET', '/recipient-confirmations')).toHaveLength(0)
  })

  it('FR_016a_dieuDuong_khongCoNguoiKhac_khongLapXacNhan_nhacChiHanhChinhTra', async () => {
    const { api } = setupForm({ list: [conf()], role: 'NURSING_STAFF' })
    expect(within(screen.getByLabelText('Người nhận')).queryByRole('option', { name: /Người khác/ })).toBeNull()
    pickId(92, 'Tiền mặt')
    fireEvent.change(screen.getByLabelText('Người nhận'), { target: { value: 'RESIDENT' } })
    expect(screen.getByText(/chỉ Hành chính trả/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Lập xác nhận ngay' })).not.toBeInTheDocument()
    expect(api.calls('GET', '/recipient-confirmations')).toHaveLength(0)
  })
})

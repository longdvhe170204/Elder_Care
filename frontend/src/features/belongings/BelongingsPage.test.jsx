import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { BelongingsPage } from './BelongingsPage'

const TYPES = [
  { id: 1, name: 'Quần áo', hasCashAmount: false, requiresConsentForUse: true, valuable: false, status: 'ACTIVE' },
  { id: 2, name: 'Điện thoại', hasCashAmount: false, requiresConsentForUse: true, valuable: true, status: 'ACTIVE' },
]
const PHONE = {
  id: 90,
  residentId: 7,
  typeId: 2,
  description: 'Điện thoại Nokia',
  quantity: 1,
  status: 'HELD',
  holder: 'STAFF',
  storageLocationId: 21,
  lastHandoverAt: '2026-11-06T02:00:00Z',
  version: 1,
}

const LAN = { id: 7, profileCode: 'NCT-0007', personalInfo: { fullName: 'Nguyễn Thị Lan' } }

function setup(permissions = ['BELONGINGS:VIEW', 'BELONGINGS:CREATE', 'BELONGINGS:UPDATE'], residents = [LAN]) {
  const api = mockRoutes()
  api.on(
    'GET',
    '/me/permissions',
    json(
      200,
      permissions.map((k) => ({ function: k.split(':')[0], operation: k.split(':')[1] })),
    ),
  )
  api.on('GET', '/belongings/types', json(200, TYPES))
  api.on(
    'GET',
    '/belongings/condition-levels',
    json(200, [{ id: 11, name: 'Nguyên vẹn', order: 1, unusable: false, status: 'ACTIVE' }]),
  )
  api.on('GET', '/belongings/storage-locations', json(200, [{ id: 21, name: 'Kho tầng 2', status: 'ACTIVE' }]))
  api.on(
    'GET',
    '/residents',
    json(200, {
      items: residents,
      nextCursor: null,
    }),
  )
  api.on('GET', '/residents/7/belongings', json(200, [PHONE]))
  api.on(
    'GET',
    '/belongings/90',
    json(200, {
      belonging: PHONE,
      handovers: [
        {
          id: 300,
          belongingId: 90,
          kind: 'INTAKE',
          targetStatus: 'HELD',
          giverType: 'FAMILY_MEMBER',
          giverName: 'Trần Văn Bình',
          recipientType: 'STAFF',
          recipientName: 'Trần Thu Hà',
          occurredAt: '2026-11-06T02:00:00Z',
          quantity: 1,
          conditionDescription: 'Nguyên vẹn',
          photoFileIds: [501],
        },
      ],
    }),
  )
  render(
    <PermissionsProvider>
      <BelongingsPage role="OFFICE_STAFF" />
    </PermissionsProvider>,
  )
  return api
}

async function openResident() {
  fireEvent.change(screen.getByLabelText('Tìm người cao tuổi'), { target: { value: 'Lan' } })
  fireEvent.click(screen.getByRole('button', { name: 'Tìm' }))
  fireEvent.click(await screen.findByRole('button', { name: 'Nguyễn Thị Lan, NCT-0007' }))
}

describe('BelongingsPage (013 T011, US1)', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('FR_011_chonNguoiCaoTuoi_danhSachConGiu_chonNguoiKhacThiDoiHoSoVaDongKhungChiTiet', async () => {
    const minh = { id: 8, profileCode: 'NCT-0008', personalInfo: { fullName: 'Lê Văn Minh' } }
    const api = setup(undefined, [LAN, minh])
    api.on('GET', '/residents/8/belongings', json(200, []))
    fireEvent.click(await screen.findByRole('button', { name: 'Nguyễn Thị Lan, NCT-0007' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Chi tiết Điện thoại Nokia' }))
    expect(await screen.findByText('Trần Thu Hà', { exact: false })).toBeInTheDocument()

    expect(screen.getByRole('button', { name: 'Lê Văn Minh, NCT-0008', hidden: true })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Nguyễn Thị Lan, NCT-0007', hidden: true })).toHaveAttribute(
      'aria-pressed',
      'true',
    )
    fireEvent.click(screen.getByRole('button', { name: 'Lê Văn Minh, NCT-0008', hidden: true }))

    expect(await screen.findByRole('heading', { name: 'Đồ gửi của Lê Văn Minh' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Chi tiết Điện thoại Nokia' })).not.toBeInTheDocument()
    expect(screen.queryByText('Trần Thu Hà', { exact: false })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Lê Văn Minh, NCT-0008', hidden: true })).toHaveAttribute(
      'aria-pressed',
      'true',
    )
  })

  it('FR_011_danhSachDoGui_loaiTrangThaiNguoiGiuViTri', async () => {
    setup()
    await openResident()
    const row = (await screen.findByText('Điện thoại Nokia')).closest('tr')
    expect(within(row).getByText('Điện thoại')).toBeInTheDocument()
    expect(within(row).getByText('Đang giữ')).toBeInTheDocument()
    expect(within(row).getByText('Nhân viên')).toBeInTheDocument()
    expect(within(row).getByText('Kho tầng 2')).toBeInTheDocument()
  })

  it('FR_012_chiTietDoGui_lichSuBanGiao', async () => {
    setup()
    await openResident()
    fireEvent.click(await screen.findByRole('button', { name: 'Chi tiết Điện thoại Nokia' }))
    expect(await screen.findByText('Tiếp nhận')).toBeInTheDocument()
    expect(screen.getByText(/Trần Văn Bình → Trần Thu Hà/)).toBeInTheDocument()
  })

  it('FR_011_lichSuTiepNhan_benNhanLaNhanVienKhongCoTen_hienVaiTro', async () => {
    const api = setup()
    api.on(
      'GET',
      '/belongings/90',
      json(200, {
        belonging: PHONE,
        handovers: [
          {
            id: 300,
            belongingId: 90,
            kind: 'INTAKE',
            targetStatus: 'HELD',
            giverType: 'FAMILY_MEMBER',
            giverName: 'Trần Thu Hà',
            recipientType: 'STAFF',
            occurredAt: '2026-11-06T02:00:00Z',
            quantity: 1,
            photoFileIds: [],
          },
        ],
      }),
    )
    await openResident()
    fireEvent.click(await screen.findByRole('button', { name: 'Chi tiết Điện thoại Nokia' }))
    expect(await screen.findByText(/Trần Thu Hà → Nhân viên/)).toBeInTheDocument()
  })

  it('FR_029_khongCoQuyenTao_khongCoNutTiepNhan', async () => {
    setup(['BELONGINGS:VIEW'])
    await openResident()
    await screen.findByText('Điện thoại Nokia')
    expect(screen.queryByRole('button', { name: 'Tiếp nhận đồ' })).not.toBeInTheDocument()
  })

  it('FR_015a_ghiDongYTheoBanKy_guiMaNguoiDaiDienVaTep', async () => {
    const api = setup()
    api.on('POST', '/files', json(201, { fileId: 640 }))
    api.on('POST', '/self-custody-consents', json(201, { id: 55 }))
    await openResident()
    fireEvent.click(await screen.findByRole('button', { name: 'Chi tiết Điện thoại Nokia' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Ghi đồng ý cho tự giữ' }))
    fireEvent.change(screen.getByLabelText('Mã người đại diện'), { target: { value: '33' } })
    fireEvent.change(screen.getByLabelText('Bản ký của người đại diện'), {
      target: { files: [new File(['p'], 'dong-y.pdf', { type: 'application/pdf' })] },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu đồng ý' }))

    await waitFor(() => expect(api.calls('POST', '/self-custody-consents')).toHaveLength(1))
    const [, init] = api.calls('POST', '/self-custody-consents')[0]
    expect(JSON.parse(init.body)).toEqual({ belongingId: 90, representativeId: 33, signedFormFileId: 640 })
    expect(await screen.findByText('Đã ghi đồng ý cho tự giữ.')).toBeInTheDocument()
  })

  it('FR_015a_rutDongY_dongYDangHieuLuc_guiLyDoNguoiDaiDienVaTep', async () => {
    const api = setup()
    api.on(
      'GET',
      '/belongings/90',
      json(200, {
        belonging: PHONE,
        handovers: [],
        selfCustodyConsent: { id: 55, status: 'EFFECTIVE', method: 'SIGNED_FORM', representativeId: 33 },
      }),
    )
    api.on('POST', '/files', json(201, { fileId: 641 }))
    api.on('POST', '/self-custody-consents/55/withdraw', json(200, {}))
    await openResident()
    fireEvent.click(await screen.findByRole('button', { name: 'Chi tiết Điện thoại Nokia' }))
    expect(await screen.findByText(/Có hiệu lực, người đại diện #33/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Ghi đồng ý cho tự giữ' })).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Rút đồng ý' }))
    fireEvent.change(screen.getByLabelText('Lý do rút đồng ý'), { target: { value: 'Người nhà đổi ý' } })
    fireEvent.change(screen.getByLabelText('Bản ký của người đại diện'), {
      target: { files: [new File(['p'], 'rut.pdf', { type: 'application/pdf' })] },
    })
    fireEvent.click(screen.getAllByRole('button', { name: 'Rút đồng ý' }).at(-1))

    await waitFor(() => expect(api.calls('POST', '/self-custody-consents/55/withdraw')).toHaveLength(1))
    const [, init] = api.calls('POST', '/self-custody-consents/55/withdraw')[0]
    expect(JSON.parse(init.body)).toEqual({ reason: 'Người nhà đổi ý', representativeId: 33, signedFormFileId: 641 })
    expect(await screen.findByText('Đã rút đồng ý cho tự giữ.')).toBeInTheDocument()
  })

  it('FR_024_lienKetSuCoGhiSau_motLan', async () => {
    const api = setup()
    api.on(
      'GET',
      '/belongings/91',
      json(200, {
        belonging: { ...PHONE, id: 91, status: 'LOST', holder: null },
        handovers: [
          {
            id: 301,
            belongingId: 91,
            kind: 'REPORT_LOST',
            targetStatus: 'LOST',
            occurredAt: '2026-11-06T03:00:00Z',
            reason: 'Không thấy',
            incidentId: null,
          },
        ],
      }),
    )
    api.on(
      'GET',
      '/residents/7/belongings',
      json(200, [{ ...PHONE, id: 91, description: 'Kính lão', status: 'LOST', holder: null }]),
    )
    api.on('POST', '/belongings/handovers/301/link-incident', json(200, {}))
    await openResident()
    fireEvent.click(await screen.findByRole('button', { name: 'Chi tiết Kính lão' }))
    fireEvent.change(await screen.findByLabelText('Mã sự cố cho báo thất lạc'), { target: { value: '44' } })
    fireEvent.click(screen.getByRole('button', { name: 'Liên kết sự cố' }))

    await waitFor(() => expect(api.calls('POST', '/belongings/handovers/301/link-incident')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/belongings/handovers/301/link-incident')[0][1].body)).toEqual({
      incidentId: 44,
    })
    expect(await screen.findByText('Đã liên kết sự cố.')).toBeInTheDocument()
  })

  it('FR_011_tabNhanVienDangGiu', async () => {
    setup()
    fireEvent.click(
      await screen
        .findByRole('radio', { name: 'Nhân viên đang giữ' })
        .catch(() => screen.getByText('Nhân viên đang giữ')),
    )
    expect(await screen.findByLabelText('Mã nhân viên')).toBeInTheDocument()
  })

  it('FR_011_moManHienNgayDanhSachNguoiCaoTuoi_khongCanTim', async () => {
    setup()
    fireEvent.click(await screen.findByRole('button', { name: 'Nguyễn Thị Lan, NCT-0007' }))
    expect(await screen.findByText('Điện thoại Nokia')).toBeInTheDocument()
  })
})

// Ca 4 kiểm lại (D6): trả bằng mã xác nhận người nhận khác thì mục xác nhận trên cùng hồ sơ đổi ngay, không cần F5.
// Khối chọn trong form trả gọi GET /recipient-confirmations kèm status; mục ngoài màn gọi không kèm status.
describe('BelongingsPage: trả đồ bằng xác nhận người nhận khác (013 FR-017)', () => {
  afterEach(() => vi.unstubAllGlobals())

  const SWEATER = {
    id: 18,
    residentId: 7,
    typeId: 1,
    description: 'Áo len A',
    quantity: 3,
    status: 'HELD',
    holder: 'STAFF',
    storageLocationId: 21,
    lastHandoverAt: '2026-11-06T02:00:00Z',
    version: 1,
  }
  const confirmation = (id, status) => ({
    id,
    residentId: 7,
    recipientName: 'Lê Văn Nam',
    recipientIdDocument: 'CCCD 079000000102',
    recipientIsResident: false,
    belongingIds: [18],
    status,
    expiresAt: '2099-01-01T00:00:00Z',
  })
  const outerCalls = (api) => api.calls('GET', '/recipient-confirmations').filter(([url]) => !url.includes('status='))

  function setupReturn(returnResponse, initial = [confirmation(7, 'EFFECTIVE')]) {
    const api = setup()
    const state = { items: initial, returned: false }
    api.on('GET', '/residents/7/belongings', () =>
      json(200, [state.returned ? { ...SWEATER, status: 'RETURNED', version: 2 } : SWEATER]),
    )
    api.on('GET', '/recipient-confirmations', (url) => {
      const statuses = url.searchParams.get('status')?.split(',')
      return json(200, statuses ? state.items.filter((c) => statuses.includes(c.status)) : state.items)
    })
    api.on('POST', '/files', json(201, { fileId: 700 }))
    api.on('POST', '/belongings/returns', () => {
      const response = typeof returnResponse === 'function' ? returnResponse() : returnResponse.clone()
      if (response.ok) {
        state.returned = true
        state.items = state.items.map((c) => ({ ...c, status: 'USED' }))
      }
      return response
    })
    return { api, state }
  }

  async function openReturn() {
    fireEvent.click(await screen.findByRole('button', { name: 'Nguyễn Thị Lan, NCT-0007' }))
    const section = await screen.findByRole('region', { name: 'Xác nhận người nhận khác' })
    fireEvent.click(screen.getByRole('button', { name: 'Trả đồ' }))
    fireEvent.click(await screen.findByRole('checkbox', { name: 'Trả Áo len A, mã 18' }))
    fireEvent.change(screen.getByLabelText('Mức tình trạng lúc trả của Áo len A'), { target: { value: '11' } })
    fireEvent.change(screen.getByLabelText('Người nhận'), { target: { value: 'OTHER_PERSON' } })
    fireEvent.change(screen.getByLabelText('Họ tên người nhận'), { target: { value: 'Lê Văn Nam' } })
    fireEvent.change(screen.getByLabelText('Loại giấy tờ'), { target: { value: 'CCCD' } })
    fireEvent.change(screen.getByLabelText('Số giấy tờ'), { target: { value: '079000000102' } })
    return section
  }

  function signAndSave() {
    fireEvent.change(screen.getByLabelText('Bản ký của người nhận'), {
      target: { files: [new File(['k'], 'chu-ky.pdf', { type: 'application/pdf' })] },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu trả đồ' }))
  }

  it('FR_017_traThanhCong_xacNhanChuyenDaDungNgayTrenHoSoDangMo', async () => {
    const { api } = setupReturn(json(201, { handovers: [], belongings: [] }))
    const section = await openReturn()
    expect(await within(section).findByText('Hiệu lực')).toBeInTheDocument()
    fireEvent.click(await screen.findByRole('button', { name: 'Chọn xác nhận mã 7' }))
    signAndSave()

    expect(await screen.findByText('Đã trả đồ.')).toBeInTheDocument()
    expect(await within(section).findByText('Đã dùng')).toBeInTheDocument()
    expect(within(section).queryByText('Hiệu lực')).not.toBeInTheDocument()
    const row = screen.getByText('Áo len A').closest('tr')
    expect(within(row).getByText('Đã trả')).toBeInTheDocument()
    expect(outerCalls(api)).toHaveLength(2)
  })

  it('FR_017_traBiChan_khongTaiLaiXacNhan_vanHieuLuc', async () => {
    const { api } = setupReturn(
      problem(409, 'INVALID_RECIPIENT_CONFIRMATION', { detail: 'Xác nhận người nhận khác không hợp lệ (FR-017)' }),
    )
    const section = await openReturn()
    fireEvent.click(await screen.findByRole('button', { name: 'Chọn xác nhận mã 7' }))
    signAndSave()

    await waitFor(() => expect(api.calls('GET', '/residents/7/belongings')).toHaveLength(2))
    expect(outerCalls(api)).toHaveLength(1)
    expect(within(section).getByText('Hiệu lực')).toBeInTheDocument()
  })

  it('FR_017_lapNgayTrongFormTra_mucXacNhanNgoaiManCoNgayXacNhanMoi', async () => {
    const { api, state } = setupReturn(json(201, { handovers: [], belongings: [] }), [])
    api.on('POST', '/recipient-confirmations', () => {
      state.items = [confirmation(9, 'EFFECTIVE')]
      return json(201, { id: 9 })
    })
    const section = await openReturn()
    fireEvent.click(await screen.findByRole('button', { name: 'Lập xác nhận ngay' }))
    fireEvent.change(screen.getByLabelText('Bản ký của người đại diện (không bắt buộc)'), {
      target: { files: [new File(['k'], 'dong-y.pdf', { type: 'application/pdf' })] },
    })
    fireEvent.change(screen.getByLabelText('Mã người đại diện đã ký'), { target: { value: '1' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lập xác nhận' }))

    expect(await screen.findByRole('button', { name: 'Chọn xác nhận mã 9' })).toHaveAttribute('aria-pressed', 'true')
    expect(await within(section).findByText(/Mã 9/)).toBeInTheDocument()
    expect(within(screen.getByText('Áo len A').closest('tr')).getByText('Đang giữ')).toBeInTheDocument()
  })
})

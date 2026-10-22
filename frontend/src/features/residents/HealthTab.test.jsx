import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { HealthTab } from './HealthTab'

// 001 T033 (US2; FR-017 → FR-022; DBR-04): tab Sức khỏe trong khung chi tiết hồ sơ. Hồ sơ sức khỏe ban đầu có
// bản nháp, chỉ Bác sĩ xác nhận, sau đó chỉ đính chính; mục sức khỏe chỉ ghi nhận và loại trừ, không sửa, không xóa.

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

const ALL_COMMANDS = [
  'RECORD_HEALTH_ITEM',
  'EXCLUDE_HEALTH_ITEM',
  'WRITE_INITIAL_HEALTH_RECORD',
  'CONFIRM_INITIAL_HEALTH_RECORD',
]

function record(overrides = {}) {
  return {
    id: 5,
    bloodType: 'O+',
    mobility: 'Đi lại cần gậy',
    currentMedications: 'Amlodipine 5mg',
    confirmed: false,
    confirmedBy: null,
    corrected: false,
    ...overrides,
  }
}

function item(overrides = {}) {
  return {
    id: 9,
    type: 'ALLERGY',
    allergen: { id: 3, name: 'Penicillin' },
    freeText: null,
    notAutoChecked: false,
    pendingCheck: false,
    severity: 'Nặng',
    source: 'Hồ sơ y tế kèm theo',
    recordedBy: 'Bác sĩ Lê',
    at: '2026-10-20T09:30:00+07:00',
    status: 'ACTIVE',
    exclusion: null,
    ...overrides,
  }
}

function catalogs() {
  api.on(
    'GET',
    '/catalogs/allergens',
    json(200, [{ id: 3, name: 'Penicillin', group: 'ACTIVE_INGREDIENT', status: 'ACTIVE' }]),
  )
  api.on(
    'GET',
    '/catalogs/allergy-severities',
    json(200, [
      { id: 2, name: 'Trung bình' },
      { id: 3, name: 'Nặng' },
    ]),
  )
  api.on('GET', '/catalogs/information-sources', json(200, [{ id: 4, name: 'Bác sĩ khám' }]))
}

function routes({ initial = record(), items = [item()], etag = '"1"' } = {}) {
  if (initial) api.on('GET', '/residents/7/initial-health-record', json(200, initial, { ETag: etag }))
  else api.on('GET', '/residents/7/initial-health-record', problem(404, 'NOT_FOUND'))
  api.on('GET', '/residents/7/health-items', (u) => {
    const status = u.searchParams.get('status') ?? 'ACTIVE'
    return json(
      200,
      items.filter((i) => status === 'ALL' || i.status === status),
    )
  })
}

function open(allowedCommands = ALL_COMMANDS) {
  return render(<HealthTab residentId={7} allowedCommands={allowedCommands} />)
}

describe('HealthTab, hồ sơ sức khỏe ban đầu', () => {
  it('FR_017_chuaCoHoSo_hienTrongVaNutLapKhiCoQuyen', async () => {
    routes({ initial: null, items: [] })

    open()

    expect(await screen.findByText('Chưa lập hồ sơ sức khỏe ban đầu')).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Lập hồ sơ' })).toBeTruthy()
  })

  it('FR_017_khongCoQuyenGhi_khongCoNutLapHoacSua', async () => {
    routes({ initial: null, items: [] })

    open([])

    await screen.findByText('Chưa lập hồ sơ sức khỏe ban đầu')
    expect(screen.queryByRole('button', { name: 'Lập hồ sơ' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Ghi nhận mục' })).toBeNull()
  })

  it('FR_017_lapBanNhap_guiPutKhongIfMatch_roiHienBanNhap', async () => {
    routes({ initial: null, items: [] })
    api.on('PUT', '/residents/7/initial-health-record', json(200, record(), { ETag: '"0"' }))

    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Lập hồ sơ' }))
    fireEvent.change(screen.getByLabelText('Nhóm máu'), { target: { value: 'O+' } })
    fireEvent.change(screen.getByLabelText('Thuốc đang sử dụng khi tiếp nhận'), {
      target: { value: 'Amlodipine 5mg' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu bản nháp' }))

    await waitFor(() => expect(api.calls('PUT', '/residents/7/initial-health-record')).toHaveLength(1))
    const [, init] = api.calls('PUT', '/residents/7/initial-health-record')[0]
    expect(init.headers['If-Match']).toBeUndefined()
    expect(JSON.parse(init.body)).toMatchObject({ bloodType: 'O+', currentMedications: 'Amlodipine 5mg' })
    expect(await screen.findByText('Bản nháp')).toBeTruthy()
    expect(screen.getByText('Amlodipine 5mg')).toBeTruthy()
  })

  it('FR_017_suaBanNhap_guiIfMatchTheoETag', async () => {
    routes({ items: [] })
    api.on(
      'PUT',
      '/residents/7/initial-health-record',
      json(200, record({ currentMedications: 'Amlodipine 10mg' }), { ETag: '"2"' }),
    )

    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Sửa bản nháp' }))
    fireEvent.change(screen.getByLabelText('Thuốc đang sử dụng khi tiếp nhận'), {
      target: { value: 'Amlodipine 10mg' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu bản nháp' }))

    await waitFor(() => expect(api.calls('PUT', '/residents/7/initial-health-record')).toHaveLength(1))
    expect(api.calls('PUT', '/residents/7/initial-health-record')[0][1].headers['If-Match']).toBe('"1"')
    expect(await screen.findByText('Amlodipine 10mg')).toBeTruthy()
  })

  it('FR_017_412_baoDuLieuDaDoi_vaChoXemGiaTriMoi', async () => {
    routes({ items: [] })
    api.on('PUT', '/residents/7/initial-health-record', problem(412, 'PRECONDITION_FAILED'))

    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Sửa bản nháp' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu bản nháp' }))

    expect(await screen.findByText('Dữ liệu đã thay đổi')).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Xem giá trị mới' })).toBeTruthy()
  })

  it('FR_017_chiBacSiXacNhan_nutXacNhanTheoLenhDuocPhep', async () => {
    routes({ items: [] })

    open(['WRITE_INITIAL_HEALTH_RECORD'])

    await screen.findByText('Bản nháp')
    expect(screen.queryByRole('button', { name: 'Xác nhận hồ sơ' })).toBeNull()
  })

  it('FR_017_xacNhan_guiIfMatch_roiKhongConSuaDuoc', async () => {
    routes({ items: [] })
    api.on('POST', '/residents/7/initial-health-record/confirm', () => {
      api.on(
        'GET',
        '/residents/7/initial-health-record',
        json(200, record({ confirmed: true, confirmedBy: 'Bác sĩ Lê' }), { ETag: '"2"' }),
      )
      return json(200, record({ confirmed: true, confirmedBy: 'Bác sĩ Lê' }), { ETag: '"2"' })
    })

    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Xác nhận hồ sơ' }))

    await waitFor(() => expect(api.calls('POST', '/residents/7/initial-health-record/confirm')).toHaveLength(1))
    expect(api.calls('POST', '/residents/7/initial-health-record/confirm')[0][1].headers['If-Match']).toBe('"1"')
    expect(await screen.findByText('Đã xác nhận')).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Sửa bản nháp' })).toBeNull()
    expect(screen.getByRole('button', { name: 'Đính chính' })).toBeTruthy()
  })

  it('FR_017_daDinhChinh_hienNhan_vaMoKhungCacLanDinhChinh', async () => {
    routes({ initial: record({ confirmed: true, confirmedBy: 'Bác sĩ Lê', corrected: true }), items: [] })
    api.on(
      'GET',
      '/corrections/M01.INITIAL_HEALTH_RECORD/5',
      json(200, {
        recordType: 'M01.INITIAL_HEALTH_RECORD',
        originalRecordId: 5,
        corrected: true,
        voided: false,
        originalContent: { currentMedications: 'Amlodipine 5mg' },
        currentContent: { currentMedications: 'Amlodipine 10mg' },
        corrections: [],
      }),
    )

    open()
    expect(await screen.findByText('Đã đính chính')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Đính chính' }))

    expect(await screen.findByText('Các lần đính chính')).toBeTruthy()
    expect(await screen.findByText('Amlodipine 10mg')).toBeTruthy()
  })
})

describe('HealthTab, mục sức khỏe', () => {
  it('FR_018a_dangDiUngTachRieng_hienNhanKhongKiemTraTuDongVaChoKiemTra', async () => {
    routes({
      items: [
        item(),
        item({ id: 10, allergen: null, freeText: 'Phấn hoa', notAutoChecked: true, severity: 'Nhẹ' }),
        item({ id: 11, type: 'CHRONIC_CONDITION', allergen: null, freeText: 'Tăng huyết áp', severity: null }),
        item({ id: 12, pendingCheck: true, allergen: { id: 4, name: 'Aspirin' } }),
      ],
    })

    open()

    expect(await screen.findByText(/Penicillin \(Nặng\), Phấn hoa \(Nhẹ\)/)).toBeTruthy()
    const list = screen.getByRole('list', { name: 'Mục sức khỏe' })
    expect(within(list).getByText('Tăng huyết áp')).toBeTruthy()
    expect(within(list).getByText('Phấn hoa')).toBeTruthy()
    expect(within(list).getAllByText('Không kiểm tra tự động')).toHaveLength(1)
    expect(within(list).getAllByText('Chờ kiểm tra thuốc và chế độ ăn')).toHaveLength(1)
    expect(screen.queryByRole('button', { name: /Sửa mục|Xóa/ })).toBeNull()
  })

  it('FR_019_locDaLoaiTru_hienLyDoVaNguoiLoaiTru', async () => {
    routes({
      items: [
        item(),
        item({
          id: 13,
          freeText: 'Gãy xương đùi',
          allergen: null,
          type: 'MEDICAL_HISTORY',
          severity: null,
          status: 'EXCLUDED',
          exclusion: { at: '2026-10-21T08:00:00+07:00', by: 'Điều dưỡng Mai', reason: 'Ghi nhầm người' },
        }),
      ],
    })

    open()
    await screen.findByText('Penicillin (Nặng)')
    fireEvent.click(screen.getByRole('button', { name: /Đã loại trừ/ }))

    expect(await screen.findByText('Gãy xương đùi')).toBeTruthy()
    expect(screen.getByText(/Ghi nhầm người/)).toBeTruthy()
    expect(screen.getByText(/Điều dưỡng Mai/)).toBeTruthy()
    expect(screen.queryByRole('button', { name: 'Loại trừ' })).toBeNull()
  })

  it('FR_019_loaiTru_batBuocLyDo_roiGuiPost', async () => {
    routes()
    api.on('POST', '/health-items/9/exclude', json(200, item({ status: 'EXCLUDED' })))

    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Loại trừ' }))
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận loại trừ' }))

    expect(screen.getByText('Nhập lý do loại trừ')).toBeTruthy()
    expect(api.calls('POST', '/health-items/9/exclude')).toHaveLength(0)

    fireEvent.change(screen.getByLabelText('Lý do loại trừ'), { target: { value: 'Ghi nhầm người' } })
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận loại trừ' }))

    await waitFor(() => expect(api.calls('POST', '/health-items/9/exclude')).toHaveLength(1))
    const [, init] = api.calls('POST', '/health-items/9/exclude')[0]
    expect(JSON.parse(init.body)).toEqual({ reason: 'Ghi nhầm người' })
    expect(init.headers['Idempotency-Key']).toBeTruthy()
  })

  it('FR_019_khongCoQuyenLoaiTru_khongCoNutLoaiTru', async () => {
    routes()

    open(['RECORD_HEALTH_ITEM'])

    await screen.findByText('Penicillin (Nặng)')
    expect(screen.queryByRole('button', { name: 'Loại trừ' })).toBeNull()
  })

  it('BR_M01_11_ghiDiUngTuDanhMuc_batBuocDiNguyenMucDoNguon_roiGuiPost', async () => {
    routes({ items: [] })
    catalogs()
    api.on('POST', '/residents/7/health-items', json(201, { items: item(), warnings: [] }))

    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Ghi nhận mục' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Ghi nhận' }))

    expect(screen.getByText('Chọn dị nguyên')).toBeTruthy()
    expect(screen.getByText('Chọn mức độ')).toBeTruthy()
    expect(screen.getByText('Chọn nguồn thông tin')).toBeTruthy()
    expect(api.calls('POST', '/residents/7/health-items')).toHaveLength(0)

    fireEvent.change(await screen.findByLabelText('Dị nguyên'), { target: { value: '3' } })
    fireEvent.change(screen.getByLabelText('Mức độ'), { target: { value: '3' } })
    fireEvent.change(screen.getByLabelText('Nguồn thông tin'), { target: { value: '4' } })
    fireEvent.click(screen.getByRole('button', { name: 'Ghi nhận' }))

    await waitFor(() => expect(api.calls('POST', '/residents/7/health-items')).toHaveLength(1))
    const [, init] = api.calls('POST', '/residents/7/health-items')[0]
    expect(JSON.parse(init.body)).toEqual({ type: 'ALLERGY', allergenId: 3, severityId: 3, sourceId: 4 })
  })

  it('FR_018a_diUngKhac_guiFreeText_vaBaoKhongKiemTraTuDong', async () => {
    routes({ items: [] })
    catalogs()
    api.on(
      'POST',
      '/residents/7/health-items',
      json(201, {
        items: item({ allergen: null, freeText: 'Phấn hoa', notAutoChecked: true }),
        warnings: [{ code: 'ALLERGY_NOT_AUTO_CHECKED', description: 'Xung đột sẽ không được phát hiện tự động' }],
      }),
    )

    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Ghi nhận mục' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Dị ứng khác, nhập tay' }))
    fireEvent.change(screen.getByLabelText('Nội dung'), { target: { value: 'Phấn hoa' } })
    fireEvent.change(await screen.findByLabelText('Mức độ'), { target: { value: '2' } })
    fireEvent.change(screen.getByLabelText('Nguồn thông tin'), { target: { value: '4' } })
    fireEvent.click(screen.getByRole('button', { name: 'Ghi nhận' }))

    expect(await screen.findByText(/Xung đột sẽ không được phát hiện tự động/)).toBeTruthy()
    const [, init] = api.calls('POST', '/residents/7/health-items')[0]
    expect(JSON.parse(init.body)).toEqual({ type: 'ALLERGY', freeText: 'Phấn hoa', severityId: 2, sourceId: 4 })
  })

  it('FR_021_409Trung_hoiXacNhan_roiGuiLaiVoiConfirmDuplicate', async () => {
    routes({ items: [] })
    catalogs()
    let calls = 0
    api.on('POST', '/residents/7/health-items', () => {
      calls += 1
      return calls === 1
        ? problem(409, 'DUPLICATE_HEALTH_ITEM', { detail: 'Đã có mục trùng', existingItemId: 9 })
        : json(201, { items: item(), warnings: [] })
    })

    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Ghi nhận mục' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Bệnh nền' }))
    fireEvent.change(screen.getByLabelText('Nội dung'), { target: { value: 'Tăng huyết áp' } })
    fireEvent.change(await screen.findByLabelText('Nguồn thông tin'), { target: { value: '4' } })
    fireEvent.click(screen.getByRole('button', { name: 'Ghi nhận' }))

    fireEvent.click(await screen.findByRole('button', { name: 'Vẫn ghi nhận mục này' }))

    await waitFor(() => expect(api.calls('POST', '/residents/7/health-items')).toHaveLength(2))
    expect(JSON.parse(api.calls('POST', '/residents/7/health-items')[0][1].body).confirmDuplicate).toBeUndefined()
    expect(JSON.parse(api.calls('POST', '/residents/7/health-items')[1][1].body)).toMatchObject({
      type: 'CHRONIC_CONDITION',
      freeText: 'Tăng huyết áp',
      confirmDuplicate: true,
    })
  })

  it('FR_019_khongCoQuyenGhiMuc_khongCoNutGhiNhanMuc', async () => {
    routes()

    open(['EXCLUDE_HEALTH_ITEM'])

    await screen.findByText('Penicillin (Nặng)')
    expect(screen.queryByRole('button', { name: 'Ghi nhận mục' })).toBeNull()
  })

  it('FR_018_chuaCoMuc_hienTrongVaKhongGhiKhongCo', async () => {
    routes({ initial: null, items: [] })

    open()

    expect(await screen.findByText('Chưa ghi nhận dị ứng, bệnh nền hay tiền sử bệnh')).toBeTruthy()
    expect(screen.queryByText('Không có')).toBeNull()
  })
})

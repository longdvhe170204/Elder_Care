import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { json, mockRoutes } from '@/test/fetchRoutes'
import { HealthCatalogPage } from './HealthCatalogPage'

// Feature 007 Màn danh mục sức khỏe và sự cố theo FR-001, FR-002, FR-039, FR-042, FR-048a.

let api

const DEMO_VITAL_SIGNS = [
  {
    id: 1,
    code: 'SPO2',
    name: 'Độ bão hòa oxy trong máu',
    unit: '%',
    vitalSign: true,
    components: [],
    validRange: { min: 50, max: 100 },
    missingValidRange: false,
    active: true,
  },
  {
    id: 2,
    code: 'HUYET_AP',
    name: 'Huyết áp',
    unit: 'mmHg',
    vitalSign: true,
    components: [
      { code: 'TAM_THU', name: 'Tâm thu' },
      { code: 'TAM_TRUONG', name: 'Tâm trương' },
    ],
    validRange: null,
    missingValidRange: false,
    active: true,
  },
  {
    id: 3,
    code: 'CAN_NANG',
    name: 'Cân nặng',
    unit: 'kg',
    vitalSign: false,
    components: [],
    validRange: null,
    missingValidRange: true,
    active: true,
  },
]

const DEMO_TREND_RULES = [
  {
    code: 'WEIGHT_LOSS',
    name: 'Sụt cân bất thường',
    enabled: true,
    level: 'LOW',
    parameterCode: 'CFG-M05-03',
  },
  {
    code: 'MISSED_MEASUREMENT',
    name: 'Bỏ lỡ lần đo theo lịch',
    enabled: true,
    level: 'MEDIUM',
    parameterCode: null,
  },
]

const DEMO_INCIDENT_TYPES = [
  {
    id: 10,
    code: 'FALL',
    name: 'Ngã',
    defaultLevel: 'CRITICAL',
    nonHealth: false,
    allowsNoResident: false,
    active: true,
  },
  {
    id: 11,
    code: 'FACILITY',
    name: 'Cơ sở vật chất hư hỏng',
    defaultLevel: 'LOW',
    nonHealth: true,
    allowsNoResident: true,
    active: true,
  },
]

const DEMO_PROGRESSION_TYPES = [
  {
    id: 20,
    code: 'CHEST_COMPRESSION',
    name: 'Ép tim ngoài lồng ngực',
    group: 'ACTION',
    resuscitation: true,
  },
  {
    id: 21,
    code: 'EVALUATION',
    name: 'Đánh giá ban đầu',
    group: 'EVALUATION',
    resuscitation: false,
  },
]

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/vital-sign-catalog', json(200, DEMO_VITAL_SIGNS, { ETag: '"v1"' }))
  api.on('GET', '/trend-rules', json(200, DEMO_TREND_RULES, { ETag: '"t1"' }))
  api.on('GET', '/incident-types', json(200, DEMO_INCIDENT_TYPES, { ETag: '"i1"' }))
  api.on('GET', '/progression-types', json(200, DEMO_PROGRESSION_TYPES))
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('HealthCatalogPage', () => {
  it('FR_001_traDanhMucChiSo_hienDuKhoangVatLyVaSinhHieu', async () => {
    render(<HealthCatalogPage role="DOCTOR" />)

    expect(await screen.findByText('Độ bão hòa oxy trong máu')).toBeInTheDocument()
    expect(screen.getByText('Huyết áp')).toBeInTheDocument()
    expect(screen.getByText('Cân nặng')).toBeInTheDocument()
    expect(screen.getByText('Chưa có khoảng hợp lệ')).toBeInTheDocument()
  })

  it('FR_002_suaChiSo_guiETagVaIfMatch', async () => {
    api.on('PUT', '/vital-sign-catalog/1', json(200, {}))
    render(<HealthCatalogPage role="NURSING_HOME_MANAGER" />)

    expect(await screen.findByText('Độ bão hòa oxy trong máu')).toBeInTheDocument()
    const editBtn = screen.getByTitle('Sửa chỉ số Độ bão hòa oxy trong máu')
    fireEvent.click(editBtn)

    const nameInput = screen.getByLabelText('Tên chỉ số *')
    fireEvent.change(nameInput, { target: { value: 'Nồng độ oxy SpO2' } })

    fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }))

    await vi.waitFor(() => expect(api.calls('PUT', '/vital-sign-catalog/1')).toHaveLength(1))
    const [, init] = api.calls('PUT', '/vital-sign-catalog/1')[0]
    expect(init.headers['If-Match']).toBe('"v1"')
  })

  it('FR_002_ngungHieuLucChiSo_batBuocLyDo', async () => {
    api.on('POST', '/vital-sign-catalog/1/deactivate', json(200, {}))
    render(<HealthCatalogPage role="NURSING_HOME_MANAGER" />)

    expect(await screen.findByText('Độ bão hòa oxy trong máu')).toBeInTheDocument()
    const deactivateBtn = screen.getByTitle('Ngừng hiệu lực Độ bão hòa oxy trong máu')
    fireEvent.click(deactivateBtn)

    const confirmBtn = screen.getByRole('button', { name: 'Xác nhận ngừng' })
    fireEvent.click(confirmBtn)
    // Chưa nhập lý do thì không được gọi API
    expect(api.calls('POST', '/vital-sign-catalog/1/deactivate')).toHaveLength(0)

    const reasonInput = screen.getByLabelText('Lý do ngừng hiệu lực *')
    fireEvent.change(reasonInput, { target: { value: 'Thay thế bằng cảm biến đo liên tục' } })
    fireEvent.click(confirmBtn)

    await vi.waitFor(() => expect(api.calls('POST', '/vital-sign-catalog/1/deactivate')).toHaveLength(1))
  })

  it('FR_039_quyTacXuHuong_doiMucVaBatTat_guiLyDo', async () => {
    api.on('PUT', '/trend-rules/WEIGHT_LOSS', json(200, {}))
    render(<HealthCatalogPage role="NURSING_HOME_MANAGER" />)

    expect(await screen.findByText('Độ bão hòa oxy trong máu')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Quy tắc xu hướng' }))

    expect(await screen.findByText('Sụt cân bất thường')).toBeInTheDocument()
    const configBtn = screen.getByTitle('Cấu hình quy tắc Sụt cân bất thường')
    fireEvent.click(configBtn)

    const reasonInput = screen.getByLabelText('Lý do điều chỉnh *')
    fireEvent.change(reasonInput, { target: { value: 'Tăng cường giám sát suy dinh dưỡng' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu cấu hình' }))

    await vi.waitFor(() => expect(api.calls('PUT', '/trend-rules/WEIGHT_LOSS')).toHaveLength(1))
    const [, init] = api.calls('PUT', '/trend-rules/WEIGHT_LOSS')[0]
    expect(init.headers['If-Match']).toBe('"t1"')
  })

  it('FR_042_loaiSuCo_hienMucMacDinhVaDauKhongThuocSucKhoe', async () => {
    render(<HealthCatalogPage role="DOCTOR" />)

    expect(await screen.findByText('Độ bão hòa oxy trong máu')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Loại sự cố' }))

    expect(await screen.findByText('Ngã')).toBeInTheDocument()
    expect(screen.getByText('Cơ sở vật chất hư hỏng')).toBeInTheDocument()
    expect(screen.getByText('Không thuộc sức khỏe')).toBeInTheDocument()
  })

  it('FR_048a_loaiDienBien_hienDauBienPhapHoiSuc', async () => {
    render(<HealthCatalogPage role="DOCTOR" />)

    expect(await screen.findByText('Độ bão hòa oxy trong máu')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Loại diễn biến' }))

    expect(await screen.findByText('Ép tim ngoài lồng ngực')).toBeInTheDocument()
    expect(screen.getByText('Biện pháp hồi sức')).toBeInTheDocument()
  })
})


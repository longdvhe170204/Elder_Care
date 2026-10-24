import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { ReportsPage } from './ReportsPage'

// 016 màn Báo cáo: tab theo bảng FR-010, bảng tổng và chia nhóm, bấm giá trị mở danh sách bản ghi, hộp xuất (FR-061:
// định danh chỉ Quản lý viện, mục đích bắt buộc), tab Lần xuất (FR-062).

let api

const REPORT = {
  group: 'HEALTH_MEDICATION',
  from: '2026-10-01',
  to: '2026-10-24',
  asOf: '2026-10-24T03:15:00Z',
  filters: {},
  values: [
    {
      metricCode: 'MED.MISSED_RATE',
      value: 2.1,
      numerator: 12,
      denominator: 571,
      labels: ['DAY_NOT_ENDED'],
      pendingReviewCount: 0,
      breakdown: [{ key: 'Ca sáng', value: 1.8, numerator: 5, denominator: 280 }],
    },
  ],
  notes: [],
}

beforeEach(() => {
  api = mockRoutes()
  api.on(
    'GET',
    '/metrics',
    json(200, [
      { code: 'MED.MISSED_RATE', group: 'MEDICATION_DOSES', name: 'Tỷ lệ liều Bỏ lỡ', definition: '', source: '006' },
    ]),
  )
  api.on('GET', (u) => u.pathname.startsWith('/api/v1/reports/') && u.pathname !== '/api/v1/reports/exports', () =>
    json(200, REPORT),
  )
  vi.stubGlobal('crypto', { randomUUID: () => '00000000-0000-4000-8000-000000000001' })
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('ReportsPage', () => {
  it('FR_010_tabTheoVaiTro', () => {
    const { unmount } = render(<ReportsPage role="ACCOUNTANT" />)
    expect(screen.getByRole('button', { name: 'Chi phí' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Sức khỏe, thuốc' })).toBeNull()
    expect(screen.getByRole('button', { name: 'Lần xuất' })).toBeInTheDocument()
    unmount()

    render(<ReportsPage role="FLOOR_MANAGER" />)
    expect(screen.queryByRole('button', { name: 'Chi phí' })).toBeNull()
    expect(screen.getByRole('button', { name: 'Nhân sự, ca' })).toBeInTheDocument()
  })

  it('FR_009_bangTongVaChiaNhom_bamGiaTriMoDanhSach', async () => {
    api.on(
      'GET',
      '/metric-details/MED.MISSED_RATE',
      json(200, { metricCode: 'MED.MISSED_RATE', total: 0, rows: [] }),
    )
    render(<ReportsPage role="DOCTOR" />)
    fireEvent.click(screen.getByRole('button', { name: 'Sức khỏe, thuốc' }))

    expect(await screen.findByText('Tỷ lệ liều Bỏ lỡ')).toBeInTheDocument()
    expect(screen.getByText('12 / 571')).toBeInTheDocument()
    expect(screen.getByText('ngày chưa kết thúc')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '1,8' }))

    expect(await screen.findByText('Không có bản ghi')).toBeInTheDocument()
    const [url] = api.calls('GET', '/metric-details/MED.MISSED_RATE')[0]
    expect(new URL(url, 'http://localhost').searchParams.get('breakdownKey')).toBe('Ca sáng')
  })

  it('FR_060_khoangQuaDai_hienLoi', async () => {
    api.on('GET', '/reports/RESIDENTS', problem(422, 'RANGE_EXCEEDS_MAXIMUM', { detail: 'Khoảng dài hơn 12 tháng' }))
    render(<ReportsPage role="NURSING_HOME_MANAGER" />)

    expect(await screen.findByText('Khoảng dài hơn 12 tháng')).toBeInTheDocument()
  })

  it('FR_061_quanLyVien_coDinhDanh_batBuocMucDich_roiXuat', async () => {
    api.on('POST', '/reports/RESIDENTS/export', json(201, { fileId: 7, auditLogId: 9 }))
    render(<ReportsPage role="NURSING_HOME_MANAGER" />)
    await screen.findByText('Tỷ lệ liều Bỏ lỡ')

    fireEvent.click(screen.getByRole('button', { name: 'Xuất file' }))
    fireEvent.click(screen.getByRole('checkbox', { name: 'Có danh sách định danh người cao tuổi' }))
    const dialogExport = screen.getAllByRole('button', { name: 'Xuất file' }).at(-1)
    fireEvent.click(dialogExport)
    expect(await screen.findByText('Nhập mục đích xuất danh sách có định danh.')).toBeInTheDocument()
    expect(api.calls('POST', '/reports/RESIDENTS/export')).toHaveLength(0)

    fireEvent.change(screen.getByLabelText('Mục đích xuất (bắt buộc)'), {
      target: { value: 'Họp giao ban an toàn thuốc' },
    })
    fireEvent.click(dialogExport)

    expect(await screen.findByRole('button', { name: 'Tải file' })).toBeInTheDocument()
    const [, init] = api.calls('POST', '/reports/RESIDENTS/export')[0]
    expect(JSON.parse(init.body)).toMatchObject({
      includeIdentity: true,
      purpose: 'Họp giao ban an toàn thuốc',
      format: 'XLSX',
    })
    expect(init.headers['Idempotency-Key']).toBeTruthy()
  })

  it('FR_061_vaiTroKhac_chiTongHop', async () => {
    render(<ReportsPage role="FLOOR_MANAGER" />)
    await screen.findByText('Tỷ lệ liều Bỏ lỡ')

    fireEvent.click(screen.getByRole('button', { name: 'Xuất file' }))

    expect(screen.getByText('File chỉ có số liệu tổng hợp.')).toBeInTheDocument()
    expect(screen.queryByRole('checkbox')).toBeNull()
  })

  it('FR_062_lanXuat_bang', async () => {
    api.on(
      'GET',
      '/reports/exports',
      json(200, [
        {
          auditLogId: 9,
          fileId: 7,
          exportedAt: '2026-10-24T03:20:00Z',
          exportedBy: 5,
          role: 'AC-02',
          group: 'HEALTH_MEDICATION',
          from: '2026-10-01',
          to: '2026-10-24',
          includeIdentity: false,
          purpose: null,
          rowCount: 4,
        },
      ]),
    )
    render(<ReportsPage role="FLOOR_MANAGER" />)
    fireEvent.click(screen.getByRole('button', { name: 'Lần xuất' }))

    expect(await screen.findByText('Trưởng tầng')).toBeInTheDocument()
    expect(screen.getByText('01/10/2026 – 24/10/2026')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Tải file' })).toBeInTheDocument()
  })
})

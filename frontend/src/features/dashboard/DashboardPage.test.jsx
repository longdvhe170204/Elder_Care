import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { DashboardPage } from './DashboardPage'

// 016 T010: khối theo nhóm backend trả, "—" khi mẫu số 0, số mở danh sách bản ghi, 304 giữ số đang hiện,
// không có phạm vi thì thông điệp (FR-020, FR-021, FR-009).

let api

const LICENSE = {
  metricCode: 'LICENSE.FACILITY',
  value: 1,
  numerator: null,
  denominator: null,
  labels: [],
  pendingReviewCount: 0,
  breakdown: [],
}

function dashboard(blocks, message = null) {
  return json(200, { asOf: '2026-10-24T03:15:30Z', blocks, message }, { ETag: '"a1"' })
}

beforeEach(() => {
  api = mockRoutes()
  api.on(
    'GET',
    '/metrics',
    json(200, [
      {
        code: 'LICENSE.FACILITY',
        group: 'FACILITY_LICENSES',
        name: 'Giấy phép cơ sở sắp hết hạn',
        definition: 'Hết hạn trong CFG-M06-02',
        source: '007',
      },
    ]),
  )
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('DashboardPage', () => {
  it('FR_020_khoiTheoNhom_soVaNhan_khoiChuaCoNguon', async () => {
    api.on(
      'GET',
      '/dashboard',
      dashboard([
        { group: 'FACILITY_LICENSES', status: 'COMPLETE', values: [LICENSE] },
        { group: 'INCURRED_COSTS', status: 'COMPLETE', values: [] },
      ]),
    )
    render(<DashboardPage role="NURSING_HOME_MANAGER" />)

    expect(await screen.findByText('Giấy phép cơ sở')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Tổng quan toàn viện hôm nay' })).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: /Giấy phép cơ sở sắp hết hạn/ })).toHaveTextContent('1')
    expect(screen.getByText(/Chưa có dữ liệu: module nguồn/)).toBeInTheDocument()
    expect(screen.getByText(/Cập nhật lúc 10:15:30/)).toBeInTheDocument()
  })

  it('FR_008_mauSo0_hienGachNgang', async () => {
    api.on(
      'GET',
      '/dashboard',
      dashboard([
        {
          group: 'FACILITY_LICENSES',
          status: 'COMPLETE',
          values: [{ ...LICENSE, value: null, labels: ['NO_DATA'] }],
        },
      ]),
    )
    render(<DashboardPage role="NURSING_HOME_MANAGER" />)

    const button = await screen.findByRole('button', { name: /Giấy phép cơ sở sắp hết hạn/ })
    expect(within(button).getByText('—')).toBeInTheDocument()
    expect(within(button).getAllByText('không có dữ liệu').length).toBeGreaterThan(0)
  })

  it('FR_009_bamSo_moDanhSachBanGhi', async () => {
    api.on('GET', '/dashboard', dashboard([{ group: 'FACILITY_LICENSES', status: 'COMPLETE', values: [LICENSE] }]))
    api.on(
      'GET',
      '/metric-details/LICENSE.FACILITY',
      json(200, {
        metricCode: 'LICENSE.FACILITY',
        total: 1,
        rows: [
          {
            sourceKey: 'M06.FACILITY_LICENSE:1',
            feature: '007',
            type: 'FACILITY_LICENSE',
            status: null,
            referenceTime: null,
            floorArea: null,
            resident: null,
            reduced: false,
            performedBy: null,
            result: { name: 'Giấy phép hoạt động', daysRemaining: 45 },
            pendingReview: false,
            canOpenRecord: false,
          },
        ],
      }),
    )
    render(<DashboardPage role="NURSING_HOME_MANAGER" />)

    fireEvent.click(await screen.findByRole('button', { name: /Giấy phép cơ sở sắp hết hạn/ }))
    expect(await screen.findByText(/name: Giấy phép hoạt động, daysRemaining: 45/)).toBeInTheDocument()
    expect(screen.getByText('1 bản ghi')).toBeInTheDocument()
  })

  it('FR_021_lamMoi_304_giuSoDangHien_guiIfNoneMatch', async () => {
    let calls = 0
    api.on('GET', '/dashboard', () => {
      calls += 1
      return calls === 1
        ? dashboard([{ group: 'FACILITY_LICENSES', status: 'COMPLETE', values: [LICENSE] }])
        : new Response(null, { status: 304 })
    })
    render(<DashboardPage role="NURSING_HOME_MANAGER" />)
    await screen.findByText('Giấy phép cơ sở')

    fireEvent.click(screen.getByRole('button', { name: 'Làm mới' }))
    await vi.waitFor(() => expect(calls).toBe(2))

    expect(screen.getByText('Giấy phép cơ sở')).toBeInTheDocument()
    const [, init] = api.calls('GET', '/dashboard')[1]
    expect(init.headers['If-None-Match']).toBe('"a1"')
  })

  it('FR_020_khongCoPhamVi_thongDiep', async () => {
    api.on('GET', '/dashboard', dashboard([], 'Hiện không có phạm vi phân công'))
    render(<DashboardPage role="NURSING_STAFF" />)

    expect(await screen.findByText('Hiện không có phạm vi phân công')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Tổng quan trong phạm vi của bạn hôm nay' })).toBeInTheDocument()
  })

  it('loiTai_hienThuLai', async () => {
    api.on('GET', '/dashboard', problem(403, 'PERMISSION_DENIED'))
    render(<DashboardPage role="NURSING_HOME_MANAGER" />)

    expect(await screen.findByText('Không tải được tổng quan')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Thử lại' })).toBeInTheDocument()
  })
})

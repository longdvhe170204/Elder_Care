import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { FamilyPortal } from '../FamilyPortal'

// 012 T028 (US5; FR-046 → FR-048, BR-M10-01): cổng người thân với phiên thật. Người cao tuổi và quyền có tác dụng lấy từ
// /family-portal/residents; mục "Thêm" ẩn phần không có quyền; Hôm nay đọc thông tin chung; Sức khỏe đọc từng phần qua
// 012 (phần chưa có nguồn ghi rõ chưa có dữ liệu); Hợp đồng chỉ người đại diện.

let api

const RIGHTS = (o = {}) => ({
  VIEW_HEALTH: false,
  VIEW_COSTS: false,
  RECEIVE_URGENT: false,
  REGISTER_VISITS: false,
  PICKUP_ALLOWED: false,
  REQUEST_SERVICES: false,
  ...o,
})

function resident(o = {}) {
  return {
    residentId: 5,
    fullName: 'Nguyễn Thị Lan',
    relationship: 'CHILD',
    representative: false,
    primaryContact: false,
    rights: RIGHTS(),
    canViewBelongings: false,
    ...o,
  }
}

const GENERAL = {
  residentId: 5,
  fullName: 'Nguyễn Thị Lan',
  status: 'IN_RESIDENCE',
  location: 'Phòng 201, giường A',
  visits: [{ id: 81, date: '2026-11-06', startTime: '09:00', endTime: '11:00', status: 'APPROVED' }],
  absences: [],
  dayCareSchedule: [],
  incidents: [],
  unavailable: ['menu', 'incidents'],
}

function signedIn(residents) {
  api.on(
    'GET',
    '/me',
    json(200, { accountId: 41, type: 'FAMILY_MEMBER', fullName: 'Lê M', roles: [], sessionIdleTimeoutSeconds: 1800 }),
  )
  api.on('GET', '/family-portal/residents', json(200, residents))
  api.on('GET', '/family-portal/residents/5/general', json(200, GENERAL))
}

beforeEach(() => {
  api = mockRoutes()
  vi.stubGlobal('scrollTo', () => {})
})

afterEach(() => {
  vi.unstubAllGlobals()
})

async function openMore() {
  render(<FamilyPortal />)
  const nav = await screen.findAllByRole('navigation', { name: 'Cổng người thân' })
  fireEvent.click(within(nav[0]).getByRole('button', { name: 'Thêm' }))
}

describe('Cổng người thân, phiên thật', () => {
  it('US5_2_khongCoQuyenSucKhoe_khongCoMucSucKhoe_HomNayCoDongKhoa', async () => {
    signedIn([resident()])
    render(<FamilyPortal />)
    const nav = await screen.findAllByRole('navigation', { name: 'Cổng người thân' })
    fireEvent.click(within(nav[0]).getByRole('button', { name: 'Hôm nay' }))

    expect(await screen.findByText('Phòng 201, giường A')).toBeInTheDocument()
    expect(screen.getByText('Đang lưu trú')).toBeInTheDocument()
    expect(screen.getByText('06/11/2026, 09:00–11:00')).toBeInTheDocument()
    expect(screen.getByText(/chỉ hiện với người thân được người cao tuổi đồng ý chia sẻ/)).toBeInTheDocument()

    fireEvent.click(within(nav[0]).getByRole('button', { name: 'Thêm' }))
    expect(await screen.findByRole('button', { name: /Tài khoản/ })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Sức khỏe/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Hợp đồng/ })).not.toBeInTheDocument()
  })

  it('US5_1_coQuyenSucKhoe_xemTungPhan_phanChuaCoNguonGhiRo', async () => {
    signedIn([resident({ rights: RIGHTS({ VIEW_HEALTH: true }) })])
    api.on(
      'GET',
      (u) => u.pathname.startsWith('/api/v1/family-portal/residents/5/health/'),
      json(200, { section: 'x', available: false, note: 'Chưa có dữ liệu, tính năng nguồn chưa hoạt động', items: [] }),
    )
    api.on(
      'GET',
      '/family-portal/residents/5/health/allergies-conditions',
      json(200, {
        section: 'allergies-conditions',
        available: true,
        items: [{ type: 'CHRONIC_CONDITION', text: 'Tăng huyết áp' }],
      }),
    )
    await openMore()
    fireEvent.click(await screen.findByRole('button', { name: /Sức khỏe/ }))

    expect(await screen.findByText('Tăng huyết áp')).toBeInTheDocument()
    expect((await screen.findAllByText('Chưa có dữ liệu, tính năng nguồn chưa hoạt động')).length).toBeGreaterThan(0)
    expect(screen.getByText(/Mỗi lần xem mục này được viện ghi lại/)).toBeInTheDocument()
  })

  it('FR_046_anUong_hienLuongNuocTrungBinhVaSoNgayCoDuLieu', async () => {
    // Chủ dự án chốt 2026-11-12: trung bình theo ngày có ghi, kèm số ngày có dữ liệu
    signedIn([resident({ rights: RIGHTS({ VIEW_HEALTH: true }) })])
    api.on(
      'GET',
      (u) => u.pathname.startsWith('/api/v1/family-portal/residents/5/health/'),
      json(200, { section: 'x', available: true, items: [] }),
    )
    api.on(
      'GET',
      '/family-portal/residents/5/health/eating',
      json(200, { section: 'eating', available: true, items: [{ averageWaterMl: 1450, waterDays: 6 }] }),
    )
    await openMore()
    fireEvent.click(await screen.findByRole('button', { name: /Sức khỏe/ }))

    expect(await screen.findByText('Lượng nước trung bình')).toBeInTheDocument()
    expect(screen.getByText('1.450 ml/ngày, tính trên 6 ngày có ghi')).toBeInTheDocument()
  })

  it('US5_4_nguoiDaiDien_xemHopDong', async () => {
    signedIn([resident({ representative: true, rights: RIGHTS({ VIEW_COSTS: true }) })])
    api.on(
      'GET',
      '/family-portal/residents/5/contract',
      json(200, {
        contracts: [
          {
            id: 3,
            contractNumber: 'HD-2026-003',
            residenceType: 'LONG_TERM',
            startDate: '2026-01-01',
            currentEndDate: '2026-12-31',
            status: 'ACTIVE',
          },
        ],
      }),
    )
    await openMore()
    fireEvent.click(await screen.findByRole('button', { name: /Hợp đồng/ }))
    expect(await screen.findByText('Hợp đồng HD-2026-003')).toBeInTheDocument()
    expect(screen.getByText('01/01/2026 → 31/12/2026')).toBeInTheDocument()
  })

  it('FR_046b_doGui_phienThat_hienPhanDoGuiCua013_theoNguoiCaoTuoi', async () => {
    signedIn([resident({ canViewBelongings: true })])
    const Belongings = ({ residentId }) => <p>Đồ gửi của người cao tuổi {residentId}</p>
    render(<FamilyPortal belongings={Belongings} />)
    const nav = await screen.findAllByRole('navigation', { name: 'Cổng người thân' })
    fireEvent.click(within(nav[0]).getByRole('button', { name: 'Thêm' }))
    fireEvent.click(await screen.findByRole('button', { name: /Đồ gửi/ }))

    expect(await screen.findByText('Đồ gửi của người cao tuổi 5')).toBeInTheDocument()
    expect(screen.queryByText(/bản mẫu/)).not.toBeInTheDocument()
  })

  it('US5_1_bi_tuChoi_hienDongKhoa_khongLoNoiDung', async () => {
    signedIn([resident({ rights: RIGHTS({ VIEW_HEALTH: true }) })])
    api.on(
      'GET',
      (u) => u.pathname.startsWith('/api/v1/family-portal/residents/5/health/'),
      problem(403, 'RIGHT_NOT_EFFECTIVE'),
    )
    await openMore()
    fireEvent.click(await screen.findByRole('button', { name: /Sức khỏe/ }))
    await waitFor(() =>
      expect(screen.getAllByText(/chỉ hiện với người thân được người cao tuổi đồng ý chia sẻ/)).toHaveLength(1),
    )
  })
})

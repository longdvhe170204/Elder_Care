import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { StaffingRatioPanel } from './StaffingRatioPanel'

let api
beforeEach(() => {
  api = mockRoutes()
})
afterEach(() => vi.unstubAllGlobals())

it.each([null, undefined])('FR_034_khongNhanVien_khongHienNaN_khiTyLeRong %s', async (ratio) => {
  api.on(
    'GET',
    '/shifts/20/staffing-ratios',
    json(200, [
      {
        at: '2026-10-25T00:00:00Z',
        event: 'STAFF_ABSENT',
        totalWeight: 35,
        staffCount: 0,
        ratio,
        threshold: 8,
        meetsThreshold: false,
      },
    ]),
  )
  render(<StaffingRatioPanel shiftId={20} />)
  expect(await screen.findByText('Chưa có nhân viên được tính')).toBeVisible()
  expect(screen.queryByText(/NaN/)).not.toBeInTheDocument()
})

it('FR_037_hienLichSuTongTrongSoNhanVienNguong_vaPhanBietTyLeRong', async () => {
  api.on(
    'GET',
    '/shifts/20/staffing-ratios',
    json(200, [
      {
        at: '2026-10-25T00:00:00Z',
        event: 'DRAFT_SAVED',
        totalWeight: 35,
        staffCount: 4,
        ratio: 8.75,
        threshold: 8,
        meetsThreshold: false,
      },
      {
        at: '2026-10-25T01:00:00Z',
        event: 'STAFF_ADDED',
        totalWeight: 35,
        staffCount: 5,
        ratio: 7,
        threshold: 8,
        meetsThreshold: true,
      },
      {
        at: '2026-10-25T02:00:00Z',
        event: 'STAFF_ABSENT',
        totalWeight: 35,
        staffCount: 0,
        ratio: null,
        threshold: 8,
        meetsThreshold: false,
      },
    ]),
  )
  render(<StaffingRatioPanel shiftId={20} />)
  expect(await screen.findByText('Lần tính gần nhất: Dưới ngưỡng')).toBeVisible()
  expect(await screen.findByText('Chưa có nhân viên được tính')).toBeVisible()
  expect(screen.getByText('8,75')).toBeVisible()
  expect(screen.getByText('Bổ sung nhân viên')).toBeVisible()
  expect(screen.getByText('Đạt ngưỡng')).toBeVisible()
  expect(screen.getAllByText('Dưới ngưỡng phục vụ')).toHaveLength(2)
  expect(screen.getByText('25/10/2026 07:00')).toBeVisible()
})

it('FR_037_chuaCoLanTinh_khongDungSoKhongGia', async () => {
  api.on('GET', '/shifts/20/staffing-ratios', json(200, []))
  render(<StaffingRatioPanel shiftId={20} />)
  expect(await screen.findByText('Chưa có lần tính tỷ lệ phục vụ.')).toBeVisible()
})

it('FR_048_nguonLoi_hienLoiVaThuLai', async () => {
  api.on('GET', '/shifts/20/staffing-ratios', problem(503, 'SOURCE_UNAVAILABLE', { detail: 'Chưa đọc được số liệu' }))
  render(<StaffingRatioPanel shiftId={20} />)
  expect(await screen.findByText('Chưa đọc được số liệu')).toBeVisible()
  api.on('GET', '/shifts/20/staffing-ratios', json(200, []))
  fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }))
  expect(await screen.findByText('Chưa có lần tính tỷ lệ phục vụ.')).toBeVisible()
})

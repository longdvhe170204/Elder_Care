import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes } from '@/test/fetchRoutes'
import { MonitoringSettingsPage } from './MonitoringSettingsPage'

// 007 US2 (T024 phần web): lịch đo và ngưỡng cá nhân của một người cao tuổi theo api-007, danh sách "chưa có ngưỡng cá
// nhân" (FR-025). Hợp đồng không trả lệnh được phép nên nút theo vai trò Bác sĩ (Phụ lục 27), backend kiểm lại.

let api

const RESIDENTS = [
  { id: 9, profileCode: 'HS-0009', personalInfo: { fullName: 'Trần Thị Mai' } },
  { id: 10, profileCode: 'HS-0010', personalInfo: { fullName: 'Lê Văn Bình' } },
]
const CATALOG = [
  {
    id: 1,
    code: 'BLOOD_PRESSURE',
    name: 'Huyết áp',
    unit: 'mmHg',
    active: true,
    components: [
      { code: 'SYSTOLIC', name: 'Tâm thu' },
      { code: 'DIASTOLIC', name: 'Tâm trương' },
    ],
  },
  {
    id: 2,
    code: 'HEART_RATE',
    name: 'Nhịp tim',
    unit: 'nhịp/phút',
    active: true,
    components: [{ code: 'VALUE', name: 'Giá trị' }],
  },
]
const SCHEDULE = {
  id: 5,
  indicatorIds: [1, 2],
  frequency: { type: 'CLOCK_TIMES', times: ['07:00', '19:00'] },
  startAt: '2026-11-01T00:00:00Z',
  priority: 'MANDATORY',
  status: 'EFFECTIVE',
}
const THRESHOLD = {
  id: 7,
  scope: 'PERSONAL',
  residentId: 9,
  indicatorId: 2,
  effectiveFrom: '2026-11-01T00:00:00Z',
  status: 'EFFECTIVE',
  components: [{ code: 'VALUE', dangerBelow: 40, warningBelow: 50, warningAbove: 100, dangerAbove: 120 }],
}

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/residents', json(200, { items: RESIDENTS }))
  api.on('GET', '/vital-sign-catalog', json(200, CATALOG))
  api.on('GET', '/vital-signs/missing-personal-threshold', json(200, [10]))
  api.on('GET', '/residents/9/measurement-schedules', json(200, [SCHEDULE]))
  api.on('GET', '/thresholds', json(200, [THRESHOLD]))
})

afterEach(() => vi.unstubAllGlobals())

async function pick() {
  fireEvent.click(await screen.findByRole('button', { name: 'Trần Thị Mai, HS-0009' }))
}

describe('MonitoringSettingsPage', () => {
  it('FR_025_danhSachChuaCoNguongCaNhan', async () => {
    render(<MonitoringSettingsPage role="DOCTOR" />)
    const region = await screen.findByRole('region', { name: 'Chưa có ngưỡng cá nhân' })
    expect(await within(region).findByText('Lê Văn Bình')).toBeTruthy()
  })

  it('FR_003_lichDoVaNguongCaNhanCuaNguoiDaChon', async () => {
    render(<MonitoringSettingsPage role="DOCTOR" />)
    await pick()
    const schedules = await screen.findByRole('region', { name: 'Lịch đo' })
    expect(await within(schedules).findByText('Huyết áp, Nhịp tim')).toBeTruthy()
    expect(within(schedules).getByText(/07:00, 19:00/)).toBeTruthy()
    expect(within(schedules).getByText('Hiệu lực')).toBeTruthy()
    const thresholds = screen.getByRole('region', { name: 'Ngưỡng cá nhân' })
    expect(within(thresholds).getByText('Nhịp tim')).toBeTruthy()
    expect(
      within(thresholds).getByText(
        /Giá trị: nguy hiểm dưới 40, cảnh báo dưới 50, cảnh báo trên 100, nguy hiểm trên 120/,
      ),
    ).toBeTruthy()
    expect(JSON.stringify(api.calls('GET', '/thresholds')[0][0])).toContain('residentId=9')
  })

  it('FR_006_ngungLichDo_batBuocLyDo', async () => {
    api.on('POST', '/measurement-schedules/5/stop', json(200, {}))
    render(<MonitoringSettingsPage role="DOCTOR" />)
    await pick()
    const schedules = await screen.findByRole('region', { name: 'Lịch đo' })
    fireEvent.click(await within(schedules).findByRole('button', { name: 'Ngừng lịch đo' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu và ngừng' }))
    expect(await screen.findByText('Nhập lý do')).toBeTruthy()
    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Ổn định' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu và ngừng' }))
    await waitFor(() => expect(api.calls('POST', '/measurement-schedules/5/stop')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/measurement-schedules/5/stop')[0][1].body)).toEqual({ reason: 'Ổn định' })
  })

  it('FR_004_taoLichDoTheoGio', async () => {
    api.on('POST', '/residents/9/measurement-schedules', json(201, SCHEDULE))
    render(<MonitoringSettingsPage role="DOCTOR" />)
    await pick()
    fireEvent.click(await screen.findByRole('button', { name: 'Tạo lịch đo' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Nhịp tim' }))
    fireEvent.change(screen.getByLabelText('Giờ đo'), { target: { value: '08:00, 20:00' } })
    fireEvent.change(screen.getByLabelText('Bắt đầu'), { target: { value: '2026-11-09T00:00' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu lịch đo' }))
    await waitFor(() => expect(api.calls('POST', '/residents/9/measurement-schedules')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/residents/9/measurement-schedules')[0][1].body)).toEqual({
      indicatorIds: [2],
      frequency: { type: 'CLOCK_TIMES', times: ['08:00', '20:00'] },
      startAt: '2026-11-09T00:00:00+07:00',
      priority: 'MANDATORY',
    })
  })

  it('FR_020_thietLapNguongCaNhan', async () => {
    api.on('POST', '/thresholds', json(201, {}))
    render(<MonitoringSettingsPage role="DOCTOR" />)
    await pick()
    fireEvent.click(await screen.findByRole('button', { name: 'Thiết lập ngưỡng' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Nhịp tim' }))
    fireEvent.change(screen.getByLabelText('Giá trị, cảnh báo dưới'), { target: { value: '55' } })
    fireEvent.change(screen.getByLabelText('Giá trị, cảnh báo trên'), { target: { value: '95' } })
    fireEvent.change(screen.getByLabelText('Hiệu lực từ'), { target: { value: '2026-11-09T00:00' } })
    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Theo chỉ định' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu ngưỡng' }))
    await waitFor(() => expect(api.calls('POST', '/thresholds')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/thresholds')[0][1].body)).toEqual({
      scope: 'PERSONAL',
      residentId: 9,
      indicatorId: 2,
      effectiveFrom: '2026-11-09T00:00:00+07:00',
      reason: 'Theo chỉ định',
      components: [{ code: 'VALUE', warningBelow: 55, warningAbove: 95 }],
    })
  })

  it('FR_080_khongPhaiBacSi_khongCoNutLenh', async () => {
    render(<MonitoringSettingsPage role="NURSING_STAFF" />)
    await pick()
    await screen.findByText('Huyết áp, Nhịp tim')
    expect(screen.queryByRole('button', { name: 'Tạo lịch đo' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Ngừng lịch đo' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Thiết lập ngưỡng' })).toBeNull()
  })
})

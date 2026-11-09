import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { AlertsPage } from './AlertsPage'

// 007 US3 (T031 phần web): danh sách cảnh báo đọc từ api-007 `GET /alerts`, họ tên từ api-001; nút lệnh theo
// `allowedCommands`; Đóng cần kết quả, Đổi mức cần lý do; lệnh gửi Idempotency-Key; 403 khi tải là không có quyền.

let api

const ALERT_MEDIUM = {
  id: 1,
  residentId: 9,
  type: 'CHI_SO',
  level: 'MEDIUM',
  occurrenceCount: 2,
  firstAt: '2026-11-08T01:00:00Z',
  latestAt: '2026-11-08T01:30:00Z',
  status: 'NEW',
  escalationLevel: 0,
  maxEscalationReached: false,
  acknowledgementDeadline: '2026-11-08T01:45:00Z',
  origin: 'SYSTEM',
  sources: [],
  allowedCommands: ['receive', 'change-level'],
}
const ALERT_CRITICAL = {
  ...ALERT_MEDIUM,
  id: 2,
  residentId: 10,
  type: 'PROLONGED_INSOMNIA',
  level: 'CRITICAL',
  status: 'ESCALATED',
  escalationLevel: 3,
  maxEscalationReached: true,
  allowedCommands: [],
}
const ALERT_IN_PROGRESS = {
  ...ALERT_MEDIUM,
  id: 3,
  status: 'IN_PROGRESS',
  assignee: 'Trần Thu Hà',
  allowedCommands: ['close', 'change-level'],
}
const ALERT_CLOSED = { ...ALERT_MEDIUM, id: 4, status: 'CLOSED', allowedCommands: [] }

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/alerts', json(200, [ALERT_MEDIUM, ALERT_CRITICAL, ALERT_IN_PROGRESS, ALERT_CLOSED]))
  api.on(
    'GET',
    '/residents/9',
    json(200, { id: 9, profileCode: 'HS-0009', personalInfo: { fullName: 'Trần Thị Mai' } }),
  )
  api.on(
    'GET',
    '/residents/10',
    json(200, { id: 10, profileCode: 'HS-0010', personalInfo: { fullName: 'Lê Văn Bình' } }),
  )
  api.on('GET', '/alerts/3', json(200, ALERT_IN_PROGRESS))
  api.on('GET', '/alerts/1', json(200, ALERT_MEDIUM))
})

afterEach(() => vi.unstubAllGlobals())

describe('AlertsPage', () => {
  it('FR_027_danhSachDangMo_khanCapVaLeoThangToiDaLenDau_hoTenTuHoSo', async () => {
    render(<AlertsPage role="NURSING_STAFF" />)
    const cards = await screen.findAllByRole('article')
    expect(cards).toHaveLength(3)
    expect(within(cards[0]).getByText('Lê Văn Bình')).toBeTruthy()
    expect(within(cards[0]).getByText('Đã leo thang tối đa')).toBeTruthy()
    expect(within(cards[0]).getByText('Mất ngủ kéo dài')).toBeTruthy()
    expect(await within(cards[1]).findByText('Trần Thị Mai')).toBeTruthy()
    expect(within(cards[1]).getByText(/2 lần cùng loại đã gộp/)).toBeTruthy()
    expect(screen.queryByRole('button', { name: /Tiếp nhận.*Lê Văn Bình/ })).toBeNull()
  })

  it('FR_029_tiepNhanTheoAllowedCommands_guiIdempotencyKey_roiTaiLai', async () => {
    api.on('POST', '/alerts/1/receive', json(200, {}))
    render(<AlertsPage role="NURSING_STAFF" />)
    const receive = await screen.findByRole('button', { name: 'Tiếp nhận cảnh báo của Trần Thị Mai' })
    fireEvent.click(receive)
    await waitFor(() => expect(api.calls('POST', '/alerts/1/receive')).toHaveLength(1))
    const [, init] = api.calls('POST', '/alerts/1/receive')[0]
    expect(new Headers(init.headers).get('Idempotency-Key')).toBeTruthy()
    await waitFor(() => expect(api.calls('GET', '/alerts').length).toBeGreaterThan(1))
    expect(await screen.findByText(/Đã tiếp nhận cảnh báo/)).toBeTruthy()
  })

  it('FR_029_dongCanKetQua_doiMucCanLyDo', async () => {
    api.on('POST', '/alerts/3/close', json(200, {}))
    render(<AlertsPage role="NURSING_STAFF" />)
    const cards = await screen.findAllByRole('article')
    fireEvent.click(within(cards[2]).getByRole('button', { name: /Chi tiết/ }))
    fireEvent.click(await screen.findByRole('button', { name: 'Đóng cảnh báo' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu và đóng cảnh báo' }))
    expect(await screen.findByText('Nhập kết quả xử lý')).toBeTruthy()
    expect(api.calls('POST', '/alerts/3/close')).toHaveLength(0)
    fireEvent.change(screen.getByLabelText('Kết quả xử lý'), { target: { value: 'Đã đo lại, ổn định' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu và đóng cảnh báo' }))
    await waitFor(() => expect(api.calls('POST', '/alerts/3/close')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/alerts/3/close')[0][1].body)).toEqual({ resolution: 'Đã đo lại, ổn định' })
  })

  it('FR_029_409_hienDieuKienChuaThoa', async () => {
    api.on('POST', '/alerts/1/receive', problem(409, 'ALREADY_RECEIVED'))
    render(<AlertsPage role="NURSING_STAFF" />)
    fireEvent.click(await screen.findByRole('button', { name: 'Tiếp nhận cảnh báo của Trần Thị Mai' }))
    expect(await screen.findByRole('alert')).toBeTruthy()
  })

  it('FR_080_403_khongCoQuyen', async () => {
    api.on('GET', '/alerts', problem(403, 'PERMISSION_DENIED'))
    render(<AlertsPage role="KITCHEN_STAFF" />)
    expect(await screen.findByText(/không có quyền/i)).toBeTruthy()
  })

  it('FR_029_nhomDaDong', async () => {
    render(<AlertsPage role="NURSING_STAFF" />)
    await screen.findAllByRole('article')
    fireEvent.click(screen.getByRole('button', { name: /^Đã đóng/ }))
    const cards = await screen.findAllByRole('article')
    expect(cards).toHaveLength(1)
    expect(within(cards[0]).getByText('Đã đóng')).toBeTruthy()
  })
})

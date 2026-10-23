import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { NotificationInbox } from './NotificationInbox'

// 009 T035: hộp thông báo (FR-033 → FR-036; hợp đồng /me/notifications).

function notification(overrides = {}) {
  return {
    id: 11,
    level: 'MEDIUM',
    eventType: 'TASK_OVERDUE',
    title: 'Công việc quá hạn 30 phút',
    content: [{ informationType: 'GENERAL', content: 'Đo huyết áp lúc 08:00 chưa ghi nhận' }],
    hiddenParts: [],
    identityOnly: false,
    status: 'SENT',
    sentAt: '2026-10-23T01:15:00Z',
    viewedAt: null,
    acknowledgedAt: null,
    acknowledgementMethod: null,
    sourceRecord: { type: 'M05.CARE_TASK', id: '88', canOpen: false },
    canAcknowledgeAndTakeCharge: false,
    ...overrides,
  }
}

const URGENT = notification({ id: 12, level: 'URGENT', title: 'Ngã trong phòng', eventType: 'INCIDENT_URGENT' })
const VIEWED = notification({
  id: 13,
  level: 'LOW',
  title: 'Yêu cầu phê duyệt mới',
  status: 'VIEWED',
  viewedAt: '2026-10-23T01:00:00Z',
})
const PAGE = { unreadCount: 2, unacknowledgedUrgentCount: 1, items: [URGENT, notification(), VIEWED], nextCursor: null }

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

function inboxCalls() {
  return api.fetchMock.mock.calls
    .map(([url]) => new URL(url, 'http://localhost'))
    .filter((u) => u.pathname === '/api/v1/me/notifications')
}

describe('NotificationInbox', () => {
  it('FR_033_khanCapChuaXacNhanTachLenDau_demChuaXem', async () => {
    api.on('GET', '/me/notifications', json(200, PAGE))
    render(<NotificationInbox />)

    const urgent = await screen.findByRole('region', { name: 'Khẩn cấp chưa xác nhận' })
    expect(urgent).toHaveTextContent('Ngã trong phòng')
    expect(urgent).toHaveTextContent('Khẩn cấp')
    expect(urgent).toHaveTextContent('Chưa xác nhận')
    const list = screen.getByRole('list', { name: 'Thông báo' })
    expect(within(list).getAllByRole('listitem')).toHaveLength(2)
    expect(list).toHaveTextContent('Chưa xem')
    expect(list).toHaveTextContent('Đã xem')
    expect(screen.getByText(/2 chưa xem, 1 khẩn cấp chưa xác nhận/)).toBeInTheDocument()
  })

  it('FR_033_locMucVaChuaXem_guiThamSo', async () => {
    api.on('GET', '/me/notifications', json(200, PAGE))
    render(<NotificationInbox />)
    await screen.findByRole('list', { name: 'Thông báo' })

    fireEvent.click(screen.getByRole('button', { name: 'Trung bình' }))
    await screen.findByRole('list', { name: 'Thông báo' })
    expect(inboxCalls().at(-1).searchParams.get('level')).toBe('MEDIUM')

    fireEvent.click(screen.getByRole('button', { name: 'Chưa xem' }))
    await screen.findByRole('list', { name: 'Thông báo' })
    expect(inboxCalls().at(-1).searchParams.get('unread')).toBe('true')
  })

  it('FR_034_moChiTiet_phanTheoLoai_phanBiAnThanhDongKhoa', async () => {
    api.on('GET', '/me/notifications', json(200, PAGE))
    api.on(
      'GET',
      '/me/notifications/11',
      json(200, notification({ status: 'VIEWED', viewedAt: '2026-10-23T02:00:00Z', hiddenParts: ['HEALTH'] })),
    )
    render(<NotificationInbox />)
    fireEvent.click(await screen.findByRole('button', { name: 'Công việc quá hạn 30 phút' }))

    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByRole('heading', { name: 'Phần chung' })).toBeInTheDocument()
    expect(dialog).toHaveTextContent('Đo huyết áp lúc 08:00 chưa ghi nhận')
    expect(dialog).toHaveTextContent('Phần sức khỏe')
    expect(dialog).toHaveTextContent('Bạn không còn quyền xem phần này')
    expect(dialog).toHaveTextContent('M05.CARE_TASK 88')
  })

  it('FR_036_anSauKhiXem_boKhoiDanhSach_coIdempotencyKey', async () => {
    api.on('GET', '/me/notifications', json(200, PAGE))
    api.on('GET', '/me/notifications/13', json(200, VIEWED))
    api.on('POST', '/me/notifications/13/hide', json(200, {}))
    render(<NotificationInbox />)
    fireEvent.click(await screen.findByRole('button', { name: 'Yêu cầu phê duyệt mới' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Ẩn khỏi danh sách' }))

    await screen.findByText('Đã ẩn thông báo khỏi danh sách')
    const [, init] = api.calls('POST', '/me/notifications/13/hide')[0]
    expect(init.headers['Idempotency-Key']).toMatch(/^[0-9a-f-]{36}$/)
    expect(within(screen.getByRole('list', { name: 'Thông báo' })).queryByText('Yêu cầu phê duyệt mới')).toBeNull()
  })

  it('loiTai_hienThuLai', async () => {
    api.on('GET', '/me/notifications', problem(403, 'PERMISSION_DENIED'))
    render(<NotificationInbox />)
    expect(await screen.findByText('Không tải được thông báo')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Thử lại' })).toBeInTheDocument()
  })

  it('FR_026_xacNhanKhanCap_coIdempotencyKey_taiLai', async () => {
    api.on('GET', '/me/notifications', json(200, PAGE))
    api.on('POST', '/notifications/12/acknowledge', json(200, null))
    render(<NotificationInbox />)
    const urgent = await screen.findByRole('region', { name: 'Khẩn cấp chưa xác nhận' })

    fireEvent.click(within(urgent).getByRole('button', { name: 'Xác nhận đã nhận' }))
    await screen.findByText('Đã xác nhận thông báo khẩn cấp')
    const [, init] = api.calls('POST', '/notifications/12/acknowledge')[0]
    expect(init.headers['Idempotency-Key']).toMatch(/^[0-9a-f-]{36}$/)
    expect(inboxCalls().length).toBeGreaterThan(1)
  })

  it('FR_026_daCoNguoiXacNhan_409_baoLyDo', async () => {
    api.on('GET', '/me/notifications', json(200, PAGE))
    api.on('POST', '/notifications/12/acknowledge', problem(409, 'ALREADY_ACKNOWLEDGED', { detail: 'Đã xác nhận' }))
    render(<NotificationInbox />)
    const urgent = await screen.findByRole('region', { name: 'Khẩn cấp chưa xác nhận' })

    fireEvent.click(within(urgent).getByRole('button', { name: 'Xác nhận đã nhận' }))
    expect(await within(urgent).findByText('Chưa thực hiện được')).toBeInTheDocument()
  })
})

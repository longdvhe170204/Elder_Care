import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { NotificationsPage } from './NotificationsPage'
import { NotificationHistory } from './NotificationHistory'
import { DeliveryErrors } from './DeliveryErrors'
import { DirectContacts } from './DirectContacts'
import { NotificationChannels } from './NotificationChannels'

// 009 T035 (lịch sử gửi, lỗi gửi), T028 (liên hệ trực tiếp, kênh); tab theo bảng quyền FR-039.

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('NotificationsPage', () => {
  it('FR_039_tabTheoVaiTro', () => {
    api.on(
      'GET',
      '/me/notifications',
      json(200, { unreadCount: 0, unacknowledgedUrgentCount: 0, items: [], nextCursor: null }),
    )
    const { unmount } = render(<NotificationsPage role="NURSING_HOME_MANAGER" />)
    for (const tab of ['Hộp thông báo', 'Lịch sử gửi', 'Lỗi gửi', 'Liên hệ trực tiếp', 'Kênh']) {
      expect(screen.getByRole('button', { name: tab })).toBeInTheDocument()
    }
    unmount()

    render(<NotificationsPage role="OFFICE_STAFF" />)
    expect(screen.getByRole('button', { name: 'Liên hệ trực tiếp' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Lịch sử gửi' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Kênh' })).toBeNull()
  })

  it('FR_039_nhanVienChamSocChiCoHopThongBao_khongCoThanhTab', () => {
    api.on(
      'GET',
      '/me/notifications',
      json(200, { unreadCount: 0, unacknowledgedUrgentCount: 0, items: [], nextCursor: null }),
    )
    render(<NotificationsPage role="CAREGIVER" />)
    expect(screen.queryByRole('button', { name: 'Lỗi gửi' })).toBeNull()
    expect(screen.getByRole('heading', { name: 'Thông báo' })).toBeInTheDocument()
  })
})

describe('NotificationHistory', () => {
  it('FR_037_bangLichSu_ketQuaTungKenh', async () => {
    api.on(
      'GET',
      '/notification-history',
      json(200, [
        {
          requestId: 5,
          triggerSource: 'INCIDENT',
          sourceRecord: { type: 'M05.INCIDENT', id: '9' },
          level: 'URGENT',
          recipient: 'STAFF:301',
          basis: ['AssignedNurse'],
          informationSubject: null,
          q354Case: null,
          receivedAt: '2026-10-23T02:42:00Z',
          sentAt: '2026-10-23T02:42:01Z',
          channels: [
            { channel: 'IN_APP', result: 'SENT', occurredAt: '2026-10-23T02:42:01Z' },
            { channel: 'SMS', result: 'FAILED', occurredAt: '2026-10-23T02:42:02Z' },
          ],
          viewedAt: null,
          acknowledgedAt: null,
          callReminders: [],
          flags: [],
        },
      ]),
    )
    render(<NotificationHistory />)

    const row = (await screen.findAllByRole('row'))[1]
    expect(row).toHaveTextContent('Khẩn cấp')
    expect(row).toHaveTextContent('Nhân viên 301')
    expect(row).toHaveTextContent('Điều dưỡng phụ trách')
    expect(row).toHaveTextContent('Trong ứng dụng: Đã gửi')
    expect(row).toHaveTextContent('Tin nhắn: Thất bại')
    expect(row).toHaveTextContent('Chưa xem')
  })
})

describe('DeliveryErrors', () => {
  it('FR_038_locLoaiLoi_hienLyDo', async () => {
    api.on(
      'GET',
      '/delivery-errors',
      json(200, [
        {
          type: 'REJECTED',
          requestId: 3,
          notificationId: null,
          eventType: 'TEST_EVENT',
          sourceRecordType: 'M05.TEST',
          sourceRecordId: '1',
          eventKey: 'k',
          reason: 'MISSING_CONTENT',
          receivedAt: '2026-10-23T03:05:00Z',
        },
      ]),
    )
    render(<DeliveryErrors />)
    const row = (await screen.findAllByRole('row'))[1]
    expect(row).toHaveTextContent('Bị từ chối')
    expect(row).toHaveTextContent('Thiếu nội dung')

    fireEvent.click(screen.getByRole('button', { name: 'Không có người nhận' }))
    await screen.findAllByRole('row')
    const last = api.fetchMock.mock.calls.map(([u]) => new URL(u, 'http://localhost')).at(-1)
    expect(last.searchParams.get('type')).toBe('NO_RECIPIENT')
  })
})

const CONTACT = {
  id: 4,
  fullName: 'Trần Thị M',
  relationship: null,
  phoneNumber: '0901000123',
  shortContent: 'Bảng chi phí tháng 10 đã chốt',
  status: 'PENDING',
  createdAt: '2026-10-23T03:20:00Z',
}

describe('DirectContacts', () => {
  it('FR_021_hanhChinhGhiKetQua_khongLienHeDuocBatBuocLyDo', async () => {
    api.on('GET', '/direct-contacts', json(200, [CONTACT]))
    api.on('POST', '/direct-contacts/4/results', json(201, null))
    render(<DirectContacts canRecord />)

    const row = (await screen.findAllByRole('row'))[1]
    expect(row).toHaveTextContent('Chờ liên hệ')
    fireEvent.click(within(row).getByRole('button', { name: 'Ghi kết quả' }))
    const dialog = await screen.findByRole('dialog')
    fireEvent.click(within(dialog).getByRole('button', { name: 'Không liên hệ được' }))
    fireEvent.click(within(dialog).getByRole('button', { name: 'Lưu kết quả' }))
    expect(await within(dialog).findByText('Nhập lý do khi không liên hệ được')).toBeInTheDocument()
    expect(api.calls('POST', '/direct-contacts/4/results')).toHaveLength(0)

    fireEvent.change(within(dialog).getByLabelText('Lý do'), { target: { value: 'Gọi ba lần không nghe máy' } })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Lưu kết quả' }))
    await screen.findByText('Đã lưu kết quả liên hệ')
    const [, init] = api.calls('POST', '/direct-contacts/4/results')[0]
    const body = JSON.parse(init.body)
    expect(body.result).toBe('UNREACHABLE')
    expect(body.reason).toBe('Gọi ba lần không nghe máy')
    expect(body.occurredAt).toMatch(/Z$/)
    expect(init.headers['Idempotency-Key']).toMatch(/^[0-9a-f-]{36}$/)
  })

  it('FR_039_quanLyVienChiXem_khongCoNutGhi', async () => {
    api.on('GET', '/direct-contacts', json(200, [CONTACT]))
    render(<DirectContacts canRecord={false} />)
    const row = (await screen.findAllByRole('row'))[1]
    expect(within(row).queryByRole('button', { name: 'Ghi kết quả' })).toBeNull()
  })
})

describe('NotificationChannels', () => {
  const CHANNELS = [
    { code: 'FAMILY_PORTAL', name: 'Cổng người thân', enabled: true, levels: ['URGENT'] },
    { code: 'IN_APP', name: 'Trong ứng dụng', enabled: true, levels: ['URGENT'] },
    { code: 'SMS', name: 'Tin nhắn', enabled: false, levels: ['URGENT'] },
  ]

  it('FR_016_batTinNhan_coLyDo_IfMatch', async () => {
    api.on('GET', '/notification-channels', json(200, CHANNELS, { ETag: '"3"' }))
    api.on('PUT', '/notification-channels/SMS', json(200, null, { ETag: '"4"' }))
    render(<NotificationChannels />)

    const rows = await screen.findAllByRole('row')
    expect(rows[1]).toHaveTextContent('Mọi mức')
    expect(rows[3]).toHaveTextContent('Trung bình, Khẩn cấp')
    expect(within(rows[1]).queryByRole('button', { name: 'Đổi' })).toBeNull()
    fireEvent.click(within(rows[3]).getByRole('button', { name: 'Đổi' }))
    const dialog = await screen.findByRole('dialog')
    fireEvent.click(within(dialog).getByRole('checkbox', { name: 'Bật kênh' }))
    fireEvent.click(within(dialog).getByRole('button', { name: 'Lưu thay đổi' }))
    expect(await within(dialog).findByText('Nhập lý do thay đổi')).toBeInTheDocument()

    fireEvent.change(within(dialog).getByLabelText('Lý do'), { target: { value: 'Đã có nhà cung cấp' } })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Lưu thay đổi' }))
    await screen.findByText('Đã lưu thay đổi kênh')
    const [, init] = api.calls('PUT', '/notification-channels/SMS')[0]
    expect(init.headers['If-Match']).toBe('"3"')
    expect(JSON.parse(init.body)).toMatchObject({ code: 'SMS', enabled: true, reason: 'Đã có nhà cung cấp' })
  })

  it('FR_016_412_baoDaDoi_taiLaiBang', async () => {
    api.on('GET', '/notification-channels', json(200, CHANNELS, { ETag: '"3"' }))
    api.on('PUT', '/notification-channels/SMS', problem(412, 'VERSION_MISMATCH'))
    render(<NotificationChannels />)
    const rows = await screen.findAllByRole('row')
    fireEvent.click(within(rows[3]).getByRole('button', { name: 'Đổi' }))
    const dialog = await screen.findByRole('dialog')
    fireEvent.change(within(dialog).getByLabelText('Lý do'), { target: { value: 'Thử' } })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Lưu thay đổi' }))
    expect(await within(dialog).findByText('Dữ liệu đã thay đổi')).toBeInTheDocument()
    expect(api.calls('GET', '/notification-channels').length).toBeGreaterThan(1)
  })
})

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { NotificationInbox } from './NotificationInbox'

// 009 (bổ sung 2026-11-01): lượt nhắc gọi trên web (FR-027 → FR-031, Q-307): khối "Cần gọi điện" đứng trên bộ lọc, khung
// chi tiết, ghi kết quả gọi; cùng chữ với app nhân viên (009 T022).

function reminder(overrides = {}) {
  return {
    id: 8,
    callChainId: 3,
    personToCall: { type: 'FAMILY_MEMBER', fullName: 'Trần Thị Mai', relationship: null, phoneNumber: '0901234567' },
    residentId: 12,
    contentToReport: [{ occurredAt: '2026-11-01T03:02:00Z', content: 'Té ngã ở phòng 205, đã sơ cứu' }],
    currentlyReminded: 'Lê Văn Hùng',
    dueAt: '2026-11-01T03:15:00Z',
    status: 'AWAITING_RESULT',
    stopReason: null,
    handoffs: [
      { reason: 'INITIAL', occurredAt: '2026-11-01T03:05:00Z' },
      { reason: 'OVERDUE', occurredAt: '2026-11-01T03:10:00Z' },
    ],
    results: [],
    canRecordResult: true,
    ...overrides,
  }
}

function notification(overrides = {}) {
  return {
    id: 5,
    level: 'URGENT',
    eventType: 'CALL_REMINDER',
    title: 'Nhắc gọi điện',
    content: [{ informationType: 'GENERAL', content: 'Có lượt nhắc gọi' }],
    hiddenParts: [],
    identityOnly: false,
    status: 'SENT',
    sentAt: '2026-11-01T03:07:00Z',
    viewedAt: null,
    acknowledgedAt: null,
    acknowledgementMethod: null,
    sourceRecord: { type: 'M13.CALL_REMINDER', id: '8', canOpen: false },
    canAcknowledgeAndTakeCharge: false,
    ...overrides,
  }
}

const EMPTY = { unreadCount: 0, unacknowledgedUrgentCount: 0, items: [], nextCursor: null }

let api

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/me/notifications', json(200, EMPTY))
  api.on('GET', '/residents', problem(403, 'PERMISSION_DENIED'))
  api.on('GET', '/me/call-reminders', () => json(200, [reminder()]))
  api.on('GET', '/call-reminders/8', () => json(200, reminder()))
  api.on('POST', '/call-reminders/8/results', () => new Response(null, { status: 201 }))
})

afterEach(() => {
  vi.unstubAllGlobals()
})

const block = () => screen.findByRole('region', { name: 'Cần gọi điện' })

async function openForm() {
  render(<NotificationInbox />)
  fireEvent.click(within(await block()).getByRole('button', { name: 'Ghi kết quả gọi' }))
  return screen.findByRole('dialog')
}

function sentBody() {
  const [[, init]] = api.calls('POST', '/call-reminders/8/results')
  return { body: JSON.parse(init.body), headers: init.headers }
}

describe('Lượt nhắc gọi', () => {
  it('FR_027_khoiCanGoiDien_dungTrenBoLoc_coSoDienThoai', async () => {
    render(<NotificationInbox />)

    const section = await block()
    expect(section).toHaveTextContent('Gọi Trần Thị Mai')
    expect(section).toHaveTextContent('Đang nhắc Lê Văn Hùng. Hạn 10:15')
    expect(within(section).getByRole('link', { name: '0901234567' })).toHaveAttribute('href', 'tel:0901234567')
    const filter = await screen.findByRole('button', { name: 'Tất cả mức' })
    expect(section.compareDocumentPosition(filter) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  })

  it('FR_027_khongCoLuot_khongCoKhoi', async () => {
    api.on('GET', '/me/call-reminders', json(200, []))
    render(<NotificationInbox />)

    await screen.findByText(/0 chưa xem/)
    expect(screen.queryByRole('region', { name: 'Cần gọi điện' })).toBeNull()
  })

  it('FR_027_khongCoSo_nutMoLuotNhac', async () => {
    const noPhone = reminder({ personToCall: { ...reminder().personToCall, phoneNumber: null } })
    api.on('GET', '/me/call-reminders', json(200, [noPhone]))
    render(<NotificationInbox />)

    const section = await block()
    expect(section).toHaveTextContent('Chưa có số điện thoại')
    expect(within(section).getByRole('button', { name: 'Mở lượt nhắc gọi' })).toBeInTheDocument()
  })

  it('FR_029_khongCoQuyenGhi_nutMoLuotNhac_khungChiXem', async () => {
    const readOnly = reminder({ canRecordResult: false })
    api.on('GET', '/me/call-reminders', json(200, [readOnly]))
    api.on('GET', '/call-reminders/8', json(200, readOnly))
    render(<NotificationInbox />)

    fireEvent.click(within(await block()).getByRole('button', { name: 'Mở lượt nhắc gọi' }))

    expect(await screen.findByText('Bạn chỉ xem lượt nhắc này, không ghi được kết quả.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Ghi kết quả gọi' })).toBeNull()
  })

  it('FR_033_loiDanhSachLuotNhac_khongHongHop', async () => {
    api.on('GET', '/me/call-reminders', problem(500, 'INTERNAL'))
    render(<NotificationInbox />)

    expect(await screen.findByText(/0 chưa xem/)).toBeInTheDocument()
    expect(screen.queryByRole('region', { name: 'Cần gọi điện' })).toBeNull()
  })

  it('FR_027_FR_030a_chiTiet_dungHopDong', async () => {
    render(<NotificationInbox />)
    fireEvent.click(within(await block()).getByRole('button', { name: 'Gọi Trần Thị Mai' }))

    const sheet = await screen.findByRole('dialog')
    expect(await within(sheet).findByText('Chờ kết quả')).toBeInTheDocument()
    expect(sheet).toHaveTextContent('Người thân')
    expect(sheet).toHaveTextContent('Té ngã ở phòng 205, đã sơ cứu')
    expect(sheet).toHaveTextContent('10:05 Nhắc lần đầu')
    expect(sheet).toHaveTextContent('10:10 Chuyển nhắc, quá hạn')
    expect(within(sheet).getByRole('button', { name: 'Ghi kết quả gọi' })).toBeInTheDocument()
  })

  it('FR_030_dungLuot_hienLyDo', async () => {
    api.on(
      'GET',
      '/call-reminders/8',
      json(200, reminder({ status: 'STOPPED', canRecordResult: false, stopReason: 'Người nhận đã tự xác nhận' })),
    )
    render(<NotificationInbox />)
    fireEvent.click(within(await block()).getByRole('button', { name: 'Gọi Trần Thị Mai' }))

    const sheet = await screen.findByRole('dialog')
    expect(await within(sheet).findByText('Đã dừng')).toBeInTheDocument()
    expect(sheet).toHaveTextContent('Người nhận đã tự xác nhận')
  })

  it('FR_029a_sauKhongLienLac_lanGoiBoSung', async () => {
    api.on(
      'GET',
      '/call-reminders/8',
      json(
        200,
        reminder({
          status: 'RESULT_RECORDED',
          results: [{ result: 'NOT_REACHED', calledAt: '2026-11-01T03:09:00Z', reason: 'NO_ANSWER' }],
        }),
      ),
    )
    render(<NotificationInbox />)
    fireEvent.click(within(await block()).getByRole('button', { name: 'Gọi Trần Thị Mai' }))

    const sheet = await screen.findByRole('dialog')
    expect(await within(sheet).findByText('Không liên lạc được, Không nghe máy')).toBeInTheDocument()
    expect(within(sheet).getByRole('button', { name: 'Ghi lần gọi bổ sung' })).toBeInTheDocument()
  })

  it('FR_030_chuaChonKetQua_khongGui', async () => {
    const sheet = await openForm()

    fireEvent.click(await within(sheet).findByRole('button', { name: 'Lưu kết quả' }))

    expect(await within(sheet).findByText('Chọn kết quả gọi.')).toBeInTheDocument()
    expect(api.calls('POST', '/call-reminders/8/results')).toHaveLength(0)
  })

  it('FR_030_daLienLac_batBuocGhiChu', async () => {
    const sheet = await openForm()
    fireEvent.click(await within(sheet).findByRole('button', { name: 'Đã liên lạc được' }))

    fireEvent.click(within(sheet).getByRole('button', { name: 'Lưu kết quả' }))

    expect(await within(sheet).findByText('Cần ghi chú khi đã liên lạc được.')).toBeInTheDocument()
  })

  it('FR_031_daLienLac_guiDuTruong_veChiTiet', async () => {
    const sheet = await openForm()
    fireEvent.click(await within(sheet).findByRole('button', { name: 'Đã liên lạc được' }))
    fireEvent.change(within(sheet).getByLabelText('Người nghe máy (nếu khác người cần gọi)'), {
      target: { value: 'Chồng bà Mai' },
    })
    fireEvent.change(within(sheet).getByLabelText('Ghi chú'), { target: { value: 'Đã báo tin' } })
    api.on('GET', '/call-reminders/8', () => json(200, reminder({ status: 'RESULT_RECORDED', canRecordResult: false })))
    api.on('GET', '/me/call-reminders', json(200, []))

    fireEvent.click(within(sheet).getByRole('button', { name: 'Lưu kết quả' }))

    expect(await within(sheet).findByText('Đã có kết quả')).toBeInTheDocument()
    const { body, headers } = sentBody()
    expect(body).toMatchObject({
      result: 'REACHED',
      calledPhoneNumber: '0901234567',
      answeredBy: 'Chồng bà Mai',
      note: 'Đã báo tin',
    })
    expect(body.calledAt).toMatch(/Z$/)
    expect(headers['Idempotency-Key']).toBeTruthy()
  })

  it('FR_030_khongLienLac_baLyDoCuaSpec_batBuocChon', async () => {
    const sheet = await openForm()
    fireEvent.click(await within(sheet).findByRole('button', { name: 'Không liên lạc được' }))

    for (const reason of ['Không nghe máy', 'Sai số', 'Không có số']) {
      expect(within(sheet).getByRole('button', { name: reason })).toBeInTheDocument()
    }
    expect(within(sheet).queryByRole('button', { name: 'Khác' })).toBeNull()
    expect(within(sheet).queryByLabelText('Người nghe máy (nếu khác người cần gọi)')).toBeNull()

    fireEvent.click(within(sheet).getByRole('button', { name: 'Lưu kết quả' }))
    expect(await within(sheet).findByText('Chọn lý do không liên lạc được.')).toBeInTheDocument()

    fireEvent.click(within(sheet).getByRole('button', { name: 'Sai số' }))
    fireEvent.click(within(sheet).getByRole('button', { name: 'Lưu kết quả' }))
    await within(sheet).findByText('Chờ kết quả')

    const { body } = sentBody()
    expect(body).toMatchObject({ result: 'NOT_REACHED', reason: 'WRONG_NUMBER' })
    expect(body).not.toHaveProperty('answeredBy')
  })

  it('FR_030_loi422_baoTaiO', async () => {
    api.on('POST', '/call-reminders/8/results', problem(422, 'INVALID_VALUE', { detail: 'Ghi chú không hợp lệ' }))
    const sheet = await openForm()
    fireEvent.click(await within(sheet).findByRole('button', { name: 'Đã liên lạc được' }))
    fireEvent.change(within(sheet).getByLabelText('Ghi chú'), { target: { value: 'x' } })

    fireEvent.click(within(sheet).getByRole('button', { name: 'Lưu kết quả' }))

    expect(await within(sheet).findByText('Ghi chú không hợp lệ')).toBeInTheDocument()
    expect(within(sheet).getByRole('button', { name: 'Lưu kết quả' })).toBeInTheDocument()
  })

  it('FR_030_daKetThuc409_baoLoi_taiLai_veChiTiet', async () => {
    api.on(
      'POST',
      '/call-reminders/8/results',
      problem(409, 'CALL_REMINDER_ENDED', { detail: 'Lượt nhắc gọi đã kết thúc' }),
    )
    const sheet = await openForm()
    fireEvent.click(await within(sheet).findByRole('button', { name: 'Đã liên lạc được' }))
    fireEvent.change(within(sheet).getByLabelText('Ghi chú'), { target: { value: 'Đã báo tin' } })
    api.on('GET', '/call-reminders/8', () => json(200, reminder({ status: 'STOPPED', canRecordResult: false })))

    fireEvent.click(within(sheet).getByRole('button', { name: 'Lưu kết quả' }))

    expect(await within(sheet).findByText('Đã dừng')).toBeInTheDocument()
    expect(sheet).toHaveTextContent('Lượt nhắc gọi đã kết thúc')
    expect(within(sheet).queryByRole('button', { name: 'Lưu kết quả' })).toBeNull()
  })

  it('FR_026_thongBaoNhacGoi_coNutMoLuot_vanXacNhanRieng', async () => {
    api.on('GET', '/me/call-reminders', json(200, []))
    api.on('GET', '/me/notifications', json(200, { ...EMPTY, unacknowledgedUrgentCount: 1, items: [notification()] }))
    render(<NotificationInbox />)

    const urgent = await screen.findByRole('region', { name: 'Khẩn cấp chưa xác nhận' })
    expect(within(urgent).getByRole('button', { name: 'Xác nhận đã nhận' })).toBeInTheDocument()
    fireEvent.click(within(urgent).getByRole('button', { name: 'Mở lượt nhắc gọi' }))

    const sheet = await screen.findByRole('dialog')
    expect(await within(sheet).findByText('Chờ kết quả')).toBeInTheDocument()
    expect(api.calls('POST', '/notifications/5/acknowledge')).toHaveLength(0)
  })

  it('T035_chiTietThongBaoNhacGoi_coNutMoLuot', async () => {
    const viewed = notification({ level: 'LOW', title: 'Lượt nhắc gọi' })
    api.on('GET', '/me/call-reminders', json(200, []))
    api.on('GET', '/me/notifications', json(200, { ...EMPTY, items: [viewed] }))
    api.on('GET', '/me/notifications/5', json(200, viewed))
    render(<NotificationInbox />)

    fireEvent.click(await screen.findByRole('button', { name: 'Lượt nhắc gọi' }))
    const sheet = await screen.findByRole('dialog')
    fireEvent.click(await within(sheet).findByRole('button', { name: 'Mở lượt nhắc gọi' }))

    expect(await screen.findByText('Chờ kết quả')).toBeInTheDocument()
    expect(api.calls('GET', '/call-reminders/8')).toHaveLength(1)
  })

  it('T035_thongBaoKhac_khongCoNutMoLuot', async () => {
    const other = notification({
      level: 'LOW',
      title: 'Yêu cầu mới',
      sourceRecord: { type: 'M15.REQUEST', id: '8', canOpen: false },
    })
    api.on('GET', '/me/call-reminders', json(200, []))
    api.on('GET', '/me/notifications', json(200, { ...EMPTY, items: [other] }))
    api.on('GET', '/me/notifications/5', json(200, other))
    render(<NotificationInbox />)

    fireEvent.click(await screen.findByRole('button', { name: 'Yêu cầu mới' }))
    const sheet = await screen.findByRole('dialog')
    await within(sheet).findByText('Gửi lúc')

    expect(within(sheet).queryByRole('button', { name: 'Mở lượt nhắc gọi' })).toBeNull()
  })

  it('FR_016_thongBaoMoiQuaSSE_taiLaiLuotNhac', async () => {
    const listeners = {}
    class FakeEventSource {
      addEventListener(name, fn) {
        listeners[name] = fn
      }
      close() {}
    }
    vi.stubGlobal('EventSource', FakeEventSource)
    render(<NotificationInbox />)
    await block()

    api.on('GET', '/me/call-reminders', json(200, []))
    listeners.notification()

    await vi.waitFor(() => expect(screen.queryByRole('region', { name: 'Cần gọi điện' })).toBeNull())
  })
})

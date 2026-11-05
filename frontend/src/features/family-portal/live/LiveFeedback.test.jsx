import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes } from '@/test/fetchRoutes'
import { LiveFeedbackPage } from './LiveFeedbackPage'

// 012 T030 (US6; FR-058, FR-061, FR-064, FR-066): màn Phản hồi của cổng với phiên thật. Gửi cho người cao tuổi đang
// xem (loại, nhóm nội dung, nội dung bắt buộc); danh sách phản hồi của mình; Đã phản hồi thì Đồng ý đóng hoặc Mở lại
// có lý do (nút theo allowedCommands của chi tiết); trả lời là thông tin sức khỏe mà người xem không có quyền thì hiện dòng khóa.

let api

const VIEWER = { residentId: 5, residentName: 'Nguyễn Thị Lan', perms: { health: false } }

const REPLIED = {
  id: 91,
  residentId: 5,
  senderType: 'FAMILY_MEMBER',
  familyMemberId: 31,
  type: 'QUESTION',
  group: 'HEALTH_MEDICATION',
  content: 'Vì sao đổi thuốc?',
  fileIds: [],
  status: 'REPLIED',
  priority: 'NORMAL',
  dueAt: '2026-11-08T02:00:00Z',
  overdue: false,
  assignee: 'Lê Thu Trang',
}

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/feedback', json(200, [REPLIED]))
  api.on(
    'GET',
    '/feedback/91',
    json(200, {
      ...REPLIED,
      source: 'PORTAL',
      submittedAt: '2026-11-05T02:00:00Z',
      items: [{ id: 1, type: 'CONTENT', content: 'Vì sao đổi thuốc?', fileIds: [], at: '2026-11-05T02:00:00Z' }],
      healthReplyHidden: true,
      allowedCommands: ['agree-to-close', 'reopen'],
    }),
  )
})

afterEach(() => {
  vi.unstubAllGlobals()
})

function bodyOf(call) {
  return JSON.parse(call[1].body)
}

describe('Phản hồi trên cổng', () => {
  it('FR_058_gui_batBuocLoaiNhomNoiDung', async () => {
    api.on('POST', '/feedback', json(201, { id: 92 }))
    render(<LiveFeedbackPage viewer={VIEWER} />)
    await screen.findByText('Vì sao đổi thuốc?')
    // Cùng tiêu đề với app người thân (DESIGN.md 13 điều 7; bố cục duyệt 2026-11-05)
    expect(screen.getByText('Gửi phản hồi về Nguyễn Thị Lan')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Gửi phản hồi' }))
    expect(await screen.findByText('Chọn loại phản hồi')).toBeInTheDocument()
    expect(api.calls('POST', '/feedback')).toHaveLength(0)

    fireEvent.click(screen.getByRole('button', { name: 'Khiếu nại' }))
    fireEvent.click(screen.getByRole('button', { name: 'Chăm sóc, sinh hoạt' }))
    fireEvent.change(screen.getByLabelText('Nội dung'), { target: { value: 'Phòng chưa được dọn' } })
    fireEvent.click(screen.getByRole('button', { name: 'Gửi phản hồi' }))

    await waitFor(() => expect(api.calls('POST', '/feedback')).toHaveLength(1))
    expect(bodyOf(api.calls('POST', '/feedback')[0])).toEqual({
      residentId: 5,
      type: 'COMPLAINT',
      group: 'CARE',
      content: 'Phòng chưa được dọn',
    })
    expect(await screen.findByRole('status')).toHaveTextContent('Đã gửi phản hồi')
  })

  it('FR_058_traLoiSucKhoe_khongCoQuyen_hienDongKhoa', async () => {
    render(<LiveFeedbackPage viewer={VIEWER} />)
    fireEvent.click(await screen.findByRole('button', { name: /Xem trả lời/ }))
    expect(await screen.findByText(/Thông tin sức khỏe chỉ hiện/)).toBeInTheDocument()
  })

  it('FR_066_moLai_batBuocLyDo_dongY_dong', async () => {
    api.on('POST', '/feedback/91/reopen', json(200, { ...REPLIED, status: 'REOPENED', items: [] }))
    api.on('POST', '/feedback/91/agree-to-close', json(200, { ...REPLIED, status: 'CLOSED', items: [] }))
    render(<LiveFeedbackPage viewer={VIEWER} />)
    const item = (await screen.findByText('Vì sao đổi thuốc?')).closest('li')

    fireEvent.click(await within(item).findByRole('button', { name: 'Mở lại' }))
    fireEvent.click(within(item).getByRole('button', { name: 'Gửi mở lại' }))
    expect(await within(item).findByText('Nhập lý do mở lại')).toBeInTheDocument()
    fireEvent.change(within(item).getByLabelText('Lý do mở lại'), { target: { value: 'Chưa rõ' } })
    fireEvent.click(within(item).getByRole('button', { name: 'Gửi mở lại' }))
    await waitFor(() => expect(api.calls('POST', '/feedback/91/reopen')).toHaveLength(1))
    expect(bodyOf(api.calls('POST', '/feedback/91/reopen')[0])).toEqual({ reason: 'Chưa rõ' })
  })

  it('FR_066_khongCoLenhDuocPhep_khongHienNut', async () => {
    api.on(
      'GET',
      '/feedback/91',
      json(200, { ...REPLIED, source: 'PORTAL', items: [], healthReplyHidden: false, allowedCommands: [] }),
    )
    render(<LiveFeedbackPage viewer={VIEWER} />)
    const item = (await screen.findByText('Vì sao đổi thuốc?')).closest('li')

    await waitFor(() => expect(api.calls('GET', '/feedback/91')).toHaveLength(1))
    expect(within(item).queryByRole('button', { name: 'Mở lại' })).not.toBeInTheDocument()
    expect(within(item).queryByRole('button', { name: 'Đồng ý đóng' })).not.toBeInTheDocument()
  })
})

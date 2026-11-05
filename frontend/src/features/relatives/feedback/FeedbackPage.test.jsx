import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { FeedbackPage } from './FeedbackPage'

// 012 T030 (US6; FR-058 → FR-066, BR-M10-05): màn "Phản hồi" của nhân viên. Bảng phản hồi trong phạm vi, lọc theo
// trạng thái và "Quá hạn"; khung chi tiết có nội dung, các mục, lệnh theo trạng thái (Nhận xử lý, Trả lời có hướng xử lý
// và nội dung, Đổi mức, Giao lại kèm lý do); người gửi không có quyền xem sức khỏe thì cảnh báo lúc soạn trả lời.

let api

const COMPLAINT = {
  id: 91,
  residentId: 5,
  senderType: 'FAMILY_MEMBER',
  familyMemberId: 31,
  type: 'COMPLAINT',
  group: 'CARE',
  content: 'Phòng P201 không được dọn hai ngày',
  fileIds: [],
  status: 'NEW',
  priority: 'HIGH',
  dueAt: '2026-11-06T02:00:00Z',
  overdue: true,
  assignee: 'Lê Thu Trang',
}

const QUESTION = {
  ...COMPLAINT,
  id: 92,
  type: 'QUESTION',
  group: 'HEALTH_MEDICATION',
  content: 'Vì sao đổi thuốc?',
  status: 'IN_PROGRESS',
  priority: 'NORMAL',
  overdue: false,
}

function detail(f, extra = {}) {
  return {
    ...f,
    source: 'PORTAL',
    submittedAt: '2026-11-05T02:00:00Z',
    items: [{ id: 1, type: 'CONTENT', content: f.content, fileIds: [], byAccountId: 7, at: '2026-11-05T02:00:00Z' }],
    senderHealthRightEffective: true,
    ...extra,
  }
}

beforeEach(() => {
  api = mockRoutes()
  api.on(
    'GET',
    '/me/permissions',
    json(200, [
      { function: 'FEEDBACK', operation: 'VIEW' },
      { function: 'FEEDBACK', operation: 'UPDATE' },
    ]),
  )
  api.on('GET', '/feedback', json(200, [COMPLAINT, QUESTION]))
  api.on(
    'GET',
    '/residents/5',
    json(200, { id: 5, profileCode: 'NCT-0005', personalInfo: { fullName: 'Nguyễn Thị Lan' } }),
  )
  api.on('GET', '/feedback/91', json(200, detail(COMPLAINT)))
  api.on('GET', '/feedback/92', json(200, detail(QUESTION, { senderHealthRightEffective: false })))
})

afterEach(() => {
  vi.unstubAllGlobals()
})

function renderPage() {
  render(
    <PermissionsProvider>
      <FeedbackPage />
    </PermissionsProvider>,
  )
  return screen.findByRole('table', { name: 'Phản hồi' })
}

function bodyOf(call) {
  return JSON.parse(call[1].body)
}

describe('Phản hồi của người thân', () => {
  it('BR_M10_05_bangPhanHoi_quaHanChuDo_locQuaHan', async () => {
    const table = await renderPage()

    const row = within(table).getByRole('row', { name: /Phòng P201/ })
    expect(row).toHaveTextContent('Khiếu nại')
    expect(row).toHaveTextContent('Quá hạn')
    expect(row).toHaveTextContent('Lê Thu Trang')
    expect(await within(row).findByText('Nguyễn Thị Lan')).toBeInTheDocument()

    fireEvent.click(
      within(screen.getByRole('group', { name: 'Lọc phản hồi' })).getByRole('button', { name: 'Quá hạn' }),
    )
    await waitFor(() => {
      const calls = api.calls('GET', '/feedback')
      const query = new URL(calls[calls.length - 1][0], 'http://x').searchParams
      expect(query.get('overdue')).toBe('true')
    })
  })

  it('FR_066_nhanXuLy_goiLenh', async () => {
    api.on('POST', '/feedback/91/start-handling', json(200, detail({ ...COMPLAINT, status: 'IN_PROGRESS' })))
    const table = await renderPage()

    fireEvent.click(within(table).getByRole('button', { name: /Phòng P201/ }))
    fireEvent.click(await screen.findByRole('button', { name: 'Nhận xử lý' }))

    await waitFor(() => expect(api.calls('POST', '/feedback/91/start-handling')).toHaveLength(1))
    expect(await screen.findByRole('status')).toHaveTextContent('Đã nhận xử lý')
  })

  it('FR_058_traLoi_canhBaoQuyenSucKhoe_batBuocHaiO', async () => {
    api.on('POST', '/feedback/92/reply', json(200, detail({ ...QUESTION, status: 'REPLIED' })))
    const table = await renderPage()

    fireEvent.click(within(table).getByRole('button', { name: /Vì sao đổi thuốc/ }))
    expect(await screen.findByText(/Người gửi không có quyền xem sức khỏe/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Gửi trả lời' }))
    expect(await screen.findByText('Nhập hướng xử lý')).toBeInTheDocument()
    expect(api.calls('POST', '/feedback/92/reply')).toHaveLength(0)

    fireEvent.change(screen.getByLabelText('Hướng xử lý'), { target: { value: 'Mời gia đình gặp Bác sĩ' } })
    fireEvent.change(screen.getByLabelText('Nội dung trả lời'), { target: { value: 'Mời liên hệ trực tiếp' } })
    fireEvent.click(screen.getByRole('button', { name: 'Gửi trả lời' }))

    await waitFor(() => expect(api.calls('POST', '/feedback/92/reply')).toHaveLength(1))
    expect(bodyOf(api.calls('POST', '/feedback/92/reply')[0])).toEqual({
      handlingDirection: 'Mời gia đình gặp Bác sĩ',
      content: 'Mời liên hệ trực tiếp',
    })
  })

  it('FR_062_doiMuc_batBuocLyDo', async () => {
    api.on('POST', '/feedback/92/change-priority', json(200, detail({ ...QUESTION, priority: 'HIGH' })))
    const table = await renderPage()

    fireEvent.click(within(table).getByRole('button', { name: /Vì sao đổi thuốc/ }))
    fireEvent.click(await screen.findByRole('button', { name: 'Đổi mức' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu mức mới' }))
    expect(await screen.findByText('Nhập lý do')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Ảnh hưởng sức khỏe' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu mức mới' }))
    await waitFor(() => expect(api.calls('POST', '/feedback/92/change-priority')).toHaveLength(1))
    expect(bodyOf(api.calls('POST', '/feedback/92/change-priority')[0])).toEqual({
      priority: 'HIGH',
      reason: 'Ảnh hưởng sức khỏe',
    })
  })

  it('FR_067_khongPhaiNguoiPhuTrach_hienLoi', async () => {
    api.on(
      'POST',
      '/feedback/91/start-handling',
      problem(403, 'PERMISSION_DENIED', { detail: 'Chỉ người phụ trách xử lý phản hồi' }),
    )
    const table = await renderPage()

    fireEvent.click(within(table).getByRole('button', { name: /Phòng P201/ }))
    fireEvent.click(await screen.findByRole('button', { name: 'Nhận xử lý' }))
    expect(await screen.findByText('Chỉ người phụ trách xử lý phản hồi')).toBeInTheDocument()
  })
})

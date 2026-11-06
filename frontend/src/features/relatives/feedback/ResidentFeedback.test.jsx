import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes } from '@/test/fetchRoutes'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { FeedbackPage } from './FeedbackPage'

// 012 T072 (US9; FR-058a, FR-058b, FR-066, BR-M10-12; bố cục duyệt 2026-11-05): màn Phản hồi có phản hồi của người cao
// tuổi. Hành chính, Điều dưỡng ghi lời người cao tuổi (nút theo quyền Tạo); cột Người gửi ghi "Chính người cao tuổi";
// nhóm sức khỏe nhắc trả lời trực tiếp; Mong muốn về người đến thăm, người đón gắn mong muốn đã ghi hoặc ghi mới, Mong
// muốn việc khác nhắc ghi sở thích; Đổi loại có lý do; ghi thay Đồng ý đóng, Mở lại khi backend cho phép.

let api

const BASE = {
  residentId: 5,
  senderType: 'RESIDENT',
  familyMemberId: null,
  familyMemberName: null,
  fileIds: [],
  priority: 'NORMAL',
  dueAt: '2026-11-08T02:00:00Z',
  overdue: false,
  assignee: 'Lê Thu Trang',
  submittedAt: '2026-11-05T02:10:00Z',
}

const FROM_APP = {
  ...BASE,
  id: 101,
  type: 'QUESTION',
  group: 'HEALTH_MEDICATION',
  content: 'Vì sao đổi thuốc tối?',
  status: 'IN_PROGRESS',
}
const ON_BEHALF = {
  ...BASE,
  id: 102,
  type: 'SUGGESTION',
  group: 'CARE',
  content: 'Nước tắm hơi lạnh',
  status: 'REPLIED',
}
const WISH_VISITOR = {
  ...BASE,
  id: 103,
  type: 'WISH',
  group: 'OTHER',
  wishTopic: 'VISITORS_PICKERS',
  content: 'Không muốn gặp ông Nguyễn Văn X',
  status: 'IN_PROGRESS',
}
const WISH_OTHER = { ...WISH_VISITOR, id: 104, group: 'CARE', wishTopic: 'OTHER', content: 'Muốn nghe nhạc buổi tối' }

function detail(f, source, allowedCommands, extra = {}) {
  return {
    ...f,
    source,
    allowedCommands,
    items: [{ id: 1, type: 'CONTENT', content: f.content, fileIds: [], byAccountId: 7, at: f.submittedAt }],
    senderHealthRightEffective: false,
    ...extra,
  }
}

function permissions(...operations) {
  return json(
    200,
    operations.map((operation) => ({ function: 'FEEDBACK', operation })),
  )
}

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/me/permissions', permissions('VIEW', 'UPDATE'))
  api.on('GET', '/feedback', json(200, [FROM_APP, ON_BEHALF, WISH_VISITOR, WISH_OTHER]))
  api.on('GET', '/residents/5', json(200, { id: 5, profileCode: 'NCT-0005', personalInfo: { fullName: 'Nguyễn Thị Lan' } }))
  api.on('GET', '/feedback/101', json(200, detail(FROM_APP, 'RESIDENT_CHANNEL', ['reply', 'change-priority', 'add-note'])))
  api.on('GET', '/feedback/102', json(200, detail(ON_BEHALF, 'RECORDED_DIRECTLY', ['add-note', 'agree-to-close', 'reopen'])))
  api.on(
    'GET',
    '/feedback/103',
    json(200, detail(WISH_VISITOR, 'RESIDENT_CHANNEL', ['reply', 'change-priority', 'change-type', 'link-opinion', 'add-note'])),
  )
  api.on(
    'GET',
    '/feedback/104',
    json(200, detail(WISH_OTHER, 'RESIDENT_CHANNEL', ['reply', 'change-priority', 'change-type', 'link-opinion', 'add-note'])),
  )
  api.on(
    'GET',
    '/residents/5/visit-pickup-wishes',
    json(200, {
      effective: [
        {
          id: 71,
          relationshipId: null,
          fullName: 'Nguyễn Văn X',
          type: 'VISIT',
          wanted: false,
          source: { type: 'RESIDENT' },
          at: '2026-11-05T03:00:00Z',
          status: 'ACTIVE',
        },
        {
          id: 72,
          relationshipId: null,
          fullName: 'Trần Văn Y',
          type: 'PICKUP',
          wanted: true,
          source: { type: 'FAMILY_MEMBER', familyMemberId: 31 },
          at: '2026-11-04T03:00:00Z',
          status: 'ACTIVE',
        },
      ],
      history: [],
      conflictingRights: [],
    }),
  )
  api.on('GET', '/residents/5/relationships', json(200, []))
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

async function open(table, content) {
  fireEvent.click(within(table).getByRole('button', { name: new RegExp(content) }))
}

function bodyOf(call) {
  return JSON.parse(call[1].body)
}

describe('Phản hồi của người cao tuổi trên màn nhân viên', () => {
  it('FR_058a_tieuDeVaNguoiGui_chinhNguoiCaoTuoi', async () => {
    const table = await renderPage()

    expect(screen.getByRole('heading', { name: 'Phản hồi' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Ghi lời người cao tuổi' })).not.toBeInTheDocument()
    const row = within(table).getByRole('row', { name: /Vì sao đổi thuốc tối/ })
    expect(row).toHaveTextContent('Chính người cao tuổi')
    expect(within(table).getByRole('row', { name: /Muốn nghe nhạc/ })).toHaveTextContent('Mong muốn')

    await open(table, 'Nước tắm hơi lạnh')
    expect(await screen.findByText(/Nhân viên ghi thay theo lời người cao tuổi/)).toBeInTheDocument()
  })

  it('FR_058a_ghiLoiNguoiCaoTuoi_mongMuon', async () => {
    api.on('GET', '/me/permissions', permissions('VIEW', 'CREATE', 'UPDATE'))
    api.on(
      'GET',
      '/residents',
      json(200, { items: [{ id: 5, profileCode: 'NCT-0005', personalInfo: { fullName: 'Nguyễn Thị Lan' } }] }),
    )
    api.on('POST', '/feedback', json(201, { id: 110 }))
    await renderPage()

    fireEvent.click(await screen.findByRole('button', { name: 'Ghi lời người cao tuổi' }))
    const form = await screen.findByRole('dialog', { name: 'Ghi phản hồi theo lời người cao tuổi' })
    fireEvent.click(within(form).getByRole('button', { name: 'Lưu phản hồi' }))
    expect(await within(form).findByText('Chọn người cao tuổi')).toBeInTheDocument()
    expect(api.calls('POST', '/feedback')).toHaveLength(0)

    const resident = within(form).getByLabelText('Người cao tuổi')
    await within(resident).findByRole('option', { name: /Nguyễn Thị Lan/ })
    fireEvent.change(resident, { target: { value: '5' } })
    fireEvent.click(within(form).getByRole('button', { name: 'Mong muốn' }))
    fireEvent.click(within(form).getByRole('button', { name: 'Người đến thăm, người đón' }))
    fireEvent.change(within(form).getByLabelText('Nội dung'), { target: { value: 'Không muốn gặp ông X' } })
    fireEvent.click(within(form).getByRole('button', { name: 'Lưu phản hồi' }))

    await waitFor(() => expect(api.calls('POST', '/feedback')).toHaveLength(1))
    expect(bodyOf(api.calls('POST', '/feedback')[0])).toEqual({
      residentId: 5,
      senderType: 'RESIDENT',
      type: 'WISH',
      group: 'OTHER',
      wishTopic: 'VISITORS_PICKERS',
      content: 'Không muốn gặp ông X',
    })
    expect(await screen.findByText('Đã ghi phản hồi theo lời người cao tuổi')).toBeInTheDocument()
  })

  it('FR_058a_sucKhoe_nhacTraLoiTrucTiep', async () => {
    const table = await renderPage()
    await open(table, 'Vì sao đổi thuốc tối')
    expect(await screen.findByText(/Người cao tuổi chỉ thấy phần chung của câu trả lời/)).toBeInTheDocument()
    expect(screen.getByText(/Nguyễn Thị Lan tự gửi qua ứng dụng/)).toBeInTheDocument()
  })

  it('BR_M10_12_ganMongMuonDaGhi_chiNguonNguoiCaoTuoi', async () => {
    api.on('POST', '/feedback/103/link-opinion', json(200, detail(WISH_VISITOR, 'RESIDENT_CHANNEL', ['reply'])))
    const table = await renderPage()
    await open(table, 'Không muốn gặp ông Nguyễn Văn X')

    expect(await screen.findByRole('button', { name: 'Ghi mong muốn mới' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Gắn mong muốn đã ghi' }))
    const select = await screen.findByLabelText('Mong muốn của người cao tuổi đã ghi')
    await within(select).findByRole('option', { name: /Nguyễn Văn X/ })
    expect(within(select).queryByRole('option', { name: /Trần Văn Y/ })).not.toBeInTheDocument()
    fireEvent.change(select, { target: { value: '71' } })
    fireEvent.click(screen.getByRole('button', { name: 'Gắn vào phản hồi' }))

    await waitFor(() => expect(api.calls('POST', '/feedback/103/link-opinion')).toHaveLength(1))
    expect(bodyOf(api.calls('POST', '/feedback/103/link-opinion')[0])).toEqual({
      opinion: { itemType: 'VISIT_PICKUP_WISH', itemId: 71 },
    })
  })

  it('BR_M10_12_mongMuonViecKhac_nhacGhiSoThich', async () => {
    const table = await renderPage()
    await open(table, 'Muốn nghe nhạc buổi tối')
    expect(await screen.findByText(/Ghi sở thích ở hồ sơ hoạt động/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Gắn mong muốn đã ghi' })).not.toBeInTheDocument()
  })

  it('FR_066_doiLoai_batBuocLyDo', async () => {
    api.on('POST', '/feedback/104/change-type', json(200, detail({ ...WISH_OTHER, type: 'QUESTION' }, 'RESIDENT_CHANNEL', [])))
    const table = await renderPage()
    await open(table, 'Muốn nghe nhạc buổi tối')

    fireEvent.click(await screen.findByRole('button', { name: 'Đổi loại' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu loại mới' }))
    expect(await screen.findByText('Nhập lý do')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Là câu hỏi giờ nghe nhạc' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu loại mới' }))

    await waitFor(() => expect(api.calls('POST', '/feedback/104/change-type')).toHaveLength(1))
    expect(bodyOf(api.calls('POST', '/feedback/104/change-type')[0])).toEqual({
      newType: 'QUESTION',
      reason: 'Là câu hỏi giờ nghe nhạc',
    })
  })

  it('FR_058a_ghiThayDongYDong', async () => {
    api.on('POST', '/feedback/102/agree-to-close', json(200, detail({ ...ON_BEHALF, status: 'CLOSED' }, 'RECORDED_DIRECTLY', [])))
    const table = await renderPage()
    await open(table, 'Nước tắm hơi lạnh')

    expect(await screen.findByRole('button', { name: 'Ghi mở lại' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Ghi đồng ý đóng' }))
    await waitFor(() => expect(api.calls('POST', '/feedback/102/agree-to-close')).toHaveLength(1))
  })
})

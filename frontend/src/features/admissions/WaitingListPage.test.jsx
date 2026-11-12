import { beforeEach, describe, expect, it } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { WaitingListPage } from './WaitingListPage'

// 004 T028 (US2; FR-007, FR-009, FR-012 → FR-014, FR-017, BR-M02-01, BR-M02-10): danh sách chờ ưu tiên trước, "từng lưu
// trú" chỉ hiển thị; hồ sơ phù hợp với giường Trống không chọn sẵn, bỏ qua thứ tự phải có lý do; gỡ dấu Ưu tiên cần lý do.

let api

const FIRST = {
  id: 21,
  residentId: 5,
  fullName: 'Nguyễn Thị Lan',
  applicationDate: '2026-10-01',
  residenceType: 'LONG_TERM_RESIDENTIAL',
  careLevel: 'REGULAR',
  notAssessed: false,
  priority: true,
  priorityReason: 'Hoàn cảnh khó khăn',
  previouslyResided: true,
  status: 'WAITING',
}
const SECOND = { ...FIRST, id: 22, residentId: 6, fullName: 'Lê Văn Hải', priority: false, previouslyResided: false }
const CONTACTED = {
  ...FIRST,
  id: 23,
  residentId: 7,
  fullName: 'Phạm Thị Mai',
  priority: false,
  status: 'CONTACTED',
  heldBedId: 9,
}

function renderPage() {
  return render(
    <PermissionsProvider>
      <WaitingListPage />
    </PermissionsProvider>,
  )
}

beforeEach(() => {
  api = mockRoutes()
  api.on(
    'GET',
    '/me/permissions',
    json(
      200,
      ['VIEW', 'CREATE', 'UPDATE'].map((operation) => ({ function: 'WAITING_LIST', operation })),
    ),
  )
  api.on('GET', '/waiting-list-entries', json(200, [FIRST, SECOND, CONTACTED]))
  api.on(
    'GET',
    '/beds',
    json(200, {
      beds: [
        { id: 9, code: 'A-101-1' },
        { id: 10, code: 'A-101-2' },
      ],
    }),
  )
  api.on(
    'GET',
    '/beds/10/matching-entries',
    json(200, { bedId: 10, vacancyStart: '2026-11-05T01:00:00Z', entries: [FIRST, SECOND] }),
  )
  api.on('GET', '/admission-applications', json(200, []))
})

describe('WaitingListPage', () => {
  it('FR_009_uuTienTruoc_hienTungLuuTruKhongDanhDauSan', async () => {
    renderPage()

    const first = await screen.findByRole('button', { name: 'Nguyễn Thị Lan' })
    const second = screen.getByRole('button', { name: 'Lê Văn Hải' })
    expect(first.compareDocumentPosition(second) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(screen.getByText('Ưu tiên')).toBeInTheDocument()
    expect(screen.getByText('Từng lưu trú tại viện')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Đang chờ 2' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Đã liên hệ 1' })).toBeInTheDocument()
  })

  it('FR_012_giuongTrong_hienHoSoPhuHop_khongChonSan_boQuaThuTuCanLyDo', async () => {
    api.on(
      'POST',
      '/waiting-list-entries/22/record-contacted',
      json(200, { ...SECOND, status: 'CONTACTED', heldBedId: 10 }),
    )
    renderPage()
    await screen.findByRole('button', { name: 'Nguyễn Thị Lan' })

    fireEvent.click(screen.getByRole('button', { name: 'Hồ sơ phù hợp với giường trống' }))
    const panel = await screen.findByRole('dialog')
    fireEvent.change(await within(panel).findByLabelText('Giường trống'), { target: { value: '10' } })
    expect(await within(panel).findByText('Nguyễn Thị Lan')).toBeInTheDocument()
    expect(within(panel).queryByRole('button', { name: 'Ghi nhận đã liên hệ' })).not.toBeInTheDocument()

    fireEvent.click(within(panel).getByRole('button', { name: 'Chọn Lê Văn Hải' }))
    fireEvent.click(within(panel).getByRole('button', { name: 'Ghi nhận đã liên hệ' }))
    expect(await within(panel).findByText('Nhập lý do bỏ qua thứ tự')).toBeInTheDocument()
    expect(api.calls('POST', '/waiting-list-entries/22/record-contacted')).toHaveLength(0)

    fireEvent.change(within(panel).getByLabelText('Lý do bỏ qua thứ tự'), {
      target: { value: 'Gia đình người đầu chưa liên lạc được' },
    })
    fireEvent.click(within(panel).getByRole('button', { name: 'Ghi nhận đã liên hệ' }))
    await waitFor(() => expect(api.calls('POST', '/waiting-list-entries/22/record-contacted')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/waiting-list-entries/22/record-contacted')[0][1].body)).toEqual({
      bedId: 10,
      outOfOrderReason: 'Gia đình người đầu chưa liên lạc được',
    })
  })

  it('BR_M02_10_goDauUuTien_canLyDo_roiGuiPriorityFalse', async () => {
    api.on('POST', '/waiting-list-entries/21/mark-priority', json(200, { ...FIRST, priority: false }))
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Nguyễn Thị Lan' }))

    fireEvent.click(await screen.findByRole('button', { name: 'Gỡ dấu Ưu tiên' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu dấu Ưu tiên' }))
    expect(await screen.findByText('Nhập lý do')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Đã giải quyết' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu dấu Ưu tiên' }))
    await waitFor(() => expect(api.calls('POST', '/waiting-list-entries/21/mark-priority')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/waiting-list-entries/21/mark-priority')[0][1].body)).toEqual({
      priority: false,
      reason: 'Đã giải quyết',
    })
  })

  it('FR_014_409_hienDieuKienChuaThoaTrongKhung', async () => {
    api.on(
      'POST',
      '/waiting-list-entries/21/confirm-still-needed',
      problem(409, 'WAITING_ENTRY_NOT_WAITING', { detail: 'Hồ sơ không còn đang chờ' }),
    )
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Nguyễn Thị Lan' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Xác nhận còn nhu cầu' }))

    expect(await screen.findByText('Hồ sơ không còn đang chờ')).toBeInTheDocument()
  })

  it('FR_018_403_hienKhongCoQuyenXem', async () => {
    api.on('GET', '/waiting-list-entries', problem(403, 'PERMISSION_DENIED'))
    renderPage()
    expect(await screen.findByText(/Bạn không có quyền xem/)).toBeInTheDocument()
  })
})

// 016 T021 (Q12, chủ dự án duyệt 2026-11-12): mở thẳng một hồ sơ chờ từ dòng chi tiết báo cáo
it('016_Q12_moTheoIdTuDuongDan', async () => {
  render(
    <PermissionsProvider>
      <WaitingListPage entryId={22} />
    </PermissionsProvider>,
  )
  expect(await screen.findByRole('dialog')).toBeInTheDocument()
})

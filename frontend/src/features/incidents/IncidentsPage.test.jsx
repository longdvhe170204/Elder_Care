import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { IncidentsPage } from './IncidentsPage'

// 007 US4 (T037 phần web): danh sách sự cố từ api-007 `GET /incidents`, tên loại từ `/incident-types`, họ tên từ
// api-001; chi tiết có diễn biến, nút lệnh theo `allowedCommands`; ghi diễn biến cần loại và nội dung; đóng cần kết
// quả; ghi nhận sự cố mới cần người cao tuổi, loại, mô tả và lý do khi chọn mức thấp hơn mặc định (FR-042).

let api

const TYPES = [
  {
    id: 31,
    code: 'FALL',
    name: 'Ngã',
    defaultLevel: 'CRITICAL',
    nonHealth: false,
    allowsNoResident: false,
    status: 'ACTIVE',
  },
  {
    id: 32,
    code: 'BEHAVIOR',
    name: 'Kích động',
    defaultLevel: 'MEDIUM',
    nonHealth: false,
    allowsNoResident: false,
    status: 'ACTIVE',
  },
]
const PROGRESSION_TYPES = [{ id: 51, code: 'CALL_SUPPORT', name: 'Gọi hỗ trợ', status: 'ACTIVE' }]
const content = (residentId, typeId, level) => ({
  residentId,
  incidentTypeId: typeId,
  discoveredAt: '2026-11-08T02:00:00Z',
  description: 'Ngã trong phòng tắm',
  level,
})
const NEW_INCIDENT = {
  id: 11,
  content: content(9, 31, 'CRITICAL'),
  currentTypeId: 31,
  status: 'NEW',
  level: 'CRITICAL',
  allowedCommands: ['receive', 'progressions', 'change-level'],
  progressions: [],
}
const HANDLING = {
  id: 12,
  content: content(10, 32, 'MEDIUM'),
  currentTypeId: 32,
  status: 'IN_PROGRESS',
  level: 'MEDIUM',
  assignee: 'Trần Thu Hà',
  allowedCommands: ['progressions', 'change-level', 'move-to-monitoring', 'close'],
  progressions: [{ id: 1, progressionTypeId: 51, content: 'Đã gọi điều dưỡng', occurredAt: '2026-11-08T02:10:00Z' }],
}
const CLOSED = { ...HANDLING, id: 13, status: 'CLOSED', allowedCommands: [], resolution: 'Ổn định' }
const VOIDED = { ...HANDLING, id: 14, status: 'VOIDED', allowedCommands: [] }

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/incidents', json(200, [HANDLING, NEW_INCIDENT, CLOSED, VOIDED]))
  api.on('GET', '/incidents/12', json(200, HANDLING))
  api.on('GET', '/incidents/11', json(200, NEW_INCIDENT))
  api.on('GET', '/incident-types', json(200, TYPES))
  api.on('GET', '/progression-types', json(200, PROGRESSION_TYPES))
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
  api.on(
    'GET',
    '/residents',
    json(200, { items: [{ id: 9, profileCode: 'HS-0009', personalInfo: { fullName: 'Trần Thị Mai' } }] }),
  )
})

afterEach(() => vi.unstubAllGlobals())

async function rows() {
  const table = await screen.findByRole('table')
  await within(table).findByText('Trần Thị Mai')
  return within(table).getAllByRole('row').slice(1)
}

describe('IncidentsPage', () => {
  it('FR_045_dangMo_khanCapLenDau_tenLoaiVaHoTen', async () => {
    render(<IncidentsPage />)
    const list = await rows()
    expect(list).toHaveLength(2)
    expect(within(list[0]).getByText('Trần Thị Mai')).toBeTruthy()
    expect(within(list[0]).getByText('Ngã')).toBeTruthy()
    expect(within(list[0]).getByText('Khẩn cấp')).toBeTruthy()
    expect(within(list[1]).getByText('Đang xử lý')).toBeTruthy()
    expect(within(list[1]).getByText('Trần Thu Hà')).toBeTruthy()
  })

  it('FR_046_nhomDaKetThuc_coDaHuy', async () => {
    render(<IncidentsPage />)
    await rows()
    fireEvent.click(screen.getByRole('button', { name: /^Đã kết thúc/ }))
    const table = screen.getByRole('table')
    await waitFor(() => expect(within(table).getAllByRole('row')).toHaveLength(3))
    expect(within(table).getByText('Đã hủy')).toBeTruthy()
  })

  it('FR_045_ghiDienBien_canLoaiVaNoiDung_guiIdempotencyKey', async () => {
    api.on('POST', '/incidents/12/progressions', json(201, {}))
    render(<IncidentsPage />)
    const list = await rows()
    fireEvent.click(within(list[1]).getByRole('button', { name: /Lê Văn Bình/ }))
    expect(await screen.findByText('Đã gọi điều dưỡng')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Ghi diễn biến' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu diễn biến' }))
    expect(await screen.findByText('Chọn loại diễn biến')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Gọi hỗ trợ' }))
    fireEvent.change(screen.getByLabelText('Nội dung'), { target: { value: 'Đã báo bác sĩ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu diễn biến' }))
    await waitFor(() => expect(api.calls('POST', '/incidents/12/progressions')).toHaveLength(1))
    const [, init] = api.calls('POST', '/incidents/12/progressions')[0]
    expect(new Headers(init.headers).get('Idempotency-Key')).toBeTruthy()
    expect(JSON.parse(init.body)).toEqual({ progressionTypeId: 51, content: 'Đã báo bác sĩ' })
  })

  it('FR_045_dongSuCo_canKetQua_409HienDieuKien', async () => {
    api.on(
      'POST',
      '/incidents/12/close',
      problem(409, 'CONDITIONS_NOT_MET', {
        unmetConditions: [{ code: 'X', description: 'Chưa có diễn biến đánh giá' }],
      }),
    )
    render(<IncidentsPage />)
    const list = await rows()
    fireEvent.click(within(list[1]).getByRole('button', { name: /Lê Văn Bình/ }))
    fireEvent.click(await screen.findByRole('button', { name: 'Đóng sự cố' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu và đóng sự cố' }))
    expect(await screen.findByText('Nhập kết quả xử lý')).toBeTruthy()
    fireEvent.change(screen.getByLabelText('Kết quả xử lý'), { target: { value: 'Ổn định' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu và đóng sự cố' }))
    expect(await screen.findByText('Chưa có diễn biến đánh giá')).toBeTruthy()
  })

  it('FR_042_ghiNhan_mucThapHonMacDinh_batBuocLyDo', async () => {
    api.on('POST', '/incidents', json(201, { incident: NEW_INCIDENT }))
    render(<IncidentsPage />)
    await rows()
    fireEvent.click(screen.getByRole('button', { name: 'Ghi nhận sự cố' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Trần Thị Mai, HS-0009' }))
    fireEvent.click(screen.getByRole('button', { name: 'Ngã' }))
    fireEvent.click(screen.getByRole('button', { name: 'Trung bình' }))
    fireEvent.change(screen.getByLabelText('Mô tả'), { target: { value: 'Trượt chân' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu sự cố' }))
    expect(await screen.findByText('Nhập lý do chọn mức thấp hơn mặc định')).toBeTruthy()
    fireEvent.change(screen.getByLabelText('Lý do chọn mức thấp hơn'), { target: { value: 'Không chấn thương' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu sự cố' }))
    await waitFor(() => expect(api.calls('POST', '/incidents')).toHaveLength(1))
    const body = JSON.parse(api.calls('POST', '/incidents')[0][1].body)
    expect(body).toMatchObject({
      residentId: 9,
      incidentTypeId: 31,
      level: 'MEDIUM',
      description: 'Trượt chân',
      lowerLevelReason: 'Không chấn thương',
    })
    expect(body.discoveredAt).toMatch(/\+07:00$/)
  })

  it('FR_080_403_khongCoQuyen', async () => {
    api.on('GET', '/incidents', problem(403, 'PERMISSION_DENIED'))
    render(<IncidentsPage />)
    expect(await screen.findByText(/không có quyền/i)).toBeTruthy()
  })
})

// 016 T021 (Q12, chủ dự án duyệt 2026-11-12): mở thẳng một sự cố từ dòng chi tiết báo cáo
it('016_Q12_moTheoIdTuDuongDan', async () => {
  render(<IncidentsPage incidentId={12} />)
  expect(await screen.findByRole('dialog')).toBeInTheDocument()
})

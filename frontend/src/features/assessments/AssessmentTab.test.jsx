import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { AssessmentTab } from './AssessmentTab'

// 001 T042 (US3; FR-024 → FR-029, FR-033, FR-036): tab Đánh giá trong khung chi tiết hồ sơ. Điều dưỡng lập và sửa nháp,
// chỉ Bác sĩ xác nhận; thiếu nhận xét hoặc mức báo tại ô; không có ô điểm hay mức đề xuất; dữ liệu chưa kết nối (chỉ số
// đo) hiện đúng trạng thái.

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

const NURSE = ['CREATE_ASSESSMENT']
const DOCTOR = ['CREATE_ASSESSMENT', 'CONFIRM_ASSESSMENT']

function assessment(overrides = {}) {
  return {
    id: 7,
    type: 'INITIAL',
    basis: null,
    createdBy: 'Điều dưỡng Hoa',
    comment: null,
    careNote: null,
    careLevel: null,
    careNeeds: [],
    proposedResidenceType: null,
    status: 'DRAFT',
    confirmedBy: null,
    confirmedAt: null,
    corrected: false,
    ...overrides,
  }
}

function detail(overrides = {}) {
  return {
    ...assessment(overrides),
    reference: {
      initialHealthRecord: { id: 1, confirmed: true, bloodType: 'O+', mobility: 'Đi lại cần gậy' },
      activeHealthItems: [
        { id: 3, type: 'ALLERGY', allergen: { id: 1, name: 'Penicillin' }, severity: 'Nặng', freeText: null },
      ],
      latestVitalSigns: [],
    },
  }
}

function routes({ list = [], level = null, draft = null, failList = null } = {}) {
  api.on('GET', '/residents/5/care-level', level ? json(200, level) : problem(404, 'NOT_FOUND'))
  api.on('GET', '/residents/5/assessments', failList ?? json(200, list))
  api.on('GET', '/catalogs/care-needs', json(200, [{ id: 11, name: 'Hỗ trợ vận động', status: 'ACTIVE' }]))
  if (draft) api.on('GET', `/assessments/${draft.id}`, json(200, draft, { ETag: '"2"' }))
}

function open(props = {}) {
  return render(
    <AssessmentTab residentId={5} residentStatus="ADMISSION_IN_PROGRESS" allowedCommands={DOCTOR} {...props} />,
  )
}

describe('AssessmentTab', () => {
  it('chưa có mức hiện hành thì nói rõ mức đầu tiên đặt khi xác nhận đánh giá đầu vào', async () => {
    routes()
    open({ allowedCommands: NURSE })

    expect(await screen.findByText(/Chưa có mức chăm sóc hiện hành/)).toBeInTheDocument()
    expect(screen.getByText('Chưa có lần đánh giá nào đã xác nhận hoặc đã hủy.')).toBeInTheDocument()
  })

  it('Điều dưỡng lập đánh giá đầu vào khi đang tiếp nhận: POST kiểu INITIAL', async () => {
    routes()
    api.on('POST', '/residents/5/assessments', json(201, detail(), { ETag: '"0"' }))
    open({ allowedCommands: NURSE })

    fireEvent.click(await screen.findByRole('button', { name: 'Lập đánh giá đầu vào' }))

    await waitFor(() => expect(api.calls('POST', '/residents/5/assessments')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/residents/5/assessments')[0][1].body)).toEqual({ type: 'INITIAL' })
  })

  it('đang lưu trú thì lập đánh giá lại; đang điều trị bệnh viện thì không lập được', async () => {
    routes({ list: [assessment({ status: 'CONFIRMED', careLevel: 'BASIC', comment: 'Khỏe' })] })
    const view = open({ residentStatus: 'IN_RESIDENCE' })
    expect(await screen.findByRole('button', { name: 'Lập đánh giá lại' })).toBeInTheDocument()
    view.unmount()

    routes()
    open({ residentStatus: 'HOSPITALIZED' })
    expect(await screen.findByText(/không cho phép lập đánh giá/)).toBeInTheDocument()
  })

  it('không có quyền lập thì không có nút lập', async () => {
    routes()
    open({ allowedCommands: [] })

    expect(await screen.findByText('Bạn không có quyền lập đánh giá.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Lập đánh giá/ })).not.toBeInTheDocument()
  })

  it('vai trò không được xem đánh giá thấy trạng thái không có quyền', async () => {
    routes({ failList: problem(403, 'PERMISSION_DENIED') })
    open()

    expect(await screen.findByText('Không có quyền xem đánh giá')).toBeInTheDocument()
  })

  it('phiếu nháp có hai khối: phiếu và thông tin tham khảo; không có ô điểm hay mức đề xuất', async () => {
    const draft = detail()
    routes({ list: [assessment()], draft })
    open()

    expect(await screen.findByRole('heading', { name: /Đánh giá đầu vào đang soạn/ })).toBeInTheDocument()
    const reference = screen.getByRole('complementary', { name: 'Thông tin tham khảo' })
    expect(within(reference).getByText('Đi lại cần gậy')).toBeInTheDocument()
    expect(within(reference).getByText(/Penicillin, mức Nặng/)).toBeInTheDocument()
    // 007 chưa kết nối: nói đúng trạng thái, không dữ liệu giả
    expect(within(reference).getByText(/Chưa có dữ liệu chỉ số đo/)).toBeInTheDocument()
    expect(screen.queryByLabelText(/điểm/i)).not.toBeInTheDocument()
    expect(screen.queryByRole('spinbutton')).not.toBeInTheDocument()
    expect(screen.queryByText(/mức đề xuất|gợi ý mức/i)).not.toBeInTheDocument()
    expect(screen.getByText(/chưa áp dụng cho tới khi Bác sĩ xác nhận/)).toBeInTheDocument()
  })

  it('Điều dưỡng không có nút Xác nhận; Lưu nháp gửi If-Match và các trường đã nhập', async () => {
    const draft = detail()
    routes({ list: [assessment()], draft })
    api.on('PUT', '/assessments/7', json(200, detail({ comment: 'Tỉnh táo', careLevel: 'REGULAR' }), { ETag: '"3"' }))
    open({ allowedCommands: NURSE })

    fireEvent.change(await screen.findByLabelText(/Nhận xét đánh giá/), { target: { value: 'Tỉnh táo' } })
    fireEvent.click(screen.getByRole('button', { name: 'Chăm sóc thường xuyên' }))
    fireEvent.click(screen.getByRole('checkbox', { name: 'Hỗ trợ vận động' }))
    expect(screen.queryByRole('button', { name: 'Xác nhận đánh giá' })).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Lưu nháp' }))

    await waitFor(() => expect(api.calls('PUT', '/assessments/7')).toHaveLength(1))
    const [, init] = api.calls('PUT', '/assessments/7')[0]
    expect(new Headers(init.headers).get('If-Match')).toBe('"2"')
    expect(JSON.parse(init.body)).toMatchObject({
      comment: 'Tỉnh táo',
      careLevel: 'REGULAR',
      careNeedIds: [11],
    })
    expect(await screen.findByText('Đã lưu bản nháp.')).toBeInTheDocument()
  })

  it('Bác sĩ xác nhận thiếu nhận xét và mức: báo tại từng ô, không phải lỗi chung', async () => {
    const draft = detail()
    routes({ list: [assessment()], draft })
    api.on('PUT', '/assessments/7', json(200, draft, { ETag: '"3"' }))
    api.on(
      'POST',
      '/assessments/7/confirm',
      problem(409, 'CONDITION_NOT_MET', {
        unmetConditions: [
          { code: 'comment', description: 'Thiếu nhận xét đánh giá (DBR-05)' },
          { code: 'careLevel', description: 'Thiếu mức chăm sóc (DBR-05)' },
        ],
      }),
    )
    open()

    fireEvent.click(await screen.findByRole('button', { name: 'Xác nhận đánh giá' }))
    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText(/trở thành mức chăm sóc hiện hành ngay/)).toBeInTheDocument()
    fireEvent.click(within(dialog).getByRole('button', { name: 'Xác nhận' }))

    expect(await screen.findByText('Nhập nhận xét đánh giá trước khi xác nhận')).toBeInTheDocument()
    expect(screen.getByText('Chọn một mức chăm sóc trước khi xác nhận')).toBeInTheDocument()
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('Bác sĩ xác nhận hợp lệ: lưu nháp rồi xác nhận với ETag mới, hiện tác động', async () => {
    const draft = detail({ comment: 'Tỉnh táo', careLevel: 'REGULAR' })
    routes({ list: [assessment()], draft })
    api.on('PUT', '/assessments/7', json(200, draft, { ETag: '"3"' }))
    api.on(
      'POST',
      '/assessments/7/confirm',
      json(200, {
        assessment: assessment({ status: 'CONFIRMED' }),
        effects: {
          currentCareLevelChanged: true,
          supersededRequestIds: [],
          administratorNotified: false,
          carePlanReviewNotified: false,
        },
      }),
    )
    open()

    fireEvent.click(await screen.findByRole('button', { name: 'Xác nhận đánh giá' }))
    fireEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Xác nhận' }))

    expect(await screen.findByText(/Mức chăm sóc hiện hành đã được cập nhật/)).toBeInTheDocument()
    const [, init] = api.calls('POST', '/assessments/7/confirm')[0]
    expect(new Headers(init.headers).get('If-Match')).toBe('"3"')
  })

  it('đánh giá lại khi đã lưu trú với mức khác: hộp xác nhận nói mức hiện hành giữ nguyên và báo Hành chính', async () => {
    const draft = detail({ type: 'REASSESSMENT', comment: 'Yếu đi', careLevel: 'SPECIAL' })
    routes({
      list: [assessment({ type: 'REASSESSMENT' })],
      draft,
      level: { careLevel: 'BASIC', effectiveFrom: '2026-10-01T00:00:00Z', basisType: 'ASSESSMENT', basisId: 1 },
    })
    open({ residentStatus: 'IN_RESIDENCE' })

    fireEvent.click(await screen.findByRole('button', { name: 'Xác nhận đánh giá' }))
    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByText(/Mức hiện hành \(Chăm sóc cơ bản\) giữ nguyên/)).toBeInTheDocument()
    expect(within(dialog).getByText(/Hành chính được báo/)).toBeInTheDocument()
  })

  it('hủy nháp bắt buộc có lý do', async () => {
    routes({ list: [assessment()], draft: detail() })
    api.on('POST', '/assessments/7/discard-draft', json(200, assessment({ status: 'DISCARDED' })))
    open()

    fireEvent.click(await screen.findByRole('button', { name: 'Hủy nháp…' }))
    fireEvent.click(screen.getByRole('button', { name: 'Hủy bản nháp này' }))
    expect(await screen.findByText('Nhập lý do hủy nháp')).toBeInTheDocument()
    expect(api.calls('POST', '/assessments/7/discard-draft')).toHaveLength(0)

    fireEvent.change(screen.getByLabelText('Lý do hủy nháp'), { target: { value: 'Nhập nhầm' } })
    fireEvent.click(screen.getByRole('button', { name: 'Hủy bản nháp này' }))
    await waitFor(() => expect(api.calls('POST', '/assessments/7/discard-draft')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/assessments/7/discard-draft')[0][1].body)).toEqual({ reason: 'Nhập nhầm' })
  })

  it('mức hiện hành khác mức trong phiếu: thẻ mức hiện hành hiện lưu ý và lịch sử có nút Đính chính cho Bác sĩ', async () => {
    routes({
      list: [
        assessment({
          id: 4,
          status: 'CONFIRMED',
          careLevel: 'REGULAR',
          comment: 'Khỏe',
          careNote: 'Dễ ngã',
          confirmedBy: 'Bác sĩ Lê',
          confirmedAt: '2026-10-20T09:30:00+07:00',
          corrected: true,
        }),
      ],
      level: { careLevel: 'REGULAR', effectiveFrom: '2026-10-20T02:30:00Z', basisType: 'ASSESSMENT', basisId: 4 },
    })
    open({ residentStatus: 'IN_RESIDENCE' })

    const card = (await screen.findByRole('heading', { name: 'Mức chăm sóc hiện hành' })).closest('section')
    expect(within(card).getByText('Chăm sóc thường xuyên')).toBeInTheDocument()
    expect(within(card).getByText(/Dễ ngã/)).toBeInTheDocument()
    expect(screen.getByText('Đã đính chính')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Đính chính' })).toBeInTheDocument()
  })

  it('Điều dưỡng không thấy nút Đính chính', async () => {
    routes({ list: [assessment({ id: 4, status: 'CONFIRMED', careLevel: 'BASIC', comment: 'Khỏe' })] })
    open({ allowedCommands: NURSE, residentStatus: 'IN_RESIDENCE' })

    await screen.findByText('Đánh giá đầu vào')
    expect(screen.queryByRole('button', { name: 'Đính chính' })).not.toBeInTheDocument()
  })
})

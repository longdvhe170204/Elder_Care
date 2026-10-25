import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { TreatmentWish } from './TreatmentWish'

// 001 T080 (US7 #9 → #11; FR-023j → FR-023m): khối Nguyện vọng khi điều trị. Thẻ phiếu hiện hành, form một hoặc hai bệnh
// viện, người cần báo, ý kiến khác có nguồn, cảnh báo thiếu người cần báo, lịch sử phiên bản; Hành chính chỉ thấy "Đã có
// phiếu", Kế toán không thấy khối này.

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

const ACTIVE = {
  id: 12,
  preferredHospitals: [
    { name: 'Bệnh viện X', address: '12 Lê Lợi' },
    { name: 'Bệnh viện Y', address: '5 Hai Bà Trưng' },
  ],
  healthInsurance: { initialRegistrationPlace: 'Trạm y tế P1', cardNumber: 'GD4797912345678', cardExpiry: '2027-06-30' },
  askFamilyFirst: true,
  notifyFamilyMembers: [{ familyMemberId: 31, fullName: 'Nguyễn Thị Con', relationship: null, phoneNumber: '0912345678' }],
  accompaniedBy: 'FACILITY_STAFF',
  note: 'Dị ứng penicillin',
  respondent: { type: 'RESIDENT', familyMemberId: null },
  otherOpinions: [
    {
      source: { type: 'FAMILY_MEMBER', familyMemberId: 31 },
      content: 'Muốn bệnh viện Z',
      recordedBy: 'Điều dưỡng Lan',
      at: '2026-10-10T03:00:00Z',
    },
  ],
  signedCopyFileId: null,
  recordedBy: 'Điều dưỡng Lan',
  at: '2026-10-10T02:00:00Z',
  status: 'ACTIVE',
}

const OLD = {
  ...ACTIVE,
  id: 7,
  preferredHospitals: [{ name: 'Bệnh viện Cũ', address: '1 Nguyễn Huệ' }],
  healthInsurance: null,
  otherOpinions: [],
  status: 'EXPIRED',
}

function routes({ active = ACTIVE, history = [ACTIVE, OLD], forbidden = false } = {}) {
  api.on(
    'GET',
    '/residents/5/treatment-wishes',
    forbidden ? problem(403, 'PERMISSION_DENIED') : json(200, { active, history, hiddenFields: [] }),
  )
}

function open(props = {}) {
  return render(<TreatmentWish residentId={5} status="IN_RESIDENCE" allowedCommands={[]} {...props} />)
}

function fillMinimum() {
  fireEvent.change(screen.getByLabelText('Tên bệnh viện 1'), { target: { value: 'Bệnh viện X' } })
  fireEvent.change(screen.getByLabelText('Địa chỉ bệnh viện 1'), { target: { value: '12 Lê Lợi' } })
  fireEvent.click(screen.getByRole('radio', { name: 'Gia đình' }))
}

describe('TreatmentWish', () => {
  it('hiện phiếu hiện hành đủ bệnh viện, bảo hiểm, người cần báo, người đi kèm; lịch sử có bản hết hiệu lực', async () => {
    routes()
    open()

    const now = await screen.findByRole('region', { name: 'Nguyện vọng khi điều trị' })
    expect(within(now).getByText('Hiệu lực')).toBeInTheDocument()
    expect(within(now).getByText('Bệnh viện X · 12 Lê Lợi')).toBeInTheDocument()
    expect(within(now).getByText('Bệnh viện Y · 5 Hai Bà Trưng')).toBeInTheDocument()
    expect(within(now).getByText(/Số thẻ GD4797912345678/)).toBeInTheDocument()
    expect(within(now).getByText('Nguyễn Thị Con, 0912345678')).toBeInTheDocument()
    expect(within(now).getByText('Viện cử nhân viên')).toBeInTheDocument()
    expect(within(now).getByText('Muốn bệnh viện Z')).toBeInTheDocument()

    const history = screen.getByRole('list', { name: 'Lịch sử phiếu nguyện vọng khi điều trị' })
    expect(within(history).getByText('Hết hiệu lực')).toBeInTheDocument()
    expect(within(history).getByText('Bệnh viện Cũ · 1 Nguyễn Huệ')).toBeInTheDocument()
  })

  it('nút ghi phiếu chỉ hiện theo allowedCommands và hồ sơ chưa ở trạng thái cuối', async () => {
    routes()
    const view = open({ allowedCommands: ['RECORD_TREATMENT_WISH'] })
    expect(await screen.findByRole('button', { name: 'Ghi phiếu mới' })).toBeInTheDocument()
    view.unmount()

    routes()
    const finalView = open({ status: 'RESIDENCY_ENDED', allowedCommands: ['RECORD_TREATMENT_WISH'] })
    await screen.findByText('Hiệu lực')
    expect(screen.queryByRole('button', { name: /Ghi phiếu/ })).not.toBeInTheDocument()
    finalView.unmount()

    routes()
    open()
    await screen.findByText('Hiệu lực')
    expect(screen.queryByRole('button', { name: /Ghi phiếu/ })).not.toBeInTheDocument()
  })

  it('chưa có phiếu: báo không chặn việc nào và mời ghi phiếu', async () => {
    routes({ active: null, history: [] })
    open({ allowedCommands: ['RECORD_TREATMENT_WISH'] })

    expect(await screen.findByText('Chưa có phiếu nguyện vọng khi điều trị Hiệu lực')).toBeInTheDocument()
    expect(screen.getByText(/không chặn việc nào/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ghi phiếu nguyện vọng' })).toBeInTheDocument()
  })

  it('Hành chính thấy "Đã có phiếu" không thấy nội dung; Kế toán không thấy khối này', async () => {
    routes({ forbidden: true })
    const office = open({ hasWish: true })
    expect(await screen.findByText('Đã có phiếu nguyện vọng khi điều trị.')).toBeInTheDocument()
    expect(screen.queryByText(/Bệnh viện/)).not.toBeInTheDocument()
    office.unmount()

    routes({ forbidden: true })
    const accountant = open({ hasWish: undefined })
    await waitFor(() => expect(api.calls('GET', '/residents/5/treatment-wishes')).toHaveLength(2))
    await waitFor(() => expect(accountant.container).toBeEmptyDOMElement())
  })

  it('ghi phiếu tối thiểu: một bệnh viện, gia đình đi kèm, người cao tuổi trả lời, không bản ký thì không tải tệp', async () => {
    routes({ active: null, history: [] })
    api.on('POST', '/residents/5/treatment-wishes', json(201, { wish: ACTIVE, warnings: [] }))
    const onChanged = vi.fn()
    open({ allowedCommands: ['RECORD_TREATMENT_WISH'], onChanged })

    fireEvent.click(await screen.findByRole('button', { name: 'Ghi phiếu nguyện vọng' }))
    fillMinimum()
    fireEvent.click(screen.getByRole('button', { name: 'Lưu phiếu' }))

    await waitFor(() => expect(api.calls('POST', '/residents/5/treatment-wishes')).toHaveLength(1))
    const body = JSON.parse(api.calls('POST', '/residents/5/treatment-wishes')[0][1].body)
    expect(body).toMatchObject({
      preferredHospitals: [{ name: 'Bệnh viện X', address: '12 Lê Lợi' }],
      askFamilyFirst: false,
      notifyFamilyMemberIds: [],
      accompaniedBy: 'FAMILY',
      respondent: { type: 'RESIDENT' },
      otherOpinions: [],
    })
    expect(body.healthInsurance).toBeUndefined()
    expect(body.signedCopyFileId).toBeUndefined()
    expect(api.calls('POST', '/files')).toHaveLength(0)
    await waitFor(() => expect(onChanged).toHaveBeenCalled())
    expect(await screen.findByText('Đã ghi phiếu nguyện vọng khi điều trị.')).toBeInTheDocument()
  })

  it('thiếu bệnh viện hoặc người đi kèm thì báo tại ô và không gọi backend', async () => {
    routes({ active: null, history: [] })
    open({ allowedCommands: ['RECORD_TREATMENT_WISH'] })

    fireEvent.click(await screen.findByRole('button', { name: 'Ghi phiếu nguyện vọng' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu phiếu' }))

    expect(await screen.findByText('Nhập tên và địa chỉ bệnh viện ưu tiên')).toBeInTheDocument()
    expect(screen.getByText('Chọn người đi kèm')).toBeInTheDocument()
    expect(api.calls('POST', '/residents/5/treatment-wishes')).toHaveLength(0)
  })

  it('bệnh viện thứ hai: thêm được, bỏ được, tối đa hai; bệnh viện thứ hai dở dang bị chặn', async () => {
    routes({ active: null, history: [] })
    api.on('POST', '/residents/5/treatment-wishes', json(201, { wish: ACTIVE, warnings: [] }))
    open({ allowedCommands: ['RECORD_TREATMENT_WISH'] })

    fireEvent.click(await screen.findByRole('button', { name: 'Ghi phiếu nguyện vọng' }))
    fillMinimum()
    fireEvent.click(screen.getByRole('button', { name: 'Thêm bệnh viện thứ hai' }))
    expect(screen.queryByRole('button', { name: 'Thêm bệnh viện thứ hai' })).not.toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Tên bệnh viện 2'), { target: { value: 'Bệnh viện Y' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu phiếu' }))
    expect(await screen.findByText(/Nhập đủ tên và địa chỉ bệnh viện thứ hai/)).toBeInTheDocument()
    expect(api.calls('POST', '/residents/5/treatment-wishes')).toHaveLength(0)

    fireEvent.change(screen.getByLabelText('Địa chỉ bệnh viện 2'), { target: { value: '5 Hai Bà Trưng' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu phiếu' }))
    await waitFor(() => expect(api.calls('POST', '/residents/5/treatment-wishes')).toHaveLength(1))
    const body = JSON.parse(api.calls('POST', '/residents/5/treatment-wishes')[0][1].body)
    expect(body.preferredHospitals).toHaveLength(2)
  })

  it('bảo hiểm y tế, hỏi trước, người cần báo và ý kiến gia đình có nguồn được gửi đủ', async () => {
    routes({ active: null, history: [] })
    api.on('POST', '/residents/5/treatment-wishes', json(201, { wish: ACTIVE, warnings: [] }))
    open({ allowedCommands: ['RECORD_TREATMENT_WISH'] })

    fireEvent.click(await screen.findByRole('button', { name: 'Ghi phiếu nguyện vọng' }))
    fillMinimum()
    fireEvent.change(screen.getByLabelText('Số thẻ'), { target: { value: 'GD4797912345678' } })
    fireEvent.change(screen.getByLabelText('Hạn thẻ'), { target: { value: '2027-06-30' } })
    fireEvent.click(screen.getByRole('checkbox', { name: 'Hỏi gia đình trước khi chuyển viện không khẩn cấp' }))
    fireEvent.change(screen.getByLabelText('Mã người thân cần báo'), { target: { value: 'abc' } })
    fireEvent.click(screen.getByRole('button', { name: 'Thêm mã' }))
    expect(await screen.findByText('Mã người thân là số nguyên dương')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Mã người thân cần báo'), { target: { value: '31' } })
    fireEvent.click(screen.getByRole('button', { name: 'Thêm mã' }))
    fireEvent.change(screen.getByLabelText('Mã người thân nêu ý kiến'), { target: { value: '32' } })
    fireEvent.change(screen.getByLabelText('Nội dung ý kiến'), { target: { value: 'Muốn bệnh viện Z' } })
    fireEvent.click(screen.getByRole('button', { name: 'Thêm ý kiến' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu phiếu' }))

    await waitFor(() => expect(api.calls('POST', '/residents/5/treatment-wishes')).toHaveLength(1))
    const body = JSON.parse(api.calls('POST', '/residents/5/treatment-wishes')[0][1].body)
    expect(body).toMatchObject({
      healthInsurance: { cardNumber: 'GD4797912345678', cardExpiry: '2027-06-30' },
      askFamilyFirst: true,
      notifyFamilyMemberIds: [31],
      otherOpinions: [{ source: { type: 'FAMILY_MEMBER', familyMemberId: 32 }, content: 'Muốn bệnh viện Z' }],
    })
  })

  it('hỏi trước mà không có người cần báo: lưu được và hiện cảnh báo', async () => {
    routes({ active: null, history: [] })
    api.on(
      'POST',
      '/residents/5/treatment-wishes',
      json(201, { wish: ACTIVE, warnings: [{ code: 'NO_FAMILY_MEMBER_TO_NOTIFY', description: 'x' }] }),
    )
    open({ allowedCommands: ['RECORD_TREATMENT_WISH'] })

    fireEvent.click(await screen.findByRole('button', { name: 'Ghi phiếu nguyện vọng' }))
    fillMinimum()
    fireEvent.click(screen.getByRole('checkbox', { name: 'Hỏi gia đình trước khi chuyển viện không khẩn cấp' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu phiếu' }))

    expect(await screen.findByText(/chưa chọn người thân cần báo/)).toBeInTheDocument()
    expect(screen.getByText('Đã ghi phiếu nguyện vọng khi điều trị.')).toBeInTheDocument()
  })

  it('backend từ chối người cần báo không có quan hệ: báo tại ô mã người thân', async () => {
    routes({ active: null, history: [] })
    api.on('POST', '/residents/5/treatment-wishes', problem(409, 'INVALID_FAMILY_MEMBER'))
    open({ allowedCommands: ['RECORD_TREATMENT_WISH'] })

    fireEvent.click(await screen.findByRole('button', { name: 'Ghi phiếu nguyện vọng' }))
    fillMinimum()
    fireEvent.change(screen.getByLabelText('Mã người thân cần báo'), { target: { value: '99' } })
    fireEvent.click(screen.getByRole('button', { name: 'Thêm mã' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu phiếu' }))

    expect(await screen.findByText(/Người cần báo phải là người thân có quan hệ Hiệu lực/)).toBeInTheDocument()
  })

  it('bản ký tùy chọn: có thì tải lên rồi gửi mã tệp', async () => {
    routes({ active: null, history: [] })
    api.on('POST', '/files', json(201, { fileId: 77 }))
    api.on('POST', '/residents/5/treatment-wishes', json(201, { wish: ACTIVE, warnings: [] }))
    open({ allowedCommands: ['RECORD_TREATMENT_WISH'] })

    fireEvent.click(await screen.findByRole('button', { name: 'Ghi phiếu nguyện vọng' }))
    fillMinimum()
    fireEvent.change(screen.getByLabelText('Bản ký đã scan (không bắt buộc)'), {
      target: { files: [new File(['%PDF-1.4'], 'phieu.pdf', { type: 'application/pdf' })] },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu phiếu' }))

    await waitFor(() => expect(api.calls('POST', '/residents/5/treatment-wishes')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/residents/5/treatment-wishes')[0][1].body).signedCopyFileId).toBe(77)
  })
})

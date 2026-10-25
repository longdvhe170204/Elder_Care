import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { EndOfLifeWish } from './EndOfLifeWish'

// 001 T070 (US7; FR-023a → FR-023i): khối Nguyện vọng cuối đời. Thẻ phiếu hiện hành kèm nhận định chỉ bên trong phiếu,
// nút theo allowedCommands, form theo nhận định (tự quyết được thì người cao tuổi ký; không thì người đại diện), lịch sử
// các phiên bản, Hành chính và Kế toán chỉ thấy cờ có/không.

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

const ASSESSMENT = {
  canSelfDetermine: true,
  at: '2026-10-10T02:00:00Z',
  supportMethod: 'Dùng tranh minh họa, hỏi lại từng ý',
  reviewDate: '2026-11-20',
}

const CURRENT = {
  id: 12,
  choice: 'STAY_PALLIATIVE',
  firstContact: 'Con trai: 0901 234 567',
  note: 'Muốn có người thân bên cạnh',
  selfDeterminationAssessment: ASSESSMENT,
  signer: 'Nguyễn Văn A',
  signerType: 'RESIDENT',
  representativeSelection: null,
  otherOpinions: [
    {
      source: { type: 'FAMILY_MEMBER', familyMemberId: 31 },
      content: 'Muốn chuyển bệnh viện',
      recordedBy: 'Điều dưỡng Lan',
      at: '2026-10-10T03:00:00Z',
    },
  ],
  signedAt: '2026-10-10T02:00:00Z',
  source: 'ADMISSION',
  status: 'ACTIVE',
}

const OLD = {
  ...CURRENT,
  id: 7,
  choice: 'TRANSFER_TO_HOSPITAL',
  selfDeterminationAssessment: { ...ASSESSMENT, canSelfDetermine: false },
  signer: 'Trần Thị B',
  signerType: 'PATIENT_REPRESENTATIVE',
  representativeSelection: 'CHOSEN_BY_FAMILY',
  otherOpinions: [],
  source: 'FAMILY_REQUEST',
  status: 'SUPERSEDED',
}

function routes({ current = CURRENT, history = [CURRENT, OLD], forbidden = false } = {}) {
  api.on(
    'GET',
    '/residents/5/end-of-life-wishes',
    forbidden
      ? problem(403, 'PERMISSION_DENIED')
      : json(200, { hasEndOfLifeWish: !!current, current, history, hiddenFields: [] }),
  )
}

function open(props = {}) {
  return render(<EndOfLifeWish residentId={5} status="IN_RESIDENCE" allowedCommands={[]} {...props} />)
}

const pdf = (name = 'ban-ky.pdf') => new File(['%PDF-1.4'], name, { type: 'application/pdf' })

function choose(label, file) {
  fireEvent.change(screen.getByLabelText(label), { target: { files: [file] } })
}

function fillCommon({ self = true } = {}) {
  fireEvent.click(screen.getByRole('radio', { name: 'Đưa về nhà' }))
  fireEvent.change(screen.getByLabelText('Người cần liên hệ trước khi thực hiện'), { target: { value: 'Con gái 0912' } })
  fireEvent.change(screen.getByLabelText('Mã tài khoản Bác sĩ nhận định'), { target: { value: '8' } })
  fireEvent.click(within(screen.getByRole('radiogroup', { name: /tự quyết được/ })).getByRole('radio', { name: self ? 'Có' : 'Không' }))
  fireEvent.change(screen.getByLabelText('Cách đã hỗ trợ người cao tuổi hiểu thông tin'), { target: { value: 'Giải thích bằng lời' } })
  fireEvent.change(screen.getByLabelText('Ngày xem lại nhận định'), { target: { value: '2099-01-01' } })
  choose('Bản ký đã scan', pdf())
}

describe('EndOfLifeWish', () => {
  it('hiện phiếu hiện hành: người quyết định, nhận định chỉ trong phiếu, ý kiến khác có nguồn; lịch sử có bản cũ', async () => {
    routes()
    open()

    const now = await screen.findByRole('region', { name: 'Nguyện vọng cuối đời' })
    expect(within(now).getByText('Hiệu lực')).toBeInTheDocument()
    expect(within(now).getByText('Ở lại viện chăm sóc giảm nhẹ')).toBeInTheDocument()
    expect(within(now).getByText(/Người cao tuổi · Nguyễn Văn A/)).toBeInTheDocument()
    const assessment = within(now).getByRole('region', { name: 'Nhận định của Bác sĩ cho phiếu này' })
    expect(within(assessment).getByText('Dùng tranh minh họa, hỏi lại từng ý')).toBeInTheDocument()
    expect(within(assessment).getByText('2026-11-20')).toBeInTheDocument()
    const opinions = within(now).getByRole('list', { name: 'Ý kiến khác' })
    expect(within(opinions).getByText(/Người thân \(mã 31\)/)).toBeInTheDocument()
    expect(within(opinions).getByText('Muốn chuyển bệnh viện')).toBeInTheDocument()

    const history = screen.getByRole('list', { name: 'Lịch sử phiếu nguyện vọng cuối đời' })
    expect(within(history).getByText('Được thay thế')).toBeInTheDocument()
    expect(within(history).getByText('Chuyển bệnh viện điều trị tích cực')).toBeInTheDocument()
    expect(within(history).getByText(/Người đại diện của người bệnh · Trần Thị B · Do thành viên gia đình chọn/)).toBeInTheDocument()
    // không có nhãn năng lực chung
    expect(screen.queryByText(/năng lực/i)).not.toBeInTheDocument()
  })

  it('nút ghi phiếu chỉ hiện theo allowedCommands và hồ sơ chưa ở trạng thái cuối', async () => {
    routes()
    const view = open({ allowedCommands: ['RECORD_END_OF_LIFE_WISH'] })
    expect(await screen.findByRole('button', { name: 'Ghi phiếu mới' })).toBeInTheDocument()
    view.unmount()

    routes()
    const finalView = open({ status: 'DECEASED', allowedCommands: ['RECORD_END_OF_LIFE_WISH'] })
    await screen.findByText('Hiệu lực')
    expect(screen.queryByRole('button', { name: /Ghi phiếu/ })).not.toBeInTheDocument()
    finalView.unmount()

    routes()
    open()
    await screen.findByText('Hiệu lực')
    expect(screen.queryByRole('button', { name: /Ghi phiếu/ })).not.toBeInTheDocument()
  })

  it('chưa có phiếu: cảnh báo và mời ghi phiếu', async () => {
    routes({ current: null, history: [] })
    open({ allowedCommands: ['RECORD_END_OF_LIFE_WISH'] })

    expect(await screen.findByText('Chưa có phiếu nguyện vọng cuối đời Hiệu lực')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ghi phiếu nguyện vọng' })).toBeInTheDocument()
  })

  it('Hành chính, Kế toán không xem được nội dung: chỉ thấy đã có hay chưa có phiếu', async () => {
    routes({ forbidden: true })
    const view = open({ hasWish: true })
    expect(await screen.findByText('Đã có phiếu nguyện vọng cuối đời.')).toBeInTheDocument()
    expect(screen.queryByText('Ở lại viện chăm sóc giảm nhẹ')).not.toBeInTheDocument()
    view.unmount()

    routes({ forbidden: true })
    open({ hasWish: false })
    expect(await screen.findByText('Chưa có phiếu nguyện vọng cuối đời.')).toBeInTheDocument()
  })

  it('người cao tuổi tự quyết được: tải bản ký lên, gửi nhận định và người ký là người cao tuổi', async () => {
    routes({ current: null, history: [] })
    api.on('POST', '/files', json(201, { fileId: 90 }))
    api.on('POST', '/residents/5/end-of-life-wishes', json(201, CURRENT))
    const onChanged = vi.fn()
    open({ allowedCommands: ['RECORD_END_OF_LIFE_WISH'], onChanged })

    fireEvent.click(await screen.findByRole('button', { name: 'Ghi phiếu nguyện vọng' }))
    fillCommon()
    expect(screen.getByText(/Người quyết định:/)).toHaveTextContent('Người cao tuổi')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu phiếu' }))

    await waitFor(() => expect(api.calls('POST', '/residents/5/end-of-life-wishes')).toHaveLength(1))
    const body = JSON.parse(api.calls('POST', '/residents/5/end-of-life-wishes')[0][1].body)
    expect(body).toMatchObject({
      choice: 'RETURN_HOME',
      firstContact: 'Con gái 0912',
      respondentType: 'RESIDENT',
      assessingDoctorId: 8,
      selfDeterminationAssessment: { canSelfDetermine: true, supportMethod: 'Giải thích bằng lời', reviewDate: '2099-01-01' },
      signerType: 'RESIDENT',
      signedCopyFileId: 90,
      source: 'ADMISSION',
    })
    expect(body.signerFamilyMemberId).toBeUndefined()
    expect(body.courtDecision).toBeUndefined()
    await waitFor(() => expect(onChanged).toHaveBeenCalled())
    expect(await screen.findByText('Đã ghi phiếu nguyện vọng cuối đời.')).toBeInTheDocument()
  })

  it('thiếu lựa chọn, nhận định hoặc bản ký thì báo tại ô và không gọi backend', async () => {
    routes({ current: null, history: [] })
    open({ allowedCommands: ['RECORD_END_OF_LIFE_WISH'] })

    fireEvent.click(await screen.findByRole('button', { name: 'Ghi phiếu nguyện vọng' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu phiếu' }))

    expect(await screen.findByText('Chọn một lựa chọn')).toBeInTheDocument()
    expect(screen.getByText('Chọn nhận định của Bác sĩ')).toBeInTheDocument()
    expect(screen.getByText('Nhập cách đã hỗ trợ người cao tuổi hiểu thông tin')).toBeInTheDocument()
    expect(screen.getByText('Chọn ngày xem lại nhận định')).toBeInTheDocument()
    expect(screen.getByText('Chọn tệp bản ký đã scan')).toBeInTheDocument()
    expect(api.calls('POST', '/residents/5/end-of-life-wishes')).toHaveLength(0)
    expect(api.calls('POST', '/files')).toHaveLength(0)
  })

  it('không tự quyết được: bắt buộc người ký thay, quan hệ và cách được chọn; người đại diện theo pháp luật cần quyết định của Tòa án', async () => {
    routes({ current: null, history: [] })
    api.on('POST', '/files', () => json(201, { fileId: 100 + api.calls('POST', '/files').length }))
    api.on('POST', '/residents/5/end-of-life-wishes', json(201, OLD))
    open({ allowedCommands: ['RECORD_END_OF_LIFE_WISH'] })

    fireEvent.click(await screen.findByRole('button', { name: 'Ghi phiếu nguyện vọng' }))
    fillCommon({ self: false })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu phiếu' }))
    expect(await screen.findByText('Chọn người ký thay')).toBeInTheDocument()
    expect(screen.getByText('Nhập mã người thân là người ký')).toBeInTheDocument()
    expect(screen.getByText('Nhập quan hệ với người cao tuổi')).toBeInTheDocument()
    expect(api.calls('POST', '/residents/5/end-of-life-wishes')).toHaveLength(0)

    fireEvent.click(screen.getByRole('radio', { name: 'Người đại diện theo pháp luật' }))
    fireEvent.change(screen.getByLabelText('Mã người thân là người ký'), { target: { value: '41' } })
    fireEvent.change(screen.getByLabelText('Quan hệ với người cao tuổi'), { target: { value: 'Con gái' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu phiếu' }))
    expect(await screen.findByText('Nhập số quyết định')).toBeInTheDocument()
    expect(screen.getByText('Chọn tệp quyết định của Tòa án')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Số quyết định'), { target: { value: '15/2026/QĐ-TA' } })
    fireEvent.change(screen.getByLabelText('Ngày quyết định'), { target: { value: '2026-03-02' } })
    fireEvent.change(screen.getByLabelText('Tòa án'), { target: { value: 'TAND quận 1' } })
    choose('Bản scan quyết định của Tòa án', pdf('qd.pdf'))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu phiếu' }))

    await waitFor(() => expect(api.calls('POST', '/residents/5/end-of-life-wishes')).toHaveLength(1))
    const body = JSON.parse(api.calls('POST', '/residents/5/end-of-life-wishes')[0][1].body)
    expect(body).toMatchObject({
      respondentType: 'LEGAL_REPRESENTATIVE',
      respondentFamilyMemberId: 41,
      relationship: 'Con gái',
      signerType: 'LEGAL_REPRESENTATIVE',
      signerFamilyMemberId: 41,
      selfDeterminationAssessment: { canSelfDetermine: false },
      courtDecision: { number: '15/2026/QĐ-TA', date: '2026-03-02', court: 'TAND quận 1' },
    })
    expect(body.courtDecision.fileId).not.toBe(body.signedCopyFileId)
    expect(body.representativeSelection).toBeUndefined()
  })

  it('người đại diện của người bệnh: phải chọn cách được chọn và gửi kèm', async () => {
    routes({ current: null, history: [] })
    api.on('POST', '/files', json(201, { fileId: 91 }))
    api.on('POST', '/residents/5/end-of-life-wishes', json(201, OLD))
    open({ allowedCommands: ['RECORD_END_OF_LIFE_WISH'] })

    fireEvent.click(await screen.findByRole('button', { name: 'Ghi phiếu nguyện vọng' }))
    fillCommon({ self: false })
    fireEvent.click(screen.getByRole('radio', { name: 'Người đại diện của người bệnh' }))
    fireEvent.change(screen.getByLabelText('Mã người thân là người ký'), { target: { value: '41' } })
    fireEvent.change(screen.getByLabelText('Quan hệ với người cao tuổi'), { target: { value: 'Con gái' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu phiếu' }))
    expect(await screen.findByText('Chọn cách người đại diện được chọn')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('radio', { name: 'Do thành viên gia đình chọn' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu phiếu' }))

    await waitFor(() => expect(api.calls('POST', '/residents/5/end-of-life-wishes')).toHaveLength(1))
    const body = JSON.parse(api.calls('POST', '/residents/5/end-of-life-wishes')[0][1].body)
    expect(body.representativeSelection).toBe('CHOSEN_BY_FAMILY')
    expect(body.signerType).toBe('PATIENT_REPRESENTATIVE')
  })

  it('ý kiến khác: mỗi ý kiến một mục có nguồn, ý kiến người thân cần mã người thân', async () => {
    routes({ current: null, history: [] })
    api.on('POST', '/files', json(201, { fileId: 90 }))
    api.on('POST', '/residents/5/end-of-life-wishes', json(201, CURRENT))
    open({ allowedCommands: ['RECORD_END_OF_LIFE_WISH'] })

    fireEvent.click(await screen.findByRole('button', { name: 'Ghi phiếu nguyện vọng' }))
    fillCommon()
    fireEvent.change(screen.getByLabelText('Nội dung ý kiến'), { target: { value: 'Muốn chuyển bệnh viện' } })
    fireEvent.click(screen.getByRole('button', { name: 'Thêm ý kiến' }))
    expect(await screen.findByText('Nhập mã người thân nêu ý kiến')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Mã người thân nêu ý kiến'), { target: { value: '31' } })
    fireEvent.click(screen.getByRole('button', { name: 'Thêm ý kiến' }))
    fireEvent.click(screen.getByRole('radio', { name: 'Nhân viên quan sát' }))
    fireEvent.change(screen.getByLabelText('Nội dung ý kiến'), { target: { value: 'Cụ hay nhắc nhớ nhà' } })
    fireEvent.click(screen.getByRole('button', { name: 'Thêm ý kiến' }))
    expect(within(screen.getByRole('list', { name: 'Ý kiến khác đã thêm' })).getAllByRole('listitem')).toHaveLength(2)
    fireEvent.click(screen.getByRole('button', { name: 'Lưu phiếu' }))

    await waitFor(() => expect(api.calls('POST', '/residents/5/end-of-life-wishes')).toHaveLength(1))
    const body = JSON.parse(api.calls('POST', '/residents/5/end-of-life-wishes')[0][1].body)
    expect(body.otherOpinions).toEqual([
      { source: { type: 'FAMILY_MEMBER', familyMemberId: 31 }, content: 'Muốn chuyển bệnh viện' },
      { source: { type: 'OBSERVING_STAFF' }, content: 'Cụ hay nhắc nhớ nhà' },
    ])
  })

  it('backend từ chối người ký không có quan hệ: báo tại ô mã người ký', async () => {
    routes({ current: null, history: [] })
    api.on('POST', '/files', json(201, { fileId: 5 }))
    api.on('POST', '/residents/5/end-of-life-wishes', problem(409, 'INVALID_SIGNER'))
    open({ allowedCommands: ['RECORD_END_OF_LIFE_WISH'] })

    fireEvent.click(await screen.findByRole('button', { name: 'Ghi phiếu nguyện vọng' }))
    fillCommon({ self: false })
    fireEvent.click(screen.getByRole('radio', { name: 'Người đại diện của người bệnh' }))
    fireEvent.change(screen.getByLabelText('Mã người thân là người ký'), { target: { value: '99' } })
    fireEvent.change(screen.getByLabelText('Quan hệ với người cao tuổi'), { target: { value: 'Bạn' } })
    fireEvent.click(screen.getByRole('radio', { name: 'Do người cao tuổi chọn' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu phiếu' }))

    expect(await screen.findByText(/khớp nhận định và là người thân có quan hệ Hiệu lực/)).toBeInTheDocument()
  })

  it('lỗi tải tệp quá lớn báo tại ô bản ký, lỗi khác hiện cảnh báo chung', async () => {
    routes({ current: null, history: [] })
    api.on('POST', '/files', problem(422, 'FILE_TOO_LARGE'))
    open({ allowedCommands: ['RECORD_END_OF_LIFE_WISH'] })

    fireEvent.click(await screen.findByRole('button', { name: 'Ghi phiếu nguyện vọng' }))
    fillCommon()
    fireEvent.click(screen.getByRole('button', { name: 'Lưu phiếu' }))

    expect(await screen.findByText('Tệp quá 10 MB')).toBeInTheDocument()
    expect(api.calls('POST', '/residents/5/end-of-life-wishes')).toHaveLength(0)
  })
})

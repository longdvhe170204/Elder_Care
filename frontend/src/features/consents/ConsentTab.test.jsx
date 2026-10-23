import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { ConsentTab } from './ConsentTab'

// 001 T059 (US5; FR-010 → FR-016): tab Đồng ý trong khung chi tiết hồ sơ. Nút lệnh theo allowedCommands, ghi nhận kèm
// bản ký tải lên, người ký thay phải có quyết định của Tòa án, rút lại kèm lý do và bằng chứng, tải bản ký qua backend.

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

const ACTIVE = {
  id: 12,
  consenterType: 'RESIDENT',
  consenterFamilyMemberId: null,
  courtDecision: null,
  familyMemberIdsAllowedHealthView: [31, 32],
  photoConsent: true,
  notificationConsent: false,
  consentedAt: '2026-10-10T02:00:00Z',
  evidenceFileId: 90,
  status: 'ACTIVE',
  withdrawal: null,
}

const OLD = {
  id: 7,
  consenterType: 'LEGAL_REPRESENTATIVE',
  consenterFamilyMemberId: 41,
  courtDecision: { number: '12/2026/QĐ-TA', date: '2026-03-05', court: 'Tòa án nhân dân quận 1', fileId: 70 },
  familyMemberIdsAllowedHealthView: [41],
  photoConsent: false,
  notificationConsent: true,
  consentedAt: '2026-04-01T02:00:00Z',
  evidenceFileId: 71,
  status: 'WITHDRAWN',
  withdrawal: { at: '2026-10-10T02:00:00Z', reason: 'Được thay thế bởi bản đồng ý mới', evidenceFileId: null, supersededById: 12 },
}

function routes({ consents = [ACTIVE, OLD], forbidden = false } = {}) {
  api.on('GET', '/residents/5/consents', forbidden ? problem(403, 'PERMISSION_DENIED') : json(200, consents))
}

function open(props = {}) {
  return render(<ConsentTab residentId={5} status="IN_RESIDENCE" allowedCommands={[]} {...props} />)
}

const pdf = (name = 'ban-ky.pdf') => new File(['%PDF-1.4'], name, { type: 'application/pdf' })

function choose(label, file) {
  fireEvent.change(screen.getByLabelText(label), { target: { files: [file] } })
}

describe('ConsentTab', () => {
  it('hiện bản Hiệu lực và lịch sử: bản được thay thế ghi rõ, không có lý do do người nhập', async () => {
    routes()
    open()

    expect(await screen.findByText('Bản đồng ý hiện hành')).toBeInTheDocument()
    const now = screen.getByRole('region', { name: 'Bản đồng ý hiện hành' })
    expect(within(now).getByText('Hiệu lực')).toBeInTheDocument()
    expect(within(now).getByText('Người cao tuổi')).toBeInTheDocument()
    expect(within(now).getByText(/31, 32/)).toBeInTheDocument()
    const history = screen.getByRole('list', { name: 'Lịch sử bản đồng ý' })
    expect(within(history).getByText('Đã rút lại')).toBeInTheDocument()
    expect(within(history).getByText(/Được thay thế bởi bản đồng ý mới/)).toBeInTheDocument()
    expect(within(history).getByText(/12\/2026\/QĐ-TA/)).toBeInTheDocument()
  })

  it('nút lệnh chỉ hiện theo allowedCommands', async () => {
    routes()
    const view = open({ allowedCommands: ['RECORD_CONSENT', 'WITHDRAW_CONSENT'] })
    expect(await screen.findByRole('button', { name: 'Ghi nhận bản mới' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Rút lại đồng ý' })).toBeInTheDocument()
    view.unmount()

    routes()
    open()
    await screen.findByText('Bản đồng ý hiện hành')
    expect(screen.queryByRole('button', { name: /Ghi nhận|Rút lại/ })).not.toBeInTheDocument()
  })

  it('chưa có bản Hiệu lực: báo người thân chưa xem được sức khỏe và mời ghi nhận', async () => {
    routes({ consents: [] })
    open({ allowedCommands: ['RECORD_CONSENT'] })

    expect(await screen.findByText(/chưa xem được thông tin sức khỏe/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ghi nhận bản đồng ý' })).toBeInTheDocument()
  })

  it('hồ sơ trạng thái cuối: không ghi nhận bản mới, vẫn rút lại được', async () => {
    routes()
    open({ status: 'DECEASED', allowedCommands: ['RECORD_CONSENT', 'WITHDRAW_CONSENT'] })

    expect(await screen.findByRole('button', { name: 'Rút lại đồng ý' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Ghi nhận/ })).not.toBeInTheDocument()
  })

  it('vai trò không được xem thì hiện trạng thái không có quyền', async () => {
    routes({ forbidden: true })
    open()
    expect(await screen.findByText('Không có quyền xem bản đồng ý')).toBeInTheDocument()
  })

  it('ghi nhận bản của người cao tuổi: tải bản ký lên rồi gửi mã tệp, mã người thân và hai lựa chọn', async () => {
    routes({ consents: [] })
    api.on('POST', '/files', json(201, { fileId: 90 }))
    api.on('POST', '/residents/5/consents', json(201, ACTIVE))
    const onChanged = vi.fn()
    open({ allowedCommands: ['RECORD_CONSENT'], onChanged })

    fireEvent.click(await screen.findByRole('button', { name: 'Ghi nhận bản đồng ý' }))
    choose('Bản ký đã scan', pdf())
    fireEvent.change(screen.getByLabelText('Mã người thân được xem sức khỏe'), { target: { value: '31' } })
    fireEvent.click(screen.getByRole('button', { name: 'Thêm mã' }))
    fireEvent.click(screen.getByRole('checkbox', { name: 'Đồng ý dùng hình ảnh' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu bản đồng ý' }))

    await waitFor(() => expect(api.calls('POST', '/residents/5/consents')).toHaveLength(1))
    const [, init] = api.calls('POST', '/residents/5/consents')[0]
    const body = JSON.parse(init.body)
    expect(body).toMatchObject({
      consenterType: 'RESIDENT',
      familyMemberIdsAllowedHealthView: [31],
      photoConsent: true,
      notificationConsent: false,
      evidenceFileId: 90,
    })
    expect(body.consentedAt).toMatch(/^\d{4}-\d{2}-\d{2}T/)
    expect(body.courtDecision).toBeUndefined()
    const upload = api.calls('POST', '/files')[0][1]
    expect(upload.body).toBeInstanceOf(FormData)
    expect(upload.headers['X-Requested-From']).toBe('web')
    await waitFor(() => expect(onChanged).toHaveBeenCalled())
  })

  it('thiếu bản ký hoặc mã người thân sai thì báo tại ô và không gọi backend', async () => {
    routes({ consents: [] })
    open({ allowedCommands: ['RECORD_CONSENT'] })

    fireEvent.click(await screen.findByRole('button', { name: 'Ghi nhận bản đồng ý' }))
    fireEvent.change(screen.getByLabelText('Mã người thân được xem sức khỏe'), { target: { value: 'abc' } })
    fireEvent.click(screen.getByRole('button', { name: 'Thêm mã' }))
    expect(await screen.findByText('Mã người thân là số nguyên dương')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Lưu bản đồng ý' }))
    expect(await screen.findByText('Chọn tệp bản ký đã scan')).toBeInTheDocument()
    expect(api.calls('POST', '/residents/5/consents')).toHaveLength(0)
    expect(api.calls('POST', '/files')).toHaveLength(0)
  })

  it('người đại diện theo pháp luật: bắt buộc mã người ký và đủ quyết định của Tòa án, gửi kèm tệp quyết định', async () => {
    routes({ consents: [] })
    api.on('POST', '/files', () => json(201, { fileId: 100 + api.calls('POST', '/files').length }))
    api.on('POST', '/residents/5/consents', json(201, OLD))
    open({ allowedCommands: ['RECORD_CONSENT'] })

    fireEvent.click(await screen.findByRole('button', { name: 'Ghi nhận bản đồng ý' }))
    fireEvent.click(screen.getByRole('radio', { name: 'Người đại diện theo pháp luật' }))
    choose('Bản ký đã scan', pdf())
    fireEvent.click(screen.getByRole('button', { name: 'Lưu bản đồng ý' }))
    expect(await screen.findByText('Nhập mã người thân là người ký')).toBeInTheDocument()
    expect(screen.getByText('Nhập số quyết định')).toBeInTheDocument()
    expect(screen.getByText('Chọn tệp quyết định của Tòa án')).toBeInTheDocument()
    expect(api.calls('POST', '/residents/5/consents')).toHaveLength(0)

    fireEvent.change(screen.getByLabelText('Mã người thân là người ký'), { target: { value: '41' } })
    fireEvent.change(screen.getByLabelText('Số quyết định'), { target: { value: '12/2026/QĐ-TA' } })
    fireEvent.change(screen.getByLabelText('Ngày quyết định'), { target: { value: '2026-03-05' } })
    fireEvent.change(screen.getByLabelText('Tòa án'), { target: { value: 'Tòa án nhân dân quận 1' } })
    choose('Bản scan quyết định của Tòa án', pdf('quyet-dinh.pdf'))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu bản đồng ý' }))

    await waitFor(() => expect(api.calls('POST', '/residents/5/consents')).toHaveLength(1))
    const body = JSON.parse(api.calls('POST', '/residents/5/consents')[0][1].body)
    expect(body.consenterType).toBe('LEGAL_REPRESENTATIVE')
    expect(body.consenterFamilyMemberId).toBe(41)
    expect(body.courtDecision).toMatchObject({ number: '12/2026/QĐ-TA', date: '2026-03-05', court: 'Tòa án nhân dân quận 1' })
    expect(body.courtDecision.fileId).toBeGreaterThan(0)
    expect(body.evidenceFileId).not.toBe(body.courtDecision.fileId)
  })

  it('backend từ chối người ký thay không có quan hệ: báo tại ô mã người ký', async () => {
    routes({ consents: [] })
    api.on('POST', '/files', json(201, { fileId: 5 }))
    api.on('POST', '/residents/5/consents', problem(409, 'INVALID_SIGNER'))
    open({ allowedCommands: ['RECORD_CONSENT'] })

    fireEvent.click(await screen.findByRole('button', { name: 'Ghi nhận bản đồng ý' }))
    fireEvent.click(screen.getByRole('radio', { name: 'Người đại diện theo pháp luật' }))
    choose('Bản ký đã scan', pdf())
    fireEvent.change(screen.getByLabelText('Mã người thân là người ký'), { target: { value: '99' } })
    fireEvent.change(screen.getByLabelText('Số quyết định'), { target: { value: '1' } })
    fireEvent.change(screen.getByLabelText('Ngày quyết định'), { target: { value: '2026-03-05' } })
    fireEvent.change(screen.getByLabelText('Tòa án'), { target: { value: 'TA' } })
    choose('Bản scan quyết định của Tòa án', pdf('qd.pdf'))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu bản đồng ý' }))

    expect(await screen.findByText(/phải là người thân có quan hệ Hiệu lực/)).toBeInTheDocument()
  })

  it('tệp sai kiểu bị backend từ chối: báo tại ô tệp, không gửi bản đồng ý', async () => {
    routes({ consents: [] })
    api.on('POST', '/files', problem(422, 'FILE_TYPE_NOT_ALLOWED'))
    open({ allowedCommands: ['RECORD_CONSENT'] })

    fireEvent.click(await screen.findByRole('button', { name: 'Ghi nhận bản đồng ý' }))
    choose('Bản ký đã scan', new File(['x'], 'a.txt', { type: 'text/plain' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu bản đồng ý' }))

    expect(await screen.findByText(/Tệp phải là PDF, JPEG hoặc PNG/, {}, { timeout: 3000 })).toBeInTheDocument()
    expect(api.calls('POST', '/residents/5/consents')).toHaveLength(0)
  })

  it('rút lại đồng ý: lý do và bản yêu cầu rút lại bắt buộc, gửi tới /consents/{id}/withdraw', async () => {
    routes()
    api.on('POST', '/files', json(201, { fileId: 95 }))
    api.on('POST', '/consents/12/withdraw', json(200, { ...ACTIVE, status: 'WITHDRAWN' }))
    open({ allowedCommands: ['WITHDRAW_CONSENT'] })

    fireEvent.click(await screen.findByRole('button', { name: 'Rút lại đồng ý' }))
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận rút lại' }))
    expect(await screen.findByText('Nhập lý do rút lại')).toBeInTheDocument()
    expect(screen.getByText('Chọn tệp bản yêu cầu rút lại')).toBeInTheDocument()
    expect(api.calls('POST', '/consents/12/withdraw')).toHaveLength(0)

    fireEvent.change(screen.getByLabelText('Lý do rút lại (bắt buộc)'), { target: { value: 'Người đại diện yêu cầu' } })
    choose('Bản yêu cầu rút lại đã scan', pdf('rut-lai.pdf'))
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận rút lại' }))

    await waitFor(() => expect(api.calls('POST', '/consents/12/withdraw')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/consents/12/withdraw')[0][1].body)).toEqual({
      reason: 'Người đại diện yêu cầu',
      evidenceFileId: 95,
    })
  })

  it('tải bản ký đi qua backend (có cookie phiên), mở nội dung ở thẻ mới; không có quyền thì báo', async () => {
    routes()
    api.on('GET', '/files/90', () => new Response(new Blob(['%PDF']), { status: 200, headers: { 'Content-Type': 'application/pdf' } }))
    api.on('GET', '/files/71', problem(403, 'PERMISSION_DENIED'))
    const createObjectURL = vi.fn(() => 'blob:ban-ky')
    vi.stubGlobal('URL', Object.assign(URL, { createObjectURL, revokeObjectURL: vi.fn() }))
    const opened = vi.spyOn(window, 'open').mockReturnValue(null)
    open()

    const now = await screen.findByRole('region', { name: 'Bản đồng ý hiện hành' })
    fireEvent.click(within(now).getByRole('button', { name: 'Tải bản ký' }))
    await waitFor(() => expect(opened).toHaveBeenCalledWith('blob:ban-ky', '_blank', 'noopener'))
    const [url, init] = api.fetchMock.mock.calls.find(([u]) => String(u).endsWith('/files/90'))
    expect(String(url)).toBe('/api/v1/files/90')
    expect(init.credentials).toBe('include')

    const history = screen.getByRole('list', { name: 'Lịch sử bản đồng ý' })
    fireEvent.click(within(history).getByRole('button', { name: 'Tải bản ký' }))
    expect(await screen.findByText('Không có quyền thực hiện')).toBeInTheDocument()
  })
})

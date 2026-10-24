import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { StatusTab } from './StatusTab'

// 001 T052 (US4; FR-044 → FR-047b): tab Trạng thái trong khung chi tiết hồ sơ. Nút lệnh theo allowedCommands, xem trước
// điều kiện chưa thỏa trước khi gửi, lý do bắt buộc, lỗi 409 và 422 báo trong khung, lịch sử trạng thái.

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

const HISTORY = [
  {
    from: null,
    to: 'ADMISSION_IN_PROGRESS',
    command: 'CREATE_PROFILE',
    at: '2026-10-01T02:00:00Z',
    performedBy: 'Hành chính Lan',
    bySystem: false,
    reason: null,
    basis: null,
    conditionWaiver: null,
  },
  {
    from: 'IN_RESIDENCE',
    to: 'OFF_SITE_ACTIVITY',
    command: 'ROLL_CALL_DEPARTURE',
    at: '2026-10-10T01:00:00Z',
    performedBy: 'Điều dưỡng Hoa',
    bySystem: true,
    reason: null,
    basis: { type: 'SOURCE_RECORD', id: 4 },
    conditionWaiver: null,
  },
  {
    from: 'IN_RESIDENCE',
    to: 'RESIDENCY_ENDED',
    command: 'END_RESIDENCY',
    at: '2026-10-20T01:00:00Z',
    performedBy: 'Hành chính Lan',
    bySystem: false,
    reason: 'Gia đình đón về',
    basis: null,
    conditionWaiver: { approvalRequestId: 501, conditionCodes: ['UNPAID_FEES'] },
  },
]

function routes({ history = HISTORY, conditions = [], forbidden = false } = {}) {
  api.on('GET', '/residents/5/status-history', forbidden ? problem(403, 'PERMISSION_DENIED') : json(200, history))
  api.on('GET', (u) => u.pathname.startsWith('/api/v1/residents/5/conditions/'), json(200, conditions))
}

const pdf = (name = 'giay-to.pdf') => new File(['%PDF-1.4'], name, { type: 'application/pdf' })

function pick(label, files) {
  fireEvent.change(screen.getByLabelText(label), { target: { files } })
}

function open(props = {}) {
  return render(<StatusTab residentId={5} status="IN_RESIDENCE" allowedCommands={[]} {...props} />)
}

// Nút xác nhận bị khóa tới khi xem trước điều kiện xong
async function submitReady(name) {
  const button = await screen.findByRole('button', { name })
  await waitFor(() => expect(button).toBeEnabled())
  fireEvent.click(button)
}

const result = (status) => json(200, { status, history: HISTORY[0], warnings: [] })

describe('StatusTab', () => {
  it('hiện trạng thái hiện tại và lịch sử: dòng do hệ thống ghi rõ, miễn điều kiện có dấu', async () => {
    routes()
    open()

    expect(await screen.findByText('Hành chính Lan, tạo hồ sơ')).toBeInTheDocument()
    const list = screen.getByRole('list', { name: 'Lịch sử trạng thái' })
    expect(within(list).getByText(/Hệ thống \(điểm danh\)/)).toBeInTheDocument()
    expect(within(list).getByText('Gia đình đón về')).toBeInTheDocument()
    expect(within(list).getByText('Miễn điều kiện')).toBeInTheDocument()
    expect(screen.getByText('Trạng thái hiện tại')).toBeInTheDocument()
  })

  it('nút lệnh chỉ hiện theo allowedCommands; hồ sơ trạng thái cuối không có nút', async () => {
    routes()
    const view = open({ allowedCommands: ['GRANT_TEMPORARY_ABSENCE', 'END_RESIDENCY'] })
    expect(await screen.findByRole('button', { name: 'Cho tạm vắng' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Kết thúc lưu trú' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Chuyển viện' })).not.toBeInTheDocument()
    view.unmount()

    routes()
    open({ status: 'DECEASED', allowedCommands: [] })
    expect(await screen.findByText(/chỉ xem/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Cho tạm vắng|Kết thúc/ })).not.toBeInTheDocument()
  })

  it('vai trò không được xem thì hiện trạng thái không có quyền', async () => {
    routes({ forbidden: true })
    open()
    expect(await screen.findByText('Không có quyền xem lịch sử trạng thái')).toBeInTheDocument()
  })

  it('mở lệnh thì xem trước điều kiện: còn điều kiện chưa thỏa thì không gửi được', async () => {
    routes({
      conditions: [
        { code: 'BELONGINGS', description: 'Còn đồ gửi chưa trả', basis: 'BR-M12-03', waivable: false },
        { code: 'UNPAID', description: 'Còn khoản chưa thanh toán', basis: 'BR-M11-22', waivable: true },
      ],
    })
    open({ allowedCommands: ['END_RESIDENCY'] })

    fireEvent.click(await screen.findByRole('button', { name: 'Kết thúc lưu trú' }))

    expect(await screen.findByText('Còn đồ gửi chưa trả')).toBeInTheDocument()
    expect(screen.getByText('Còn khoản chưa thanh toán')).toBeInTheDocument()
    expect(screen.getByText('Miễn được')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Xác nhận kết thúc lưu trú' })).toBeDisabled()
  })

  it('thiếu lý do thì báo tại ô và không gửi', async () => {
    routes()
    open({ allowedCommands: ['GRANT_TEMPORARY_ABSENCE'] })

    fireEvent.click(await screen.findByRole('button', { name: 'Cho tạm vắng' }))
    await submitReady('Xác nhận cho tạm vắng')

    expect(await screen.findByText('Nhập lý do của lệnh')).toBeInTheDocument()
    expect(api.calls('POST', '/residents/5/grant-temporary-absence')).toHaveLength(0)
  })

  it('gửi lệnh: POST có Idempotency-Key và lý do; xong báo kết quả và nạp lại', async () => {
    routes()
    api.on('POST', '/residents/5/grant-temporary-absence', result('TEMPORARILY_ABSENT'))
    const onChanged = vi.fn()
    open({ allowedCommands: ['GRANT_TEMPORARY_ABSENCE'], onChanged })

    fireEvent.click(await screen.findByRole('button', { name: 'Cho tạm vắng' }))
    fireEvent.change(await screen.findByLabelText(/Lý do/), { target: { value: 'Gia đình đón cuối tuần' } })
    await submitReady('Xác nhận cho tạm vắng')

    await waitFor(() => expect(api.calls('POST', '/residents/5/grant-temporary-absence')).toHaveLength(1))
    const [, init] = api.calls('POST', '/residents/5/grant-temporary-absence')[0]
    expect(new Headers(init.headers).get('Idempotency-Key')).toBeTruthy()
    expect(JSON.parse(init.body)).toMatchObject({ reason: 'Gia đình đón cuối tuần' })
    expect(await screen.findByRole('status')).toHaveTextContent('Đã chuyển sang Tạm vắng')
    expect(onChanged).toHaveBeenCalled()
  })

  it('Chuyển viện bắt buộc căn cứ: gửi details.basis theo loại đã chọn', async () => {
    routes()
    api.on('POST', '/residents/5/transfer-out', result('HOSPITALIZED'))
    open({ allowedCommands: ['TRANSFER_OUT'] })

    fireEvent.click(await screen.findByRole('button', { name: 'Chuyển viện' }))
    fireEvent.change(await screen.findByLabelText(/Lý do/), { target: { value: 'Đau ngực' } })
    await submitReady('Xác nhận chuyển viện')
    expect(await screen.findByText('Chọn và nhập căn cứ chuyển viện')).toBeInTheDocument()
    expect(api.calls('POST', '/residents/5/transfer-out')).toHaveLength(0)

    fireEvent.click(screen.getByRole('radio', { name: 'Chỉ định của Bác sĩ' }))
    fireEvent.change(screen.getByLabelText('Nội dung căn cứ'), { target: { value: 'Chuyển cấp cứu tim mạch' } })
    await submitReady('Xác nhận chuyển viện')

    await waitFor(() => expect(api.calls('POST', '/residents/5/transfer-out')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/residents/5/transfer-out')[0][1].body).details).toEqual({
      basis: { doctorOrder: 'Chuyển cấp cứu tim mạch' },
    })
  })

  it('409 điều kiện chưa thỏa khi gửi: liệt kê trong khung đang thao tác', async () => {
    routes()
    api.on(
      'POST',
      '/residents/5/end-residency',
      problem(409, 'UNMET_CONDITIONS', {
        detail: 'Còn điều kiện chưa thỏa',
        unmetConditions: [{ code: 'X', description: 'Còn thuốc gửi chưa trả', basis: 'BR-M07-01', waivable: false }],
      }),
    )
    open({ allowedCommands: ['END_RESIDENCY'] })

    fireEvent.click(await screen.findByRole('button', { name: 'Kết thúc lưu trú' }))
    fireEvent.change(await screen.findByLabelText(/Lý do/), { target: { value: 'Hết hợp đồng' } })
    await submitReady('Xác nhận kết thúc lưu trú')

    const alerts = await screen.findAllByRole('alert')
    expect(alerts.some((a) => a.textContent.includes('Còn thuốc gửi chưa trả'))).toBe(true)
  })

  it('Ghi nhận qua đời thiếu giấy tờ bằng chứng (422) báo tại ô chọn tệp', async () => {
    routes()
    api.on('POST', '/residents/5/record-death', problem(422, 'EVIDENCE_REQUIRED', { detail: 'Phải kèm giấy tờ' }))
    open({ status: 'TEMPORARILY_ABSENT', allowedCommands: ['RECORD_DEATH'] })

    fireEvent.click(await screen.findByRole('button', { name: 'Ghi nhận qua đời' }))
    fireEvent.change(await screen.findByLabelText(/Lý do/), { target: { value: 'Mất tại nhà' } })
    await submitReady('Xác nhận ghi nhận qua đời')

    expect(await screen.findByText(/Phải kèm giấy tờ bằng chứng/)).toBeInTheDocument()
    expect(screen.getByLabelText(/Giấy tờ bằng chứng/)).toHaveAttribute('aria-invalid', 'true')
  })

  it('Ghi nhận qua đời: tải tệp bằng chứng lên rồi gửi details.evidenceFileId', async () => {
    routes()
    api.on('POST', '/files', json(201, { fileId: 4242 }))
    api.on('POST', '/residents/5/record-death', result('DECEASED'))
    open({ status: 'TEMPORARILY_ABSENT', allowedCommands: ['RECORD_DEATH'] })

    fireEvent.click(await screen.findByRole('button', { name: 'Ghi nhận qua đời' }))
    fireEvent.change(await screen.findByLabelText(/Lý do/), { target: { value: 'Mất tại nhà' } })
    pick(/Giấy tờ bằng chứng/, [pdf('giay-bao-tu.pdf')])
    await submitReady('Xác nhận ghi nhận qua đời')

    await waitFor(() => expect(api.calls('POST', '/residents/5/record-death')).toHaveLength(1))
    expect(api.calls('POST', '/files')).toHaveLength(1)
    expect(JSON.parse(api.calls('POST', '/residents/5/record-death')[0][1].body).details).toEqual({
      evidenceFileId: 4242,
    })
  })

  describe('giấy tờ bệnh viện (T082; FR-047c)', () => {
    it('Ghi nhận trở về từ Điều trị tại bệnh viện: tải từng tệp rồi gửi attachmentFileIds', async () => {
      routes()
      let next = 90
      api.on('POST', '/files', () => json(201, { fileId: next++ }))
      api.on('POST', '/residents/5/record-return', result('IN_RESIDENCE'))
      open({ status: 'HOSPITALIZED', allowedCommands: ['RECORD_RETURN'] })

      fireEvent.click(await screen.findByRole('button', { name: 'Ghi nhận trở về' }))
      fireEvent.change(await screen.findByLabelText(/Lý do/), { target: { value: 'Ra viện' } })
      pick(/Giấy tờ bệnh viện/, [pdf('giay-ra-vien.pdf'), pdf('don-thuoc.pdf')])
      expect(screen.getByText('giay-ra-vien.pdf')).toBeInTheDocument()
      expect(screen.getByText('don-thuoc.pdf')).toBeInTheDocument()
      await submitReady('Xác nhận ghi nhận trở về')

      await waitFor(() => expect(api.calls('POST', '/residents/5/record-return')).toHaveLength(1))
      expect(api.calls('POST', '/files')).toHaveLength(2)
      expect(JSON.parse(api.calls('POST', '/residents/5/record-return')[0][1].body)).toMatchObject({
        reason: 'Ra viện',
        attachmentFileIds: [90, 91],
      })
    })

    it('bỏ một tệp đã chọn thì không tải và không gửi tệp đó', async () => {
      routes()
      api.on('POST', '/files', json(201, { fileId: 7 }))
      api.on('POST', '/residents/5/transfer-out', result('HOSPITALIZED'))
      open({ allowedCommands: ['TRANSFER_OUT'] })

      fireEvent.click(await screen.findByRole('button', { name: 'Chuyển viện' }))
      fireEvent.change(await screen.findByLabelText(/Lý do/), { target: { value: 'Đau ngực' } })
      fireEvent.click(screen.getByRole('radio', { name: 'Chỉ định của Bác sĩ' }))
      fireEvent.change(screen.getByLabelText('Nội dung căn cứ'), { target: { value: 'Cấp cứu' } })
      pick(/Giấy tờ bệnh viện/, [pdf('a.pdf'), pdf('b.pdf')])
      fireEvent.click(screen.getByRole('button', { name: 'Bỏ a.pdf' }))
      await submitReady('Xác nhận chuyển viện')

      await waitFor(() => expect(api.calls('POST', '/residents/5/transfer-out')).toHaveLength(1))
      expect(api.calls('POST', '/files')).toHaveLength(1)
      expect(JSON.parse(api.calls('POST', '/residents/5/transfer-out')[0][1].body).attachmentFileIds).toEqual([7])
    })

    it('không chọn tệp: gửi lệnh bình thường, không có attachmentFileIds', async () => {
      routes()
      api.on('POST', '/residents/5/record-return', result('IN_RESIDENCE'))
      open({ status: 'HOSPITALIZED', allowedCommands: ['RECORD_RETURN'] })

      fireEvent.click(await screen.findByRole('button', { name: 'Ghi nhận trở về' }))
      fireEvent.change(await screen.findByLabelText(/Lý do/), { target: { value: 'Ra viện' } })
      await submitReady('Xác nhận ghi nhận trở về')

      await waitFor(() => expect(api.calls('POST', '/residents/5/record-return')).toHaveLength(1))
      expect(api.calls('POST', '/files')).toHaveLength(0)
      expect(JSON.parse(api.calls('POST', '/residents/5/record-return')[0][1].body)).not.toHaveProperty(
        'attachmentFileIds',
      )
    })

    it('Ghi nhận trở về từ Tạm vắng không có ô giấy tờ bệnh viện', async () => {
      routes()
      open({ status: 'TEMPORARILY_ABSENT', allowedCommands: ['RECORD_RETURN'] })

      fireEvent.click(await screen.findByRole('button', { name: 'Ghi nhận trở về' }))
      await screen.findByLabelText(/Lý do/)
      expect(screen.queryByLabelText(/Giấy tờ bệnh viện/)).not.toBeInTheDocument()
    })

    it('chọn quá 10 tệp: báo tại ô, không tải và không gửi lệnh', async () => {
      routes()
      open({ status: 'HOSPITALIZED', allowedCommands: ['RECORD_RETURN'] })

      fireEvent.click(await screen.findByRole('button', { name: 'Ghi nhận trở về' }))
      fireEvent.change(await screen.findByLabelText(/Lý do/), { target: { value: 'Ra viện' } })
      pick(
        /Giấy tờ bệnh viện/,
        Array.from({ length: 11 }, (_, n) => pdf(`t${n}.pdf`)),
      )
      await submitReady('Xác nhận ghi nhận trở về')

      expect(await screen.findByText('Tối đa 10 tệp')).toBeInTheDocument()
      expect(api.calls('POST', '/files')).toHaveLength(0)
      expect(api.calls('POST', '/residents/5/record-return')).toHaveLength(0)
    })

    it('tệp quá lớn (422 FILE_TOO_LARGE) báo tại ô; lệnh không được gửi', async () => {
      routes()
      api.on('POST', '/files', problem(422, 'FILE_TOO_LARGE'))
      open({ status: 'HOSPITALIZED', allowedCommands: ['RECORD_RETURN'] })

      fireEvent.click(await screen.findByRole('button', { name: 'Ghi nhận trở về' }))
      fireEvent.change(await screen.findByLabelText(/Lý do/), { target: { value: 'Ra viện' } })
      pick(/Giấy tờ bệnh viện/, [pdf()])
      await submitReady('Xác nhận ghi nhận trở về')

      expect(await screen.findByText('Tệp quá 10 MB')).toBeInTheDocument()
      expect(api.calls('POST', '/residents/5/record-return')).toHaveLength(0)
    })

    it('lệnh lỗi sau khi tải: gửi lại không tải lên lần nữa', async () => {
      routes()
      api.on('POST', '/files', json(201, { fileId: 55 }))
      let attempts = 0
      api.on('POST', '/residents/5/record-return', () =>
        attempts++ === 0 ? problem(500, 'INTERNAL') : result('IN_RESIDENCE'),
      )
      open({ status: 'HOSPITALIZED', allowedCommands: ['RECORD_RETURN'] })

      fireEvent.click(await screen.findByRole('button', { name: 'Ghi nhận trở về' }))
      fireEvent.change(await screen.findByLabelText(/Lý do/), { target: { value: 'Ra viện' } })
      pick(/Giấy tờ bệnh viện/, [pdf()])
      await submitReady('Xác nhận ghi nhận trở về')
      await waitFor(() => expect(api.calls('POST', '/residents/5/record-return')).toHaveLength(1))
      await submitReady('Xác nhận ghi nhận trở về')

      await waitFor(() => expect(api.calls('POST', '/residents/5/record-return')).toHaveLength(2))
      expect(api.calls('POST', '/files')).toHaveLength(1)
    })
  })
})

import { beforeEach, afterEach, expect, it, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { mockRoutes, json, problem } from '@/test/fetchRoutes'
import { PostPublicationPanel } from './PostPublicationPanel'

const SHIFT = {
  id: 20,
  startsAt: '2026-10-25T00:00:00Z',
  endsAt: '2026-10-25T08:00:00Z',
  leadStaffId: 10,
  staff: [{ staffId: 10 }, { staffId: 11 }],
  allowedCommands: ['addStaff', 'changeLead', 'recordAbsence', 'cancelShift'],
}
const STAFF = [
  { id: 10, fullName: '[Điều dưỡng 1]', status: 'WORKING' },
  { id: 11, fullName: '[Nhân viên 2]', status: 'WORKING' },
  { id: 12, fullName: '[Điều dưỡng 3]', status: 'WORKING' },
]
let api
beforeEach(() => {
  api = mockRoutes()
})
afterEach(() => vi.unstubAllGlobals())
function fillReason() {
  fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: '[Lý do kiểm thử]' } })
}
function submit() {
  fireEvent.submit(screen.getByLabelText('Lý do').closest('form'))
}

it('FR_017_chiHienLenhBackendChoPhep', () => {
  render(<PostPublicationPanel shift={{ ...SHIFT, allowedCommands: ['recordAbsence'] }} staff={STAFF} />)
  expect(screen.getByRole('button', { name: 'Ghi nhận vắng ca' })).toBeVisible()
  expect(screen.queryByRole('button', { name: 'Bổ sung nhân viên' })).not.toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Hủy ca' })).not.toBeInTheDocument()
})

it('FR_020_chonNguoiVangNguoiThayVaLyDo_guiDungHopDong', async () => {
  const done = vi.fn()
  api.on('POST', '/shifts/20/record-absence', json(200, null))
  render(<PostPublicationPanel shift={SHIFT} staff={STAFF} onDone={done} />)
  fireEvent.click(screen.getByRole('button', { name: 'Ghi nhận vắng ca' }))
  fireEvent.change(screen.getByLabelText('Nhân viên vắng'), { target: { value: '10' } })
  fireEvent.change(screen.getByLabelText('Người phụ trách thay thế'), { target: { value: '11' } })
  fillReason()
  submit()
  await waitFor(() => expect(done).toHaveBeenCalledOnce())
  const call = api.calls('POST', '/shifts/20/record-absence')[0]
  expect(JSON.parse(call[1].body)).toEqual({ staffId: 10, replacementLeadStaffId: 11, reason: '[Lý do kiểm thử]' })
})

it('FR_053_lyDoBatBuocKhongGuiKhiTrong', () => {
  render(<PostPublicationPanel shift={SHIFT} staff={STAFF} />)
  fireEvent.click(screen.getByRole('button', { name: 'Hủy ca' }))
  submit()
  expect(screen.getByLabelText('Lý do')).toHaveAttribute('aria-invalid', 'true')
  expect(api.calls('POST', '/shifts/20/cancel')).toHaveLength(0)
})

it('FR_020_ngoaiKhoangHienHuongDanNghiDotXuatVaTaiLai', async () => {
  const reload = vi.fn()
  api.on('POST', '/shifts/20/record-absence', problem(409, 'OUTSIDE_ABSENCE_REPORTING_WINDOW'))
  render(<PostPublicationPanel shift={SHIFT} staff={STAFF} onDone={reload} />)
  fireEvent.click(screen.getByRole('button', { name: 'Ghi nhận vắng ca' }))
  fireEvent.change(screen.getByLabelText('Nhân viên vắng'), { target: { value: '11' } })
  fillReason()
  submit()
  expect(await screen.findByText(/Hãy lập yêu cầu nghỉ đột xuất/)).toBeVisible()
  fireEvent.click(screen.getByRole('button', { name: 'Tải lại ca' }))
  expect(reload).toHaveBeenCalledOnce()
})

it('FR_018_canhBaoBoSungPhaiXacNhanVaKhongTuDienLyDo', async () => {
  api.on(
    'POST',
    '/shifts/20/add-staff',
    problem(409, 'WARNING_CONFIRMATION_REQUIRED', {
      warnings: [{ code: 'CONTINUOUS_WORK_HOURS', description: 'Chuỗi ca liên tục vượt ngưỡng' }],
    }),
  )
  render(<PostPublicationPanel shift={SHIFT} staff={STAFF} />)
  fireEvent.click(screen.getByRole('button', { name: 'Bổ sung nhân viên' }))
  fireEvent.change(screen.getByLabelText('Nhân viên bổ sung'), { target: { value: '12' } })
  fillReason()
  submit()
  expect(await screen.findByText('Chuỗi ca liên tục vượt ngưỡng')).toBeVisible()
  fireEvent.click(screen.getByRole('checkbox', { name: 'Đã xem cảnh báo' }))
  api.on('POST', '/shifts/20/add-staff', json(200, {}))
  submit()
  await waitFor(() => expect(api.calls('POST', '/shifts/20/add-staff')).toHaveLength(2))
  expect(JSON.parse(api.calls('POST', '/shifts/20/add-staff')[1][1].body).confirmWarnings).toBe(true)
})

it('FR_056_traCuuCanCuTheoKhoangThoiGianVaPhamViDuocCap', async () => {
  api.on(
    'GET',
    '/duty-basis-audit-logs',
    json(200, [
      {
        auditId: 1,
        staffId: 10,
        basis: 'SHIFT_LEAD',
        at: '2026-10-25T00:20:00Z',
        targetType: 'M09.STAFF_ABSENCE',
        targetId: '30',
      },
    ]),
  )
  render(
    <PostPublicationPanel shift={{ ...SHIFT, allowedCommands: ['viewDutyBasisAudit'] }} staff={STAFF} scopeId={5} />,
  )
  fireEvent.click(screen.getByRole('button', { name: 'Lịch sử căn cứ nhiệm vụ' }))
  expect(await screen.findByText('Nhiệm vụ Người phụ trách ca')).toBeVisible()
  const call = api.calls('GET', '/duty-basis-audit-logs')[0]
  expect(new URL(call[0], 'http://localhost').searchParams.get('floorAreaId')).toBe('5')
})

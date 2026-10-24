import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { ShiftsPage } from './ShiftsPage'

const TEMPLATE = {
  id: 3,
  name: '[Mẫu ca kiểm thử]',
  startTime: '07:00',
  endTime: '15:00',
  scopeType: 'FACILITY',
  handoverRequired: false,
  active: true,
  version: 1,
}
const SHIFT = {
  id: 20,
  shiftTemplateId: 3,
  startsAt: '2026-11-01T00:00:00Z',
  endsAt: '2026-11-01T08:00:00Z',
  status: 'DRAFT',
  staff: [{ staffId: 10 }],
  allowedCommands: ['edit', 'delete'],
  version: 0,
}
let api
beforeEach(() => {
  api = mockRoutes()
  api.on(
    'GET',
    '/me/permissions',
    json(
      200,
      ['VIEW', 'CREATE', 'UPDATE', 'APPROVE'].map((operation) => ({ function: 'SHIFT_SCHEDULE', operation })),
    ),
  )
  api.on('GET', '/shift-templates', json(200, [TEMPLATE]))
  api.on(
    'GET',
    '/staff',
    json(200, [{ id: 10, fullName: '[Nhân viên kiểm thử]', position: 'Điều dưỡng', status: 'WORKING' }]),
  )
  api.on(
    'GET',
    '/shift-schedules',
    json(200, [{ id: 7, month: '2026-11', scopeType: 'FACILITY', status: 'DRAFT', version: 2, shifts: [] }]),
  )
})
afterEach(() => vi.unstubAllGlobals())

it('FR_025_moPhanCongCuaCa_vaGiaoTruongTangTuLich', async () => {
  api.on(
    'GET',
    '/shift-schedules',
    json(200, [
      { id: 7, month: '2026-11', scopeType: 'FLOOR', scopeId: 5, status: 'DRAFT', version: 2, shifts: [SHIFT] },
    ]),
  )
  api.on('GET', '/shifts/20', json(200, { ...SHIFT, allowedCommands: ['createAssignment'] }))
  api.on('GET', '/shifts/20/assignments', json(200, []))
  api.on('GET', '/floor-manager-assignments', json(200, []))
  render(<ShiftsPage initialMonth="2026-11" />)
  fireEvent.click(await screen.findByRole('button', { name: 'Phân công' }))
  expect(await screen.findByText('Chưa có phân công trong ca.')).toBeVisible()
  fireEvent.keyDown(document, { key: 'Escape' })
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  fireEvent.click(screen.getByRole('button', { name: 'Giao Trưởng tầng' }))
  expect(await screen.findByLabelText('Ngày bắt đầu giao')).toBeVisible()
})

it('FR_016_khongBatBuocLyDoNeuBackendKhongYeuCau_vaChoBoSungKhiTuDuyet', async () => {
  api.on(
    'GET',
    '/shift-schedules/7/publication-preview',
    json(200, { unmetConditions: [], warnings: [], invalidAssignments: [] }),
  )
  api.on('POST', '/shift-schedules/7/publish', problem(422, 'REASON_REQUIRED', { detail: 'Tự duyệt cần lý do' }))
  render(<ShiftsPage initialMonth="2026-11" />)
  fireEvent.click(await screen.findByRole('button', { name: 'Xem trước công bố' }))
  const publish = await screen.findByRole('button', { name: 'Công bố lịch' })
  expect(publish).toBeEnabled()
  fireEvent.click(publish)
  expect(await screen.findByText('Tự duyệt cần lý do')).toBeVisible()
  expect(publish).toBeDisabled()
  fireEvent.change(screen.getByLabelText('Lý do công bố / tự duyệt'), { target: { value: 'Đã kiểm tra lịch tự lập' } })
  expect(publish).toBeEnabled()
})

it('FR_014_themCa_guiPhienBan_vaChiXacNhanCanhBaoKhiCoLyDo', async () => {
  let attempts = 0
  api.on('POST', '/shift-schedules/7/shifts', () =>
    ++attempts === 1
      ? problem(409, 'WARNING_CONFIRMATION_REQUIRED', {
          warnings: [
            { code: 'CONTINUOUS_WORK_HOURS', description: 'Chuỗi ca liên tục vượt ngưỡng cấu hình', details: {} },
          ],
        })
      : json(201, { id: 20, warnings: [] }),
  )
  render(<ShiftsPage initialMonth="2026-11" />)
  fireEvent.click(await screen.findByRole('button', { name: 'Thêm ca' }))
  fireEvent.change(screen.getByLabelText('Ngày'), { target: { value: '2026-11-01' } })
  fireEvent.change(screen.getByLabelText('Mẫu ca'), { target: { value: '3' } })
  fireEvent.click(screen.getByLabelText('[Nhân viên kiểm thử]'))
  fireEvent.click(screen.getByRole('button', { name: 'Lưu ca' }))
  expect(await screen.findByText('Chuỗi ca liên tục vượt ngưỡng cấu hình')).toBeVisible()
  expect(api.calls('POST', '/shift-schedules/7/shifts')).toHaveLength(1)
  fireEvent.click(screen.getByLabelText('Tôi đã xem các cảnh báo'))
  fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Đã rà soát việc xếp ca' } })
  fireEvent.click(screen.getByRole('button', { name: 'Xác nhận và lưu' }))
  await waitFor(() => expect(api.calls('POST', '/shift-schedules/7/shifts')).toHaveLength(2))
  const [, first] = api.calls('POST', '/shift-schedules/7/shifts')[0]
  const [, second] = api.calls('POST', '/shift-schedules/7/shifts')[1]
  expect(first.headers['If-Match']).toBe('"2"')
  expect(JSON.parse(second.body)).toMatchObject({ confirmWarnings: true, reason: 'Đã rà soát việc xếp ca' })
  expect(second.headers['Idempotency-Key']).not.toBe(first.headers['Idempotency-Key'])
})

it('FR_016_lietKeDieuKienChan_vaKhongGuiLenhCongBo', async () => {
  api.on(
    'GET',
    '/shift-schedules/7/publication-preview',
    json(200, {
      unmetConditions: [{ code: 'SHIFT_LEAD_REQUIRED', description: 'Ca chưa có người phụ trách' }],
      warnings: [],
      invalidAssignments: [],
    }),
  )
  render(<ShiftsPage initialMonth="2026-11" />)
  fireEvent.click(await screen.findByRole('button', { name: 'Xem trước công bố' }))
  expect(await screen.findByText('Ca chưa có người phụ trách')).toBeVisible()
  expect(screen.getByRole('button', { name: 'Công bố lịch' })).toBeDisabled()
  expect(api.calls('POST', '/shift-schedules/7/publish')).toHaveLength(0)
})

it('FR_017_phienBanCuBao412_vaChoTaiLaiThayViTuGhiDe', async () => {
  api.on(
    'GET',
    '/shift-schedules',
    json(200, [{ id: 7, month: '2026-11', scopeType: 'FACILITY', status: 'DRAFT', version: 2, shifts: [SHIFT] }]),
  )
  api.on('GET', '/shifts/20', json(200, SHIFT, { ETag: '"0"' }))
  api.on('PUT', '/shifts/20', problem(412, 'VERSION_MISMATCH', { currentValue: { ...SHIFT, version: 1 } }))
  render(<ShiftsPage initialMonth="2026-11" />)
  fireEvent.click(await screen.findByRole('button', { name: 'Sửa ca' }))
  fireEvent.click(await screen.findByRole('button', { name: 'Lưu ca' }))
  expect(await screen.findByText('Dữ liệu đã thay đổi')).toBeVisible()
  expect(screen.getByRole('button', { name: 'Tải lại ca' })).toBeVisible()
  expect(screen.getByRole('button', { name: 'Lưu ca' })).toBeDisabled()
  expect(api.calls('PUT', '/shifts/20')).toHaveLength(1)
})

it('FR_054_nguoiChiCoQuyenXem_chiTaiCaCuaMinh_vaKhongCoNutGhi', async () => {
  api.on('GET', '/me/permissions', json(200, [{ function: 'SHIFT_SCHEDULE', operation: 'VIEW' }]))
  api.on('GET', '/me/shifts', json(200, [{ ...SHIFT, status: 'PUBLISHED', allowedCommands: [] }]))
  render(<ShiftsPage initialMonth="2026-11" />)
  expect(await screen.findByText(/01\/11\/2026 07:00/)).toBeVisible()
  expect(screen.queryByRole('button', { name: 'Thêm ca' })).not.toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Xem trước công bố' })).not.toBeInTheDocument()
  expect(api.calls('GET', '/shift-schedules')).toHaveLength(0)
})

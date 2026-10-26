import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { HandoverPage } from './HandoverPage'

let api, handover, version
const SHIFT = { id: 30, startsAt: '2035-04-01T00:00:00Z', endsAt: '2035-04-01T08:00:00Z', status: 'IN_PROGRESS', allowedCommands: ['createHandover'] }
const SOURCE = { group: 'TASK', sourceType: 'M04.TASK', sourceId: '7', residentId: 9, description: 'Công việc cần theo dõi', currentStatus: 'OVERDUE', critical: true, hasItem: false }
beforeEach(() => {
 api = mockRoutes(); version = 0
 handover = { id: 41, shiftId: 30, status: 'DRAFT', items: [], comments: [], residentAssessments: [], generalAssessment: '', allowedCommands: ['addItem', 'updateItem', 'deleteItem', 'assess', 'complete'], hiddenFields: [] }
 api.on('GET', '/me/permissions', json(200, [{ function: 'SHIFT_HANDOVER', operation: 'VIEW' }]))
 api.on('GET', '/shifts/30', json(200, SHIFT))
 api.on('GET', '/shifts/30/handovers', () => json(200, handover, { ETag: `"${version}"` }))
 api.on('GET', '/handovers/41', () => json(200, handover, { ETag: `"${version}"` }))
 api.on('GET', '/handovers/41/source-records', json(200, [SOURCE]))
 api.on('GET', '/residents/9', json(200, { id: 9, fullName: '[Họ tên NCT 1]' }))
})
afterEach(() => vi.unstubAllGlobals())

it('FR_045_matMangKhongGuiLenh_vaKhongXepHangTuDong', async () => {
 vi.spyOn(window.navigator, 'onLine', 'get').mockReturnValue(false)
 render(<HandoverPage initialShiftId={30} />)
 fireEvent.click(await screen.findByRole('button', { name: 'Thêm Công việc cần theo dõi' }))
 expect(await screen.findByText('Kết nối mạng rồi thực hiện lại lệnh bàn giao.')).toBeVisible()
 expect(api.calls('POST', '/handovers/41/items')).toHaveLength(0)
 vi.restoreAllMocks()
})

it('FR_041_mucNghiemTrongHienNhacGhiChuTruocKhiHoanTat', async () => {
 handover.items = [{ ...SOURCE, id: 8, note: '', section: 'FINALIZED' }]
 render(<HandoverPage initialShiftId={30} />)
 expect(await screen.findByText('Cần ghi chú trước khi hoàn tất')).toBeVisible()
})

it('FR_044_vanDeTuGhiChoPhepTimNguoiChuaCoTrongNguon', async () => {
 api.on('GET', '/residents', json(200, { items: [{ id: 19, fullName: '[Họ tên NCT 2]' }], nextCursor: null }))
 render(<HandoverPage initialShiftId={30} />)
 fireEvent.change(await screen.findByLabelText('Tìm người cao tuổi'), { target: { value: 'NCT 2' } })
 expect(await screen.findByRole('option', { name: '[Họ tên NCT 2]' })).toBeInTheDocument()
})

it('FR_039_nguonChiThamKhao_khongCoChepTatCa_vaThemTungMucBangLenh', async () => {
 api.on('POST', '/handovers/41/items', (_url, init) => {
  const body = JSON.parse(init.body)
  handover = { ...handover, items: [{ ...body, id: 8, section: 'FINALIZED' }] }; version++
  return new Response(null, { status: 201 })
 })
 render(<HandoverPage initialShiftId={30} />)
 const add = await screen.findByRole('button', { name: 'Thêm Công việc cần theo dõi' })
 expect(screen.queryByRole('button', { name: /chép tất cả/i })).not.toBeInTheDocument()
 expect(api.calls('POST', '/handovers/41/items')).toHaveLength(0)
 fireEvent.click(add)
 await waitFor(() => expect(api.calls('POST', '/handovers/41/items')).toHaveLength(1))
 expect(api.calls('POST', '/handovers/41/items')[0][1].headers['If-Match']).toBe('"0"')
 expect(await screen.findByLabelText('Ghi chú mục 8')).toBeVisible()
})

it('FR_041_409HienDanhSachThieuGhiChu_vaGiuBanNhap', async () => {
 api.on('POST', '/handovers/41/complete', problem(409, 'CRITICAL_ITEM_NOTE_REQUIRED', {
  detail: 'Các mục nghiêm trọng còn thiếu ghi chú',
  unmetConditions: [{ code: 'CRITICAL_ITEM_NOTE_REQUIRED', description: 'Công việc cần ghi chú' }],
 }))
 render(<HandoverPage initialShiftId={30} />)
 fireEvent.click(await screen.findByRole('button', { name: 'Hoàn tất bàn giao' }))
 expect(await screen.findByText('Công việc cần ghi chú')).toBeVisible()
 expect(screen.getByText('Bản nháp')).toBeVisible()
})

it('FR_040_chiSoVaBienDongKhongCoNutBoMuc', async () => {
 handover.items = [{ ...SOURCE, id: 8, group: 'VITAL_SIGN', description: 'Chỉ số đã ghi', section: 'FINALIZED' }]
 render(<HandoverPage initialShiftId={30} />)
 expect(await screen.findByText('Chỉ số đã ghi')).toBeVisible()
 expect(screen.queryByRole('button', { name: 'Bỏ mục 8' })).not.toBeInTheDocument()
 expect(screen.getByText('Không bỏ được')).toBeVisible()
})

it('FR_040_412GiuGhiChuDangNhap_vaChoTaiLai', async () => {
 handover.items = [{ ...SOURCE, id: 8, note: '', section: 'FINALIZED' }]
 api.on('PUT', '/handovers/41/items/8', problem(412, 'VERSION_MISMATCH', { currentValue: { status: 'DRAFT' } }))
 render(<HandoverPage initialShiftId={30} />)
 fireEvent.change(await screen.findByLabelText('Ghi chú mục 8'), { target: { value: 'Ghi chú đang nhập' } })
 fireEvent.click(screen.getByRole('button', { name: 'Lưu ghi chú mục 8' }))
 expect(await screen.findByText('Dữ liệu đã thay đổi')).toBeVisible()
 expect(screen.getByLabelText('Ghi chú mục 8')).toHaveValue('Ghi chú đang nhập')
 expect(screen.getByRole('button', { name: 'Tải lại bàn giao' })).toBeVisible()
})

it('FR_042_xacNhanChiKhiBackendChoPhep_vaGuiYkienBangLenh', async () => {
 handover.status = 'PREPARED'; handover.allowedCommands = ['confirm', 'comment']
 api.on('POST', '/handovers/41/comments', (_url, init) => {
  expect(JSON.parse(init.body).content).toBe('Cần bổ sung')
  handover = { ...handover, status: 'HAS_COMMENTS', allowedCommands: [] }
  return new Response(null, { status: 200 })
 })
 render(<HandoverPage initialShiftId={30} />)
 expect(await screen.findByRole('button', { name: 'Xác nhận bàn giao' })).toBeVisible()
 fireEvent.change(screen.getByLabelText('Ý kiến khi thấy thiếu sót'), { target: { value: 'Cần bổ sung' } })
 fireEvent.click(screen.getByRole('button', { name: 'Gửi ý kiến' }))
 expect(await screen.findByText('Có ý kiến')).toBeVisible()
 expect(screen.queryByRole('button', { name: 'Xác nhận bàn giao' })).not.toBeInTheDocument()
})

it('FR_047_khongCoQuyenThiKhongTaiNoiDungBanGiao', async () => {
 api.on('GET', '/me/permissions', json(200, []))
 render(<HandoverPage initialShiftId={30} />)
 expect(await screen.findByText(/không có quyền/i)).toBeVisible()
 expect(api.calls('GET', '/shifts/30/handovers')).toHaveLength(0)
})

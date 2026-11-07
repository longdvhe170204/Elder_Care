import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { QualityCheckPage } from './QualityCheckPage'

let api
const SHIFT = {
  id: 7,
  floorAreaId: 2,
  startsAt: '2026-11-07T00:00:00Z',
  endsAt: '2026-11-07T08:00:00Z',
  status: 'PUBLISHED',
}
const VIEW = {
  shiftId: 7,
  floorAreaId: 2,
  minimumItems: 1,
  eligibleTaskCount: 3,
  checklist: { id: 50, status: 'OPEN', createdAt: '2026-11-07T03:00:00Z', inspectorRole: 'FLOOR_MANAGER' },
  eligibleTasks: [
    {
      taskId: 501,
      taskTypeId: 14,
      residentId: 701,
      locationId: null,
      performerStaffId: 31,
      performedAt: '2026-11-07T02:12:00Z',
    },
    {
      taskId: 502,
      taskTypeId: 14,
      residentId: 702,
      locationId: null,
      performerStaffId: 32,
      performedAt: '2026-11-07T02:40:00Z',
    },
  ],
  items: [{ id: 88, taskId: 488, status: 'PENDING', note: null, inspectorRole: null, recordedAt: null }],
}

beforeEach(() => {
  api = mockRoutes()
  api.on(
    'GET',
    '/me/permissions',
    json(200, [
      { function: 'QUALITY_CHECK', operation: 'VIEW' },
      { function: 'QUALITY_CHECK', operation: 'CREATE' },
    ]),
  )
  api.on('GET', '/me/shifts', json(200, [SHIFT]))
  api.on('GET', '/quality-checks/shifts/7', json(200, VIEW))
})
afterEach(() => vi.unstubAllGlobals())

async function page() {
  render(
    <PermissionsProvider>
      <QualityCheckPage initialDate="2026-11-07" />
    </PermissionsProvider>,
  )
  // Chờ lựa chọn ca tải xong rồi mới chọn (ô chọn có trước khi có lựa chọn).
  await screen.findByText(/–/, { selector: 'option' })
  fireEvent.change(screen.getByLabelText(/^Ca/), { target: { value: '7' } })
  await screen.findByText('Cần chọn ít nhất 1 trên 3 công việc đủ điều kiện')
}

it('FR_059_danhSachTheoThuTuMaKhongXepHangVaChonDeThemMuc', async () => {
  api.on('POST', '/quality-checks/shifts/7', json(201, {}))
  await page()
  expect(api.calls('GET', '/quality-checks/shifts/7')[0][0]).toContain('floorAreaId=2')
  const rows = within(screen.getByRole('table', { name: 'Công việc đủ điều kiện' }))
    .getAllByRole('row')
    .slice(1)
  expect(rows.map((r) => within(r).getAllByRole('cell')[1].textContent)).toEqual(['CV-501', 'CV-502'])
  expect(screen.queryByText(/tỷ lệ Đạt|xếp hạng/i)).not.toBeInTheDocument()
  fireEvent.click(screen.getByRole('checkbox', { name: 'Chọn công việc CV-502' }))
  fireEvent.click(screen.getByRole('button', { name: 'Thêm mục (1)' }))
  await screen.findByText('Đã thêm mục kiểm tra')
  expect(JSON.parse(api.calls('POST', '/quality-checks/shifts/7')[0][1].body)).toEqual({
    floorAreaId: 2,
    careTaskIds: [502],
  })
})

it('FR_064_khongDatCanGhiChuVaCongViecLamLaiGiuNoiDungKhiBiChan', async () => {
  api.on(
    'POST',
    '/quality-check-items/88/result',
    problem(409, 'OUTSIDE_SHIFT_TIME', { detail: 'Chỉ ghi kết quả trong thời gian của ca' }),
  )
  await page()
  fireEvent.click(screen.getByRole('button', { name: 'Ghi kết quả công việc CV-488' }))
  fireEvent.click(await screen.findByRole('radio', { name: 'Không đạt' }))
  expect(screen.getByRole('button', { name: 'Lưu kết quả' })).toBeDisabled()
  fireEvent.change(screen.getByLabelText(/^Ghi chú/), { target: { value: 'Chưa thay ga' } })
  fireEvent.change(screen.getByLabelText(/^Giờ làm lại dự kiến/), { target: { value: '2026-11-07T13:00' } })
  fireEvent.click(screen.getByRole('button', { name: 'Lưu kết quả' }))
  expect(await screen.findByText('Chỉ ghi kết quả trong thời gian của ca')).toBeInTheDocument()
  expect(screen.getByLabelText(/^Ghi chú/)).toHaveValue('Chưa thay ga')
  expect(JSON.parse(api.calls('POST', '/quality-check-items/88/result')[0][1].body)).toEqual({
    result: 'FAIL',
    note: 'Chưa thay ga',
    reworkTask: { plannedAt: '2026-11-07T13:00:00+07:00' },
  })
})

it('FR_066_khongPhaiNguoiKiemTraThiBaoKhongCoQuyen', async () => {
  api.on('GET', '/quality-checks/shifts/7', problem(403, 'PERMISSION_DENIED', { detail: 'Chỉ người kiểm tra của ca' }))
  render(
    <PermissionsProvider>
      <QualityCheckPage initialDate="2026-11-07" />
    </PermissionsProvider>,
  )
  // Chờ lựa chọn ca tải xong rồi mới chọn (ô chọn có trước khi có lựa chọn).
  await screen.findByText(/–/, { selector: 'option' })
  fireEvent.change(screen.getByLabelText(/^Ca/), { target: { value: '7' } })
  expect(await screen.findByText('Chỉ người kiểm tra của ca')).toBeInTheDocument()
  expect(screen.queryByRole('table', { name: 'Công việc đủ điều kiện' })).not.toBeInTheDocument()
})

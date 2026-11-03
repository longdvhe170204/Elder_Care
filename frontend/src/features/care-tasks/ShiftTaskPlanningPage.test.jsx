import { beforeEach, afterEach, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { ShiftTaskPlanningPage } from './ShiftTaskPlanningPage'

let api
beforeEach(() => {
  api = mockRoutes()
  api.on(
    'GET',
    '/me/permissions',
    json(200, [
      { function: 'SHIFT_TASK_PLANNING', operation: 'CREATE' },
      { function: 'SHIFT_TASK_PLANNING', operation: 'VIEW' },
    ]),
  )
  api.on('GET', '/residents', json(200, { items: [{ id: 77, fullName: '[Họ tên kiểm thử]' }], nextCursor: null }))
  api.on('GET', '/task-types', json(200, [{ id: 9, name: 'Loại công việc kiểm thử', status: 'ACTIVE' }]))
  api.on(
    'GET',
    '/shifts/5/source-data',
    json(200, {
      shift: { id: 5, startsAt: '2026-11-01T00:00:00Z', endsAt: '2026-11-01T08:00:00Z', floorAreaId: 3 },
      residents: [
        {
          residentId: 77,
          assignments: [{ staffId: 22, fullName: '[Nhân viên kiểm thử]', role: 'CAREGIVER' }],
          planItems: [{ id: 90, content: 'Mục kế hoạch kiểm thử' }],
          otherSources: [],
        },
      ],
    }),
  )
  api.on('GET', '/shifts/5/care-tasks', json(200, []))
  api.on(
    'GET',
    '/shifts/5',
    json(200, { id: 5, staff: [{ staffId: 22 }, { staffId: 33 }, { staffId: 44, absent: true }] }),
  )
})
afterEach(() => vi.unstubAllGlobals())
const view = () =>
  render(
    <PermissionsProvider>
      <ShiftTaskPlanningPage shiftId={5} />
    </PermissionsProvider>,
  )

it('FR_017_nguonChiDeDoiChieuVaKhongTuDienDongCongViec', async () => {
  view()
  expect(await screen.findByText('Chưa có dòng công việc.')).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: /chép|gợi ý/i })).not.toBeInTheDocument()
  expect(api.calls('POST', '/shifts/5/care-tasks/plan')).toHaveLength(0)
  fireEvent.click(screen.getByRole('button', { name: 'Thêm dòng công việc' }))
  expect(screen.getByLabelText('Nội dung dòng 1')).toHaveValue('')
})

async function enterRow() {
  await screen.findByText('Chưa có dòng công việc.')
  fireEvent.click(screen.getByRole('button', { name: 'Thêm dòng công việc' }))
  fireEvent.change(screen.getByLabelText('Người cao tuổi dòng 1'), { target: { value: '77' } })
  fireEvent.change(screen.getByLabelText('Loại công việc dòng 1'), { target: { value: '9' } })
  fireEvent.change(screen.getByLabelText('Loại căn cứ dòng 1'), { target: { value: 'CARE_PLAN_ITEM' } })
  fireEvent.change(screen.getByLabelText('Căn cứ dòng 1'), { target: { value: '90' } })
  fireEvent.change(screen.getByLabelText('Nội dung dòng 1'), { target: { value: 'Nội dung tự nhập' } })
  fireEvent.change(screen.getByLabelText('Thời điểm dự kiến dòng 1'), { target: { value: '2026-11-01T08:00' } })
  fireEvent.change(screen.getByLabelText('Mức quan trọng dòng 1'), { target: { value: 'NORMAL' } })
  fireEvent.change(screen.getByLabelText('Người thực hiện dòng 1'), { target: { value: '22' } })
}

it('BR_M04_01_guiMangTuNhapKemIdempotencyKeyVaMuiGioVietNam', async () => {
  api.on('POST', '/shifts/5/care-tasks/plan', json(201, []))
  view()
  await enterRow()
  fireEvent.click(screen.getByRole('button', { name: 'Lập công việc của ca' }))
  await waitFor(() => expect(api.calls('POST', '/shifts/5/care-tasks/plan')).toHaveLength(1))
  const [, options] = api.calls('POST', '/shifts/5/care-tasks/plan')[0]
  expect(JSON.parse(options.body)).toMatchObject([
    {
      residentId: 77,
      taskTypeId: 9,
      basis: { type: 'CARE_PLAN_ITEM', id: 90 },
      content: 'Nội dung tự nhập',
      plannedAt: '2026-11-01T08:00:00+07:00',
      importance: 'NORMAL',
      performerId: 22,
    },
  ])
  expect(options.headers['Idempotency-Key']).toBeTruthy()
})

it('BR_M04_01_loiTheoDongGiuNoiDungVaKhongBaoDaLuu', async () => {
  api.on(
    'POST',
    '/shifts/5/care-tasks/plan',
    problem(409, 'UNMET_CONDITIONS', { rowErrors: [{ row: 1, codes: ['DUPLICATE_BASIS'] }] }),
  )
  view()
  await enterRow()
  fireEvent.click(screen.getByRole('button', { name: 'Lập công việc của ca' }))
  expect(await screen.findByText('Trùng căn cứ và thời điểm dự kiến.')).toBeInTheDocument()
  expect(screen.getByLabelText('Nội dung dòng 1')).toHaveValue('Nội dung tự nhập')
  expect(screen.queryByText('Đã lập công việc của ca.')).not.toBeInTheDocument()
})

it('FR_017_khongCoQuyenThiKhongHienLenhLap', async () => {
  api.on('GET', '/me/permissions', json(200, []))
  view()
  expect(await screen.findByText('Bạn không có quyền xem lập công việc của ca')).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Lập công việc của ca' })).not.toBeInTheDocument()
})

it('BR_M04_24_chonDuocThanhVienCaKhongCoPhanCongTheoNguoi', async () => {
  view()
  await screen.findByText('Chưa có dòng công việc.')
  fireEvent.click(screen.getByRole('button', { name: 'Thêm dòng công việc' }))
  fireEvent.change(screen.getByLabelText('Vị trí chung dòng 1'), { target: { value: 'FLOOR' } })
  const options = screen.getByLabelText('Người thực hiện dòng 1')
  expect([...options.options].map((o) => o.value)).toContain('33')
  expect([...options.options].map((o) => o.value)).not.toContain('44')
})

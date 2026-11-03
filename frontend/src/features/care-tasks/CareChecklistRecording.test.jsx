import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { CareChecklistDetail } from './CareChecklistDetail'

let api
const saved = vi.fn()
const Title = ({ children }) => <h2>{children}</h2>
const Description = ({ children }) => <p>{children}</p>
function view(resultType) {
  render(
    <CareChecklistDetail
      task={{
        id: 5,
        title: 'Tên loại đã đổi',
        status: 'Đến hạn',
        commands: ['record'],
        raw: { resultType, importance: 'NORMAL' },
      }}
      reasons={[]}
      Title={Title}
      Description={Description}
      onSaved={saved}
      onBusy={() => {}}
    />,
  )
}
beforeEach(() => {
  api = mockRoutes()
  saved.mockClear()
})
afterEach(() => vi.unstubAllGlobals())

it.each([
  ['ML', 'Lượng nước (ml)', '200', { numericValue: 200 }],
  ['ITEM_QUANTITY', 'Số lượng', '2', { numericValue: 2 }],
  ['MEASURED_VALUE', 'Giá trị đo', '36.5', { numericValue: 36.5 }],
  ['EATING', 'Mức ăn', 'MOST', { codeValue: 'MOST' }],
  ['MOVEMENT', 'Khả năng vận động', 'ASSISTED', { codeValue: 'ASSISTED' }],
])('FR_038_form_%s_dungSnapshotVaGuiDungTruong', async (type, label, value, expected) => {
  api.on('POST', '/care-tasks/5/record', json(200, {}))
  view(type)
  fireEvent.change(screen.getByLabelText(label), { target: { value } })
  fireEvent.click(screen.getByRole('button', { name: 'Lưu kết quả' }))
  await waitFor(() => expect(saved).toHaveBeenCalled())
  expect(JSON.parse(api.calls('POST', '/care-tasks/5/record')[0][1].body)).toEqual(expected)
})

it('FR_034_giuGiaTriVaHoiXacNhanKhiVuotGioiHan', async () => {
  api.on(
    'POST',
    '/care-tasks/5/record',
    problem(422, 'CONFIRMATION_REQUIRED', { detail: 'Cần xác nhận giá trị vượt giới hạn hợp lý' }),
  )
  view('ML')
  fireEvent.change(screen.getByLabelText('Lượng nước (ml)'), { target: { value: '900' } })
  fireEvent.click(screen.getByRole('button', { name: 'Lưu kết quả' }))
  fireEvent.click(await screen.findByRole('checkbox', { name: 'Xác nhận giá trị đã nhập vượt giới hạn hợp lý' }))
  expect(screen.getByLabelText('Lượng nước (ml)')).toHaveValue(900)
  api.on('POST', '/care-tasks/5/record', json(200, {}))
  fireEvent.click(screen.getByRole('button', { name: 'Lưu kết quả' }))
  await waitFor(() => expect(saved).toHaveBeenCalled())
  expect(JSON.parse(api.calls('POST', '/care-tasks/5/record')[1][1].body)).toEqual({
    numericValue: 900,
    upperLimitConfirmed: true,
  })
})

it('FR_041_202_thongBaoChoDoLaiKhongBaoHoanThanh', async () => {
  api.on('POST', '/care-tasks/5/record', json(202, {}))
  view('MEASURED_VALUE')
  fireEvent.change(screen.getByLabelText('Giá trị đo'), { target: { value: '39' } })
  fireEvent.click(screen.getByRole('button', { name: 'Lưu kết quả' }))
  await waitFor(() => expect(saved).toHaveBeenCalledWith('Đã gửi lần đo. Chờ xác nhận đo lại.'))
})

it.each([
  ['SLEEP', 'SLEEP_QUALITY', 'Chất lượng ngủ', { numericValue: 90, codeValue: 'GOOD' }],
  ['MOOD', 'MOOD', 'Tâm trạng', { codeValue: 'GOOD' }],
])('FR_034_%s_chiDungDanhMucHienHanhDungLoai', async (type, group, label, body) => {
  api.on(
    'GET',
    '/result-values',
    json(200, [
      { type: group, code: 'GOOD', name: 'Giá trị đang dùng', status: 'ACTIVE' },
      { type: group, code: 'OLD', name: 'Giá trị đã ngừng', status: 'INACTIVE' },
      { type: 'BEHAVIOR', code: 'OTHER', name: 'Khác loại', status: 'ACTIVE' },
    ]),
  )
  api.on('POST', '/care-tasks/5/record', json(200, {}))
  view(type)
  await screen.findByRole('option', { name: 'Giá trị đang dùng' })
  expect(screen.queryByRole('option', { name: 'Khác loại' })).not.toBeInTheDocument()
  expect(screen.queryByRole('option', { name: 'Giá trị đã ngừng' })).not.toBeInTheDocument()
  expect(api.calls('GET', '/result-values')[0][0]).not.toContain('?')
  if (type === 'SLEEP') fireEvent.change(screen.getByLabelText('Thời gian ngủ (phút)'), { target: { value: '90' } })
  fireEvent.change(screen.getByLabelText(label), { target: { value: 'GOOD' } })
  fireEvent.click(screen.getByRole('button', { name: 'Lưu kết quả' }))
  await waitFor(() => expect(saved).toHaveBeenCalled())
  expect(JSON.parse(api.calls('POST', '/care-tasks/5/record')[0][1].body)).toEqual(body)
})

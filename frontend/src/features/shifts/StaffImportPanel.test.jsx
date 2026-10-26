import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes } from '@/test/fetchRoutes'
import { StaffImportPanel } from './StaffImportPanel'
let api
const FILE = new File(['xlsx'], 'staff.xlsx', {
  type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
})
const PREVIEW = {
  fileName: 'staff.xlsx',
  rowCount: 1,
  rows: [
    {
      rowNumber: 2,
      fullName: '[Nhân viên import]',
      position: 'Điều dưỡng',
      phoneNumber: '0901234567',
      startDate: '2026-10-25',
    },
  ],
  errors: [],
  canConfirm: true,
}
beforeEach(() => {
  api = mockRoutes()
})
afterEach(() => vi.unstubAllGlobals())
async function preview() {
  fireEvent.change(screen.getByLabelText('File Excel'), { target: { files: [FILE] } })
  fireEvent.click(screen.getByRole('button', { name: 'Xem trước' }))
  await screen.findByText('[Nhân viên import]')
}
it('FR_001b_loiTungCot_vaKhongChoXacNhan', async () => {
  api.on(
    'POST',
    '/staff/import/preview',
    json(200, {
      ...PREVIEW,
      canConfirm: false,
      errors: [
        {
          rowNumber: 2,
          column: 'phoneNumber',
          errorCode: 'DUPLICATE_STAFF_PROFILE',
          reason: 'Trùng số điện thoại với hồ sơ đã có',
          duplicateStaffId: 9,
        },
      ],
    }),
  )
  render(<StaffImportPanel />)
  await preview()
  expect(screen.getAllByText(/Trùng số điện thoại/)[0]).toBeVisible()
  expect(screen.getByRole('button', { name: 'Xác nhận nhập' })).toBeDisabled()
  expect(api.calls('POST', '/staff/import/preview')[0][1].body).toBeInstanceOf(FormData)
})
it('FR_001c_xacNhanGuiLaiFile_vaBaoSoHoSoDaTao', async () => {
  api.on('POST', '/staff/import/preview', json(200, PREVIEW))
  api.on('POST', '/staff/import/confirm', json(201, { createdProfileCount: 1, importBatchId: 3, staffIds: [9] }))
  const done = vi.fn()
  render(<StaffImportPanel onDone={done} />)
  await preview()
  fireEvent.click(screen.getByRole('button', { name: 'Xác nhận nhập' }))
  await waitFor(() => expect(done).toHaveBeenCalled())
  const call = api.calls('POST', '/staff/import/confirm')[0][1]
  expect(call.body.get('file')).toBe(FILE)
  expect(call.headers['Idempotency-Key']).toBeTruthy()
  expect(await screen.findByText(/Đã tạo 1 hồ sơ/)).toBeVisible()
})
it('FR_001b_xacNhan409HienLaiBangLoi_vaKhongBaoThanhCong', async () => {
  api.on('POST', '/staff/import/preview', json(200, PREVIEW))
  api.on(
    'POST',
    '/staff/import/confirm',
    json(409, {
      ...PREVIEW,
      canConfirm: false,
      errors: [{ rowNumber: 2, column: 'phoneNumber', reason: 'Số điện thoại vừa được sử dụng' }],
    }),
  )
  render(<StaffImportPanel />)
  await preview()
  fireEvent.click(screen.getByRole('button', { name: 'Xác nhận nhập' }))
  expect((await screen.findAllByText(/vừa được sử dụng/))[0]).toBeVisible()
  expect(screen.getByRole('button', { name: 'Xác nhận nhập' })).toBeDisabled()
})
it('FR_001b_doiFilePhaiXemTruocLai', async () => {
  api.on('POST', '/staff/import/preview', json(200, PREVIEW))
  render(<StaffImportPanel />)
  await preview()
  expect(screen.getByRole('button', { name: 'Xác nhận nhập' })).toBeEnabled()
  fireEvent.change(screen.getByLabelText('File Excel'), { target: { files: [new File(['new'], 'new.xlsx')] } })
  expect(screen.getByRole('button', { name: 'Xác nhận nhập' })).toBeDisabled()
})

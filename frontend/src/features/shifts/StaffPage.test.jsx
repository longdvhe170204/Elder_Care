import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { StaffPage } from './StaffPage'
let api
const PERSON = {
  id: 10,
  fullName: '[Nhân viên kiểm thử]',
  position: 'Điều dưỡng',
  status: 'WORKING',
  phoneNumber: '0901234567',
  startDate: '2026-01-01',
  departmentId: 2,
}
beforeEach(() => {
  api = mockRoutes()
  api.on(
    'GET',
    '/me/permissions',
    json(
      200,
      ['VIEW', 'CREATE', 'UPDATE'].map((operation) => ({ function: 'STAFF_PROFILE', operation })),
    ),
  )
  api.on('GET', '/staff', json(200, [PERSON]))
  api.on('GET', '/staff/10', json(200, PERSON, { ETag: '"4"' }))
  api.on('GET', '/departments', json(200, [{ id: 2, name: 'Chăm sóc', active: true, leadStaffId: 10 }]))
  api.on('GET', '/credential-types', json(200, [{ id: 3, name: 'Sơ cứu', hasExpiry: true, group: 'TRAINING' }]))
  api.on('GET', '/practice-scopes', json(200, []))
  api.on('GET', '/staff/10/credentials', json(200, []))
})
afterEach(() => vi.unstubAllGlobals())
async function open() {
  fireEvent.click(await screen.findByRole('button', { name: '[Nhân viên kiểm thử]' }, { timeout: 4000 }))
  await screen.findByRole('heading', { name: '[Nhân viên kiểm thử]' })
}
it('FR_052_nguoiChiXemKhongCoLenhSuaHayImport', async () => {
  api.on('GET', '/me/permissions', json(200, [{ function: 'STAFF_PROFILE', operation: 'VIEW' }]))
  api.on('GET', '/staff/10', json(200, { ...PERSON, phoneNumber: undefined, hiddenFields: ['phoneNumber', 'email'] }))
  render(<StaffPage />)
  await open()
  expect(screen.queryByRole('button', { name: 'Cho nghỉ việc' })).not.toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Import Excel' })).not.toBeInTheDocument()
  expect(screen.queryByText('0901234567')).not.toBeInTheDocument()
})
it('FR_003_nghiViecGuiLyDoHieuLuc_vaNguoiThay', async () => {
  api.on('POST', '/staff/10/terminate-employment', json(200, null))
  render(<StaffPage />)
  await open()
  fireEvent.click(screen.getByRole('button', { name: 'Cho nghỉ việc' }))
  fireEvent.change(screen.getByLabelText('Hiệu lực từ'), { target: { value: '2026-11-01T08:00' } })
  fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Kết thúc hợp đồng' } })
  fireEvent.click(screen.getByRole('button', { name: 'Xác nhận cho nghỉ việc' }))
  await waitFor(() => expect(api.calls('POST', '/staff/10/terminate-employment')).toHaveLength(1))
  const body = JSON.parse(api.calls('POST', '/staff/10/terminate-employment')[0][1].body)
  expect(body.reason).toBe('Kết thúc hợp đồng')
  expect(body.effectiveAt).toBe('2026-11-01T08:00:00+07:00')
})
it('FR_001_suaMoTaGuiIfMatch_va412CoTaiLai', async () => {
  api.on('PUT', '/staff/10', problem(412, 'VERSION_MISMATCH', { currentValue: PERSON }))
  render(<StaffPage />)
  await open()
  fireEvent.click(screen.getByRole('button', { name: 'Sửa hồ sơ' }))
  fireEvent.click(screen.getByRole('button', { name: 'Lưu hồ sơ' }))
  expect(await screen.findByRole('button', { name: 'Tải lại hồ sơ' })).toBeVisible()
  expect(api.calls('PUT', '/staff/10')[0][1].headers['If-Match']).toBe('"4"')
})
it('FR_001a_doiBoPhanCoNguoiThay_vaLyDoBatBuoc', async () => {
  render(<StaffPage />)
  await open()
  fireEvent.click(screen.getByRole('button', { name: 'Đổi bộ phận' }))
  expect(screen.getByLabelText('Bộ phận mới')).toBeVisible()
  expect(screen.getByLabelText('Người thay Phụ trách bộ phận')).toBeVisible()
  expect(screen.getByLabelText('Lý do')).toBeRequired()
})
it('FR_006_chungChiCoLenhGiaHanThuHoiSuaSai', async () => {
  api.on(
    'GET',
    '/staff/10/credentials',
    json(200, [
      { id: 8, credentialTypeId: 3, issueDate: '2026-01-01', expiryDate: '2026-12-31', validityStatus: 'VALID' },
    ]),
  )
  render(<StaffPage />)
  await open()
  expect(await screen.findByRole('button', { name: 'Gia hạn' })).toBeVisible()
  expect(screen.getByRole('button', { name: 'Thu hồi' })).toBeVisible()
  expect(screen.getByRole('button', { name: 'Sửa sai' })).toBeVisible()
  fireEvent.click(screen.getByRole('button', { name: 'Thu hồi' }))
  expect(screen.getByLabelText('Ngày hiệu lực thu hồi')).toBeRequired()
  expect(screen.getByLabelText('Lý do')).toBeRequired()
})

it('FR_005_quanLyVienThemLoaiDaoTao_vaCoBatBuocVaiTro', async () => {
  api.on('POST', '/credential-types', json(201, null))
  render(<StaffPage />)
  fireEvent.click(await screen.findByRole('button', { name: 'Loại chứng chỉ' }, { timeout: 4000 }))
  fireEvent.click(await screen.findByRole('button', { name: 'Thêm loại chứng chỉ' }))
  fireEvent.change(screen.getByLabelText('Tên loại chứng chỉ'), { target: { value: '[Đào tạo mới]' } })
  fireEvent.change(screen.getByLabelText('Nhóm chứng chỉ'), { target: { value: 'TRAINING' } })
  fireEvent.click(screen.getByLabelText('Điều dưỡng'))
  fireEvent.click(screen.getByRole('button', { name: 'Lưu loại chứng chỉ' }))
  await waitFor(() => expect(api.calls('POST', '/credential-types')).toHaveLength(1))
  expect(JSON.parse(api.calls('POST', '/credential-types')[0][1].body)).toMatchObject({
    name: '[Đào tạo mới]',
    group: 'TRAINING',
    requiredForRoles: ['NURSING_STAFF'],
  })
})

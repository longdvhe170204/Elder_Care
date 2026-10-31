import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { CareCatalogPage } from './CareCatalogPage'

let api
beforeEach(() => {
  api = mockRoutes()
  api.on(
    'GET',
    '/me/permissions',
    json(200, [
      { function: 'CARE_TASK_CATALOG', operation: 'CREATE' },
      { function: 'CARE_TASK_CATALOG', operation: 'UPDATE' },
    ]),
  )
  api.on('GET', '/task-types', json(200, []))
  api.on(
    'GET',
    '/non-performance-reason-types',
    json(200, [{ id: 8, version: 3, name: 'Lý do kiểm thử', fixed: false, status: 'ACTIVE' }]),
  )
})
afterEach(() => vi.unstubAllGlobals())

it('T017_suaDanhMucDungVersionDaDuyetDeGuiIfMatch', async () => {
  api.on('PUT', '/non-performance-reason-types/8', json(200, null, { ETag: '"4"' }))
  render(
    <PermissionsProvider>
      <CareCatalogPage />
    </PermissionsProvider>,
  )
  fireEvent.click(screen.getByRole('button', { name: 'Lý do không thực hiện' }))
  fireEvent.click(await screen.findByRole('button', { name: 'Sửa Lý do kiểm thử' }))
  const dialog = await screen.findByRole('dialog')
  fireEvent.change(within(dialog).getByLabelText('Tên'), { target: { value: 'Tên mới' } })
  fireEvent.click(within(dialog).getByRole('button', { name: 'Lưu danh mục' }))
  await waitFor(() => expect(api.calls('PUT', '/non-performance-reason-types/8')).toHaveLength(1))
  const [, request] = api.calls('PUT', '/non-performance-reason-types/8')[0]
  expect(request.headers['If-Match']).toBe('"3"')
  expect(JSON.parse(request.body)).toEqual({ name: 'Tên mới' })
})

it('FR_028a_danhMucCoDinhKhongChoSuaHayNgung', async () => {
  api.on(
    'GET',
    '/non-performance-reason-types',
    json(200, [{ id: 1, version: 0, name: 'Người cao tuổi từ chối', fixed: true, status: 'ACTIVE' }]),
  )
  render(
    <PermissionsProvider>
      <CareCatalogPage />
    </PermissionsProvider>,
  )
  fireEvent.click(screen.getByRole('button', { name: 'Lý do không thực hiện' }))
  expect(await screen.findByText('Người cao tuổi từ chối')).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Sửa Người cao tuổi từ chối' })).not.toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Ngừng Người cao tuổi từ chối' })).not.toBeInTheDocument()
})

it('FR_004_412DanhMucGiuFormVaHienGiaTriMoi', async () => {
  api.on(
    'PUT',
    '/non-performance-reason-types/8',
    problem(412, 'VERSION_MISMATCH', { currentValue: { name: 'Đã đổi ở phiên khác', version: 4 } }),
  )
  render(
    <PermissionsProvider>
      <CareCatalogPage />
    </PermissionsProvider>,
  )
  fireEvent.click(screen.getByRole('button', { name: 'Lý do không thực hiện' }))
  fireEvent.click(await screen.findByRole('button', { name: 'Sửa Lý do kiểm thử' }))
  fireEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Lưu danh mục' }))
  expect(await screen.findByText('Đã đổi ở phiên khác')).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Tải lại danh mục' })).toBeInTheDocument()
})

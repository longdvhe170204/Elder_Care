import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { AdminPage } from './AdminPage'

// 001 T072 (Opus #12, #13): tab "Danh mục" theo quyền thật của người dùng (GET /me/permissions, HEALTH_CATALOGS), không
// theo vai trò. Không có quyền xem thì ẩn tab; có xem mà không có tạo, sửa thì không có nút thay đổi.

let api

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/parameters', json(200, []))
  api.on(
    'GET',
    '/catalogs/allergens',
    json(200, [{ id: 1, name: 'Penicillin', group: 'FOOD', status: 'ACTIVE', version: 0 }]),
  )
  api.on('GET', '/catalogs/allergy-severities', json(200, []))
})

afterEach(() => {
  vi.unstubAllGlobals()
})

const grant = (...operations) =>
  api.on(
    'GET',
    '/me/permissions',
    json(
      200,
      operations.map((operation) => ({ function: 'HEALTH_CATALOGS', operation })),
    ),
  )

describe('AdminPage tab Danh mục', () => {
  it('có quyền xem, tạo, sửa: thấy tab, nút Thêm mục và nút thao tác từng dòng', async () => {
    grant('VIEW', 'CREATE', 'UPDATE')
    render(<AdminPage role="NURSING_HOME_MANAGER" />)

    fireEvent.click(await screen.findByRole('button', { name: 'Danh mục' }))

    expect(await screen.findByText('Penicillin')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Thêm mục' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sửa' })).toBeInTheDocument()
  })

  it('chỉ có quyền xem (Bác sĩ, Điều dưỡng): thấy danh sách, không có nút thay đổi dù vai trò là Quản lý viện', async () => {
    grant('VIEW')
    // vai trò truyền vào là Quản lý viện nhưng quyền hiện hành đã bị thu hẹp (002): quyền thật thắng vai trò
    render(<AdminPage role="NURSING_HOME_MANAGER" />)

    fireEvent.click(await screen.findByRole('button', { name: 'Danh mục' }))

    expect(await screen.findByText('Penicillin')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Thêm mục' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Sửa' })).not.toBeInTheDocument()
  })

  it('có tạo nhưng không có sửa: chỉ có nút Thêm mục', async () => {
    grant('VIEW', 'CREATE')
    render(<AdminPage role="NURSING_HOME_MANAGER" />)

    fireEvent.click(await screen.findByRole('button', { name: 'Danh mục' }))

    expect(await screen.findByText('Penicillin')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Thêm mục' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Sửa' })).not.toBeInTheDocument()
  })

  it('không có quyền xem (Hành chính): không có tab Danh mục, không gọi API danh mục', async () => {
    api.on('GET', '/me/permissions', json(200, [{ function: 'TASK_CHECKLIST', operation: 'VIEW' }]))
    render(<AdminPage role="OFFICE_STAFF" />)

    await screen.findByRole('button', { name: 'Tham số' })
    await waitFor(() => expect(api.calls('GET', '/me/permissions')).toHaveLength(1))
    expect(screen.queryByRole('button', { name: 'Danh mục' })).not.toBeInTheDocument()
    expect(api.calls('GET', '/catalogs/allergens')).toHaveLength(0)
  })

  it('chưa tải xong quyền hoặc tải quyền lỗi: ẩn tab Danh mục', async () => {
    api.on('GET', '/me/permissions', problem(403, 'PERMISSION_DENIED'))
    render(<AdminPage role="NURSING_HOME_MANAGER" />)

    await screen.findByRole('button', { name: 'Tham số' })
    await waitFor(() => expect(api.calls('GET', '/me/permissions')).toHaveLength(1))
    expect(screen.queryByRole('button', { name: 'Danh mục' })).not.toBeInTheDocument()
  })
})

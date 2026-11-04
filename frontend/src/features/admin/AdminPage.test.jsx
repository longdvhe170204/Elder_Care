import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { AdminPage } from './AdminPage'

// 001 T072 (Opus #12, #13): tab "Danh mục" theo quyền thật của người dùng (GET /me/permissions, HEALTH_CATALOGS), không
// theo vai trò. Không có quyền xem thì ẩn tab; có xem mà không có tạo, sửa thì không có nút thay đổi.
// Chi tiết của tab mở trong DetailLayout ở cấp màn (DESIGN.md 5); đổi tab thì đóng khung.

const PARAMETER = {
  code: 'CFG-M15-05',
  description: 'Mốc nhắc người duyệt',
  type: 'DURATION',
  unit: 'HOURS',
  constraints: { min: 1 },
  defaultValue: { value: 48, unit: 'HOURS' },
  currentValue: { value: 48, unit: 'HOURS' },
  version: 0,
  changedAt: null,
}

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

describe('AdminPage', () => {
  // Từ 1280px khung cố định bên phải, tab vẫn bấm được khi khung mở
  it('doiTab_dongKhungChiTiet', async () => {
    vi.stubGlobal('matchMedia', () => ({ matches: true, addEventListener() {}, removeEventListener() {} }))
    api.on('GET', '/parameters', json(200, [PARAMETER]))
    api.on('GET', '/parameters/CFG-M15-05', json(200, PARAMETER, { ETag: '"0"' }))
    render(<AdminPage role="NURSING_HOME_MANAGER" />)

    fireEvent.click(await screen.findByRole('button', { name: 'CFG-M15-05' }))
    expect(await screen.findByRole('complementary', { name: 'Chi tiết tham số' })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Tài khoản' }))
    await waitFor(() => expect(screen.queryByRole('complementary', { name: 'Chi tiết tham số' })).toBeNull())
  })
})

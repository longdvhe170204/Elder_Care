import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { AccountList } from './AccountList'

// 002 T035: màn quản trị tài khoản nhân viên (US2; FR-002 → FR-006, FR-011, FR-012, FR-042; hợp đồng /accounts).

function account(id, overrides = {}) {
  return {
    id,
    username: `nv-${id}`,
    fullName: null,
    type: 'STAFF',
    staffId: 100 + id,
    status: 'ACTIVE',
    roles: ['AC-05'],
    lastLoginAt: null,
    allowedCommands: ['lock', 'reset-password', 'assign-role', 'remove-role'],
    ...overrides,
  }
}

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

async function openDetail(item) {
  api.on('GET', `/accounts/${item.id}`, json(200, item, { ETag: '"4"' }))
  fireEvent.click(await screen.findByRole('button', { name: item.username }))
  return screen.findByRole('dialog')
}

describe('AccountList', () => {
  it('US2_hienBang_hoTenHoacMaHoSo_vaiTro_trangThai', async () => {
    api.on(
      'GET',
      '/accounts',
      json(200, {
        items: [
          account(1, { fullName: 'Trần Thị Điều Dưỡng', roles: ['AC-04'], lastLoginAt: '2026-10-19T01:12:00Z' }),
          account(2, { status: 'TEMPORARILY_LOCKED' }),
          account(3, { status: 'INACTIVE', type: 'FAMILY_MEMBER', staffId: null, familyMemberId: 9, roles: ['AC-10'] }),
          account(4, { status: 'LOCKED' }),
        ],
        nextCursor: null,
      }),
    )
    render(<AccountList canCreate />)

    const rows = (await screen.findAllByRole('row')).slice(1)
    expect(rows).toHaveLength(4)
    expect(rows[0]).toHaveTextContent('Trần Thị Điều Dưỡng')
    expect(rows[0]).toHaveTextContent('Điều dưỡng')
    expect(rows[0]).toHaveTextContent('19/10/2026 08:12')
    expect(rows[1]).toHaveTextContent('Hồ sơ nhân viên #102')
    expect(within(rows[1]).getByText('Khóa tạm')).toHaveAttribute('data-variant', 'attention')
    expect(within(rows[0]).getByText('Hoạt động')).toHaveAttribute('data-variant', 'settled')
    expect(within(rows[2]).getByText('Không hoạt động')).toHaveAttribute('data-variant', 'neutral')
    expect(within(rows[3]).getByText('Đã khóa')).toHaveAttribute('data-variant', 'neutral-outline')
  })

  it('locTheoLoai_trangThai_tuKhoa_goiQuery', async () => {
    api.on('GET', '/accounts', json(200, { items: [], nextCursor: null }))
    render(<AccountList canCreate={false} />)
    await screen.findByText('Không có tài khoản khớp')

    fireEvent.change(screen.getByLabelText('Loại'), { target: { value: 'STAFF' } })
    fireEvent.change(screen.getByLabelText('Trạng thái'), { target: { value: 'LOCKED' } })
    fireEvent.change(screen.getByLabelText('Tìm tên đăng nhập'), { target: { value: ' nv ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lọc' }))

    await waitFor(() => expect(api.calls('GET', '/accounts')).toHaveLength(2))
    const url = new URL(api.calls('GET', '/accounts')[1][0], 'http://localhost')
    expect(url.searchParams.get('type')).toBe('STAFF')
    expect(url.searchParams.get('status')).toBe('LOCKED')
    expect(url.searchParams.get('q')).toBe('nv')
    expect(screen.queryByRole('button', { name: 'Tạo tài khoản nhân viên' })).toBeNull()
  })

  it('FR_042_nutLenhTheoAllowedCommands', async () => {
    const locked = account(5, {
      status: 'LOCKED',
      lockCause: 'RESIGNATION',
      allowedCommands: ['unlock', 'reset-password'],
    })
    api.on('GET', '/accounts', json(200, { items: [locked], nextCursor: null }))
    render(<AccountList canCreate />)

    const sheet = await openDetail(locked)
    await within(sheet).findByRole('button', { name: 'Mở khóa' })
    expect(within(sheet).getByText('Nghỉ việc')).toBeInTheDocument()
    expect(within(sheet).getByRole('button', { name: 'Cấp lại mật khẩu' })).toBeInTheDocument()
    expect(within(sheet).queryByRole('button', { name: 'Khóa tài khoản' })).toBeNull()
    expect(within(sheet).queryByRole('button', { name: 'Gán vai trò' })).toBeNull()
  })

  it('US2_5_khoa_lyDoBatBuoc_guiIfMatch', async () => {
    const item = account(6)
    api.on('GET', '/accounts', json(200, { items: [item], nextCursor: null }))
    api.on('POST', '/accounts/6/lock', json(200, { ...item, status: 'LOCKED' }, { ETag: '"5"' }))
    render(<AccountList canCreate />)

    const sheet = await openDetail(item)
    fireEvent.click(await within(sheet).findByRole('button', { name: 'Khóa tài khoản' }))
    fireEvent.click(within(sheet).getAllByRole('button', { name: 'Khóa tài khoản' }).at(-1))
    expect(await within(sheet).findByText('Nhập lý do')).toBeInTheDocument()
    expect(api.calls('POST', '/accounts/6/lock')).toHaveLength(0)

    fireEvent.change(within(sheet).getByLabelText('Lý do'), { target: { value: 'Nghỉ dài hạn' } })
    fireEvent.click(within(sheet).getAllByRole('button', { name: 'Khóa tài khoản' }).at(-1))
    await waitFor(() => expect(api.calls('POST', '/accounts/6/lock')).toHaveLength(1))
    const [, init] = api.calls('POST', '/accounts/6/lock')[0]
    expect(init.headers['If-Match']).toBe('"4"')
    expect(JSON.parse(init.body)).toEqual({ reason: 'Nghỉ dài hạn' })
  })

  it('Q_292_phuTrachBoPhan_409_hienONguoiThay_guiLai', async () => {
    const item = account(7)
    api.on('GET', '/accounts', json(200, { items: [item], nextCursor: null }))
    let calls = 0
    api.on('POST', '/accounts/7/lock', () =>
      ++calls === 1
        ? problem(409, 'REPLACEMENT_LEAD_REQUIRED', { detail: 'Người này đang là Phụ trách bộ phận' })
        : json(200, { ...item, status: 'LOCKED' }, { ETag: '"5"' }),
    )
    render(<AccountList canCreate />)

    const sheet = await openDetail(item)
    fireEvent.click(await within(sheet).findByRole('button', { name: 'Khóa tài khoản' }))
    fireEvent.change(within(sheet).getByLabelText('Lý do'), { target: { value: 'Nghỉ việc' } })
    fireEvent.click(within(sheet).getAllByRole('button', { name: 'Khóa tài khoản' }).at(-1))

    const field = await within(sheet).findByLabelText('Mã hồ sơ người thay Phụ trách bộ phận')
    expect(within(sheet).getByText('Người này đang là Phụ trách bộ phận')).toBeInTheDocument()
    fireEvent.change(field, { target: { value: '321' } })
    fireEvent.click(within(sheet).getAllByRole('button', { name: 'Khóa tài khoản' }).at(-1))
    await waitFor(() => expect(api.calls('POST', '/accounts/7/lock')).toHaveLength(2))
    expect(JSON.parse(api.calls('POST', '/accounts/7/lock')[1][1].body)).toEqual({
      reason: 'Nghỉ việc',
      replacementDepartmentLead: 321,
    })
  })

  it('BR_M15_17_ganVaiTro_guiETagTapQuyen', async () => {
    const item = account(8)
    api.on('GET', '/accounts', json(200, { items: [item], nextCursor: null }))
    api.on('GET', '/accounts/8/permissions', json(200, { roles: ['AC-05'] }, { ETag: '"2"' }))
    api.on('POST', '/accounts/8/assign-role', json(200, { roles: ['AC-04', 'AC-05'] }, { ETag: '"3"' }))
    render(<AccountList canCreate />)

    const sheet = await openDetail(item)
    fireEvent.click(await within(sheet).findByRole('button', { name: 'Gán vai trò' }))
    const role = within(sheet).getByLabelText('Vai trò')
    expect(within(role).queryByRole('option', { name: 'Nhân viên chăm sóc' })).toBeNull()
    fireEvent.change(role, { target: { value: 'AC-04' } })
    fireEvent.change(within(sheet).getByLabelText('Lý do'), { target: { value: 'Có chứng chỉ' } })
    const submit = within(sheet).getAllByRole('button', { name: 'Gán vai trò' }).at(-1)
    await waitFor(() => expect(submit).toBeEnabled())
    fireEvent.click(submit)

    await waitFor(() => expect(api.calls('POST', '/accounts/8/assign-role')).toHaveLength(1))
    const [, init] = api.calls('POST', '/accounts/8/assign-role')[0]
    expect(init.headers['If-Match']).toBe('"2"')
    expect(JSON.parse(init.body)).toEqual({ role: 'AC-04', reason: 'Có chứng chỉ' })
  })

  it('FR_006_capLaiMatKhau_hienMatKhauTamMotLan', async () => {
    const item = account(9)
    api.on('GET', '/accounts', json(200, { items: [item], nextCursor: null }))
    api.on('POST', '/accounts/9/reset-password', json(200, { temporaryPassword: 'Tam12345abcd' }))
    render(<AccountList canCreate />)

    const sheet = await openDetail(item)
    fireEvent.click(await within(sheet).findByRole('button', { name: 'Cấp lại mật khẩu' }))
    fireEvent.change(within(sheet).getByLabelText('Lý do'), { target: { value: 'Quên mật khẩu' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Cấp mật khẩu tạm' }))

    expect(await within(sheet).findByTestId('temporary-password')).toHaveTextContent('Tam12345abcd')
    expect(within(sheet).getByText(/Chỉ hiện một lần/)).toBeInTheDocument()
  })

  it('US2_1_taoTaiKhoan_itNhatMotVaiTro_hienMatKhauTam', async () => {
    api.on('GET', '/accounts', json(200, { items: [], nextCursor: null }))
    api.on('POST', '/accounts', json(201, { ...account(10), temporaryPassword: 'Moi98765wxyz' }))
    render(<AccountList canCreate />)

    fireEvent.click(await screen.findByRole('button', { name: 'Tạo tài khoản nhân viên' }))
    const sheet = await screen.findByRole('dialog')
    fireEvent.change(within(sheet).getByLabelText('Mã hồ sơ nhân viên'), { target: { value: '110' } })
    fireEvent.change(within(sheet).getByLabelText('Tên đăng nhập'), { target: { value: ' nv-10 ' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Tạo tài khoản' }))
    expect(await within(sheet).findByText('Chọn ít nhất một vai trò')).toBeInTheDocument()

    fireEvent.click(within(sheet).getByRole('checkbox', { name: 'Nhân viên chăm sóc' }))
    fireEvent.click(within(sheet).getByRole('button', { name: 'Tạo tài khoản' }))

    expect(await within(sheet).findByTestId('temporary-password')).toHaveTextContent('Moi98765wxyz')
    const [, init] = api.calls('POST', '/accounts')[0]
    expect(init.headers['Idempotency-Key']).toMatch(/^[0-9a-f-]{36}$/)
    expect(JSON.parse(init.body)).toEqual({ staffId: 110, username: 'nv-10', roles: ['AC-05'] })
  })

  it('FR_012_khongConQuanLyVien_409_hienDieuKienChuaThoa', async () => {
    const item = account(11, { roles: ['AC-01'] })
    api.on('GET', '/accounts', json(200, { items: [item], nextCursor: null }))
    api.on(
      'POST',
      '/accounts/11/lock',
      problem(409, 'NO_MANAGER_LEFT', {
        unmetConditions: [{ code: 'FR-012', description: 'Viện phải còn ít nhất một Quản lý viện Hoạt động' }],
      }),
    )
    render(<AccountList canCreate />)

    const sheet = await openDetail(item)
    fireEvent.click(await within(sheet).findByRole('button', { name: 'Khóa tài khoản' }))
    fireEvent.change(within(sheet).getByLabelText('Lý do'), { target: { value: 'Thử' } })
    fireEvent.click(within(sheet).getAllByRole('button', { name: 'Khóa tài khoản' }).at(-1))

    expect(await within(sheet).findByText('Viện phải còn ít nhất một Quản lý viện Hoạt động')).toBeInTheDocument()
    expect(within(sheet).getByText('Chưa thực hiện được')).toBeInTheDocument()
  })
})

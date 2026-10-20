import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { LoginPage } from './LoginPage'

// 002 T045 (US3; FR-006, FR-014, FR-014a; hợp đồng POST /auth/login): một màn đăng nhập web cho nhân viên và người
// thân (chủ dự án chọn hướng A, 2026-10-20); thông báo chung khi sai; mã thiết bị gửi trong X-Device-Id.

let api

beforeEach(() => {
  api = mockRoutes()
  localStorage.clear()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

function user(overrides = {}) {
  return { accountId: 7, type: 'STAFF', fullName: null, roles: ['AC-04'], mustChangePassword: false, ...overrides }
}

function fill(username, password) {
  fireEvent.change(screen.getByLabelText('Tên đăng nhập'), { target: { value: username } })
  fireEvent.change(screen.getByLabelText('Mật khẩu'), { target: { value: password } })
  fireEvent.click(screen.getByRole('button', { name: 'Đăng nhập' }))
}

describe('LoginPage', () => {
  it('FR_014_sai_thongBaoChung_khongLoTaiKhoan', async () => {
    api.on('POST', '/auth/login', problem(401, 'LOGIN_FAILED', { detail: 'Tên đăng nhập hoặc mật khẩu không đúng' }))
    render(<LoginPage onNavigate={vi.fn()} />)

    fill('nv-a', 'sai12345')

    expect(await screen.findByText('Tên đăng nhập hoặc mật khẩu không đúng')).toBeInTheDocument()
    expect(screen.getByLabelText('Mật khẩu')).toHaveValue('')
  })

  it('FR_014a_guiKenhWeb_maThietBiGiuNguyenGiuaCacLan', async () => {
    api.on('POST', '/auth/login', json(200, { mustChangePassword: false, user: user() }))
    const navigate = vi.fn()
    render(<LoginPage onNavigate={navigate} />)

    fill(' nv-a ', 'dung12345')
    await waitFor(() => expect(navigate).toHaveBeenCalledWith('/'))
    fill('nv-a', 'dung12345')
    await waitFor(() => expect(api.calls('POST', '/auth/login')).toHaveLength(2))

    const [first, second] = api.calls('POST', '/auth/login').map(([, init]) => init)
    expect(JSON.parse(first.body)).toEqual({ username: 'nv-a', password: 'dung12345', channel: 'WEB' })
    expect(first.headers['X-Device-Id']).toMatch(/^[0-9a-f-]{36}$/)
    expect(second.headers['X-Device-Id']).toBe(first.headers['X-Device-Id'])
  })

  it('FR_006_buocDoiMatKhau_chuyenSangDoiMatKhau_nguoiThan_sangCong', async () => {
    let call = 0
    api.on('POST', '/auth/login', () =>
      ++call === 1
        ? json(200, { mustChangePassword: true, user: user({ mustChangePassword: true }) })
        : json(200, { mustChangePassword: false, user: user({ type: 'FAMILY_MEMBER', roles: ['AC-10'] }) }),
    )
    const navigate = vi.fn()
    render(<LoginPage onNavigate={navigate} />)

    fill('nv-a', 'tam123456')
    await waitFor(() => expect(navigate).toHaveBeenLastCalledWith('/change-password'))
    fill('nt-b', 'dung12345')
    await waitFor(() => expect(navigate).toHaveBeenLastCalledWith('/family'))
  })

  it('FR_008_khoaTam_hienGioMo', async () => {
    api.on(
      'POST',
      '/auth/login',
      problem(423, 'ACCOUNT_TEMPORARILY_LOCKED', {
        detail: 'Tài khoản đang bị khóa tạm do đăng nhập sai nhiều lần',
        unlocksAt: '2026-10-20T03:15:00Z',
      }),
    )
    render(<LoginPage onNavigate={vi.fn()} />)

    fill('nv-a', 'dung12345')

    expect(await screen.findByText(/khóa tạm/)).toBeInTheDocument()
    expect(screen.getByText(/20\/10\/2026 10:15/)).toBeInTheDocument()
  })

  it('thieuO_baoTaiO_khongGui', async () => {
    render(<LoginPage onNavigate={vi.fn()} />)

    fireEvent.click(screen.getByRole('button', { name: 'Đăng nhập' }))

    expect(await screen.findByText('Nhập tên đăng nhập')).toBeInTheDocument()
    expect(screen.getByText('Nhập mật khẩu')).toBeInTheDocument()
    expect(api.calls('POST', '/auth/login')).toHaveLength(0)
  })
})

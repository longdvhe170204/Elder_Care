import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { LoginPage } from './LoginPage'

// 002 T105 (US3 #12, #13; FR-052a, Q-06; hợp đồng /auth/password-reset/*): "Quên mật khẩu" chỉ hiện khi kênh tin nhắn
// đã cấu hình; màn không nói mã đã được gửi hay chưa; mã sai báo lỗi chung của máy chủ.

let api

beforeEach(() => {
  api = mockRoutes()
  localStorage.clear()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

const empty = (status) => () => new Response(null, { status })

describe('ForgotPassword', () => {
  it('Q_06_kenhTinNhanChuaCauHinh_anQuenMatKhau', async () => {
    api.on('GET', '/auth/password-reset/availability', json(200, { available: false }))
    render(<LoginPage onNavigate={vi.fn()} />)

    await waitFor(() => expect(api.calls('GET', '/auth/password-reset/availability')).toHaveLength(1))
    expect(screen.queryByRole('button', { name: 'Quên mật khẩu' })).not.toBeInTheDocument()
    expect(screen.getByText(/Nhân viên liên hệ Quản lý viện/)).toBeInTheDocument()
  })

  it('FR_052a_xinMa_nhapMaVaMatKhauMoi_quayVeDangNhap', async () => {
    api.on('GET', '/auth/password-reset/availability', json(200, { available: true }))
    api.on('POST', '/auth/password-reset/request', empty(202))
    api.on('POST', '/auth/password-reset/confirm', empty(204))
    render(<LoginPage onNavigate={vi.fn()} />)

    fireEvent.click(await screen.findByRole('button', { name: 'Quên mật khẩu' }))
    fireEvent.change(screen.getByLabelText('Tên đăng nhập'), { target: { value: ' nt-a ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Gửi mã' }))

    expect(await screen.findByText(/Nếu tài khoản có số điện thoại đã đăng ký/)).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Mã trong tin nhắn'), { target: { value: '123456' } })
    fireEvent.change(screen.getByLabelText('Mật khẩu mới'), { target: { value: 'moi12345' } })
    fireEvent.change(screen.getByLabelText('Nhập lại mật khẩu mới'), { target: { value: 'moi12345' } })
    fireEvent.click(screen.getByRole('button', { name: 'Đặt mật khẩu mới' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Đã đặt mật khẩu mới')
    const [[, request]] = api.calls('POST', '/auth/password-reset/request')
    const [[, confirm]] = api.calls('POST', '/auth/password-reset/confirm')
    expect(JSON.parse(request.body)).toEqual({ username: 'nt-a' })
    expect(JSON.parse(confirm.body)).toEqual({ username: 'nt-a', code: '123456', newPassword: 'moi12345' })
  })

  it('FR_052a_maSai_baoLoiMayChu_giuManNhapMa', async () => {
    api.on('GET', '/auth/password-reset/availability', json(200, { available: true }))
    api.on('POST', '/auth/password-reset/request', empty(202))
    api.on(
      'POST',
      '/auth/password-reset/confirm',
      problem(422, 'RESET_CODE_INVALID', { detail: 'Mã không đúng, đã hết hạn hoặc không còn hiệu lực' }),
    )
    render(<LoginPage onNavigate={vi.fn()} />)

    fireEvent.click(await screen.findByRole('button', { name: 'Quên mật khẩu' }))
    fireEvent.change(screen.getByLabelText('Tên đăng nhập'), { target: { value: 'nt-a' } })
    fireEvent.click(screen.getByRole('button', { name: 'Gửi mã' }))
    fireEvent.change(await screen.findByLabelText('Mã trong tin nhắn'), { target: { value: '000000' } })
    fireEvent.change(screen.getByLabelText('Mật khẩu mới'), { target: { value: 'moi12345' } })
    fireEvent.change(screen.getByLabelText('Nhập lại mật khẩu mới'), { target: { value: 'moi12345' } })
    fireEvent.click(screen.getByRole('button', { name: 'Đặt mật khẩu mới' }))

    expect(await screen.findByText('Mã không đúng, đã hết hạn hoặc không còn hiệu lực')).toBeInTheDocument()
    expect(screen.getByLabelText('Mã trong tin nhắn')).toBeInTheDocument()
  })
})

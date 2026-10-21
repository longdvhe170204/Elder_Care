import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import App from './App'

// 002 T045 (hướng A, chủ dự án chọn 2026-10-20): có phiên thật thì khung nhân viên theo vai trò của tài khoản và có
// nút Đăng xuất; chưa đăng nhập (401) thì sang /login; chạy không có backend thì giữ bản mẫu đổi vai trò.

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('App', () => {
  it('coPhien_vaiTroThat_khongCoODoiVaiTro_dangXuatGoiApi', async () => {
    api.on(
      'GET',
      '/me',
      json(200, {
        accountId: 5,
        type: 'STAFF',
        fullName: 'Lê Văn Trưởng Tầng',
        roles: ['AC-02'],
        mustChangePassword: false,
      }),
    )
    api.on('POST', '/auth/logout', new Response(null, { status: 204 }))
    render(<App />)

    expect(await screen.findByText('Lê Văn Trưởng Tầng')).toBeInTheDocument()
    expect(screen.getByText('Trưởng tầng')).toBeInTheDocument()
    expect(screen.queryByLabelText('Xem với vai trò')).toBeNull()

    fireEvent.click(screen.getByRole('button', { name: 'Đăng xuất' }))
    await waitFor(() => expect(api.calls('POST', '/auth/logout')).toHaveLength(1))
  })

  it('chuaDangNhap_chuyenSangManDangNhap', async () => {
    const assign = vi.fn()
    vi.stubGlobal('location', { ...window.location, assign })
    api.on('GET', '/me', problem(401, 'UNAUTHORIZED'))
    render(<App />)

    await waitFor(() => expect(assign).toHaveBeenCalledWith('/login'))
    expect(screen.queryByLabelText('Xem với vai trò')).toBeNull()
  })

  it('khongCoBackend_giuBanMauDoiVaiTro', async () => {
    api.on('GET', '/me', new Response(null, { status: 500 }))
    render(<App />)

    expect(await screen.findByLabelText('Xem với vai trò')).toBeInTheDocument()
    expect(api.calls('GET', '/me')).toHaveLength(1)
    expect(screen.queryByRole('button', { name: 'Đăng xuất' })).toBeNull()
  })
})

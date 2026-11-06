import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes } from '@/test/fetchRoutes'
import { FamilyPortal } from './FamilyPortal'

// Khung cổng người thân (DESIGN.md 2.3, 9.3; team-plan mục 8 dòng 012): link "Bỏ qua menu" tới nội dung chính (WCAG
// 2.4.1); có phiên người thân thật thì bọc SessionGuard (002 T047b) và Đăng xuất gọi POST /auth/logout; chưa đăng nhập
// (401) thì sang /login; không gọi được backend thì giữ bản mẫu.

let api
let assign

beforeEach(() => {
  api = mockRoutes()
  assign = vi.fn()
  vi.stubGlobal('location', { ...window.location, assign })
  vi.stubGlobal('scrollTo', () => {})
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('Khung cổng người thân', () => {
  it('boQuaMenu_nhayToiNoiDungChinh', async () => {
    api.on('GET', '/me', json(200, { accountId: 41, type: 'FAMILY_MEMBER', fullName: 'Trần Văn Minh', roles: [] }))
    render(<FamilyPortal />)

    const skip = await screen.findByRole('link', { name: 'Bỏ qua menu' })
    expect(skip).toHaveAttribute('href', '#main-content')
    expect(document.getElementById('main-content')).not.toBeNull()
  })

  it('coPhienThat_dangXuatGoiApi_vaHienTenNguoiDangNhap', async () => {
    api.on(
      'GET',
      '/me',
      json(200, {
        accountId: 41,
        type: 'FAMILY_MEMBER',
        fullName: 'Trần Văn Minh',
        roles: [],
        sessionIdleTimeoutSeconds: 1800,
      }),
    )
    api.on('POST', '/auth/logout', new Response(null, { status: 204 }))
    render(<FamilyPortal />)

    expect(await screen.findByText('Trần Văn Minh')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Đăng xuất' }))
    await waitFor(() => expect(api.calls('POST', '/auth/logout')).toHaveLength(1))
    await waitFor(() => expect(assign).toHaveBeenCalledWith('/login'))
  })

  // Q-396 bỏ bản tin tuần: không còn mục Bản tin ở đầu trang, thanh dưới và trang chủ
  it('Q396_khongConBanTin', async () => {
    api.on('GET', '/me', json(200, { accountId: 41, type: 'FAMILY_MEMBER', fullName: 'Trần Văn Minh', roles: [] }))
    render(<FamilyPortal />)

    expect(await screen.findByText('Trần Văn Minh')).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'Thăm' }).length).toBeGreaterThan(0)
    expect(screen.queryByText(/bản tin/i)).toBeNull()
  })

  it('chuaDangNhap_sangDangNhap', async () => {
    api.on('GET', '/me', json(401, { status: 401, errorCode: 'UNAUTHORIZED' }))
    render(<FamilyPortal />)
    await waitFor(() => expect(assign).toHaveBeenCalledWith('/login'))
  })
})

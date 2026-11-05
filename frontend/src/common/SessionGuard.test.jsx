import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { getDashboard } from '@/api'
import { json, mockRoutes } from '@/test/fetchRoutes'
import { SessionGuard } from './SessionGuard'

// 002 T047b (DESIGN.md 9.1 tiêu chí 2.2.5, 9.3 tiêu chí 2.2.1): báo trước 2 phút khi sắp hết phiên; hết phiên thì đăng
// nhập lại ngay trên màn, phần đang nhập giữ nguyên nếu đúng tài khoản; yêu cầu tự làm mới không đếm lại giờ.

const TIMEOUT = 30 * 60
let api
let assign

function Draft() {
  return <input aria-label="Ghi chú đang nhập" defaultValue="" />
}

function renderGuard(onLogout = vi.fn()) {
  render(
    <SessionGuard accountId={7} timeoutSeconds={TIMEOUT} onLogout={onLogout}>
      <Draft />
    </SessionGuard>,
  )
  return onLogout
}

const advance = (ms) => act(() => vi.advanceTimersByTime(ms))

beforeEach(() => {
  vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval', 'Date'], now: new Date('2026-10-29T03:00:00Z') })
  api = mockRoutes()
  assign = vi.fn()
  vi.stubGlobal('location', { ...window.location, assign })
})

afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

describe('SessionGuard', () => {
  it('baoTruoc2Phut_tiepTucLamViec_goiMe_vaDongCanhBao', async () => {
    api.on('GET', '/me', json(200, { accountId: 7 }))
    renderGuard()

    advance((TIMEOUT - 121) * 1000)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    advance(2000)
    const dialog = await screen.findByRole('dialog', { name: 'Phiên đăng nhập sắp hết' })
    expect(dialog).toHaveTextContent('1:59')

    fireEvent.click(screen.getByRole('button', { name: 'Tiếp tục làm việc' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(api.calls('GET', '/me')).toHaveLength(1)
  })

  it('hetPhien_dangNhapLaiCungTaiKhoan_giuPhanDangNhap', async () => {
    api.on('POST', '/auth/login', json(200, { mustChangePassword: false, user: { accountId: 7 } }))
    renderGuard()
    fireEvent.change(screen.getByLabelText('Ghi chú đang nhập'), { target: { value: 'Ăn hết suất trưa' } })

    advance(TIMEOUT * 1000 + 1000)
    expect(await screen.findByRole('dialog', { name: 'Phiên đăng nhập đã hết' })).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Tên đăng nhập'), { target: { value: 'hc01' } })
    fireEvent.change(screen.getByLabelText('Mật khẩu'), { target: { value: 'matkhau123' } })
    fireEvent.click(screen.getByRole('button', { name: 'Đăng nhập lại' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(screen.getByLabelText('Ghi chú đang nhập')).toHaveValue('Ăn hết suất trưa')
    expect(assign).not.toHaveBeenCalled()
  })

  it('dangNhapTaiKhoanKhac_boPhanDangNhap_taiLaiTrang', async () => {
    api.on('POST', '/auth/login', json(200, { mustChangePassword: false, user: { accountId: 8, type: 'STAFF' } }))
    renderGuard()
    advance(TIMEOUT * 1000 + 1000)
    await screen.findByRole('dialog', { name: 'Phiên đăng nhập đã hết' })

    fireEvent.change(screen.getByLabelText('Tên đăng nhập'), { target: { value: 'dd02' } })
    fireEvent.change(screen.getByLabelText('Mật khẩu'), { target: { value: 'matkhau123' } })
    fireEvent.click(screen.getByRole('button', { name: 'Đăng nhập lại' }))

    await waitFor(() => expect(assign).toHaveBeenCalledWith('/'))
  })

  it('may401SessionExpired_hienKhungDangNhapLai', async () => {
    api.on('GET', '/dashboard', json(401, { status: 401, errorCode: 'SESSION_EXPIRED' }))
    renderGuard()

    await act(() => getDashboard().catch(() => {}))
    expect(await screen.findByRole('dialog', { name: 'Phiên đăng nhập đã hết' })).toBeInTheDocument()
  })

  it('yeuCauTuLamMoi_khongDemLaiGio', async () => {
    api.on('GET', '/dashboard', json(200, {}))
    renderGuard()

    advance((TIMEOUT - 300) * 1000)
    await act(() => getDashboard({ headers: { 'X-Background-Refresh': 'true' } }))
    advance(200 * 1000)
    expect(await screen.findByRole('dialog', { name: 'Phiên đăng nhập sắp hết' })).toBeInTheDocument()

    await act(() => getDashboard())
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  })

  it('dangXuatTuKhungCanhBao_goiOnLogout', async () => {
    const onLogout = renderGuard()
    advance((TIMEOUT - 60) * 1000)
    await screen.findByRole('dialog', { name: 'Phiên đăng nhập sắp hết' })

    fireEvent.click(screen.getByRole('button', { name: 'Đăng xuất' }))
    expect(onLogout).toHaveBeenCalled()
  })
})

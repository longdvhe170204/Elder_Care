import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { AccountList } from './AccountList'

// 002 T095 (tên logic TaiKhoanNguoiCaoTuoi.test.jsx; US8; FR-067, FR-069, FR-070, FR-072, FR-073; bố cục duyệt
// 2026-10-24): tạo tài khoản người cao tuổi kèm xác nhận, mã QR đăng nhập, gỡ thiết bị, khóa, mở khóa, đề nghị khóa
// theo lời người thân. 002 T103 (FR-006, FR-051a, FR-069; bố cục duyệt 2026-11-02): không mật khẩu tạm khi tạo, tạo mã
// kèm xác nhận và cảnh báo gỡ máy, cấp lại mật khẩu kèm xác nhận.

function residentAccount(overrides = {}) {
  return {
    id: 41,
    username: 'nct-hoa',
    fullName: 'Nguyễn Thị Hoa',
    type: 'RESIDENT',
    residentId: 501,
    status: 'ACTIVE',
    roles: ['AC-16'],
    lastLoginAt: null,
    boundDevice: { boundAt: '2026-10-20T02:03:00Z', deviceId: 'device-1' },
    allowedCommands: ['login-qr-code', 'reset-password', 'unbind-device', 'lock-resident'],
    ...overrides,
  }
}

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

async function openDetail(item) {
  api.on('GET', '/accounts', json(200, { items: [item], nextCursor: null }))
  api.on('GET', `/accounts/${item.id}`, json(200, item, { ETag: '"7"' }))
  render(<AccountList canCreate={false} canCreateResident />)
  fireEvent.click(await screen.findByRole('button', { name: item.username }))
  return screen.findByRole('dialog')
}

function bodyOf(method, path) {
  return JSON.parse(api.calls(method, path)[0][1].body)
}

describe('Tài khoản người cao tuổi', () => {
  it('FR_067_FR_006_tao_chonNguoiCaoTuoi_xacNhan_khongCoMatKhauTam', async () => {
    api.on('GET', '/accounts', json(200, { items: [], nextCursor: null }))
    api.on(
      'GET',
      '/residents',
      json(200, {
        items: [{ id: 501, profileCode: 'NCT-2026-0501', personalInfo: { fullName: 'Nguyễn Thị Hoa' } }],
        nextCursor: null,
      }),
    )
    api.on('POST', '/resident-accounts', json(201, residentAccount()))
    render(<AccountList canCreate={false} canCreateResident />)

    fireEvent.click(await screen.findByRole('button', { name: 'Tạo tài khoản người cao tuổi' }))
    const sheet = await screen.findByRole('dialog')
    const resident = await within(sheet).findByLabelText('Người cao tuổi')
    await within(resident).findByRole('option', { name: 'Nguyễn Thị Hoa · NCT-2026-0501' })
    fireEvent.change(resident, { target: { value: '501' } })
    fireEvent.change(within(sheet).getByLabelText('Tên đăng nhập'), { target: { value: ' nct-hoa ' } })
    fireEvent.click(within(sheet).getByRole('radio', { name: 'Tự yêu cầu' }))
    fireEvent.change(within(sheet).getByLabelText('Mã hồ sơ nhân viên đã nghe'), { target: { value: '7' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Tạo tài khoản' }))

    expect(
      await within(sheet).findByText(/Người cao tuổi đăng nhập bằng mã QR, không dùng mật khẩu/),
    ).toBeInTheDocument()
    expect(within(sheet).queryByTestId('temporary-password')).toBeNull()
    expect(bodyOf('POST', '/resident-accounts')).toEqual({
      residentId: 501,
      username: 'nct-hoa',
      consent: { method: 'REQUESTED', withStaffId: 7 },
    })
    expect(api.calls('POST', '/resident-accounts')[0][1].headers['Idempotency-Key']).toMatch(/^[0-9a-f-]{36}$/)
    const statusQuery = new URL(api.calls('GET', '/residents')[0][0], 'http://localhost').searchParams
    expect(statusQuery.getAll('status')).toEqual(['IN_RESIDENCE'])
  })

  it('FR_067_thieuXacNhan_baoTaiO_khongGui', async () => {
    api.on('GET', '/accounts', json(200, { items: [], nextCursor: null }))
    api.on('GET', '/residents', json(200, { items: [], nextCursor: null }))
    render(<AccountList canCreate={false} canCreateResident />)

    fireEvent.click(await screen.findByRole('button', { name: 'Tạo tài khoản người cao tuổi' }))
    const sheet = await screen.findByRole('dialog')
    fireEvent.click(within(sheet).getByRole('button', { name: 'Tạo tài khoản' }))

    expect(await within(sheet).findByText('Chọn một người cao tuổi')).toBeInTheDocument()
    expect(within(sheet).getByText('Chọn người cao tuổi tự yêu cầu hay đồng ý')).toBeInTheDocument()
    expect(within(sheet).getByText('Nhập mã hồ sơ nhân viên')).toBeInTheDocument()
    expect(api.calls('POST', '/resident-accounts')).toHaveLength(0)
  })

  it('khongPhaiHanhChinhHayQuanLy_khongCoNutTao', async () => {
    api.on('GET', '/accounts', json(200, { items: [], nextCursor: null }))
    render(<AccountList canCreate={false} canCreateResident={false} />)
    await screen.findByText('Không có tài khoản khớp')

    expect(screen.queryByRole('button', { name: 'Tạo tài khoản người cao tuổi' })).toBeNull()
  })

  it('FR_069_taoMaQr_xacNhan_canhBaoGoMay_hienMaVaThoiGianConLai', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true, now: new Date('2026-10-24T03:00:00Z') })
    api.on(
      'POST',
      '/resident-accounts/501/login-qr-code',
      json(201, { code: 'qr-code-abc', expiresAt: '2026-10-24T03:05:00Z' }),
    )
    const sheet = await openDetail(residentAccount())

    expect(within(sheet).getByText(/Đã gắn 20\/10\/2026 09:03/)).toBeInTheDocument()
    fireEvent.click(await within(sheet).findByRole('button', { name: 'Tạo mã QR' }))
    expect(within(sheet).getByText(/Quét mã sẽ gỡ máy đang gắn \(gắn 20\/10\/2026 09:03\)/)).toBeInTheDocument()
    fireEvent.click(within(sheet).getByRole('button', { name: 'Tạo mã' }))
    expect(await within(sheet).findByText('Chọn người cao tuổi tự yêu cầu hay đồng ý')).toBeInTheDocument()
    expect(api.calls('POST', '/resident-accounts/501/login-qr-code')).toHaveLength(0)

    fireEvent.click(within(sheet).getByRole('radio', { name: 'Tự yêu cầu' }))
    fireEvent.change(within(sheet).getByLabelText('Mã hồ sơ nhân viên đã nghe'), { target: { value: '7' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Tạo mã' }))

    expect(await within(sheet).findByRole('img', { name: 'Mã QR đăng nhập' })).toBeInTheDocument()
    expect(within(sheet).getByText('Còn 5:00')).toBeInTheDocument()
    expect(within(sheet).getByText('Quét mã sẽ gỡ máy đang gắn.')).toBeInTheDocument()
    const [, init] = api.calls('POST', '/resident-accounts/501/login-qr-code')[0]
    expect(init.headers['Idempotency-Key']).toMatch(/^[0-9a-f-]{36}$/)
    expect(JSON.parse(init.body)).toEqual({ consent: { method: 'REQUESTED', withStaffId: 7 } })
  })

  it('FR_069_maHetHan_taoMaMoi_dienSanXacNhan', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true, now: new Date('2026-10-24T03:00:00Z') })
    api.on(
      'POST',
      '/resident-accounts/501/login-qr-code',
      json(201, { code: 'qr-code-abc', expiresAt: '2026-10-24T03:00:02Z' }),
    )
    const sheet = await openDetail(residentAccount({ boundDevice: null, allowedCommands: ['login-qr-code'] }))

    fireEvent.click(await within(sheet).findByRole('button', { name: 'Tạo mã QR' }))
    expect(within(sheet).queryByText(/Quét mã sẽ gỡ máy/)).toBeNull()
    fireEvent.click(within(sheet).getByRole('radio', { name: 'Đồng ý' }))
    fireEvent.change(within(sheet).getByLabelText('Mã hồ sơ nhân viên đã nghe'), { target: { value: '9' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Tạo mã' }))
    await within(sheet).findByRole('img', { name: 'Mã QR đăng nhập' })

    await act(() => vi.advanceTimersByTimeAsync(3000))
    expect(await within(sheet).findByText('Mã đã hết hạn.')).toBeInTheDocument()
    fireEvent.click(within(sheet).getByRole('button', { name: 'Tạo mã mới' }))
    expect(within(sheet).getByRole('radio', { name: 'Đồng ý' })).toBeChecked()
    expect(within(sheet).getByLabelText('Mã hồ sơ nhân viên đã nghe')).toHaveValue('9')
    fireEvent.click(within(sheet).getByRole('button', { name: 'Tạo mã' }))

    await waitFor(() => expect(api.calls('POST', '/resident-accounts/501/login-qr-code')).toHaveLength(2))
    expect(JSON.parse(api.calls('POST', '/resident-accounts/501/login-qr-code')[1][1].body)).toEqual({
      consent: { method: 'AGREED', withStaffId: 9 },
    })
  })

  it('FR_051a_capLaiMatKhau_xacNhan_lyDo_hienMatKhauTam', async () => {
    api.on('POST', '/accounts/41/reset-password', json(200, { temporaryPassword: 'Tam-654321' }))
    const sheet = await openDetail(residentAccount())

    expect(within(sheet).getAllByRole('button', { name: 'Cấp lại mật khẩu' })).toHaveLength(1)
    fireEvent.click(within(sheet).getByRole('button', { name: 'Cấp lại mật khẩu' }))
    fireEvent.click(within(sheet).getByRole('button', { name: 'Cấp mật khẩu tạm' }))
    expect(await within(sheet).findByText('Nhập lý do')).toBeInTheDocument()
    expect(within(sheet).getByText('Nhập mã hồ sơ nhân viên')).toBeInTheDocument()
    expect(api.calls('POST', '/accounts/41/reset-password')).toHaveLength(0)

    fireEvent.click(within(sheet).getByRole('radio', { name: 'Tự yêu cầu' }))
    fireEvent.change(within(sheet).getByLabelText('Mã hồ sơ nhân viên đã nghe'), { target: { value: '7' } })
    fireEvent.change(within(sheet).getByLabelText('Lý do'), { target: { value: 'Bà quên mật khẩu' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Cấp mật khẩu tạm' }))

    expect(await within(sheet).findByText('Tam-654321')).toBeInTheDocument()
    expect(within(sheet).getByText(/Nếu quét mã QR trước khi dùng thì mật khẩu này hết hiệu lực/)).toBeInTheDocument()
    expect(bodyOf('POST', '/accounts/41/reset-password')).toEqual({
      reason: 'Bà quên mật khẩu',
      consent: { method: 'REQUESTED', withStaffId: 7 },
    })
  })

  it('FR_072_khoa_canCu_lyDo_guiIfMatch', async () => {
    api.on('POST', '/accounts/41/lock-resident', json(200, residentAccount({ status: 'LOCKED' }), { ETag: '"8"' }))
    const sheet = await openDetail(residentAccount())

    fireEvent.click(await within(sheet).findByRole('button', { name: 'Khóa' }))
    fireEvent.click(within(sheet).getByRole('radio', { name: 'Người cao tuổi tự yêu cầu' }))
    fireEvent.change(within(sheet).getByLabelText('Lý do'), { target: { value: 'Bà muốn tạm ngừng dùng' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Khóa tài khoản' }))

    await waitFor(() => expect(api.calls('POST', '/accounts/41/lock-resident')).toHaveLength(1))
    const [, init] = api.calls('POST', '/accounts/41/lock-resident')[0]
    expect(init.headers['If-Match']).toBe('"7"')
    expect(JSON.parse(init.body)).toEqual({ basis: 'RESIDENT_REQUESTED', reason: 'Bà muốn tạm ngừng dùng' })
  })

  it('FR_073_moKhoa_xacNhanNguoiCaoTuoi_khoaDoKetThucLuuTru_baoLyDo', async () => {
    api.on(
      'POST',
      '/accounts/41/unlock-resident',
      problem(409, 'LOCK_CAUSE_STILL_EXISTS', { detail: 'Tài khoản khóa do kết thúc lưu trú, không mở lại' }),
    )
    const sheet = await openDetail(
      residentAccount({
        status: 'LOCKED',
        lockCause: 'RESIDENT_RESIDENCY_ENDED',
        boundDevice: null,
        allowedCommands: ['unlock-resident'],
      }),
    )

    expect(within(sheet).getByText('Kết thúc lưu trú')).toBeInTheDocument()
    expect(within(sheet).getByText('Chưa gắn thiết bị')).toBeInTheDocument()
    fireEvent.click(await within(sheet).findByRole('button', { name: 'Mở khóa' }))
    fireEvent.click(within(sheet).getByRole('radio', { name: 'Đồng ý' }))
    fireEvent.change(within(sheet).getByLabelText('Mã hồ sơ nhân viên đã nghe'), { target: { value: '7' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Mở khóa tài khoản' }))

    expect(await within(sheet).findByText('Tài khoản khóa do kết thúc lưu trú, không mở lại')).toBeInTheDocument()
    expect(bodyOf('POST', '/accounts/41/unlock-resident')).toEqual({ consent: { method: 'AGREED', withStaffId: 7 } })
  })

  it('FR_070_goThietBi_lyDoBatBuoc', async () => {
    api.on('POST', '/accounts/41/unbind-device', json(200, residentAccount({ boundDevice: null }), { ETag: '"8"' }))
    const sheet = await openDetail(residentAccount())

    fireEvent.click(await within(sheet).findByRole('button', { name: 'Gỡ thiết bị' }))
    fireEvent.click(within(sheet).getByRole('button', { name: 'Gỡ thiết bị đang gắn' }))
    expect(await within(sheet).findByText('Nhập lý do')).toBeInTheDocument()

    fireEvent.change(within(sheet).getByLabelText('Lý do'), { target: { value: 'Bà báo mất máy' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Gỡ thiết bị đang gắn' }))
    await waitFor(() => expect(api.calls('POST', '/accounts/41/unbind-device')).toHaveLength(1))
    expect(bodyOf('POST', '/accounts/41/unbind-device')).toEqual({ reason: 'Bà báo mất máy' })
  })

  it('A_16_deNghiKhoaTheoLoiNguoiThan_lapYeuCauPheDuyet', async () => {
    api.on('POST', '/approval-requests', json(201, { id: 88, status: 'PENDING' }))
    const sheet = await openDetail(residentAccount())

    fireEvent.click(await within(sheet).findByRole('button', { name: 'Lập đề nghị khóa theo lời người thân' }))
    fireEvent.change(within(sheet).getByLabelText('Mã hồ sơ người thân đề nghị'), { target: { value: '55' } })
    fireEvent.change(within(sheet).getByLabelText('Lý do'), { target: { value: 'Con gái đề nghị khóa' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Gửi Quản lý viện duyệt' }))

    expect(await within(sheet).findByText(/Đã gửi Quản lý viện duyệt, mã yêu cầu 88/)).toBeInTheDocument()
    const body = bodyOf('POST', '/approval-requests')
    expect(body.type).toBe('M15.RESIDENT_ACCOUNT_LOCK')
    expect(body.content).toEqual({ accountId: 41, requestingFamilyMemberId: 55, reason: 'Con gái đề nghị khóa' })
    expect(body.submitImmediately).toBe(true)
  })
})

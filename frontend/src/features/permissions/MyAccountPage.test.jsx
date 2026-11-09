import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { MyAccountPage } from './MyAccountPage'

// Tài khoản của tôi (002 FR-042; bố cục duyệt 2026-11-09): thông tin GET /me, quyền hiện hành GET /me/permissions (chỉ
// xem), lối sang Ca của tôi khi có quyền Lịch ca, đổi mật khẩu. Bản mẫu không có backend thì theo vai trò đang chọn.

const SESSION = {
  accountId: 7,
  type: 'STAFF',
  fullName: 'Trần Thu Hà',
  roles: ['AC-04', 'AC-02'],
  mustChangePassword: false,
  sessionIdleTimeoutSeconds: 1800,
}

const PERMISSIONS = [
  { function: 'TASK_CHECKLIST', operation: 'VIEW', scope: 'ASSIGNED', footnoteCodes: [], source: 'ROLE' },
  { function: 'TASK_CHECKLIST', operation: 'CREATE', scope: 'ASSIGNED', footnoteCodes: [], source: 'ROLE' },
  { function: 'SHIFT_SCHEDULE', operation: 'VIEW', scope: 'FACILITY', footnoteCodes: [], source: 'ROLE' },
  { function: 'QUALITY_CHECK', operation: 'CONFIRM', scope: 'ASSIGNED', footnoteCodes: [], source: 'DUTY' },
]

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('MyAccountPage', () => {
  it('FR_042_coPhien_hienThongTin_vaQuyenHienHanh_chiXem', async () => {
    api.on('GET', '/me/permissions', json(200, PERMISSIONS))
    api.on('GET', '/me/shifts', json(200, []))
    render(<MyAccountPage role="NURSING_STAFF" session={SESSION} onNavigate={() => {}} />)

    // Đầu trang: ảnh chữ cái, họ tên, dòng phụ là các vai trò
    expect(screen.getByRole('heading', { level: 1, name: 'Trần Thu Hà' })).toBeInTheDocument()
    expect(screen.getAllByText('Trưởng tầng, Điều dưỡng').length).toBeGreaterThan(0)

    const table = await screen.findByRole('table')
    // Thứ tự khối theo bố cục duyệt 2026-11-09
    expect(screen.getAllByRole('heading', { level: 2 }).map((h) => h.textContent)).toEqual([
      'Thông tin tài khoản',
      'Bảo mật',
      'Ca sắp tới',
      'Quyền của tôi',
    ])
    const checklist = within(table).getByRole('row', { name: /Checklist, ghi nhận công việc/ })
    expect(within(checklist).getByText('Xem, tạo')).toBeInTheDocument()
    expect(within(checklist).getByText('Theo phân công')).toBeInTheDocument()
    expect(within(checklist).getByText('Vai trò')).toBeInTheDocument()
    expect(within(table).getByRole('row', { name: /Kiểm tra chất lượng/ })).toHaveTextContent('Nhiệm vụ')
    expect(
      within(screen.getByRole('region', { name: 'Quyền của tôi' })).queryByRole('button', { name: /Sửa|Thu hẹp/ }),
    ).toBeNull()
  })

  it('FR_054_caSapToi_hienCaDangLam_vaLoiSangLichCa', async () => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date('2026-11-10T09:00:00+07:00'))
    const onNavigate = vi.fn()
    api.on('GET', '/me/permissions', json(200, PERMISSIONS))
    api.on(
      'GET',
      '/me/shifts',
      json(200, [
        { id: 2, startsAt: '2026-11-11T07:00:00+07:00', endsAt: '2026-11-11T18:00:00+07:00', status: 'PUBLISHED' },
        { id: 1, startsAt: '2026-11-10T07:00:00+07:00', endsAt: '2026-11-10T18:00:00+07:00', status: 'IN_PROGRESS' },
      ]),
    )
    render(<MyAccountPage role="NURSING_STAFF" session={SESSION} onNavigate={onNavigate} />)

    const block = await screen.findByRole('region', { name: 'Ca sắp tới' })
    expect(await within(block).findByText('10/11/2026, 07:00–18:00')).toBeInTheDocument()
    expect(within(block).getByText('Đang diễn ra')).toBeInTheDocument()
    expect(within(block).getByText('11/11/2026, 07:00–18:00')).toBeInTheDocument()
    const [url] = api.calls('GET', '/me/shifts')[0]
    expect(new URL(url, 'http://localhost').searchParams.get('from')).toBe('2026-11-10')
    expect(new URL(url, 'http://localhost').searchParams.get('to')).toBe('2026-11-17')

    fireEvent.click(within(block).getByRole('button', { name: 'Xem lịch ca của tôi' }))
    expect(onNavigate).toHaveBeenCalledWith('shifts')
    vi.useRealTimers()
  })

  it('caSapToi_khongCoCa_noiRo; khongCoQuyenLichCa_thiKhongCoKhoi', async () => {
    api.on('GET', '/me/permissions', json(200, PERMISSIONS))
    api.on('GET', '/me/shifts', json(200, []))
    const { unmount } = render(<MyAccountPage role="NURSING_STAFF" session={SESSION} onNavigate={() => {}} />)
    const block = await screen.findByRole('region', { name: 'Ca sắp tới' })
    expect(await within(block).findByText('Không có ca trong 7 ngày tới.')).toBeInTheDocument()
    unmount()

    api.on(
      'GET',
      '/me/permissions',
      json(
        200,
        PERMISSIONS.filter((p) => p.function !== 'SHIFT_SCHEDULE'),
      ),
    )
    render(<MyAccountPage role="ACCOUNTANT" session={SESSION} onNavigate={() => {}} />)
    await screen.findByRole('table')
    expect(screen.queryByRole('region', { name: 'Ca sắp tới' })).toBeNull()
  })

  it('coPhien_coLienKetDoiMatKhau', async () => {
    api.on('GET', '/me/permissions', json(200, PERMISSIONS))
    render(<MyAccountPage role="NURSING_STAFF" session={SESSION} onNavigate={() => {}} />)

    expect(screen.getByRole('link', { name: 'Đổi mật khẩu' })).toHaveAttribute('href', '/change-password')
  })

  it('loiTaiQuyen_hienLoiTiengViet', async () => {
    api.on('GET', '/me/permissions', problem(500, 'INTERNAL_ERROR'))
    render(<MyAccountPage role="NURSING_STAFF" session={SESSION} onNavigate={() => {}} />)

    expect(await screen.findByRole('alert')).toBeInTheDocument()
    expect(screen.queryByRole('table')).toBeNull()
  })

  it('banMau_khongGoiApi_hienVaiTroDangChon_vaMucDuocDung', () => {
    render(<MyAccountPage role="KITCHEN_STAFF" session={null} onNavigate={() => {}} />)

    expect(screen.getByText('Bản mẫu, dữ liệu minh họa')).toBeInTheDocument()
    expect(screen.getByText('Nhân viên bếp')).toBeInTheDocument()
    expect(screen.getByText('Suất ăn')).toBeInTheDocument()
    expect(screen.queryByText('Phát thuốc')).toBeNull()
    expect(screen.queryByRole('link', { name: 'Đổi mật khẩu' })).toBeNull()
    expect(api.fetchMock).not.toHaveBeenCalled()
  })

  it('banMau_chiLietKeMucDaCoMan_nhuMenu', () => {
    render(<MyAccountPage role="ACCOUNTANT" built={new Set(['charges'])} onNavigate={() => {}} />)

    expect(screen.getByText('Chi phí')).toBeInTheDocument()
    expect(screen.queryByText('Tiếp nhận, hợp đồng')).toBeNull()
  })
})

// Liên hệ của chính mình (api-002 PUT /me/contact; A duyệt 2026-11-09): số điện thoại dùng nhận mã đặt lại mật khẩu
// (BR-M15-25) nên bắt buộc mật khẩu hiện tại; đổi số thì backend báo về số cũ.
describe('MyAccountPage, liên hệ', () => {
  const STAFF = { ...SESSION, phoneNumber: '0912000111', email: null }

  function openForm() {
    api.on('GET', '/me/permissions', json(200, []))
    render(<MyAccountPage role="NURSING_STAFF" session={STAFF} onNavigate={() => {}} />)
    const info = screen.getByRole('region', { name: 'Thông tin tài khoản' })
    fireEvent.click(within(info).getByRole('button', { name: 'Sửa liên hệ' }))
    return info
  }

  it('hienSoDienThoai_emailChuaCo', () => {
    api.on('GET', '/me/permissions', json(200, []))
    render(<MyAccountPage role="NURSING_STAFF" session={STAFF} onNavigate={() => {}} />)
    const info = screen.getByRole('region', { name: 'Thông tin tài khoản' })

    expect(within(info).getByText('0912000111')).toBeInTheDocument()
    expect(within(info).getByText('Chưa có')).toBeInTheDocument()
  })

  it('luu_guiMatKhauHienTai_hienGiaTriMoi_vaBaoDaLuu', async () => {
    api.on('PUT', '/me/contact', json(200, { ...STAFF, phoneNumber: '0987654321', email: 'ha@vien.vn' }))
    const info = openForm()

    expect(within(info).getByText(/đổi số sẽ gửi tin báo về số cũ/)).toBeInTheDocument()
    fireEvent.change(within(info).getByLabelText('Số điện thoại'), { target: { value: '0987 654 321' } })
    fireEvent.change(within(info).getByLabelText('Email'), { target: { value: 'ha@vien.vn' } })
    fireEvent.change(within(info).getByLabelText(/Mật khẩu hiện tại/), { target: { value: 'matkhau123' } })
    fireEvent.click(within(info).getByRole('button', { name: 'Lưu liên hệ' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Đã lưu liên hệ')
    expect(within(info).getByText('0987654321')).toBeInTheDocument()
    expect(within(info).getByText('ha@vien.vn')).toBeInTheDocument()
    const [, init] = api.calls('PUT', '/me/contact')[0]
    expect(JSON.parse(init.body)).toEqual({
      currentPassword: 'matkhau123',
      phoneNumber: '0987 654 321',
      email: 'ha@vien.vn',
    })
  })

  it('thieuMatKhau_baoTaiO_khongGoiApi', () => {
    const info = openForm()
    fireEvent.click(within(info).getByRole('button', { name: 'Lưu liên hệ' }))

    expect(within(info).getByLabelText(/Mật khẩu hiện tại/)).toHaveAttribute('aria-invalid', 'true')
    expect(within(info).getByText('Nhập mật khẩu hiện tại')).toBeInTheDocument()
    expect(api.calls('PUT', '/me/contact')).toHaveLength(0)
  })

  it('422_CONTACT_INVALID_baoTaiTungO; INVALID_VALUE_baoTaiMatKhau; giuGiaTriDaGo', async () => {
    api.on(
      'PUT',
      '/me/contact',
      problem(422, 'CONTACT_INVALID', {
        unmetConditions: [
          { code: 'phoneNumber', description: 'Số điện thoại cần 10 chữ số, bắt đầu bằng 0' },
          { code: 'email', description: 'Email chưa đúng định dạng, ví dụ ten@vien.vn' },
        ],
      }),
    )
    const info = openForm()
    fireEvent.change(within(info).getByLabelText('Số điện thoại'), { target: { value: '123' } })
    fireEvent.change(within(info).getByLabelText(/Mật khẩu hiện tại/), { target: { value: 'matkhau123' } })
    fireEvent.click(within(info).getByRole('button', { name: 'Lưu liên hệ' }))

    expect(await within(info).findByText('Số điện thoại cần 10 chữ số, bắt đầu bằng 0')).toBeInTheDocument()
    expect(within(info).getByLabelText('Số điện thoại')).toHaveAttribute('aria-invalid', 'true')
    expect(within(info).getByLabelText('Số điện thoại')).toHaveValue('123')
    expect(within(info).getByLabelText('Email')).toHaveAttribute('aria-invalid', 'true')

    api.on('PUT', '/me/contact', problem(422, 'INVALID_VALUE', { detail: 'Mật khẩu hiện tại không đúng' }))
    fireEvent.click(within(info).getByRole('button', { name: 'Lưu liên hệ' }))
    expect(await within(info).findByText('Mật khẩu hiện tại không đúng')).toBeInTheDocument()
    expect(within(info).getByLabelText(/Mật khẩu hiện tại/)).toHaveAttribute('aria-invalid', 'true')
  })
})

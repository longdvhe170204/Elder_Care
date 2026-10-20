import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { ChangePasswordPage } from './ChangePasswordPage'

// 002 T045 (US3 #9, #10; FR-006, FR-006a; hợp đồng POST /auth/change-password): đổi mật khẩu bắt buộc lần đầu; không đạt
// CFG-M15-09 thì hiện yêu cầu cần đạt; đổi xong vào đúng khung theo loại tài khoản.

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

function fill(current, next, again = next) {
  fireEvent.change(screen.getByLabelText('Mật khẩu hiện tại'), { target: { value: current } })
  fireEvent.change(screen.getByLabelText('Mật khẩu mới'), { target: { value: next } })
  fireEvent.change(screen.getByLabelText('Nhập lại mật khẩu mới'), { target: { value: again } })
  fireEvent.click(screen.getByRole('button', { name: 'Đổi mật khẩu' }))
}

describe('ChangePasswordPage', () => {
  it('US3_10_khongDat_hienYeuCauCanDat', async () => {
    api.on(
      'POST',
      '/auth/change-password',
      problem(422, 'PASSWORD_POLICY_NOT_MET', {
        detail: 'Mật khẩu mới chưa đạt yêu cầu',
        unmetConditions: [{ code: 'CFG-M15-09', description: 'Tối thiểu 8 ký tự, có cả chữ và số' }],
      }),
    )
    render(<ChangePasswordPage onNavigate={vi.fn()} />)

    fill('tam123456', 'abcdefgh')

    expect(await screen.findByText('Tối thiểu 8 ký tự, có cả chữ và số')).toBeInTheDocument()
  })

  it('US3_9_doiXong_vaoKhungTheoLoaiTaiKhoan', async () => {
    api.on('POST', '/auth/change-password', new Response(null, { status: 204 }))
    api.on(
      'GET',
      '/me',
      json(200, { accountId: 3, type: 'FAMILY_MEMBER', fullName: null, roles: ['AC-10'], mustChangePassword: false }),
    )
    const navigate = vi.fn()
    render(<ChangePasswordPage onNavigate={navigate} />)

    fill('tam123456', 'abcd1234')

    await waitFor(() => expect(navigate).toHaveBeenCalledWith('/family'))
    expect(JSON.parse(api.calls('POST', '/auth/change-password')[0][1].body)).toEqual({
      currentPassword: 'tam123456',
      newPassword: 'abcd1234',
    })
  })

  it('nhapLaiKhongKhop_baoTaiO_khongGui', async () => {
    render(<ChangePasswordPage onNavigate={vi.fn()} />)

    fill('tam123456', 'abcd1234', 'abcd12345')

    expect(await screen.findByText('Hai lần nhập mật khẩu mới chưa giống nhau')).toBeInTheDocument()
    expect(api.calls('POST', '/auth/change-password')).toHaveLength(0)
  })
})

import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { AppShell } from '@/common/layout/AppShell'

describe('Điều hướng nhân sự sau đăng nhập', () => {
  it('FR_049_managerCanRenderStaffNavigationWithMedicationCatalog', () => {
    render(
      <AppShell
        role="NURSING_HOME_MANAGER"
        session={{ accountId: 1, fullName: '[Quản lý kiểm thử]' }}
        onRoleChange={vi.fn()}
        onLogout={vi.fn()}
        page="staff"
        onPageChange={vi.fn()}
      >
        <h1>Hồ sơ nhân viên</h1>
      </AppShell>,
    )

    expect(screen.getByRole('heading', { name: 'Hồ sơ nhân viên' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Nhân sự' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('button', { name: 'Lịch ca' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Danh mục thuốc' })).toBeInTheDocument()
  })

  it('FR_049_liveMenuHidesStaffWhenStaffProfilePermissionIsMissing', () => {
    render(
      <AppShell
        role="NURSING_HOME_MANAGER"
        hasFunction={(code) => code === 'SHIFT_SCHEDULE'}
        session={{ accountId: 1, fullName: '[Quản lý kiểm thử]' }}
        onRoleChange={vi.fn()}
        onLogout={vi.fn()}
        page="shifts"
        onPageChange={vi.fn()}
      >
        <h1>Lịch làm việc</h1>
      </AppShell>,
    )

    expect(screen.getByRole('button', { name: 'Lịch ca' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Nhân sự' })).toBeNull()
  })
})

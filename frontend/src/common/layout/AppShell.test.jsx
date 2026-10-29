import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { AppShell } from './AppShell'

// Khung chung (DESIGN.md 5, 10): dưới 768px thanh trên gọn (nút mở menu, tên màn, ảnh đại diện vai trò), menu mở
// thành ngăn kéo; khối người dùng không cắt tên. jsdom không áp CSS nên thanh trên và menu trái cùng có trong DOM.

afterEach(() => {
  vi.unstubAllGlobals()
})

function shell(props = {}) {
  const onPageChange = vi.fn()
  render(
    <AppShell
      role="NURSING_STAFF"
      hasFunction={() => true}
      onRoleChange={() => {}}
      page="residents"
      onPageChange={onPageChange}
      {...props}
    >
      <p>Nội dung</p>
    </AppShell>,
  )
  return { onPageChange }
}

describe('AppShell', () => {
  it('thanh trên có nút mở menu và tên màn đang mở', () => {
    shell()
    const bar = screen.getByRole('banner')
    expect(within(bar).getByRole('button', { name: 'Mở menu' })).toBeTruthy()
    expect(within(bar).getByText('Người cao tuổi')).toBeTruthy()
  })

  it('nút mở menu mở ngăn kéo có các mục menu; chọn mục thì chuyển màn và đóng ngăn kéo', async () => {
    const { onPageChange } = shell()
    fireEvent.click(screen.getByRole('button', { name: 'Mở menu' }))

    const drawer = await screen.findByRole('dialog', { name: 'Menu' })
    fireEvent.click(within(drawer).getByRole('button', { name: 'Checklist ca' }))

    expect(onPageChange).toHaveBeenCalledWith('care-tasks')
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Menu' })).toBeNull())
  })

  it('khối người dùng không cắt tên tài khoản', () => {
    shell({ session: { accountId: 9, fullName: 'Nguyễn Thị Phương Thảo' }, onLogout: () => {} })
    const name = screen.getAllByText('Nguyễn Thị Phương Thảo')[0]
    expect(name.className).not.toContain('truncate')
  })
})

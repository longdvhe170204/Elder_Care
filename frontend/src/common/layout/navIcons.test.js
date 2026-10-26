import { describe, expect, it } from 'vitest'
import { NAV_GROUPS } from './navigation'
import { ICONS } from './navIcons'

// Mục menu có icon không khai trong ICONS thì AppShell render undefined, màn hình trắng sau đăng nhập.
describe('navIcons', () => {
  it('mọi icon của menu đều có trong ICONS', () => {
    const missing = NAV_GROUPS.flatMap((group) => group.items)
      .filter((item) => !ICONS[item.icon])
      .map((item) => `${item.key}: ${item.icon}`)

    expect(missing).toEqual([])
  })
})

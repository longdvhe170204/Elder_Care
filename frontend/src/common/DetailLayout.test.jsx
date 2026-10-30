import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { Sheet, SheetContent, SheetTitle } from '@/components/ui/sheet'
import { DetailLayout } from './DetailLayout'

// Khung chi tiết cố định từ 1280px (DESIGN.md 5, 7.2 detail-panel): Esc trong khung thì đóng khung; Esc trong Sheet
// mở chồng từ khung (portal, ví dụ phần Sức khỏe của hồ sơ) chỉ đóng Sheet đó, không đóng khung (WCAG 2.1.2).

beforeEach(() => {
  vi.stubGlobal('matchMedia', () => ({ matches: true, addEventListener() {}, removeEventListener() {} }))
})

afterEach(() => {
  vi.unstubAllGlobals()
})

function Page({ onClose, child }) {
  return (
    <DetailLayout open detailKey={1} onClose={onClose} label="Chi tiết" renderDetail={() => child}>
      <p>Danh sách</p>
    </DetailLayout>
  )
}

describe('DetailLayout', () => {
  it('Esc trong khung thì đóng khung', () => {
    const onClose = vi.fn()
    render(<Page onClose={onClose} child={<button type="button">Trong khung</button>} />)

    fireEvent.keyDown(screen.getByRole('button', { name: 'Trong khung' }), { key: 'Escape' })

    expect(onClose).toHaveBeenCalledTimes(1)
  })

  it('đang có Sheet mở chồng từ khung thì Esc chỉ để Sheet đóng, không đóng khung', async () => {
    const onClose = vi.fn()
    render(
      <Page
        onClose={onClose}
        child={
          <>
            <button type="button">Mở phần</button>
            <Sheet open>
              <SheetContent>
                <SheetTitle>Phần chồng</SheetTitle>
                <button type="button">Trong phần chồng</button>
              </SheetContent>
            </Sheet>
          </>
        }
      />,
    )
    await screen.findByRole('button', { name: 'Trong phần chồng' })

    // Focus có lúc còn ở nút đã mở Sheet (trong khung) khi bấm Esc
    fireEvent.keyDown(screen.getByRole('button', { name: 'Mở phần', hidden: true }), { key: 'Escape' })
    fireEvent.keyDown(screen.getByRole('button', { name: 'Trong phần chồng' }), { key: 'Escape' })

    expect(onClose).not.toHaveBeenCalled()
  })
})

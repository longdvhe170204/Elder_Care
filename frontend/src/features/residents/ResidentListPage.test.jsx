import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { mockRoutes } from '@/test/fetchRoutes'
import { DEMO_RESIDENTS } from '@/common/demoData'
import { ResidentListPage } from './ResidentListPage'

// Màn Người cao tuổi (DESIGN.md mục 5, 5.22.0): danh sách toàn chiều rộng, bấm dòng mở hộp chi tiết ở giữa màn.

beforeEach(() => {
  mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('ResidentListPage', () => {
  it('ban đầu chỉ có danh sách, bấm tên thì mở hộp chi tiết, Esc thì đóng', async () => {
    const first = DEMO_RESIDENTS[0]
    render(<ResidentListPage role="NURSING_STAFF" />)

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: new RegExp(first.code) }))

    const dialog = await screen.findByRole('dialog')
    expect(dialog).toHaveTextContent(first.name)
    expect(dialog).toHaveTextContent(`${first.code}, phòng ${first.room}`)

    fireEvent.keyDown(dialog, { key: 'Escape' })
    await vi.waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  })
})

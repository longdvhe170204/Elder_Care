import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { DEMO_RESIDENTS } from '@/common/demoData'
import { ResidentListPage } from './ResidentListPage'

// Màn Người cao tuổi (DESIGN.md mục 5, 5.22.0): danh sách toàn chiều rộng, bấm dòng mở hộp chi tiết ở giữa màn.
// Có phiên thật (GET /me) thì chỉ dữ liệu thật của 001 (GET /residents); không có backend thì bản mẫu (team-plan L106).

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('ResidentListPage', () => {
  it('không có phiên: bản mẫu, bấm tên thì mở hộp chi tiết, Esc thì đóng', async () => {
    api.on('GET', '/me', problem(401, 'UNAUTHENTICATED'))
    const first = DEMO_RESIDENTS[0]
    render(<ResidentListPage role="NURSING_STAFF" />)

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

    fireEvent.click(await screen.findByRole('button', { name: new RegExp(first.code) }))

    const dialog = await screen.findByRole('dialog')
    expect(dialog).toHaveTextContent(first.name)
    expect(dialog).toHaveTextContent(`${first.code}, phòng ${first.room}`)

    fireEvent.keyDown(dialog, { key: 'Escape' })
    await vi.waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  })

  it('có phiên: chỉ hồ sơ thật từ GET /residents, không còn dữ liệu mẫu', async () => {
    api.on('GET', '/me', json(200, { accountId: 1, type: 'STAFF', roles: ['NURSING_STAFF'] }))
    api.on(
      'GET',
      '/residents',
      json(200, {
        items: [
          {
            id: 7,
            profileCode: 'NCT-2026-000007',
            personalInfo: { fullName: 'Bà Thật', dateOfBirth: '1945-01-01', gender: 'FEMALE' },
            status: 'IN_RESIDENCE',
          },
        ],
        nextCursor: null,
      }),
    )
    api.on('GET', '/reassessments/due', json(200, []))
    render(<ResidentListPage role="NURSING_STAFF" />)

    expect(await screen.findByText('Bà Thật')).toBeInTheDocument()
    for (const demo of DEMO_RESIDENTS) {
      expect(screen.queryByText(demo.name)).not.toBeInTheDocument()
    }
    expect(screen.queryByRole('group', { name: 'Lọc theo trạng thái' })).not.toBeInTheDocument()
    expect(screen.queryByText(/hồ sơ trong phạm vi bạn được xem/)).not.toBeInTheDocument()
  })
})

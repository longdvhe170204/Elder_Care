import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { ResidentSearch } from './ResidentSearch'

// 001 T066 (US6; FR-041): bộ lọc "Cần đánh giá lại" và cột Đánh giá lại trong bảng hồ sơ. Danh sách lấy từ
// GET /reassessments/due; bật bộ lọc thì bảng chỉ có những người đó, sắp theo hạn.

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

function profile(id, fullName) {
  return {
    id,
    profileCode: `NCT-2026-00000${id}`,
    personalInfo: { fullName, dateOfBirth: '1945-01-01', gender: 'FEMALE' },
    status: 'IN_RESIDENCE',
  }
}

function setup({ due, dueResponse } = {}) {
  api.on(
    'GET',
    '/residents',
    json(200, { items: [profile(1, 'Bà An'), profile(2, 'Ông Bình'), profile(3, 'Bà Chi')], nextCursor: null }),
  )
  api.on('GET', '/residents/2', json(200, profile(2, 'Ông Bình')))
  api.on('GET', '/residents/3', json(200, profile(3, 'Bà Chi')))
  api.on('GET', '/reassessments/due', dueResponse ?? json(200, due ?? []))
}

const OVERDUE = {
  residentId: 3,
  status: 'OVERDUE',
  dueAt: '2026-10-20T01:00:00Z',
  overdueSince: '2026-10-20T01:00:00Z',
  basis: [],
}
const DUE = { residentId: 2, status: 'DUE', dueAt: '2026-10-25T07:30:00Z', overdueSince: null, basis: [] }
const HOSPITAL = { residentId: 1, status: 'DUE', dueAt: null, overdueSince: null, basis: [] }

describe('ResidentSearch, đánh giá lại', () => {
  it('cột Đánh giá lại: Quá hạn, hạn, hạn tính từ lúc trở về; người không cần để trống', async () => {
    setup({ due: [OVERDUE, DUE, HOSPITAL] })
    render(<ResidentSearch reloadKey={0} />)

    const rowOf = async (name) => (await screen.findByText(name)).closest('tr')
    expect(within(await rowOf('Bà Chi')).getByText('Quá hạn')).toBeTruthy()
    expect(within(await rowOf('Bà Chi')).getByText(/từ 20\/10\/2026 08:00/)).toBeTruthy()
    expect(within(await rowOf('Ông Bình')).getByText('Cần đánh giá lại')).toBeTruthy()
    expect(within(await rowOf('Ông Bình')).getByText(/Hạn 25\/10\/2026 14:30/)).toBeTruthy()
    expect(within(await rowOf('Bà An')).getByText(/tính từ lúc Ghi nhận trở về/)).toBeTruthy()
  })

  it('bộ lọc Cần đánh giá lại (n): bảng chỉ còn những người đó, quá hạn trước', async () => {
    setup({ due: [OVERDUE, DUE] })
    render(<ResidentSearch reloadKey={0} />)
    const filter = await screen.findByRole('button', { name: 'Cần đánh giá lại (2)' })
    expect(filter.getAttribute('aria-pressed')).toBe('false')

    fireEvent.click(filter)

    await waitFor(() => expect(screen.queryByText('Bà An')).toBeNull())
    const names = screen.getAllByRole('button', { name: /^(Bà|Ông) / }).map((b) => b.textContent)
    expect(names).toEqual(['Bà Chi', 'Ông Bình'])
    expect(screen.getByRole('button', { name: 'Cần đánh giá lại (2)' }).getAttribute('aria-pressed')).toBe('true')

    fireEvent.click(screen.getByRole('button', { name: 'Tất cả hồ sơ' }))
    expect(await screen.findByText('Bà An')).toBeTruthy()
  })

  it('không ai cần đánh giá lại: bộ lọc ghi (0) và hiện trạng thái trống', async () => {
    setup({ due: [] })
    render(<ResidentSearch reloadKey={0} />)

    fireEvent.click(await screen.findByRole('button', { name: 'Cần đánh giá lại (0)' }))

    expect(await screen.findByText('Chưa có ai cần đánh giá lại')).toBeTruthy()
  })

  it('không có quyền xem đánh giá (403): bảng hồ sơ vẫn hiện, không có bộ lọc', async () => {
    setup({ dueResponse: problem(403, 'PERMISSION_DENIED') })
    render(<ResidentSearch reloadKey={0} />)

    expect(await screen.findByText('Bà An')).toBeTruthy()
    expect(screen.queryByRole('button', { name: /Cần đánh giá lại/ })).toBeNull()
  })
})

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { ResidentListPage } from './ResidentListPage'

// Màn Người cao tuổi (DESIGN.md 5, 5.1, 6.7.0): một ô tìm, thanh lọc trạng thái theo tham số `status` của
// GET /residents, bảng theo trường của Profile (api-001), hồ sơ mở trong khung chi tiết (Sheet khi hẹp hơn 1280px).
// 001 T066 (US6; FR-041): bộ lọc "Cần đánh giá lại" và cột Đánh giá lại lấy từ GET /reassessments/due.

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

function profile(id, fullName, overrides = {}) {
  return {
    id,
    profileCode: `NCT-2026-00000${id}`,
    personalInfo: { fullName, dateOfBirth: '1945-01-01', gender: 'FEMALE' },
    status: 'IN_RESIDENCE',
    currentCareLevel: 'REGULAR',
    currentResidenceType: 'LONG_TERM_RESIDENTIAL',
    allowedCommands: [],
    hiddenFields: [],
    ...overrides,
  }
}

function setup({ due, dueResponse, list } = {}) {
  api.on(
    'GET',
    '/residents',
    json(200, { items: list ?? [profile(1, 'Bà An'), profile(2, 'Ông Bình'), profile(3, 'Bà Chi')], nextCursor: null }),
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

const rowOf = async (name) => (await screen.findByText(name)).closest('tr')

describe('ResidentListPage', () => {
  it('một ô tìm duy nhất; tìm gửi q, bảng hiện mức chăm sóc, loại hình, trạng thái theo hợp đồng', async () => {
    setup()
    render(<ResidentListPage role="NURSING_STAFF" />)

    const row = await rowOf('Bà An')
    expect(within(row).getByText('NCT-2026-000001')).toBeTruthy()
    // Mức chăm sóc ở cột riêng và ở dòng phụ dưới tên khi vùng danh sách hẹp (DESIGN.md 10)
    expect(within(row).getAllByText('Chăm sóc thường xuyên')).toHaveLength(2)
    expect(within(row).getByText('Nội trú dài hạn')).toBeTruthy()
    expect(within(row).getByText('Đang lưu trú')).toBeTruthy()
    expect(screen.getAllByRole('searchbox')).toHaveLength(1)

    fireEvent.change(screen.getByRole('searchbox', { name: 'Tìm hồ sơ' }), { target: { value: 'Chi' } })
    fireEvent.submit(screen.getByRole('searchbox', { name: 'Tìm hồ sơ' }))

    await waitFor(() =>
      expect(api.calls('GET', '/residents').some(([url]) => new URL(url, 'http://x').searchParams.get('q') === 'Chi')).toBe(
        true,
      ),
    )
  })

  it('lọc trạng thái gửi tham số status của hợp đồng', async () => {
    setup()
    render(<ResidentListPage role="NURSING_STAFF" />)
    await rowOf('Bà An')

    fireEvent.click(screen.getByRole('button', { name: 'Tạm vắng' }))

    await waitFor(() =>
      expect(
        api
          .calls('GET', '/residents')
          .some(([url]) => new URL(url, 'http://x').searchParams.get('status') === 'TEMPORARILY_ABSENT'),
      ).toBe(true),
    )
  })

  it('bấm tên mở hồ sơ: dị ứng, lưu ý chăm sóc, nhu cầu chăm sóc; các phần mở bằng nút', async () => {
    setup()
    api.on(
      'GET',
      '/residents/1',
      json(
        200,
        profile(1, 'Bà An', {
          statusSince: '2026-09-01T01:00:00Z',
          careNote: 'Đỡ khi đứng dậy',
          careNeeds: ['Hỗ trợ di chuyển'],
          activeHealthItems: [
            { id: 1, type: 'ALLERGY', allergen: { id: 4, name: 'Hải sản' }, status: 'ACTIVE' },
            { id: 2, type: 'CHRONIC_CONDITION', freeText: 'Tăng huyết áp', status: 'ACTIVE' },
          ],
        }),
        { ETag: '"1"' },
      ),
    )
    api.on('GET', '/residents/1/linked-profiles', json(200, []))
    render(<ResidentListPage role="NURSING_STAFF" />)

    fireEvent.click(await screen.findByRole('button', { name: /Bà An/ }))

    const detail = await screen.findByRole('dialog')
    expect(await within(detail).findByText('Dị ứng:')).toBeTruthy()
    expect(within(detail).getByText('Hải sản')).toBeTruthy()
    expect(within(detail).queryByText(/Tăng huyết áp/)).toBeNull()
    expect(within(detail).getByText('Đỡ khi đứng dậy')).toBeTruthy()
    expect(within(detail).getByText('Hỗ trợ di chuyển')).toBeTruthy()
    expect(within(detail).getByText(/từ 01\/09\/2026/)).toBeTruthy()
    for (const part of ['Sức khỏe', 'Đánh giá', 'Trạng thái', 'Đồng ý', 'Nguyện vọng']) {
      expect(within(detail).getByRole('button', { name: part })).toBeTruthy()
    }
  })

  it('cột Đánh giá lại: Quá hạn, hạn, hạn tính từ lúc trở về; người không cần để trống', async () => {
    setup({ due: [OVERDUE, DUE, HOSPITAL] })
    render(<ResidentListPage role="NURSING_STAFF" />)

    expect(within(await rowOf('Bà Chi')).getByText('Quá hạn')).toBeTruthy()
    expect(within(await rowOf('Bà Chi')).getByText(/từ 20\/10\/2026 08:00/)).toBeTruthy()
    expect(within(await rowOf('Ông Bình')).getByText('Cần đánh giá lại')).toBeTruthy()
    expect(within(await rowOf('Ông Bình')).getByText(/Hạn 25\/10\/2026 14:30/)).toBeTruthy()
    expect(within(await rowOf('Bà An')).getByText(/tính từ lúc Ghi nhận trở về/)).toBeTruthy()
  })

  it('bộ lọc Cần đánh giá lại n: bảng chỉ còn những người đó, quá hạn trước', async () => {
    setup({ due: [OVERDUE, DUE] })
    render(<ResidentListPage role="NURSING_STAFF" />)
    const filter = await screen.findByRole('button', { name: 'Cần đánh giá lại 2' })
    expect(filter.getAttribute('aria-pressed')).toBe('false')

    fireEvent.click(filter)

    await waitFor(() => expect(screen.queryByText('Bà An')).toBeNull())
    const names = screen.getAllByRole('button', { name: /^(Bà|Ông) / }).map((b) => b.getAttribute('aria-label'))
    expect(names).toEqual(['Bà Chi, NCT-2026-000003', 'Ông Bình, NCT-2026-000002'])
    expect(screen.getByRole('button', { name: 'Cần đánh giá lại 2' }).getAttribute('aria-pressed')).toBe('true')

    fireEvent.click(screen.getByRole('button', { name: 'Tất cả' }))
    expect(await screen.findByText('Bà An')).toBeTruthy()
  })

  it('không ai cần đánh giá lại: bộ lọc ghi 0 và hiện trạng thái trống', async () => {
    setup({ due: [] })
    render(<ResidentListPage role="NURSING_STAFF" />)

    fireEvent.click(await screen.findByRole('button', { name: 'Cần đánh giá lại 0' }))

    expect(await screen.findByText('Chưa có ai cần đánh giá lại')).toBeTruthy()
  })

  it('không có quyền xem đánh giá (403): bảng hồ sơ vẫn hiện, không có bộ lọc và cột Đánh giá lại', async () => {
    setup({ dueResponse: problem(403, 'PERMISSION_DENIED') })
    render(<ResidentListPage role="NURSING_STAFF" />)

    expect(await screen.findByText('Bà An')).toBeTruthy()
    expect(screen.queryByRole('button', { name: /Cần đánh giá lại/ })).toBeNull()
    expect(screen.queryByRole('columnheader', { name: 'Đánh giá lại' })).toBeNull()
  })

  it('không còn dữ liệu mẫu ngoài hợp đồng: không có cột Phòng', async () => {
    setup()
    render(<ResidentListPage role="NURSING_STAFF" />)
    await rowOf('Bà An')

    expect(screen.queryByRole('columnheader', { name: 'Phòng' })).toBeNull()
  })
})

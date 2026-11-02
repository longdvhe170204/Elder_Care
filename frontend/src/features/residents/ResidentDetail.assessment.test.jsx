import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes } from '@/test/fetchRoutes'
import { ResidentDetail } from './ResidentDetail'

// BUG-TC03-D-01: đính chính đánh giá sinh lần báo đánh giá lại; khung chi tiết phải báo lên màn danh sách để màn đó nạp
// lại GET /reassessments/due, nếu không Bác sĩ không thấy hồ sơ ở bộ lọc "Cần đánh giá lại" tới khi tải lại trang.

vi.mock('../assessments/AssessmentTab', () => ({
  AssessmentTab: ({ onChanged }) => (
    <button type="button" onClick={onChanged}>
      Giả lập đã đính chính
    </button>
  ),
}))

beforeEach(() => {
  const api = mockRoutes()
  api.on(
    'GET',
    '/residents/7',
    json(
      200,
      {
        id: 7,
        profileCode: 'NCT-2026-000007',
        personalInfo: { fullName: 'Lê Văn Dựng Tay', dateOfBirth: '1948-03-14', gender: 'MALE' },
        status: 'IN_RESIDENCE',
        allowedCommands: [],
        hiddenFields: [],
      },
      { ETag: '"1"' },
    ),
  )
  api.on('GET', '/residents/7/linked-profiles', json(200, []))
})

afterEach(() => {
  vi.unstubAllGlobals()
})

it('BUG-TC03-D-01: thay đổi trong phần Đánh giá báo onChanged cho màn danh sách', async () => {
  const onChanged = vi.fn()
  render(<ResidentDetail residentId={7} onChanged={onChanged} />)

  fireEvent.click(await screen.findByRole('button', { name: 'Đánh giá' }))
  fireEvent.click(await screen.findByRole('button', { name: 'Giả lập đã đính chính' }))

  await waitFor(() => expect(onChanged).toHaveBeenCalled())
})

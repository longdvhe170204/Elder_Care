import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { Sheet, SheetContent } from '@/components/ui/sheet'
import { CreateResident } from './CreateResident'

// Tiêu đề, mô tả của khung nằm trong Sheet nên form phải render trong Sheet đang mở
function renderForm(props = {}) {
  return render(
    <Sheet open>
      <SheetContent>
        <CreateResident onCreated={() => {}} onClose={() => {}} {...props} />
      </SheetContent>
    </Sheet>,
  )
}

// 001 T022: hỏi xác nhận khi trùng họ tên và ngày sinh mà không có CCCD (US1 #4; FR-005).

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('CreateResident: xác nhận không trùng', () => {
  it('hiện danh sách hồ sơ trùng, xác nhận thì gửi lại với khóa mới và confirmNotDuplicate', async () => {
    let calls = 0
    api.on('POST', '/residents', () => {
      calls += 1
      return calls === 1
        ? problem(409, 'POSSIBLE_DUPLICATE_PROFILE', {
            candidates: [
              {
                id: 3,
                profileCode: 'NCT-2026-000003',
                fullName: 'Trần Thị Mai',
                dateOfBirth: '1948-03-14',
                status: 'IN_RESIDENCE',
              },
            ],
          })
        : json(201, {
            profile: {
              id: 9,
              profileCode: 'NCT-2026-000009',
              personalInfo: { fullName: 'Trần Thị Mai' },
              previousProfileId: null,
            },
            warnings: [],
          })
    })
    renderForm()
    fireEvent.change(screen.getByLabelText('Họ tên'), { target: { value: 'Trần Thị Mai' } })
    fireEvent.change(screen.getByLabelText('Ngày sinh'), { target: { value: '1948-03-14' } })
    fireEvent.change(screen.getByLabelText('Giới tính'), { target: { value: 'FEMALE' } })

    fireEvent.click(screen.getByRole('button', { name: 'Tạo hồ sơ' }))

    await waitFor(() => expect(screen.getByText(/NCT-2026-000003 · Trần Thị Mai · Đang lưu trú/)).toBeTruthy())
    fireEvent.click(screen.getByRole('button', { name: /Xác nhận không trùng/ }))

    await waitFor(() => expect(screen.getByText('NCT-2026-000009')).toBeTruthy())
    const [first, second] = api.fetchMock.mock.calls.map(([, init]) => init)
    expect(JSON.parse(first.body).confirmNotDuplicate).toBeUndefined()
    expect(JSON.parse(second.body).confirmNotDuplicate).toBe(true)
    expect(second.headers['Idempotency-Key']).not.toBe(first.headers['Idempotency-Key'])
  })
})

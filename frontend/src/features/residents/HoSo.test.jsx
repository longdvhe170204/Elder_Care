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

// 001 T022: tạo hồ sơ người cao tuổi (US1 #1, #2, #4; FR-001 → FR-005; hợp đồng POST /residents).

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

function created(overrides = {}) {
  return {
    profile: {
      id: 7,
      profileCode: 'NCT-2026-000007',
      personalInfo: { fullName: 'Nguyễn Văn An' },
      previousProfileId: null,
    },
    warnings: [],
    ...overrides,
  }
}

function fill({ name = 'Nguyễn Văn An', dob = '1948-03-14', gender = 'MALE' } = {}) {
  fireEvent.change(screen.getByLabelText('Họ tên'), { target: { value: name } })
  fireEvent.change(screen.getByLabelText('Ngày sinh'), { target: { value: dob } })
  fireEvent.change(screen.getByLabelText('Giới tính'), { target: { value: gender } })
}

describe('CreateResident', () => {
  it('chặn gửi khi thiếu họ tên, ngày sinh, giới tính', () => {
    renderForm()

    fireEvent.click(screen.getByRole('button', { name: 'Tạo hồ sơ' }))

    expect(screen.getByText('Nhập họ tên')).toBeTruthy()
    expect(screen.getByText('Chọn ngày sinh')).toBeTruthy()
    expect(screen.getByText('Chọn giới tính')).toBeTruthy()
    expect(api.fetchMock).not.toHaveBeenCalled()
  })

  it('gửi thông tin kèm Idempotency-Key và hiện mã hồ sơ đã cấp', async () => {
    api.on('POST', '/residents', json(201, created()))
    const onCreated = vi.fn()
    renderForm({ onCreated })
    fill()
    fireEvent.change(screen.getByLabelText('Loại giấy tờ'), { target: { value: 'CITIZEN_ID' } })
    fireEvent.change(screen.getByLabelText('Số giấy tờ'), { target: { value: '012345678901' } })

    fireEvent.click(screen.getByRole('button', { name: 'Tạo hồ sơ' }))

    await waitFor(() => expect(screen.getByText('NCT-2026-000007')).toBeTruthy())
    const [, init] = api.fetchMock.mock.calls[0]
    expect(init.headers['Idempotency-Key']).toMatch(/^[0-9a-f-]{36}$/)
    expect(JSON.parse(init.body)).toMatchObject({
      fullName: 'Nguyễn Văn An',
      dateOfBirth: '1948-03-14',
      gender: 'MALE',
      idDocumentType: 'CITIZEN_ID',
      idDocumentNumber: '012345678901',
    })
    expect(onCreated).toHaveBeenCalled()
  })

  it('CCCD đang dùng: báo ngay ở ô số giấy tờ kèm mã hồ sơ đang mang CCCD', async () => {
    api.on('POST', '/residents', problem(409, 'CITIZEN_ID_IN_USE', { profileCode: 'NCT-2026-000001' }))
    renderForm()
    fill()
    fireEvent.change(screen.getByLabelText('Loại giấy tờ'), { target: { value: 'CITIZEN_ID' } })
    fireEvent.change(screen.getByLabelText('Số giấy tờ'), { target: { value: '012345678901' } })

    fireEvent.click(screen.getByRole('button', { name: 'Tạo hồ sơ' }))

    await waitFor(() => expect(screen.getByText('CCCD đang dùng ở hồ sơ NCT-2026-000001')).toBeTruthy())
  })

  it('cảnh báo CCCD trùng hồ sơ đã qua đời hiện sau khi tạo', async () => {
    api.on(
      'POST',
      '/residents',
      json(
        201,
        created({
          warnings: [
            {
              code: 'CITIZEN_ID_MATCHES_DECEASED_PROFILE',
              description: 'CCCD trùng hồ sơ đã qua đời, cần kiểm tra lại giấy tờ',
            },
          ],
        }),
      ),
    )
    renderForm()
    fill()

    fireEvent.click(screen.getByRole('button', { name: 'Tạo hồ sơ' }))

    await waitFor(() => expect(screen.getByText(/đã qua đời, cần kiểm tra lại giấy tờ/)).toBeTruthy())
  })
})

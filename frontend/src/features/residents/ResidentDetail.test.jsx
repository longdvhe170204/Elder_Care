import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { Sheet, SheetContent } from '@/components/ui/sheet'
import { ResidentDetail } from './ResidentDetail'

// 001 T022: chi tiết hồ sơ và cập nhật thông tin cá nhân (US1 #6 → #9; FR-006, FR-007).

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

function profile(overrides = {}) {
  return {
    id: 7,
    profileCode: 'NCT-2026-000007',
    personalInfo: {
      fullName: 'Nguyễn Văn An',
      dateOfBirth: '1948-03-14',
      gender: 'MALE',
      idDocumentType: 'CITIZEN_ID',
      idDocumentNumber: '001048000123',
      contact: { phoneNumber: '0901234567' },
    },
    status: 'ADMISSION_IN_PROGRESS',
    allowedCommands: ['UPDATE_PERSONAL_INFO'],
    hiddenFields: [],
    ...overrides,
  }
}

function open() {
  return render(
    <Sheet open>
      <SheetContent>
        <ResidentDetail residentId={7} onChanged={() => {}} onClose={() => {}} />
      </SheetContent>
    </Sheet>,
  )
}

function routes({ view = profile(), linked = [] } = {}) {
  api.on('GET', '/residents/7', json(200, view, { ETag: '"1"' }))
  api.on('GET', '/residents/7/linked-profiles', json(200, linked))
}

describe('ResidentDetail', () => {
  it('hiện thông tin hồ sơ và chuỗi hồ sơ trước', async () => {
    routes({ linked: [{ id: 3, profileCode: 'NCT-2025-000003', status: 'RESIDENCY_ENDED', viewable: true }] })

    open()

    expect(await screen.findByText('Nguyễn Văn An')).toBeTruthy()
    expect(screen.getByText('1948-03-14')).toBeTruthy()
    expect(await screen.findByText(/NCT-2025-000003/)).toBeTruthy()
  })

  it('không có nút sửa khi allowedCommands không có UPDATE_PERSONAL_INFO, và báo trường bị ẩn theo quyền', async () => {
    routes({ view: profile({ allowedCommands: [], hiddenFields: ['activeHealthItems'] }) })

    open()

    await screen.findByText('Nguyễn Văn An')
    expect(screen.queryByRole('button', { name: 'Cập nhật thông tin' })).toBeNull()
    expect(screen.getByText('Một số thông tin không hiển thị theo quyền của bạn.')).toBeTruthy()
  })

  it('bắt buộc lý do rồi gửi cập nhật kèm If-Match theo ETag', async () => {
    routes()
    api.on(
      'POST',
      '/residents/7/update-personal-info',
      json(200, profile({ personalInfo: { ...profile().personalInfo, fullName: 'Nguyễn Văn Bình' } }), { ETag: '"2"' }),
    )

    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Cập nhật thông tin' }))
    fireEvent.change(screen.getByLabelText('Họ tên'), { target: { value: 'Nguyễn Văn Bình' } })

    fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }))
    expect(screen.getByText('Nhập lý do cập nhật')).toBeTruthy()
    expect(api.calls('POST', '/residents/7/update-personal-info')).toHaveLength(0)

    fireEvent.change(screen.getByLabelText('Lý do cập nhật'), { target: { value: 'Sửa lỗi chính tả' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }))

    await waitFor(() => expect(api.calls('POST', '/residents/7/update-personal-info')).toHaveLength(1))
    const [, init] = api.calls('POST', '/residents/7/update-personal-info')[0]
    expect(init.headers['If-Match']).toBe('"1"')
    expect(init.headers['Idempotency-Key']).toBeTruthy()
    expect(JSON.parse(init.body)).toMatchObject({ fullName: 'Nguyễn Văn Bình', reason: 'Sửa lỗi chính tả' })
    expect(await screen.findByText('Nguyễn Văn Bình')).toBeTruthy()
  })

  it('412 thì báo dữ liệu đã đổi và cho xem giá trị mới', async () => {
    routes()
    api.on('POST', '/residents/7/update-personal-info', problem(412, 'PRECONDITION_FAILED'))

    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Cập nhật thông tin' }))
    fireEvent.change(screen.getByLabelText('Lý do cập nhật'), { target: { value: 'Sửa' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }))

    expect(await screen.findByText('Dữ liệu đã thay đổi')).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Xem giá trị mới' })).toBeTruthy()
  })

  it('001 T033: có tab Sức khỏe khi được xem phần sức khỏe, chuyển tab thì nạp hồ sơ ban đầu và mục sức khỏe', async () => {
    routes()
    api.on('GET', '/residents/7/initial-health-record', json(200, { id: 5, confirmed: false, corrected: false }, { ETag: '"1"' }))
    api.on('GET', '/residents/7/health-items', json(200, []))

    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Sức khỏe' }))

    expect(await screen.findByText('Hồ sơ sức khỏe ban đầu')).toBeTruthy()
    expect(await screen.findByText('Chưa ghi nhận dị ứng, bệnh nền hay tiền sử bệnh')).toBeTruthy()
    expect(screen.queryByText('Ngày sinh')).toBeNull()

    fireEvent.click(screen.getByRole('button', { name: 'Thông tin' }))
    expect(await screen.findByText('Ngày sinh')).toBeTruthy()
  })

  it('001 T033: không có tab Sức khỏe khi backend bỏ phần sức khỏe theo quyền (hiddenFields)', async () => {
    routes({ view: profile({ allowedCommands: [], hiddenFields: ['activeHealthItems'] }) })

    open()

    await screen.findByText('Nguyễn Văn An')
    expect(screen.queryByRole('button', { name: 'Sức khỏe' })).toBeNull()
    expect(api.calls('GET', '/residents/7/health-items')).toHaveLength(0)
  })

  it('CCCD trùng hiện lỗi ở ô số giấy tờ', async () => {
    routes()
    api.on(
      'POST',
      '/residents/7/update-personal-info',
      problem(409, 'CITIZEN_ID_IN_USE', { profileCode: 'NCT-2026-000002' }),
    )

    open()
    fireEvent.click(await screen.findByRole('button', { name: 'Cập nhật thông tin' }))
    fireEvent.change(screen.getByLabelText('Lý do cập nhật'), { target: { value: 'Đổi CCCD' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thay đổi' }))

    expect(await screen.findByText('CCCD đang dùng ở hồ sơ NCT-2026-000002')).toBeTruthy()
  })
})

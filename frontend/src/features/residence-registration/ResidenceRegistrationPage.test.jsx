import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { ResidenceRegistrationPage } from './ResidenceRegistrationPage'

// Màn Khai báo cư trú: việc cần làm (018 T008; bố cục K1, K2, K6, K7).

const PERMS = [
  { function: 'RESIDENCE_REGISTRATION', operation: 'VIEW' },
  { function: 'RESIDENCE_REGISTRATION', operation: 'CREATE' },
  { function: 'RESIDENCE_REGISTRATION', operation: 'UPDATE' },
]

const TASKS = [
  { residentId: 1, fullName: 'Lê Văn Cần', taskType: 'TO_CREATE', dueAt: '2026-11-20T16:59:59Z', allowedCommands: [] },
  {
    residentId: 2,
    fullName: 'Trần Thị Quá',
    taskType: 'OVERDUE',
    dueAt: '2026-10-01T16:59:59Z',
    registrationId: 7,
    allowedCommands: ['record-submitted'],
  },
  { residentId: 3, fullName: 'Phạm Văn Xóa', taskType: 'TO_DEREGISTER', registrationId: 8, allowedCommands: ['record-deregistered'] },
]

function setup(tasks = TASKS, perms = PERMS) {
  const api = mockRoutes()
  api.on('GET', '/me/permissions', json(200, perms))
  api.on('GET', '/residence-registrations/to-do', json(200, tasks))
  api.on('GET', '/residents/2/residence-registrations', json(200, []))
  render(
    <PermissionsProvider>
      <ResidenceRegistrationPage />
    </PermissionsProvider>,
  )
  return api
}

const rowNames = () =>
  screen
    .getAllByRole('row')
    .slice(1)
    .map((row) => within(row).getAllByRole('cell')[0].textContent)

describe('ResidenceRegistrationPage (018)', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('K2_bangViecCanLam_quaHanDungTruoc_khaiBaoXoaKhongCoHan', async () => {
    setup()
    expect(await screen.findByText('Trần Thị Quá')).toBeInTheDocument()
    expect(rowNames()).toEqual(['Trần Thị Quá', 'Lê Văn Cần', 'Phạm Văn Xóa'])
    const deregisterRow = screen.getByText('Phạm Văn Xóa').closest('tr')
    expect(within(deregisterRow).getByText('Cần khai báo xóa')).toBeInTheDocument()
    expect(within(deregisterRow).getByText('—')).toBeInTheDocument()
    const overdueRow = screen.getByText('Trần Thị Quá').closest('tr')
    expect(within(overdueRow).getByText('Quá hạn')).toBeInTheDocument()
    expect(within(overdueRow).getByText(/quá hạn \d+ ngày/)).toBeInTheDocument()
  })

  it('K2_thanhLoc_demTheoLoaiViec_vaLocBang', async () => {
    setup()
    await screen.findByText('Trần Thị Quá')
    expect(screen.getByRole('button', { name: 'Quá hạn 1' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Cần khai báo xóa 1' }))
    await waitFor(() => expect(rowNames()).toEqual(['Phạm Văn Xóa']))
    fireEvent.click(screen.getByRole('button', { name: 'Tất cả 3' }))
    await waitFor(() => expect(rowNames()).toHaveLength(3))
  })

  it('K2_bamMo_moKhungChiTietCuaNguoiCaoTuoi', async () => {
    setup()
    fireEvent.click(await screen.findByRole('button', { name: 'Mở khai báo của Trần Thị Quá' }))
    expect(await screen.findByRole('heading', { name: 'Trần Thị Quá' })).toBeInTheDocument()
  })

  it('7_3_khongCoViec_hienTrangThaiTrong', async () => {
    setup([])
    expect(await screen.findByText('Không có người cần khai báo')).toBeInTheDocument()
  })

  it('7_3_loiTai_hienAlertVaNutThuLai', async () => {
    const api = mockRoutes()
    api.on('GET', '/me/permissions', json(200, PERMS))
    api.on('GET', '/residence-registrations/to-do', problem(500, 'INTERNAL_ERROR'))
    render(
      <PermissionsProvider>
        <ResidenceRegistrationPage />
      </PermissionsProvider>,
    )
    expect(await screen.findByRole('button', { name: 'Thử lại' })).toBeInTheDocument()
  })

  it('K2_oTimNguoiCaoTuoi_moKhaiBaoCuaBatKyAi', async () => {
    const api = setup([])
    api.on(
      'GET',
      '/residents',
      json(200, { items: [{ id: 9, profileCode: 'HS-9', personalInfo: { fullName: 'Vũ Thị Tìm' } }] }),
    )
    api.on('GET', '/residents/9/residence-registrations', json(200, []))
    await screen.findByText('Không có người cần khai báo')
    fireEvent.change(screen.getByLabelText('Tìm người cao tuổi'), { target: { value: 'Tìm' } })
    fireEvent.click(screen.getByRole('button', { name: 'Tìm' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Vũ Thị Tìm, HS-9' }))
    expect(await screen.findByRole('heading', { name: 'Vũ Thị Tìm' })).toBeInTheDocument()
  })
})

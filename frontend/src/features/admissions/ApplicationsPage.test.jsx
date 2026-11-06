import { beforeEach, describe, expect, it } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { ApplicationsPage } from './ApplicationsPage'

// 004 T019 (US1; FR-001, FR-003, FR-004, FR-004a, FR-005): danh sách lượt đăng ký lọc theo trạng thái kèm số đếm; khung
// chi tiết có tiến trình, danh sách điều kiện tiếp nhận tách khỏi ý kiến của người cao tuổi về việc vào ở (ý kiến không là
// điều kiện, Q-356), ghi ý kiến mới và lập đăng ký cho người Đang tiếp nhận.

let api

const OPEN = {
  id: 7,
  residentId: 5,
  applicationDate: '2026-11-01',
  desiredResidenceType: 'LONG_TERM_RESIDENTIAL',
  expectedCareLevel: 'REGULAR',
  status: 'IN_PROGRESS',
  contactPerson: 'Trần Văn Minh',
  admissionOpinion: [],
}
const WAITING = { ...OPEN, id: 8, residentId: 6, status: 'WAITING' }

const CONDITIONS = {
  conditions: [
    { code: 'ADM_B_CONTRACT', name: 'Hợp đồng Hiệu lực', status: 'NOT_MET', basis: 'FR-004' },
    { code: 'ADM_C_DEPOSIT', name: 'Đặt cọc', status: 'MET', basis: 'FR-030' },
  ],
  warnings: [{ code: 'ADMISSION_OPINION_MISSING', description: 'Chưa ghi ý kiến của người cao tuổi về việc vào ở' }],
  admissionOpinion: null,
}

const PROGRESS = {
  steps: [
    { name: 'APPLICATION', done: true },
    { name: 'ASSESSMENT', done: false },
  ],
}

function permissions(list) {
  api.on(
    'GET',
    '/me/permissions',
    json(
      200,
      list.map(([fn, operation]) => ({ function: fn, operation })),
    ),
  )
}

function renderPage() {
  return render(
    <PermissionsProvider>
      <ApplicationsPage role="OFFICE_STAFF" />
    </PermissionsProvider>,
  )
}

beforeEach(() => {
  api = mockRoutes()
  permissions([
    ['ADMISSION_CONTRACT_DEPOSIT', 'VIEW'],
    ['ADMISSION_CONTRACT_DEPOSIT', 'CREATE'],
    ['ADMISSION_CONTRACT_DEPOSIT', 'UPDATE'],
  ])
  api.on('GET', '/admission-applications', json(200, [OPEN, WAITING]))
  api.on('GET', '/residents/5', json(200, { id: 5, personalInfo: { fullName: 'Nguyễn Thị Lan' } }))
  api.on('GET', '/residents/6', json(200, { id: 6, personalInfo: { fullName: 'Lê Văn Hải' } }))
  api.on('GET', '/admission-applications/7', json(200, OPEN, { ETag: '"3"' }))
  api.on('GET', '/admission-applications/7/admission-conditions', json(200, CONDITIONS))
  api.on('GET', '/admission-applications/7/progress', json(200, PROGRESS))
  api.on(
    'GET',
    '/residents',
    json(200, { items: [{ id: 9, profileCode: 'NCT-0009', personalInfo: { fullName: 'Phạm Thị Mai' } }] }),
  )
})

describe('ApplicationsPage', () => {
  it('FR_006_bangLuotDangKy_locTheoTrangThaiKemSoDem', async () => {
    renderPage()

    expect(await screen.findByRole('button', { name: 'Nguyễn Thị Lan' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Lê Văn Hải' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Đang tiếp nhận 1' })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Chờ giường 1' }))
    expect(await screen.findByRole('button', { name: 'Lê Văn Hải' })).toBeInTheDocument()
  })

  it('FR_004a_khungChiTiet_dieuKienTachKhoiYKienVaoO_vaCoCanhBao', async () => {
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Nguyễn Thị Lan' }))

    expect(await screen.findByText('Hợp đồng Hiệu lực')).toBeInTheDocument()
    expect(screen.getByText(/Chưa đạt/)).toBeInTheDocument()
    expect(screen.getByText('Chưa ghi ý kiến của người cao tuổi về việc vào ở')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Ý kiến về việc vào ở' })).toBeInTheDocument()
    expect(screen.getByText('Đăng ký')).toBeInTheDocument()
  })

  it('FR_004a_ghiYKien_guiNguonVaYKien_roiTaiLai', async () => {
    api.on('POST', '/admission-applications/7/admission-opinion', json(201, { id: 1, opinion: 'AGREE' }))
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Nguyễn Thị Lan' }))
    await screen.findByText('Hợp đồng Hiệu lực')

    fireEvent.click(screen.getByRole('button', { name: 'Ghi ý kiến mới' }))
    fireEvent.click(screen.getByRole('button', { name: 'Người cao tuổi' }))
    fireEvent.click(screen.getByRole('button', { name: 'Đồng ý' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu ý kiến' }))

    await waitFor(() => expect(api.calls('POST', '/admission-applications/7/admission-opinion')).toHaveLength(1))
    const body = JSON.parse(api.calls('POST', '/admission-applications/7/admission-opinion')[0][1].body)
    expect(body).toEqual({ opinion: 'AGREE', source: { type: 'RESIDENT' } })
  })

  it('FR_001_lapDangKy_thieuTruong_baoTaiO_vaThanhCongThiTaiLaiDanhSach', async () => {
    api.on('POST', '/admission-applications', json(201, OPEN))
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Lập đăng ký' }))

    fireEvent.click(screen.getByRole('button', { name: 'Lưu đăng ký' }))
    expect(await screen.findByText('Chọn người cao tuổi')).toBeInTheDocument()
    expect(api.calls('POST', '/admission-applications')).toHaveLength(0)

    const dialog = screen.getByRole('dialog')
    await within(dialog).findByRole('option', { name: 'Phạm Thị Mai (NCT-0009)' })
    fireEvent.change(within(dialog).getByLabelText('Người cao tuổi'), { target: { value: '9' } })
    fireEvent.change(within(dialog).getByLabelText('Ngày đăng ký'), { target: { value: '2026-11-06' } })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Nội trú dài hạn' }))
    fireEvent.click(within(dialog).getByRole('button', { name: 'Thường xuyên' }))
    fireEvent.click(within(dialog).getByRole('button', { name: 'Lưu đăng ký' }))

    await waitFor(() => expect(api.calls('POST', '/admission-applications')).toHaveLength(1))
    const body = JSON.parse(api.calls('POST', '/admission-applications')[0][1].body)
    expect(body).toMatchObject({
      residentId: 9,
      applicationDate: '2026-11-06',
      desiredResidenceType: 'LONG_TERM_RESIDENTIAL',
      expectedCareLevel: 'REGULAR',
    })
  })

  it('FR_001_409DaCoLuotDangKy_hienLoiTrongKhung', async () => {
    api.on('POST', '/admission-applications', problem(409, 'APPLICATION_ALREADY_EXISTS'))
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Lập đăng ký' }))
    const dialog = screen.getByRole('dialog')
    await within(dialog).findByRole('option', { name: 'Phạm Thị Mai (NCT-0009)' })
    fireEvent.change(within(dialog).getByLabelText('Người cao tuổi'), { target: { value: '9' } })
    fireEvent.change(within(dialog).getByLabelText('Ngày đăng ký'), { target: { value: '2026-11-06' } })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Bán trú' }))
    fireEvent.click(within(dialog).getByRole('button', { name: 'Cơ bản' }))
    fireEvent.click(within(dialog).getByRole('button', { name: 'Lưu đăng ký' }))

    expect(await within(dialog).findByRole('alert')).toBeInTheDocument()
  })

  it('FR_006_khongCoQuyenTao_anNutLapDangKy', async () => {
    api.on('GET', '/me/permissions', json(200, [{ function: 'ADMISSION_CONTRACT_DEPOSIT', operation: 'VIEW' }]))
    renderPage()
    await screen.findByRole('button', { name: 'Nguyễn Thị Lan' })
    expect(screen.queryByRole('button', { name: 'Lập đăng ký' })).not.toBeInTheDocument()
  })

  it('FR_006_403_hienKhongCoQuyenXem', async () => {
    api.on('GET', '/admission-applications', problem(403, 'PERMISSION_DENIED'))
    renderPage()
    expect(await screen.findByText(/Bạn không có quyền xem/)).toBeInTheDocument()
  })
})

import { beforeEach, describe, expect, it } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { ChangeRequestsPage } from './ChangeRequestsPage'

// 004 T047 (US5; FR-037 → FR-047): lập yêu cầu thay đổi lưu trú ở Nháp (loại thay đổi, giá trị mới, ngày hiệu lực mong muốn,
// lý do), xem giá trị trước và ảnh hưởng chi phí, sửa Nháp kèm If-Match. Gửi duyệt, duyệt, từ chối dùng màn Yêu cầu phê duyệt
// (api-000). Hợp đồng chưa có danh sách yêu cầu nên mở theo mã yêu cầu.

let api

const REQUEST = {
  id: 12,
  contractId: 31,
  changeTypes: ['CARE_LEVEL'],
  newValue: { careLevel: 'SPECIAL' },
  previousValue: { careLevel: 'REGULAR' },
  requestedEffectiveDate: '2026-11-20',
  reason: 'Sau đợt đánh giá mới',
  costImpact: [{ item: 'Chăm sóc', previousUnitPrice: 5000000, newUnitPrice: 8000000 }],
  minorChange: false,
  approvalRequestId: 90,
  status: 'DRAFT',
  requestedBy: 'Hoàng Thu Hà',
}

function renderPage() {
  return render(
    <PermissionsProvider>
      <ChangeRequestsPage />
    </PermissionsProvider>,
  )
}

async function openRequest() {
  fireEvent.change(await screen.findByLabelText('Mã yêu cầu'), { target: { value: '12' } })
  fireEvent.click(screen.getByRole('button', { name: 'Mở yêu cầu' }))
  await screen.findByText('Sau đợt đánh giá mới')
}

beforeEach(() => {
  api = mockRoutes()
  api.on(
    'GET',
    '/me/permissions',
    json(
      200,
      ['VIEW', 'CREATE', 'UPDATE'].map((operation) => ({ function: 'RESIDENCY_CHANGE_REQUEST', operation })),
    ),
  )
  api.on('GET', '/residency-change-requests/12', json(200, REQUEST, { ETag: '"2"' }))
  api.on(
    'GET',
    '/contracts',
    json(200, [
      {
        id: 31,
        residentId: 5,
        contractNumber: 'HD-031',
        status: 'ACTIVE',
        residenceType: 'LONG_TERM_RESIDENTIAL',
        careLevel: 'REGULAR',
      },
    ]),
  )
  api.on('GET', '/residents/5', json(200, { id: 5, personalInfo: { fullName: 'Nguyễn Thị Lan' } }))
  api.on('GET', '/services', json(200, []))
})

describe('ChangeRequestsPage', () => {
  it('FR_038_moTheoMa_hienGiaTriTruocGiaTriMoiVaChiPhi', async () => {
    renderPage()
    await openRequest()

    expect(screen.getByText('Mức chăm sóc')).toBeInTheDocument()
    expect(screen.getByText(/Thường xuyên/)).toBeInTheDocument()
    expect(screen.getByText(/Đặc biệt/)).toBeInTheDocument()
    expect(screen.getByText(/5\.000\.000/)).toBeInTheDocument()
    expect(screen.getByText(/8\.000\.000/)).toBeInTheDocument()
    expect(screen.getByText(/Gửi duyệt ở màn Yêu cầu phê duyệt/)).toBeInTheDocument()
  })

  it('FR_039_suaNhap_guiIfMatchTheoETag', async () => {
    api.on('PUT', '/residency-change-requests/12', json(200, {}))
    renderPage()
    await openRequest()
    fireEvent.click(screen.getByRole('button', { name: 'Sửa yêu cầu nháp' }))

    const dialog = await screen.findByRole('dialog')
    fireEvent.change(within(dialog).getByLabelText('Lý do'), { target: { value: 'Bổ sung căn cứ' } })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Lưu yêu cầu' }))

    await waitFor(() => expect(api.calls('PUT', '/residency-change-requests/12')).toHaveLength(1))
    const call = api.calls('PUT', '/residency-change-requests/12')[0][1]
    expect(call.headers['If-Match']).toBe('"2"')
    expect(JSON.parse(call.body)).toMatchObject({
      contractId: 31,
      changeTypes: ['CARE_LEVEL'],
      newValue: { careLevel: 'SPECIAL' },
      requestedEffectiveDate: '2026-11-20',
      reason: 'Bổ sung căn cứ',
    })
  })

  it('FR_040_lapYeuCau_thieuTruong_baoTaiO', async () => {
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Lập yêu cầu thay đổi' }))
    const dialog = screen.getByRole('dialog')
    fireEvent.click(within(dialog).getByRole('button', { name: 'Lưu yêu cầu' }))
    expect(await within(dialog).findByText('Chọn hợp đồng')).toBeInTheDocument()
    expect(within(dialog).getByText('Chọn ít nhất một loại thay đổi')).toBeInTheDocument()
    expect(within(dialog).getByText('Nhập ngày hiệu lực mong muốn')).toBeInTheDocument()
    expect(api.calls('POST', '/residency-change-requests')).toHaveLength(0)
  })

  it('FR_037_lapYeuCauDoiMuc_guiGiaTriMoi', async () => {
    api.on('POST', '/residency-change-requests', json(201, REQUEST))
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Lập yêu cầu thay đổi' }))
    const dialog = screen.getByRole('dialog')
    await within(dialog).findByRole('option', { name: /HD-031/ })
    fireEvent.change(within(dialog).getByLabelText('Hợp đồng'), { target: { value: '31' } })
    fireEvent.click(within(dialog).getByRole('checkbox', { name: 'Mức chăm sóc' }))
    fireEvent.click(within(dialog).getByRole('button', { name: 'Đặc biệt' }))
    fireEvent.change(within(dialog).getByLabelText('Ngày hiệu lực mong muốn'), { target: { value: '2026-11-20' } })
    fireEvent.change(within(dialog).getByLabelText('Lý do'), { target: { value: 'Sau đợt đánh giá mới' } })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Lưu yêu cầu' }))

    await waitFor(() => expect(api.calls('POST', '/residency-change-requests')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/residency-change-requests')[0][1].body)).toEqual({
      contractId: 31,
      changeTypes: ['CARE_LEVEL'],
      newValue: { careLevel: 'SPECIAL' },
      requestedEffectiveDate: '2026-11-20',
      reason: 'Sau đợt đánh giá mới',
    })
  })

  it('FR_040_422_ngayHieuLucSomHonNgayBatDau_hienLoi', async () => {
    api.on(
      'POST',
      '/residency-change-requests',
      problem(422, 'INVALID_VALUE', { detail: 'Ngày hiệu lực sớm hơn ngày bắt đầu hợp đồng' }),
    )
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'Lập yêu cầu thay đổi' }))
    const dialog = screen.getByRole('dialog')
    await within(dialog).findByRole('option', { name: /HD-031/ })
    fireEvent.change(within(dialog).getByLabelText('Hợp đồng'), { target: { value: '31' } })
    fireEvent.click(within(dialog).getByRole('checkbox', { name: 'Mức chăm sóc' }))
    fireEvent.click(within(dialog).getByRole('button', { name: 'Cơ bản' }))
    fireEvent.change(within(dialog).getByLabelText('Ngày hiệu lực mong muốn'), { target: { value: '2020-01-01' } })
    fireEvent.change(within(dialog).getByLabelText('Lý do'), { target: { value: 'Thử' } })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Lưu yêu cầu' }))
    expect(await within(dialog).findByText('Ngày hiệu lực sớm hơn ngày bắt đầu hợp đồng')).toBeInTheDocument()
  })

  it('FR_038_yeuCauDaGuiDuyet_khongCoNutSua', async () => {
    api.on(
      'GET',
      '/residency-change-requests/12',
      json(200, { ...REQUEST, status: 'PENDING_APPROVAL' }, { ETag: '"3"' }),
    )
    renderPage()
    await openRequest()
    expect(screen.queryByRole('button', { name: 'Sửa yêu cầu nháp' })).not.toBeInTheDocument()
  })
})

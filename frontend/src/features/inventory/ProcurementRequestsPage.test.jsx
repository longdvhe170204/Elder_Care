import { beforeEach, describe, expect, it } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { ProcurementRequestsPage } from './ProcurementRequestsPage'

// 019 T008 màn web (US7; FR-008 → FR-008d; bố cục duyệt 2026-11-09 mục (8)): bảng yêu cầu mua sắm lọc theo trạng thái
// kèm số đếm; khung chi tiết có nút theo allowedCommands của backend (api-019 bổ sung 2026-11-10); lập, sửa Nháp bằng Sheet.

let api

const INGREDIENT = {
  id: 41,
  departmentId: 3,
  type: 'INGREDIENT',
  reason: 'Hết gạo cho tuần sau',
  neededOn: '2026-11-12',
  lines: [
    { id: 41000001, name: 'Gạo', quantity: 50, estimatedUnitPrice: 30000 },
    { id: 41000002, name: 'Thịt', quantity: 20, estimatedUnitPrice: 25000 },
  ],
  estimatedTotal: 2000000,
  status: 'PENDING_APPROVAL',
  linkedSourceTotal: 0,
  received: null,
  version: 2,
  allowedCommands: ['approve', 'reject'],
}

const APPROVED = {
  ...INGREDIENT,
  id: 42,
  status: 'APPROVED',
  approvedAmount: 1800000,
  allowedCommands: ['record-purchased', 'cancel'],
}
const TO_RECONCILE = {
  ...INGREDIENT,
  id: 43,
  status: 'PENDING_RECONCILIATION',
  approvedAmount: 1800000,
  actualAmount: 1850000,
  allowedCommands: ['confirm-match', 'return-for-reconciliation', 'record-discrepancy'],
}

const MEDICATION = {
  id: 44,
  departmentId: 2,
  type: 'MEDICATION',
  priority: 'URGENT',
  neededBy: '2026-11-12T01:00:00Z',
  reason: 'Amlodipin sắp hết',
  lines: [
    {
      id: 44000001,
      name: 'Amlodipin',
      quantity: 30,
      estimatedUnitPrice: 2000,
      residentId: 5,
      prescriptionId: 91,
      activeIngredient: 'Amlodipin',
      strength: '5 mg',
      dosageForm: 'Viên nén',
      unit: 'viên',
    },
  ],
  estimatedTotal: 60000,
  approvedAmount: 60000,
  actualAmount: 60000,
  status: 'RECONCILED',
  linkedSourceTotal: 0,
  received: true,
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

const LEAD = [
  ['PURCHASE_REQUEST', 'VIEW'],
  ['PURCHASE_REQUEST', 'CREATE'],
  ['PURCHASE_REQUEST', 'UPDATE'],
]

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/procurement-requests', json(200, [INGREDIENT, APPROVED, TO_RECONCILE, MEDICATION]))
  api.on(
    'GET',
    '/departments',
    json(200, [
      { id: 2, name: 'Y tế', active: true },
      { id: 3, name: 'Bếp', active: true },
    ]),
  )
  api.on(
    'GET',
    '/residents/5',
    json(200, { id: 5, profileCode: 'NCT-0005', personalInfo: { fullName: 'Nguyễn Thị Lan' }, status: 'IN_RESIDENCE' }),
  )
})

function renderPage(role) {
  render(
    <PermissionsProvider>
      <ProcurementRequestsPage role={role} />
    </PermissionsProvider>,
  )
  return screen.findByRole('table', { name: 'Yêu cầu mua sắm' })
}

function openRequest(table, name) {
  fireEvent.click(within(table).getByRole('button', { name }))
  // Dưới 1280px (jsdom không có matchMedia) khung chi tiết là Sheet
  return screen.findByRole('dialog')
}

describe('Yêu cầu mua sắm', () => {
  it('FR_008_bangYeuCau_locTheoTrangThai_tenBoPhan_daNhan', async () => {
    permissions([['PURCHASE_REQUEST', 'VIEW']])
    const table = await renderPage('NURSING_HOME_MANAGER')

    expect(screen.getByRole('button', { name: 'Tất cả 4' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Chờ duyệt 1' })).toBeInTheDocument()
    const row = await within(table).findByRole('row', { name: /Yêu cầu #41/ })
    expect(row).toHaveTextContent('Nguyên liệu')
    expect(row).toHaveTextContent('Gạo, Thịt')
    expect(await within(row).findByText('Bếp')).toBeInTheDocument()
    expect(row).toHaveTextContent('2.000.000 đ')
    expect(row).toHaveTextContent('Chờ duyệt')
    const medication = within(table).getByRole('row', { name: /Yêu cầu #44/ })
    expect(medication).toHaveTextContent('Đã nhận')
    expect(medication).toHaveTextContent('Gấp')
    expect(screen.queryByRole('button', { name: 'Lập yêu cầu' })).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Chờ đối chiếu 1' }))
    const filtered = screen.getByRole('table', { name: 'Yêu cầu mua sắm' })
    expect(within(filtered).queryByRole('row', { name: /Yêu cầu #41/ })).not.toBeInTheDocument()
    expect(within(filtered).getByRole('row', { name: /Yêu cầu #43/ })).toBeInTheDocument()
  })

  it('FR_008_quanLyVienDuyetGiam_batBuocLyDo', async () => {
    permissions([
      ['PURCHASE_REQUEST', 'VIEW'],
      ['PURCHASE_REQUEST', 'APPROVE'],
    ])
    const table = await renderPage('NURSING_HOME_MANAGER')
    api.on('POST', '/procurement-requests/41/approve', json(200, {}))
    const panel = await openRequest(table, 'Yêu cầu #41')
    expect(within(panel).getByText('Gạo')).toBeInTheDocument()
    expect(within(panel).queryByRole('button', { name: 'Gửi duyệt' })).not.toBeInTheDocument()

    fireEvent.click(within(panel).getByRole('button', { name: 'Duyệt…' }))
    fireEvent.change(within(panel).getByLabelText('Số được duyệt (đồng)'), { target: { value: '1800000' } })
    fireEvent.click(within(panel).getByRole('button', { name: 'Xác nhận duyệt' }))
    expect(await within(panel).findByText('Giảm số được duyệt thì nhập lý do')).toBeInTheDocument()
    expect(api.calls('POST', '/procurement-requests/41/approve')).toHaveLength(0)

    fireEvent.change(within(panel).getByLabelText('Lý do'), { target: { value: 'Giảm theo giá chợ' } })
    fireEvent.click(within(panel).getByRole('button', { name: 'Xác nhận duyệt' }))
    await waitFor(() => expect(api.calls('POST', '/procurement-requests/41/approve')).toHaveLength(1))
    const [, init] = api.calls('POST', '/procurement-requests/41/approve')[0]
    expect(JSON.parse(init.body)).toEqual({ approvedAmount: 1800000, reason: 'Giảm theo giá chợ' })
    // Khung chi tiết (Sheet) vẫn mở nên vùng báo ngoài khung tạm bị ẩn với trình đọc màn hình; kiểm theo chữ
    expect(await screen.findByText('Đã duyệt yêu cầu #41')).toBeInTheDocument()
  })

  it('FR_008a_ghiDaMua_batBuocChungTu_taiTepRoiGui', async () => {
    permissions(LEAD)
    const table = await renderPage('KITCHEN_STAFF')
    api.on('POST', '/files', json(201, { fileId: 701 }))
    api.on(
      'POST',
      '/procurement-requests/42/record-purchased',
      problem(409, 'APPROVED_AMOUNT_EXCEEDED', {
        detail: 'Lập yêu cầu bổ sung cho phần vượt',
        unmetConditions: [{ code: 'APPROVED_AMOUNT_EXCEEDED', description: 'Số thực tế vượt số được duyệt quá 10%' }],
      }),
    )
    const panel = await openRequest(table, 'Yêu cầu #42')
    fireEvent.click(within(panel).getByRole('button', { name: 'Ghi đã mua…' }))
    fireEvent.change(within(panel).getByLabelText('Số tiền thực tế (đồng)'), { target: { value: '2500000' } })
    fireEvent.change(within(panel).getByLabelText('Ngày mua'), { target: { value: '2026-11-05' } })
    fireEvent.change(within(panel).getByLabelText('Nhà cung cấp'), { target: { value: 'Chợ Bến Thành' } })
    fireEvent.click(within(panel).getByRole('button', { name: 'Lưu đã mua' }))
    expect(await within(panel).findByText('Chọn ảnh hóa đơn hoặc phiếu bán hàng')).toBeInTheDocument()

    const file = new File(['%PDF'], 'hoa-don.pdf', { type: 'application/pdf' })
    fireEvent.change(within(panel).getByLabelText('Chứng từ'), { target: { files: [file] } })
    fireEvent.click(within(panel).getByRole('button', { name: 'Lưu đã mua' }))
    expect(await within(panel).findByText('Số thực tế vượt số được duyệt quá 10%')).toBeInTheDocument()
    const [, init] = api.calls('POST', '/procurement-requests/42/record-purchased')[0]
    expect(JSON.parse(init.body)).toEqual({
      actualAmount: 2500000,
      purchasedOn: '2026-11-05',
      supplier: 'Chợ Bến Thành',
      receiptFileIds: [701],
    })
    // Giá trị đã gõ giữ lại sau lỗi 409 (DESIGN.md 9.2 điểm 4)
    expect(within(panel).getByLabelText('Nhà cung cấp')).toHaveValue('Chợ Bến Thành')
  })

  it('FR_008b_keToanDoiChieu_ghiLech_kemSoTien', async () => {
    permissions([
      ['PURCHASE_REQUEST', 'VIEW'],
      ['PURCHASE_REQUEST', 'UPDATE'],
    ])
    const table = await renderPage('ACCOUNTANT')
    api.on('POST', '/procurement-requests/43/record-discrepancy', json(200, {}))
    const panel = await openRequest(table, 'Yêu cầu #43')
    expect(within(panel).getByText('1.850.000 đ')).toBeInTheDocument()
    expect(within(panel).getByRole('button', { name: 'Xác nhận khớp' })).toBeInTheDocument()
    expect(within(panel).queryByRole('button', { name: 'Ghi đã mua…' })).not.toBeInTheDocument()

    fireEvent.click(within(panel).getByRole('button', { name: 'Ghi lệch…' }))
    fireEvent.click(within(panel).getByRole('button', { name: 'Lưu lệch' }))
    expect(await within(panel).findByText('Nhập số tiền lệch')).toBeInTheDocument()
    fireEvent.change(within(panel).getByLabelText('Số tiền lệch (đồng)'), { target: { value: '50000' } })
    fireEvent.change(within(panel).getByLabelText('Lý do'), { target: { value: 'Hóa đơn ghi 1.800.000' } })
    fireEvent.click(within(panel).getByRole('button', { name: 'Lưu lệch' }))
    await waitFor(() => expect(api.calls('POST', '/procurement-requests/43/record-discrepancy')).toHaveLength(1))
    const [, init] = api.calls('POST', '/procurement-requests/43/record-discrepancy')[0]
    expect(JSON.parse(init.body)).toEqual({ discrepancyAmount: 50000, reason: 'Hóa đơn ghi 1.800.000' })
  })

  it('FR_008d_dieuDuongLapYeuCauThuoc_gaDonNguonVien', async () => {
    permissions([
      ['PURCHASE_REQUEST', 'VIEW'],
      ['PURCHASE_REQUEST', 'CREATE'],
      ['PURCHASE_REQUEST', 'UPDATE'],
    ])
    api.on('GET', '/procurement-requests', json(200, []))
    api.on(
      'GET',
      '/residents',
      json(200, {
        items: [
          { id: 5, profileCode: 'NCT-0005', personalInfo: { fullName: 'Nguyễn Thị Lan' }, status: 'IN_RESIDENCE' },
        ],
        nextCursor: null,
      }),
    )
    api.on(
      'GET',
      '/residents/5/prescriptions',
      json(200, [
        { id: 91, residentId: 5, status: 'ACTIVE', medicationSource: 'FACILITY', startedAt: '2026-10-01T01:00:00Z' },
        { id: 92, residentId: 5, status: 'ACTIVE', medicationSource: 'FAMILY_SENT', startedAt: '2026-10-01T01:00:00Z' },
        {
          id: 93,
          residentId: 5,
          status: 'DISCONTINUED',
          medicationSource: 'FACILITY',
          startedAt: '2026-09-01T01:00:00Z',
        },
      ]),
    )
    api.on('POST', '/procurement-requests', () => new Response(null, { status: 201 }))
    render(
      <PermissionsProvider>
        <ProcurementRequestsPage role="NURSING_STAFF" />
      </PermissionsProvider>,
    )
    await screen.findByText('Chưa có yêu cầu nào trong nhóm này.')

    fireEvent.click(screen.getByRole('button', { name: 'Lập yêu cầu' }))
    const sheet = await screen.findByRole('dialog', { name: 'Lập yêu cầu mua sắm' })
    fireEvent.change(within(sheet).getByLabelText('Bộ phận'), { target: { value: '2' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Thuốc' }))
    fireEvent.click(within(sheet).getByRole('button', { name: 'Gấp' }))
    fireEvent.change(within(sheet).getByLabelText('Thời điểm cần thuốc'), { target: { value: '2026-11-12T08:00' } })
    fireEvent.change(within(sheet).getByLabelText('Lý do'), { target: { value: 'Amlodipin sắp hết' } })

    const line = within(sheet).getByRole('group', { name: 'Dòng 1' })
    await within(line).findByRole('option', { name: /Nguyễn Thị Lan/ })
    fireEvent.change(within(line).getByLabelText('Người cao tuổi'), { target: { value: '5' } })
    const prescription = within(line).getByLabelText('Đơn thuốc nguồn viện')
    await waitFor(() => expect(within(prescription).getAllByRole('option')).toHaveLength(2))
    expect(within(prescription).queryByRole('option', { name: /Đơn #92/ })).not.toBeInTheDocument()
    fireEvent.change(prescription, { target: { value: '91' } })
    fireEvent.change(within(line).getByLabelText('Tên thuốc'), { target: { value: 'Amlodipin' } })
    fireEvent.change(within(line).getByLabelText('Hoạt chất'), { target: { value: 'Amlodipin' } })
    fireEvent.change(within(line).getByLabelText('Hàm lượng'), { target: { value: '5 mg' } })
    fireEvent.change(within(line).getByLabelText('Dạng'), { target: { value: 'Viên nén' } })
    fireEvent.change(within(line).getByLabelText('Số lượng'), { target: { value: '30' } })
    fireEvent.change(within(line).getByLabelText('Đơn vị'), { target: { value: 'viên' } })
    fireEvent.change(within(line).getByLabelText('Đơn giá dự kiến (đồng)'), { target: { value: '2000' } })
    expect(within(sheet).getByText('60.000 đ')).toBeInTheDocument()

    fireEvent.click(within(sheet).getByRole('button', { name: 'Lưu nháp' }))
    await waitFor(() => expect(api.calls('POST', '/procurement-requests')).toHaveLength(1))
    const [, init] = api.calls('POST', '/procurement-requests')[0]
    expect(JSON.parse(init.body)).toEqual({
      departmentId: 2,
      type: 'MEDICATION',
      priority: 'URGENT',
      neededBy: '2026-11-12T08:00:00+07:00',
      reason: 'Amlodipin sắp hết',
      lines: [
        {
          name: 'Amlodipin',
          quantity: 30,
          estimatedUnitPrice: 2000,
          residentId: 5,
          prescriptionId: 91,
          activeIngredient: 'Amlodipin',
          strength: '5 mg',
          dosageForm: 'Viên nén',
          unit: 'viên',
        },
      ],
    })
    expect(await screen.findByRole('status')).toHaveTextContent('Đã lập yêu cầu ở Nháp; gửi duyệt khi đủ thông tin')
  })

  it('FR_008d_thieuTruongCuaDongThuoc_baoTaiO', async () => {
    permissions(LEAD)
    api.on('GET', '/procurement-requests', json(200, []))
    api.on('GET', '/residents', json(200, { items: [], nextCursor: null }))
    render(
      <PermissionsProvider>
        <ProcurementRequestsPage role="NURSING_STAFF" />
      </PermissionsProvider>,
    )
    await screen.findByText('Chưa có yêu cầu nào trong nhóm này.')
    fireEvent.click(screen.getByRole('button', { name: 'Lập yêu cầu' }))
    const sheet = await screen.findByRole('dialog', { name: 'Lập yêu cầu mua sắm' })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Thuốc' }))
    fireEvent.click(within(sheet).getByRole('button', { name: 'Lưu nháp' }))
    expect(await within(sheet).findByText('Chọn bộ phận')).toBeInTheDocument()
    expect(within(sheet).getByText('Chọn mức ưu tiên')).toBeInTheDocument()
    expect(within(sheet).getByText('Chọn thời điểm cần thuốc')).toBeInTheDocument()
    expect(within(sheet).getByText('Chọn người cao tuổi')).toBeInTheDocument()
    expect(within(sheet).getByText('Chọn đơn thuốc')).toBeInTheDocument()
    expect(api.calls('POST', '/procurement-requests')).toHaveLength(0)
  })

  it('FR_008_khongCoQuyenXem_hien403', async () => {
    permissions([])
    api.on('GET', '/procurement-requests', problem(403, 'PERMISSION_DENIED'))
    render(
      <PermissionsProvider>
        <ProcurementRequestsPage role="CAREGIVER" />
      </PermissionsProvider>,
    )
    expect(await screen.findByText(/không có quyền/i)).toBeInTheDocument()
  })

  it('FR_008_suaNhap_guiIfMatchTheoPhienBan_nutTheoAllowedCommands', async () => {
    permissions(LEAD)
    const draft = { ...INGREDIENT, id: 45, status: 'DRAFT', version: 3, allowedCommands: ['update', 'submit', 'cancel'] }
    const other = { ...INGREDIENT, id: 46, status: 'DRAFT', version: 1, allowedCommands: [] }
    api.on('GET', '/procurement-requests', json(200, [draft, other]))
    api.on('PUT', '/procurement-requests/45', json(200, {}))
    const table = await renderPage('KITCHEN_STAFF')

    // Nháp của người khác: không có nút lệnh nào
    let panel = await openRequest(table, 'Yêu cầu #46')
    expect(within(panel).queryByRole('button', { name: 'Sửa nháp…' })).not.toBeInTheDocument()
    expect(within(panel).queryByRole('button', { name: 'Gửi duyệt' })).not.toBeInTheDocument()
    fireEvent.keyDown(panel, { key: 'Escape' })
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())

    panel = await openRequest(table, 'Yêu cầu #45')
    fireEvent.click(within(panel).getByRole('button', { name: 'Sửa nháp…' }))
    const sheet = await screen.findByRole('dialog', { name: 'Sửa yêu cầu mua sắm #45' })
    expect(within(sheet).getByLabelText('Lý do')).toHaveValue('Hết gạo cho tuần sau')
    fireEvent.change(within(sheet).getByLabelText('Lý do'), { target: { value: 'Hết gạo, thêm thịt' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Lưu nháp' }))
    await waitFor(() => expect(api.calls('PUT', '/procurement-requests/45')).toHaveLength(1))
    const [, init] = api.calls('PUT', '/procurement-requests/45')[0]
    expect(new Headers(init.headers).get('If-Match')).toBe('"3"')
    const body = JSON.parse(init.body)
    expect(body.reason).toBe('Hết gạo, thêm thịt')
    expect(body.lines).toHaveLength(2)
    expect(body.departmentId).toBe(3)
  })
})

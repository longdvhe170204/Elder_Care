import { beforeEach, describe, expect, it } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { AssetsPage } from './AssetsPage'

// 019 T018 màn web (US4; FR-011 → FR-016; bố cục duyệt 2026-11-09 mục (8)): bảng tài sản lọc theo trạng thái kèm số
// đếm, dấu quá hạn; khung chi tiết có nút theo allowedCommands của backend và lịch sử (GET /assets/{id}, api-019 bổ sung 2026-11-10); ghi tăng bằng
// Sheet; tài sản giường chỉ có lệnh của giường (Q-238, Q-242).

let api

const WHEELCHAIR = {
  id: 11,
  code: 'TS-0011',
  typeId: 2,
  name: 'Xe lăn',
  departmentId: 3,
  status: 'READY',
  location: 'Kho tài sản',
  bedId: null,
  flags: [],
  version: 4,
  allowedCommands: ['update', 'put-into-use', 'relocate', 'report-damage', 'start-maintenance', 'request-disposal'],
}
const VAN = {
  id: 12,
  code: 'XE-01',
  typeId: 1,
  name: 'Xe 16 chỗ',
  departmentId: 3,
  status: 'IN_USE',
  location: 'Bãi xe',
  bedId: null,
  flags: ['VEHICLE_INSPECTION_OVERDUE'],
  allowedCommands: ['relocate', 'report-damage', 'start-maintenance', 'request-disposal'],
}
const BED = {
  id: 13,
  code: 'BED-7',
  typeId: 3,
  name: 'Giường 203-A',
  departmentId: 2,
  status: 'DAMAGED',
  location: 'Phòng 203',
  bedId: 7,
  flags: ['AWAITING_RESIDENT_TRANSFER'],
  allowedCommands: ['remove-transfer-flag', 'start-maintenance'],
}
const MAINTENANCE = {
  ...WHEELCHAIR,
  id: 14,
  code: 'TS-0014',
  name: 'Máy hút đờm',
  status: 'UNDER_MAINTENANCE',
  allowedCommands: ['record-maintenance-result', 'relocate', 'request-disposal'],
}

const TYPES = [
  { id: 1, code: 'VEHICLE', name: 'Xe', status: 'ACTIVE' },
  { id: 2, code: null, name: 'Thiết bị y tế', status: 'ACTIVE' },
  { id: 3, code: 'BED', name: 'Giường', status: 'ACTIVE' },
]

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

const VIEW = [['ASSET_VEHICLE', 'VIEW']]
const LEAD = [
  ['ASSET_VEHICLE', 'VIEW'],
  ['ASSET_VEHICLE', 'CREATE'],
  ['ASSET_VEHICLE', 'UPDATE'],
]

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/assets', json(200, [WHEELCHAIR, VAN, BED, MAINTENANCE]))
  api.on('GET', '/asset-types', json(200, TYPES))
  api.on(
    'GET',
    '/departments',
    json(200, [
      { id: 2, name: 'Chăm sóc', active: true },
      { id: 3, name: 'Y tế', active: true },
    ]),
  )
  api.on('GET', '/areas', json(200, [{ id: 1, code: 'K1', name: 'Khu A', status: 'ACTIVE' }]))
  api.on('GET', '/floors', json(200, [{ id: 5, areaId: 1, code: 'T2', name: 'Tầng 2' }]))
  api.on('GET', '/rooms', json(200, [{ id: 9, floorId: 5, code: 'P203', name: 'Phòng 203', roomTypeId: 1 }]))
})

function renderPage(role) {
  render(
    <PermissionsProvider>
      <AssetsPage role={role} />
    </PermissionsProvider>,
  )
  return screen.findByRole('table', { name: 'Tài sản' })
}

function openAsset(table, name) {
  fireEvent.click(within(table).getByRole('button', { name }))
  // Dưới 1280px (jsdom không có matchMedia) khung chi tiết là Sheet
  return screen.findByRole('dialog')
}

describe('Tài sản', () => {
  it('FR_011_bangTaiSan_tenLoai_boPhan_dauQuaHan_locTheoTrangThai', async () => {
    permissions(VIEW)
    const table = await renderPage('OFFICE_STAFF')

    expect(screen.getByRole('button', { name: 'Tất cả 4' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sẵn sàng 1' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Đang bảo trì 1' })).toBeInTheDocument()
    const van = await within(table).findByRole('row', { name: /XE-01/ })
    expect(van).toHaveTextContent('Xe 16 chỗ')
    expect(await within(van).findByText('Xe')).toBeInTheDocument()
    expect(van).toHaveTextContent('Y tế')
    expect(van).toHaveTextContent('Bãi xe')
    expect(van).toHaveTextContent('Đang sử dụng')
    expect(van).toHaveTextContent('Quá hạn đăng kiểm')
    expect(within(table).getByRole('row', { name: /BED-7/ })).toHaveTextContent('Chờ chuyển người')
    expect(screen.queryByRole('button', { name: 'Ghi tăng tài sản' })).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Hỏng 1' }))
    const filtered = screen.getByRole('table', { name: 'Tài sản' })
    expect(within(filtered).queryByRole('row', { name: /XE-01/ })).not.toBeInTheDocument()
    expect(within(filtered).getByRole('row', { name: /BED-7/ })).toBeInTheDocument()
  })

  it('FR_013_baoHong_batBuocMoTa_giuongCoNguoiBatBuocMucAnhHuong', async () => {
    permissions(LEAD)
    const table = await renderPage('DOCTOR')
    api.on('POST', '/assets/11/report-damage', json(200, {}))
    const panel = await openAsset(table, 'TS-0011')
    expect(within(panel).queryByRole('button', { name: 'Báo hỏng…' })).toBeInTheDocument()
    expect(within(panel).getByRole('button', { name: 'Đưa vào sử dụng…' })).toBeInTheDocument()

    fireEvent.click(within(panel).getByRole('button', { name: 'Báo hỏng…' }))
    fireEvent.click(within(panel).getByRole('button', { name: 'Lưu báo hỏng' }))
    expect(await within(panel).findByText('Nhập mô tả hư hỏng')).toBeInTheDocument()
    expect(api.calls('POST', '/assets/11/report-damage')).toHaveLength(0)
    fireEvent.change(within(panel).getByLabelText('Mô tả hư hỏng'), { target: { value: 'Bánh trước vỡ' } })
    fireEvent.click(within(panel).getByRole('button', { name: 'Lưu báo hỏng' }))
    await waitFor(() => expect(api.calls('POST', '/assets/11/report-damage')).toHaveLength(1))
    const [, init] = api.calls('POST', '/assets/11/report-damage')[0]
    expect(JSON.parse(init.body)).toEqual({ description: 'Bánh trước vỡ' })
    expect(await screen.findByText('Đã báo hỏng TS-0011')).toBeInTheDocument()
  })

  it('FR_015_giuongCoNguoi_baoHongCanMucAnhHuong_goDauCanNoiDungSuaKhiMatAnToan', async () => {
    permissions(LEAD)
    const goodBed = {
      ...BED,
      id: 15,
      code: 'BED-8',
      status: 'READY',
      flags: [],
      allowedCommands: ['report-damage', 'start-maintenance', 'link-bed-preparation-damage'],
    }
    api.on('GET', '/assets', json(200, [goodBed, BED]))
    const table = await renderPage('FLOOR_MANAGER')
    api.on(
      'POST',
      '/assets/15/report-damage',
      problem(409, 'IMPACT_LEVEL_REQUIRED', {
        detail: 'Không báo hỏng được giường này',
        unmetConditions: [{ code: 'IMPACT_LEVEL_REQUIRED', description: 'Giường có người: cần mức ảnh hưởng' }],
      }),
    )
    api.on('POST', '/assets/13/remove-transfer-flag', json(200, {}))

    let panel = await openAsset(table, 'BED-8')
    // Giường không có Đưa vào sử dụng, Chuyển vị trí (Q-242, Q-243)
    expect(within(panel).queryByRole('button', { name: 'Đưa vào sử dụng…' })).not.toBeInTheDocument()
    expect(within(panel).queryByRole('button', { name: 'Chuyển vị trí…' })).not.toBeInTheDocument()
    fireEvent.click(within(panel).getByRole('button', { name: 'Báo hỏng…' }))
    fireEvent.change(within(panel).getByLabelText('Mô tả hư hỏng'), { target: { value: 'Gãy chân giường' } })
    fireEvent.click(within(panel).getByRole('button', { name: 'Mất an toàn' }))
    fireEvent.click(within(panel).getByRole('button', { name: 'Lưu báo hỏng' }))
    await waitFor(() => expect(api.calls('POST', '/assets/15/report-damage')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/assets/15/report-damage')[0][1].body)).toEqual({
      description: 'Gãy chân giường',
      impactLevel: 'UNSAFE',
    })
    expect(await within(panel).findByText('Không báo hỏng được giường này')).toBeInTheDocument()
    // Giá trị đã gõ giữ lại sau lỗi 409
    expect(within(panel).getByLabelText('Mô tả hư hỏng')).toHaveValue('Gãy chân giường')

    fireEvent.keyDown(panel, { key: 'Escape' })
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    panel = await openAsset(screen.getByRole('table', { name: 'Tài sản' }), 'BED-7')
    fireEvent.click(within(panel).getByRole('button', { name: 'Gỡ dấu chờ chuyển người…' }))
    fireEvent.change(within(panel).getByLabelText('Lý do'), { target: { value: 'Đã chuyển người sang giường khác' } })
    fireEvent.change(within(panel).getByLabelText('Nội dung đã sửa'), { target: { value: 'Thay chân giường' } })
    fireEvent.change(within(panel).getByLabelText('Người, đơn vị sửa'), { target: { value: 'Tổ kỹ thuật' } })
    fireEvent.click(within(panel).getByRole('button', { name: 'Lưu gỡ dấu' }))
    await waitFor(() => expect(api.calls('POST', '/assets/13/remove-transfer-flag')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/assets/13/remove-transfer-flag')[0][1].body)).toEqual({
      reason: 'Đã chuyển người sang giường khác',
      repairDescription: 'Thay chân giường',
      repairedBy: 'Tổ kỹ thuật',
    })
  })

  it('FR_013_ketQuaBaoTri_datKhongDat_xeDangKiemCanHanKeTiep', async () => {
    permissions(LEAD)
    const table = await renderPage('DOCTOR')
    api.on('POST', '/assets/14/record-maintenance-result', json(200, {}))
    const panel = await openAsset(table, 'TS-0014')
    expect(within(panel).queryByRole('button', { name: 'Báo hỏng…' })).not.toBeInTheDocument()
    fireEvent.click(within(panel).getByRole('button', { name: 'Ghi kết quả bảo trì…' }))
    fireEvent.click(within(panel).getByRole('button', { name: 'Lưu kết quả' }))
    expect(await within(panel).findByText('Chọn Đạt hoặc Không đạt')).toBeInTheDocument()
    fireEvent.click(within(panel).getByRole('button', { name: 'Đạt' }))
    fireEvent.change(within(panel).getByLabelText('Ngày thực hiện'), { target: { value: '2026-11-09' } })
    fireEvent.change(within(panel).getByLabelText('Nội dung'), { target: { value: 'Thay ống hút' } })
    fireEvent.change(within(panel).getByLabelText('Đơn vị thực hiện'), { target: { value: 'Công ty ABC' } })
    fireEvent.click(within(panel).getByRole('button', { name: 'Lưu kết quả' }))
    await waitFor(() => expect(api.calls('POST', '/assets/14/record-maintenance-result')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/assets/14/record-maintenance-result')[0][1].body)).toEqual({
      result: 'PASS',
      date: '2026-11-09',
      content: 'Thay ống hút',
      performedBy: 'Công ty ABC',
    })
  })

  it('FR_011_hanhChinhGhiTang_hangTangThiGhiNguon_muaMoiCanYeuCauVaNguyenGia', async () => {
    permissions(LEAD)
    renderPage('DOCTOR')
    await screen.findByRole('table', { name: 'Tài sản' })
    api.on('POST', '/assets', () => new Response(null, { status: 201 }))
    fireEvent.click(screen.getByRole('button', { name: 'Ghi tăng tài sản' }))
    const sheet = await screen.findByRole('dialog', { name: 'Ghi tăng tài sản' })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Lưu tài sản' }))
    expect(await within(sheet).findByText('Nhập mã tài sản')).toBeInTheDocument()
    expect(within(sheet).getByText('Chọn loại tài sản')).toBeInTheDocument()
    expect(within(sheet).getByText('Chọn nguồn')).toBeInTheDocument()
    // Loại Giường không có trong danh sách chọn (chỉ tạo kèm lệnh Tạo giường của 003, Q-238)
    const type = within(sheet).getByLabelText('Loại tài sản')
    await waitFor(() => expect(within(type).getAllByRole('option').length).toBeGreaterThan(1))
    expect(within(type).queryByRole('option', { name: 'Giường' })).not.toBeInTheDocument()

    fireEvent.change(within(sheet).getByLabelText('Mã tài sản'), { target: { value: 'TS-0099' } })
    fireEvent.change(within(sheet).getByLabelText('Tên tài sản'), { target: { value: 'Máy tạo oxy' } })
    fireEvent.change(within(sheet).getByLabelText('Loại tài sản'), { target: { value: '2' } })
    fireEvent.change(within(sheet).getByLabelText('Bộ phận quản lý'), { target: { value: '3' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Mua mới' }))
    fireEvent.click(within(sheet).getByRole('button', { name: 'Lưu tài sản' }))
    expect(await within(sheet).findByText('Nhập số yêu cầu mua sắm Đã duyệt')).toBeInTheDocument()
    expect(within(sheet).getByText('Nhập nguyên giá')).toBeInTheDocument()
    fireEvent.change(within(sheet).getByLabelText('Yêu cầu mua sắm đã duyệt (số)'), { target: { value: '42' } })
    fireEvent.change(within(sheet).getByLabelText('Nguyên giá (đồng)'), { target: { value: '1500000' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Lưu tài sản' }))
    await waitFor(() => expect(api.calls('POST', '/assets')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/assets')[0][1].body)).toEqual({
      code: 'TS-0099',
      typeId: 2,
      name: 'Máy tạo oxy',
      departmentId: 3,
      source: 'PURCHASED',
      procurementRequestId: 42,
      acquisitionCost: 1500000,
    })
  })

  it('FR_011_khongCoQuyenXem_hien403', async () => {
    permissions([])
    api.on('GET', '/assets', problem(403, 'PERMISSION_DENIED'))
    render(
      <PermissionsProvider>
        <AssetsPage role="CAREGIVER" />
      </PermissionsProvider>,
    )
    expect(await screen.findByText(/không có quyền/i)).toBeInTheDocument()
  })

  it('FR_012_khungChiTiet_lichSuViTriBaoTri_nutTheoAllowedCommands', async () => {
    permissions(VIEW)
    api.on('GET', '/assets', json(200, [{ ...WHEELCHAIR, allowedCommands: [] }]))
    api.on(
      'GET',
      '/assets/11',
      json(200, {
        ...WHEELCHAIR,
        allowedCommands: [],
        locations: [
          { type: 'ASSET_STORAGE', from: '2026-10-01T01:00:00Z', to: '2026-10-05T01:00:00Z', command: 'CREATE' },
          { type: 'FLOOR', id: 5, from: '2026-10-05T01:00:00Z', command: 'PUT_INTO_USE' },
        ],
        departmentTransfers: [],
        maintenances: [
          {
            id: 81,
            kind: 'MAINTENANCE',
            result: 'PASS',
            date: '2026-11-02',
            content: 'Thay bánh xe',
            performedBy: 'Tổ kỹ thuật',
            corrected: true,
            voided: false,
          },
        ],
        damages: [],
      }),
    )
    const table = await renderPage('OFFICE_STAFF')
    const panel = await openAsset(table, 'TS-0011')
    // Không có lệnh được phép: không có nút lệnh nào
    expect(within(panel).queryByRole('button', { name: 'Báo hỏng…' })).not.toBeInTheDocument()
    expect(within(panel).queryByRole('button', { name: 'Đưa vào sử dụng…' })).not.toBeInTheDocument()
    const history = await within(panel).findByRole('region', { name: 'Lịch sử vị trí' })
    expect(within(history).getByText('Kho tài sản')).toBeInTheDocument()
    expect(within(history).getByText('Tầng 2')).toBeInTheDocument()
    const maintenance = within(panel).getByRole('region', { name: 'Bảo trì, kiểm định' })
    expect(within(maintenance).getByText('Bảo trì, Đạt, 02/11/2026')).toBeInTheDocument()
    expect(within(maintenance).getByText('Thay bánh xe, Tổ kỹ thuật')).toBeInTheDocument()
    expect(within(maintenance).getByText('Đã đính chính')).toBeInTheDocument()
  })

  it('FR_011_suaMoTa_dienSanTuChiTiet_guiPutIfMatch_khongDoiNguonBoPhan', async () => {
    permissions(LEAD)
    api.on('GET', '/assets', json(200, [WHEELCHAIR]))
    api.on(
      'GET',
      '/assets/11',
      json(200, {
        ...WHEELCHAIR,
        source: 'DONATED',
        sourceDescription: 'Hội người cao tuổi phường tặng',
        acquisitionCost: 2500000,
        maintenanceIntervalDays: 90,
        locations: [],
        departmentTransfers: [],
        maintenances: [],
        damages: [],
      }),
    )
    api.on('PUT', '/assets/11', json(200, {}))
    const table = await renderPage('DOCTOR')
    const panel = await openAsset(table, 'TS-0011')
    fireEvent.click(within(panel).getByRole('button', { name: 'Sửa mô tả…' }))
    const sheet = await screen.findByRole('dialog', { name: 'Sửa mô tả tài sản TS-0011' })
    expect(await within(sheet).findByLabelText('Tên tài sản')).toHaveValue('Xe lăn')
    expect(within(sheet).getByLabelText('Mô tả nguồn')).toHaveValue('Hội người cao tuổi phường tặng')
    expect(within(sheet).getByLabelText('Bộ phận quản lý')).toBeDisabled()
    fireEvent.change(within(sheet).getByLabelText('Tên tài sản'), { target: { value: 'Xe lăn gấp' } })
    fireEvent.click(within(sheet).getByRole('button', { name: 'Lưu mô tả' }))
    await waitFor(() => expect(api.calls('PUT', '/assets/11')).toHaveLength(1))
    const [, init] = api.calls('PUT', '/assets/11')[0]
    expect(new Headers(init.headers).get('If-Match')).toBe('"4"')
    const body = JSON.parse(init.body)
    expect(body).toMatchObject({
      code: 'TS-0011',
      name: 'Xe lăn gấp',
      typeId: 2,
      departmentId: 3,
      source: 'DONATED',
      sourceDescription: 'Hội người cao tuổi phường tặng',
      acquisitionCost: 2500000,
      maintenanceIntervalDays: 90,
    })
  })
})

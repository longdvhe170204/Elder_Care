import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { CatalogList } from './CatalogList'

// 001 T072: màn danh mục M01 (FR-018, FR-018a, BR-M01-11, BR-M15-06; hợp đồng /catalogs/{type}).
// Chỉ Quản lý viện đổi danh mục (ma trận HEALTH_CATALOGS: AC-01 xem, tạo, sửa; AC-03, AC-04 chỉ xem).

const ALLERGENS = [
  { id: 1, name: 'Penicillin', group: 'ACTIVE_INGREDIENT', status: 'ACTIVE', version: 2 },
  { id: 2, name: 'Tôm', group: 'FOOD', status: 'INACTIVE', version: 1 },
]
const SEVERITIES = [{ id: 7, name: 'Nặng', order: 3, status: 'ACTIVE', version: 0 }]

let api

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/catalogs/allergens', json(200, ALLERGENS))
  api.on('GET', '/catalogs/allergy-severities', json(200, SEVERITIES))
})

afterEach(() => {
  vi.unstubAllGlobals()
})

const body = (call) => JSON.parse(call[1].body)
const type = (el, value) => fireEvent.change(el, { target: { value } })

async function rows() {
  return (await screen.findAllByRole('row')).slice(1)
}

describe('CatalogList', () => {
  it('FR_018_hienDiNguyenMacDinh_nhomVaTrangThai', async () => {
    render(<CatalogList canChange />)

    const [first, second] = await rows()
    expect(first).toHaveTextContent('Penicillin')
    expect(first).toHaveTextContent('Hoạt chất')
    expect(first).toHaveTextContent('Hiệu lực')
    expect(second).toHaveTextContent('Thực phẩm')
    expect(second).toHaveTextContent('Ngừng hiệu lực')
  })

  it('FR_018_doiLoai_taiLaiVaHienThuTuChoMucDoDiUng', async () => {
    render(<CatalogList canChange />)
    await rows()

    fireEvent.click(screen.getByRole('button', { name: 'Mức độ dị ứng' }))

    await waitFor(async () => {
      const [row] = await rows()
      expect(row).toHaveTextContent('Nặng')
      expect(row).toHaveTextContent('3')
    })
    expect(screen.queryByLabelText('Nhóm dị nguyên')).not.toBeInTheDocument()
  })

  it('locTheoTrangThaiVaNhom_goiQuery', async () => {
    render(<CatalogList canChange />)
    await rows()

    type(screen.getByLabelText('Nhóm dị nguyên'), 'FOOD')
    type(screen.getByLabelText('Trạng thái'), 'INACTIVE')

    await waitFor(() => {
      const last = api.fetchMock.mock.calls.at(-1)[0]
      expect(last).toContain('group=FOOD')
      expect(last).toContain('status=INACTIVE')
    })
  })

  it('FR_018a_tao_guiTenVaNhom_roiTaiLai', async () => {
    api.on('POST', '/catalogs/allergens', () =>
      json(201, { id: 9, name: 'Latex', group: 'OTHER', status: 'ACTIVE', version: 0 }),
    )
    render(<CatalogList canChange />)
    await rows()

    fireEvent.click(screen.getByRole('button', { name: 'Thêm mục' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu' }))
    expect(await screen.findByText('Nhập tên')).toBeInTheDocument()
    expect(api.calls('POST', '/catalogs/allergens')).toHaveLength(0)

    type(screen.getByLabelText('Tên'), 'Latex')
    type(screen.getByLabelText('Nhóm'), 'OTHER')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu' }))

    await waitFor(() => expect(api.calls('POST', '/catalogs/allergens')).toHaveLength(1))
    expect(body(api.calls('POST', '/catalogs/allergens')[0])).toEqual({ name: 'Latex', group: 'OTHER' })
    await waitFor(() => expect(api.calls('GET', '/catalogs/allergens').length).toBeGreaterThan(1))
  })

  it('FR_018a_sua_guiIfMatchLaVersionCuaDong', async () => {
    api.on('PUT', '/catalogs/allergens/1', () => json(200, { ...ALLERGENS[0], name: 'Penicillin G', version: 3 }))
    render(<CatalogList canChange />)
    const [first] = await rows()

    fireEvent.click(within(first).getByRole('button', { name: 'Sửa' }))
    expect(screen.getByLabelText('Tên')).toHaveValue('Penicillin')
    type(screen.getByLabelText('Tên'), 'Penicillin G')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu' }))

    await waitFor(() => expect(api.calls('PUT', '/catalogs/allergens/1')).toHaveLength(1))
    const call = api.calls('PUT', '/catalogs/allergens/1')[0]
    expect(call[1].headers['If-Match']).toBe('"2"')
    expect(body(call)).toEqual({ name: 'Penicillin G', group: 'ACTIVE_INGREDIENT' })
  })

  it('FR_021_sua412_baoDuLieuDaDoi_vaChoTaiLai', async () => {
    api.on('PUT', '/catalogs/allergens/1', () => problem(412, 'VERSION_MISMATCH'))
    render(<CatalogList canChange />)
    const [first] = await rows()

    fireEvent.click(within(first).getByRole('button', { name: 'Sửa' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu' }))

    expect(await screen.findByText('Dữ liệu đã thay đổi')).toBeInTheDocument()
    const before = api.calls('GET', '/catalogs/allergens').length
    fireEvent.click(screen.getByRole('button', { name: 'Tải lại' }))
    await waitFor(() => expect(api.calls('GET', '/catalogs/allergens').length).toBeGreaterThan(before))
  })

  it('BR_M15_06_tao409TrungTen_hienLoiVaGiuForm', async () => {
    api.on('POST', '/catalogs/allergens', () =>
      problem(409, 'DUPLICATE_NAME', { detail: 'Đã có mục Hiệu lực cùng tên' }),
    )
    render(<CatalogList canChange />)
    await rows()

    fireEvent.click(screen.getByRole('button', { name: 'Thêm mục' }))
    type(screen.getByLabelText('Tên'), 'Penicillin')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu' }))

    expect(await screen.findByText('Đã có mục Hiệu lực cùng tên')).toBeInTheDocument()
    expect(screen.getByLabelText('Tên')).toHaveValue('Penicillin')
  })

  it('FR_018a_ngungHieuLuc_lyDoBatBuoc', async () => {
    api.on('POST', '/catalogs/allergens/1/deactivate', json(200, null))
    render(<CatalogList canChange />)
    const [first] = await rows()

    fireEvent.click(within(first).getByRole('button', { name: 'Ngừng hiệu lực' }))
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận ngừng' }))
    expect(await screen.findByText('Nhập lý do')).toBeInTheDocument()
    expect(api.calls('POST', '/catalogs/allergens/1/deactivate')).toHaveLength(0)

    type(screen.getByLabelText('Lý do'), 'Trùng mục khác')
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận ngừng' }))

    await waitFor(() => expect(api.calls('POST', '/catalogs/allergens/1/deactivate')).toHaveLength(1))
    expect(body(api.calls('POST', '/catalogs/allergens/1/deactivate')[0])).toEqual({ reason: 'Trùng mục khác' })
  })

  it('BR_M15_06_kichHoatLai409_hienLoi', async () => {
    api.on('POST', '/catalogs/allergens/2/reactivate', () =>
      problem(409, 'DUPLICATE_NAME', { detail: 'Đang có mục Hiệu lực cùng tên' }),
    )
    render(<CatalogList canChange />)
    const [, second] = await rows()

    expect(within(second).queryByRole('button', { name: 'Ngừng hiệu lực' })).not.toBeInTheDocument()
    fireEvent.click(within(second).getByRole('button', { name: 'Kích hoạt lại' }))
    type(screen.getByLabelText('Lý do'), 'Dùng lại')
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận kích hoạt' }))

    expect(await screen.findByText('Đang có mục Hiệu lực cùng tên')).toBeInTheDocument()
  })

  it('BR_M01_11_khongCoQuyenSua_anNutThaoTac', async () => {
    render(<CatalogList canChange={false} />)
    await rows()

    expect(screen.queryByRole('button', { name: 'Thêm mục' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Sửa' })).not.toBeInTheDocument()
    expect(screen.queryByRole('columnheader', { name: 'Thao tác' })).not.toBeInTheDocument()
  })

  it('xem403_hienKhongCoQuyen', async () => {
    api.on('GET', '/catalogs/allergens', () => problem(403, 'FORBIDDEN'))
    render(<CatalogList canChange={false} />)

    expect(await screen.findByText('Bạn không có quyền xem danh mục')).toBeInTheDocument()
  })

  it('thaoTac403_hienLoiKhongCoQuyen', async () => {
    api.on('POST', '/catalogs/allergens', () => problem(403, 'FORBIDDEN'))
    render(<CatalogList canChange />)
    await rows()

    fireEvent.click(screen.getByRole('button', { name: 'Thêm mục' }))
    type(screen.getByLabelText('Tên'), 'Latex')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu' }))

    expect(await screen.findByText('Không có quyền thực hiện')).toBeInTheDocument()
  })
})

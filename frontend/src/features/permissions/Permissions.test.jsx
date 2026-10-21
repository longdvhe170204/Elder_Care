import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { RolePermissions } from './RolePermissions'
import { AccountPermissions } from './AccountPermissions'

// 002 T077 (US7; FR-018, FR-020, FR-024 → FR-027, BR-M15-16 → 18): màn phân quyền theo vai trò, mục quyền của tài
// khoản, tra quyền tại thời điểm. Màn chỉ đưa ra ô có trong ma trận (không có lệnh thêm quyền), 412 hiện giá trị mới.

const ROLES = [
  { code: 'AC-01', name: 'Quản lý viện', accountType: 'STAFF', executionScope: 'FACILITY' },
  { code: 'AC-09', name: 'Nhân viên hành chính', accountType: 'STAFF', executionScope: 'FACILITY' },
]

const grant = (fn, op, scope, footnoteCodes = []) => ({
  function: fn,
  operation: op,
  scope,
  footnoteCodes,
  source: 'ROLE',
})

function officePermissions(overrides = {}) {
  return {
    role: 'AC-09',
    maximum: [grant('COST_ADJUSTMENT', 'CREATE', 'FACILITY', ['16']), grant('CARE_PLAN', 'VIEW', 'ASSIGNED')],
    restrictions: [],
    current: [grant('COST_ADJUSTMENT', 'CREATE', 'FACILITY', ['16']), grant('CARE_PLAN', 'VIEW', 'ASSIGNED')],
    ...overrides,
  }
}

const RESTRICTION = {
  id: 7,
  function: 'COST_ADJUSTMENT',
  operation: 'CREATE',
  kind: 'REMOVE_OPERATION',
  effectiveFrom: '2026-10-21T02:00:00Z',
  reason: 'Kế toán lập thay',
}

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

async function openOffice(permissions = officePermissions()) {
  api.on('GET', '/roles', json(200, ROLES))
  api.on('GET', '/roles/AC-01/permissions', json(200, officePermissions({ role: 'AC-01' }), { ETag: '"0"' }))
  api.on('GET', '/roles/AC-09/permissions', json(200, permissions, { ETag: '"3"' }))
  render(<RolePermissions />)
  fireEvent.click(await screen.findByRole('button', { name: 'Nhân viên hành chính' }))
  return screen.findByRole('table')
}

function rowOf(table, text) {
  return within(table)
    .getAllByRole('row')
    .find((r) => r.textContent.includes(text))
}

describe('RolePermissions', () => {
  it('US7_1_bangQuyenTheoMaTran_phamVi_trangThai', async () => {
    const table = await openOffice()

    const cost = rowOf(table, 'Chi phí, khoản điều chỉnh')
    expect(cost).toHaveTextContent('Tạo')
    expect(cost).toHaveTextContent('Toàn viện')
    expect(within(cost).getByText('Theo ma trận')).toHaveAttribute('data-variant', 'neutral')
    expect(rowOf(table, 'Kế hoạch chăm sóc')).toHaveTextContent('Theo phân công')
  })

  it('FR_025_chiThuHepTrongMaTran_khongCoLenhThemQuyen_haPhamViChiOToanVien', async () => {
    const table = await openOffice()

    expect(screen.queryByRole('button', { name: /Thêm quyền/ })).toBeNull()
    fireEvent.click(within(rowOf(table, 'Kế hoạch chăm sóc')).getByRole('button', { name: 'Thu hẹp' }))
    expect(screen.queryByRole('radio', { name: 'Hạ xuống theo phân công' })).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Hủy' }))

    fireEvent.click(within(rowOf(table, 'Chi phí, khoản điều chỉnh')).getByRole('button', { name: 'Thu hẹp' }))
    expect(screen.getByRole('radio', { name: 'Hạ xuống theo phân công' })).toBeInTheDocument()
  })

  it('FR_025_thuHep_guiIfMatch_lyDoBatBuoc_capNhatBang', async () => {
    const table = await openOffice()
    api.on(
      'POST',
      '/roles/AC-09/restrict',
      json(200, officePermissions({ restrictions: [RESTRICTION], current: [grant('CARE_PLAN', 'VIEW', 'ASSIGNED')] }), {
        ETag: '"4"',
      }),
    )

    fireEvent.click(within(rowOf(table, 'Chi phí, khoản điều chỉnh')).getByRole('button', { name: 'Thu hẹp' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thu hẹp' }))
    expect(await screen.findByText('Nhập lý do')).toBeInTheDocument()
    expect(api.calls('POST', '/roles/AC-09/restrict')).toHaveLength(0)

    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Kế toán lập thay' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thu hẹp' }))

    await waitFor(() => expect(api.calls('POST', '/roles/AC-09/restrict')).toHaveLength(1))
    const [, init] = api.calls('POST', '/roles/AC-09/restrict')[0]
    expect(init.headers['If-Match']).toBe('"3"')
    expect(JSON.parse(init.body)).toEqual({
      function: 'COST_ADJUSTMENT',
      operation: 'CREATE',
      kind: 'REMOVE_OPERATION',
      reason: 'Kế toán lập thay',
    })
    const cost = rowOf(await screen.findByRole('table'), 'Chi phí, khoản điều chỉnh')
    await waitFor(() => expect(within(cost).getByText('Đã thu hẹp')).toHaveAttribute('data-variant', 'attention'))
    expect(cost).toHaveTextContent('Đã bỏ')
    expect(screen.getByRole('region', { name: 'Thu hẹp đang hiệu lực' })).toHaveTextContent('Kế toán lập thay')
  })

  it('BR_M15_17_412_hienThuHepHienHanh_taiLai', async () => {
    const table = await openOffice()
    api.on(
      'POST',
      '/roles/AC-09/restrict',
      problem(412, 'VERSION_MISMATCH', { currentValue: officePermissions({ restrictions: [RESTRICTION] }) }),
    )

    fireEvent.click(within(rowOf(table, 'Chi phí, khoản điều chỉnh')).getByRole('button', { name: 'Thu hẹp' }))
    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Thử' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thu hẹp' }))

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Dữ liệu đã thay đổi')
    expect(alert).toHaveTextContent('Chi phí, khoản điều chỉnh, tạo: Bỏ thao tác')
    fireEvent.click(within(alert).getByRole('button', { name: 'Tải lại' }))
    await waitFor(() => expect(api.calls('GET', '/roles/AC-09/permissions')).toHaveLength(2))
  })

  it('FR_012_409_hienLyDoTuMayChu', async () => {
    const table = await openOffice()
    api.on(
      'POST',
      '/roles/AC-09/restrict',
      problem(409, 'NO_MANAGER_LEFT', { detail: 'Viện phải còn ít nhất một Quản lý viện có quyền cấu hình' }),
    )

    fireEvent.click(within(rowOf(table, 'Chi phí, khoản điều chỉnh')).getByRole('button', { name: 'Thu hẹp' }))
    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Thử' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thu hẹp' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Viện phải còn ít nhất một Quản lý viện')
  })

  it('US7_5_khoiPhuc_guiRestrictionId', async () => {
    await openOffice(
      officePermissions({ restrictions: [RESTRICTION], current: [grant('CARE_PLAN', 'VIEW', 'ASSIGNED')] }),
    )
    api.on('POST', '/roles/AC-09/restore-permission', json(200, officePermissions(), { ETag: '"4"' }))

    const region = screen.getByRole('region', { name: 'Thu hẹp đang hiệu lực' })
    fireEvent.click(within(region).getByRole('button', { name: 'Khôi phục' }))
    fireEvent.change(within(region).getByLabelText('Lý do'), { target: { value: 'Hết lý do thu hẹp' } })
    fireEvent.click(within(region).getByRole('button', { name: 'Khôi phục về mức ma trận' }))

    await waitFor(() => expect(api.calls('POST', '/roles/AC-09/restore-permission')).toHaveLength(1))
    const [, init] = api.calls('POST', '/roles/AC-09/restore-permission')[0]
    expect(JSON.parse(init.body)).toEqual({ restrictionId: 7, reason: 'Hết lý do thu hẹp' })
    expect(await screen.findByText('Chưa có thu hẹp nào; vai trò dùng đúng ma trận.')).toBeInTheDocument()
  })
})

function accountPermissions(overrides = {}) {
  return {
    accountId: 5,
    asOf: '2026-10-21T03:00:00Z',
    status: 'ACTIVE',
    roles: ['AC-04'],
    roleRestrictions: [],
    accountRestrictions: [],
    additionalGrants: [],
    current: [grant('CARE_PLAN', 'VIEW', 'ASSIGNED'), grant('CARE_PLAN', 'UPDATE', 'ASSIGNED')],
    ...overrides,
  }
}

describe('AccountPermissions', () => {
  it('FR_024_ganQuyenDuyetKeHoach_theoAllowedCommands', async () => {
    api.on('GET', '/accounts/5/permissions', json(200, accountPermissions(), { ETag: '"2"' }))
    api.on(
      'POST',
      '/accounts/5/grant-care-plan-approval',
      json(
        200,
        accountPermissions({
          additionalGrants: [{ ...grant('CARE_PLAN', 'APPROVE', 'ASSIGNED'), source: 'ADDITIONAL_GRANT' }],
        }),
        { ETag: '"3"' },
      ),
    )
    const onChanged = vi.fn()
    render(
      <AccountPermissions
        accountId={5}
        allowedCommands={['restrict', 'grant-care-plan-approval']}
        onChanged={onChanged}
      />,
    )

    expect(await screen.findByText('Chưa gán thêm quyền nào')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Gỡ quyền Duyệt kế hoạch chăm sóc' })).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Gán quyền Duyệt kế hoạch chăm sóc' }))
    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Có chứng chỉ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Gán quyền' }))

    await waitFor(() => expect(api.calls('POST', '/accounts/5/grant-care-plan-approval')).toHaveLength(1))
    expect(api.calls('POST', '/accounts/5/grant-care-plan-approval')[0][1].headers['If-Match']).toBe('"2"')
    expect(await screen.findByText('Kế hoạch chăm sóc, duyệt (theo phân công)')).toBeInTheDocument()
    expect(onChanged).toHaveBeenCalled()
  })

  it('FR_025_thuHepTheoTaiKhoan_chonTuQuyenHienHanh', async () => {
    api.on('GET', '/accounts/5/permissions', json(200, accountPermissions(), { ETag: '"2"' }))
    api.on('POST', '/accounts/5/restrict', json(200, accountPermissions(), { ETag: '"3"' }))
    render(<AccountPermissions accountId={5} allowedCommands={['restrict']} onChanged={() => {}} />)

    fireEvent.click(await screen.findByRole('button', { name: 'Thu hẹp quyền' }))
    const select = screen.getByLabelText('Quyền cần thu hẹp')
    expect(
      within(select)
        .getAllByRole('option')
        .map((o) => o.textContent),
    ).toEqual(['Kế hoạch chăm sóc, xem', 'Kế hoạch chăm sóc, sửa'])
    fireEvent.change(select, { target: { value: 'CARE_PLAN/UPDATE' } })
    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Chỉ xem' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu thu hẹp' }))

    await waitFor(() => expect(api.calls('POST', '/accounts/5/restrict')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/accounts/5/restrict')[0][1].body)).toEqual({
      function: 'CARE_PLAN',
      operation: 'UPDATE',
      kind: 'REMOVE_OPERATION',
      reason: 'Chỉ xem',
    })
  })

  it('FR_026a_xemTaiThoiDiem_guiAsOf_chiXem', async () => {
    api.on('GET', '/accounts/5/permissions', (url) =>
      url.searchParams.get('asOf')
        ? json(200, accountPermissions({ asOf: url.searchParams.get('asOf'), roles: ['AC-05'] }))
        : json(200, accountPermissions(), { ETag: '"2"' }),
    )
    render(<AccountPermissions accountId={5} allowedCommands={['restrict']} onChanged={() => {}} />)

    fireEvent.change(await screen.findByLabelText('Xem tại thời điểm'), { target: { value: '2026-10-20T08:30' } })
    fireEvent.click(screen.getByRole('button', { name: 'Xem' }))

    const past = await screen.findByRole('region', { name: /Quyền lúc 20\/10\/2026 08:30/ })
    expect(past).toHaveTextContent('Nhân viên chăm sóc')
    const calls = api.calls('GET', '/accounts/5/permissions')
    expect(new URL(calls.at(-1)[0], 'http://localhost').searchParams.get('asOf')).toBe('2026-10-20T01:30:00.000Z')
    expect(within(past).queryByRole('button', { name: 'Khôi phục' })).toBeNull()
  })
})

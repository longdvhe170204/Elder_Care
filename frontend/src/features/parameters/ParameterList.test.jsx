import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { ParameterList } from './ParameterList'

// 000 T081: màn tham số (US4 #1, #3, #6; FR-011, FR-013, FR-019; hợp đồng /parameters).

function parameter(code, description, overrides = {}) {
  return {
    code,
    description,
    type: 'DURATION',
    unit: 'HOURS',
    constraints: { min: 1 },
    defaultValue: { value: 48, unit: 'HOURS' },
    currentValue: { value: 48, unit: 'HOURS' },
    usedIn: 'BR-M15-09',
    version: 0,
    changedAt: null,
    ...overrides,
  }
}

const LIST = [
  parameter('CFG-M01-01', 'Quá hạn trở lại sau tạm vắng', { currentValue: { value: 2, unit: 'HOURS' } }),
  parameter('CFG-M15-05', 'Mốc nhắc người duyệt', { changedAt: '2026-10-19T01:12:00Z', version: 2 }),
]

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('ParameterList', () => {
  it('US4_1_hienDanhSach_giaTri_ngayDoi_timTheoMaHoacMoTa', async () => {
    api.on('GET', '/parameters', json(200, LIST))
    render(<ParameterList canChange />)

    const rows = (await screen.findAllByRole('row')).slice(1)
    expect(rows).toHaveLength(2)
    expect(rows[0]).toHaveTextContent('CFG-M01-01')
    expect(rows[0]).toHaveTextContent('2 giờ')
    expect(rows[1]).toHaveTextContent('19/10/2026')

    fireEvent.change(screen.getByLabelText('Tìm mã hoặc mô tả'), { target: { value: 'nhắc người' } })
    expect(screen.getAllByRole('row').slice(1)).toHaveLength(1)
    expect(screen.getAllByRole('row')[1]).toHaveTextContent('CFG-M15-05')
  })

  it('locTheoModule_goiQueryModule', async () => {
    api.on('GET', '/parameters', json(200, LIST))
    render(<ParameterList canChange />)
    await screen.findAllByRole('row')

    fireEvent.change(screen.getByLabelText('Module'), { target: { value: 'M15' } })
    await waitFor(() => expect(api.fetchMock.mock.calls.some(([url]) => String(url).includes('module=M15'))).toBe(true))
  })

  it('US4_6_403_hienKhongCoQuyen', async () => {
    api.on('GET', '/parameters', problem(403, 'PERMISSION_DENIED'))
    render(<ParameterList canChange={false} />)
    expect(await screen.findByText('Bạn không có quyền xem tham số vận hành')).toBeInTheDocument()
  })

  it('bamDong_moKhungChiTiet_doiGiaTri_capNhatDong', async () => {
    api.on('GET', '/parameters', json(200, LIST))
    api.on('GET', '/parameters/CFG-M15-05', json(200, LIST[1], { ETag: '"2"' }))
    api.on('POST', '/parameters/CFG-M15-05/change-value', () =>
      json(200, { ...LIST[1], currentValue: { value: 72, unit: 'HOURS' }, version: 3 }, { ETag: '"3"' }),
    )
    render(<ParameterList canChange />)

    fireEvent.click(await screen.findByRole('button', { name: 'CFG-M15-05' }))
    const panel = await screen.findByRole('dialog')
    expect(within(panel).getByText('Mốc nhắc người duyệt')).toBeInTheDocument()
    expect(within(panel).getByText('Dùng tại: BR-M15-09')).toBeInTheDocument()

    fireEvent.change(await within(panel).findByLabelText('Giá trị mới (giờ)'), { target: { value: '72' } })
    fireEvent.change(within(panel).getByLabelText('Lý do'), { target: { value: 'x' } })
    fireEvent.click(within(panel).getByRole('button', { name: 'Lưu giá trị' }))

    await waitFor(() => expect(screen.getAllByRole('row', { hidden: true })[2]).toHaveTextContent('72 giờ'))
    expect(api.calls('POST', '/parameters/CFG-M15-05/change-value')[0][1].headers['If-Match']).toBe('"2"')
  })

  it('FR_019_lichSu_taiThoiDiem', async () => {
    api.on('GET', '/parameters', json(200, LIST))
    api.on('GET', '/parameters/CFG-M15-05', json(200, LIST[1], { ETag: '"2"' }))
    api.on(
      'GET',
      '/parameters/CFG-M15-05/history',
      json(200, [
        {
          value: { value: 72, unit: 'HOURS' },
          effectiveFrom: '2026-10-19T01:12:00Z',
          effectiveTo: null,
          actor: { type: 'USER', id: 1, fullName: null, role: 'AC-01' },
          reason: 'Theo họp giao ban',
          isDefaultRestore: false,
        },
        {
          value: { value: 48, unit: 'HOURS' },
          effectiveFrom: '2026-10-16T00:00:00Z',
          effectiveTo: '2026-10-19T01:12:00Z',
          actor: { type: 'SCHEDULER', id: null, fullName: null, role: null },
          reason: null,
          isDefaultRestore: false,
        },
      ]),
    )
    render(<ParameterList canChange={false} />)

    fireEvent.click(await screen.findByRole('button', { name: 'CFG-M15-05' }))
    const panel = await screen.findByRole('dialog')
    expect(within(panel).queryByLabelText('Lý do')).not.toBeInTheDocument()
    const history = await within(panel).findByRole('list', { name: 'Lịch sử giá trị' })
    expect(history).toHaveTextContent('19/10/2026 08:12')
    expect(history).toHaveTextContent('72 giờ')
    expect(history).toHaveTextContent('Theo họp giao ban')
    expect(history).toHaveTextContent('Nạp ban đầu')

    fireEvent.change(within(panel).getByLabelText('Giá trị tại ngày'), { target: { value: '2026-10-17' } })
    await waitFor(() =>
      expect(
        api.fetchMock.mock.calls.some(([url]) =>
          decodeURIComponent(String(url)).includes('asOf=2026-10-17T23:59:59.999+07:00'),
        ),
      ).toBe(true),
    )
  })
})

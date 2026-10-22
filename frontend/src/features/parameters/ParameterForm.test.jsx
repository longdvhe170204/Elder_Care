import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { ParameterForm } from './ParameterForm'

// 000 T081: form đổi giá trị tham số (US4 #2, #5, #7, #9; FR-014 lý do bắt buộc, FR-021 412 hiện giá trị mới).

function parameter(overrides = {}) {
  return {
    code: 'CFG-M15-05',
    description: 'Mốc nhắc người duyệt',
    type: 'DURATION',
    unit: 'HOURS',
    constraints: { min: 1 },
    defaultValue: { value: 48, unit: 'HOURS' },
    currentValue: { value: 48, unit: 'HOURS' },
    usedIn: 'BR-M15-09',
    version: 3,
    changedAt: null,
    ...overrides,
  }
}

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

function type(el, value) {
  fireEvent.change(el, { target: { value } })
}

function body(call) {
  return JSON.parse(call[1].body)
}

describe('ParameterForm', () => {
  it('US4_2_thoiLuong_guiGiaTriCoDonVi_vaIfMatch', async () => {
    const onSaved = vi.fn()
    api.on('POST', '/parameters/CFG-M15-05/change-value', () =>
      json(200, parameter({ currentValue: { value: 72, unit: 'HOURS' }, version: 4 }), { ETag: '"4"' }),
    )
    render(<ParameterForm parameter={parameter()} etag={'"3"'} onSaved={onSaved} onStale={vi.fn()} />)

    type(screen.getByLabelText('Giá trị mới (giờ)'), '72')
    type(screen.getByLabelText('Lý do'), 'Theo họp giao ban')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu giá trị' }))

    await waitFor(() => expect(onSaved).toHaveBeenCalled())
    const [call] = api.calls('POST', '/parameters/CFG-M15-05/change-value')
    expect(call[1].headers['If-Match']).toBe('"3"')
    expect(body(call)).toEqual({ value: { value: 72, unit: 'HOURS' }, reason: 'Theo họp giao ban' })
    expect(onSaved).toHaveBeenCalledWith(expect.objectContaining({ version: 4 }), '"4"')
    expect(await screen.findByText('Đã lưu giá trị mới.')).toBeInTheDocument()
  })

  it('FR_014_lyDoTrong_baoTaiO_khongGoiApi', () => {
    render(<ParameterForm parameter={parameter()} etag={'"3"'} onSaved={vi.fn()} onStale={vi.fn()} />)
    type(screen.getByLabelText('Giá trị mới (giờ)'), '72')
    type(screen.getByLabelText('Lý do'), '   ')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu giá trị' }))

    expect(screen.getByText('Nhập lý do đổi giá trị')).toBeInTheDocument()
    expect(api.fetchMock).not.toHaveBeenCalled()
  })

  it('giaTriTrong_baoTaiO_khongGoiApi', () => {
    render(<ParameterForm parameter={parameter()} etag={'"3"'} onSaved={vi.fn()} onStale={vi.fn()} />)
    type(screen.getByLabelText('Giá trị mới (giờ)'), '')
    type(screen.getByLabelText('Lý do'), 'x')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu giá trị' }))

    expect(screen.getByText('Nhập giá trị mới')).toBeInTheDocument()
    expect(api.fetchMock).not.toHaveBeenCalled()
  })

  it('US4_7_khoiPhucMacDinh_guiRestoreDefault_khoaOGiaTri', async () => {
    api.on('POST', '/parameters/CFG-M15-05/change-value', () => json(200, parameter({ version: 4 }), { ETag: '"4"' }))
    render(<ParameterForm parameter={parameter()} etag={'"3"'} onSaved={vi.fn()} onStale={vi.fn()} />)

    fireEvent.click(screen.getByRole('checkbox', { name: 'Khôi phục mặc định (48 giờ)' }))
    expect(screen.getByLabelText('Giá trị mới (giờ)')).toBeDisabled()
    type(screen.getByLabelText('Lý do'), 'Về mặc định')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu giá trị' }))

    await waitFor(() => expect(api.calls('POST', '/parameters/CFG-M15-05/change-value')).toHaveLength(1))
    expect(body(api.calls('POST', '/parameters/CFG-M15-05/change-value')[0])).toEqual({
      restoreDefault: true,
      reason: 'Về mặc định',
    })
  })

  it('US4_9_412_hienGiaTriMoi_vaNutTaiLai', async () => {
    const onStale = vi.fn()
    api.on('POST', '/parameters/CFG-M15-05/change-value', () =>
      problem(412, 'VERSION_MISMATCH', {
        currentValue: parameter({ currentValue: { value: 60, unit: 'HOURS' }, version: 5 }),
      }),
    )
    render(<ParameterForm parameter={parameter()} etag={'"3"'} onSaved={vi.fn()} onStale={onStale} />)
    type(screen.getByLabelText('Giá trị mới (giờ)'), '72')
    type(screen.getByLabelText('Lý do'), 'x')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu giá trị' }))

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Dữ liệu đã thay đổi')
    expect(alert).toHaveTextContent('Giá trị hiện hành mới: 60 giờ')
    fireEvent.click(within(alert).getByRole('button', { name: 'Tải lại' }))
    expect(onStale).toHaveBeenCalled()
  })

  it('US4_5_422_baoTaiOGiaTri', async () => {
    api.on('POST', '/parameters/CFG-M15-05/change-value', () =>
      problem(422, 'INVALID_VALUE', { detail: 'Phải lớn hơn CFG-M15-05' }),
    )
    render(<ParameterForm parameter={parameter()} etag={'"3"'} onSaved={vi.fn()} onStale={vi.fn()} />)
    type(screen.getByLabelText('Giá trị mới (giờ)'), '0')
    type(screen.getByLabelText('Lý do'), 'x')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu giá trị' }))

    expect(await screen.findByText('Phải lớn hơn CFG-M15-05')).toBeInTheDocument()
    expect(screen.getByLabelText('Giá trị mới (giờ)')).toHaveAttribute('aria-invalid', 'true')
  })

  it('soNguyen_vaGioTrongNgay_guiDungKieu', async () => {
    api.on('POST', '/parameters/CFG-M04-01/change-value', () => json(200, parameter(), { ETag: '"4"' }))
    const { unmount } = render(
      <ParameterForm
        parameter={parameter({ code: 'CFG-M04-01', type: 'TIME_OF_DAY', unit: null, currentValue: '06:00' })}
        etag={'"3"'}
        onSaved={vi.fn()}
        onStale={vi.fn()}
      />,
    )
    type(screen.getByLabelText('Giá trị mới'), '06:30')
    type(screen.getByLabelText('Lý do'), 'x')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu giá trị' }))
    await waitFor(() => expect(api.calls('POST', '/parameters/CFG-M04-01/change-value')).toHaveLength(1))
    expect(body(api.calls('POST', '/parameters/CFG-M04-01/change-value')[0]).value).toBe('06:30')
    unmount()

    api.on('POST', '/parameters/CFG-M12-06/change-value', () => json(200, parameter(), { ETag: '"4"' }))
    render(
      <ParameterForm
        parameter={parameter({ code: 'CFG-M12-06', type: 'INTEGER', unit: 'TIMES', currentValue: 3 })}
        etag={'"3"'}
        onSaved={vi.fn()}
        onStale={vi.fn()}
      />,
    )
    type(screen.getByLabelText('Giá trị mới (lần)'), '4')
    type(screen.getByLabelText('Lý do'), 'x')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu giá trị' }))
    await waitFor(() => expect(api.calls('POST', '/parameters/CFG-M12-06/change-value')).toHaveLength(1))
    expect(body(api.calls('POST', '/parameters/CFG-M12-06/change-value')[0]).value).toBe(4)
  })

  it('CFG_M02_05_bangDong_suaO_themDong_xoaDong_giuKieu', async () => {
    const table = {
      rows: [
        { residencyType: 'LONG_TERM', fromDay: 1, toDay: null, feeRatePercent: 100 },
        { residencyType: 'LONG_TERM', fromDay: 8, toDay: 30, feeRatePercent: 70 },
      ],
    }
    api.on('POST', '/parameters/CFG-M02-05/change-value', () => json(200, parameter(), { ETag: '"4"' }))
    render(
      <ParameterForm
        parameter={parameter({
          code: 'CFG-M02-05',
          type: 'TABLE',
          unit: null,
          currentValue: table,
          defaultValue: table,
        })}
        etag={'"3"'}
        onSaved={vi.fn()}
        onStale={vi.fn()}
      />,
    )

    type(screen.getByLabelText('rows dòng 2 feeRatePercent'), '60')
    fireEvent.click(screen.getByRole('button', { name: 'Thêm dòng rows' }))
    type(screen.getByLabelText('rows dòng 3 residencyType'), 'SHORT_TERM')
    type(screen.getByLabelText('rows dòng 3 fromDay'), '1')
    fireEvent.click(screen.getByRole('button', { name: 'Xóa dòng 1 rows' }))
    type(screen.getByLabelText('Lý do'), 'Đổi chính sách')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu giá trị' }))

    await waitFor(() => expect(api.calls('POST', '/parameters/CFG-M02-05/change-value')).toHaveLength(1))
    expect(body(api.calls('POST', '/parameters/CFG-M02-05/change-value')[0]).value).toEqual({
      rows: [
        { residencyType: 'LONG_TERM', fromDay: 8, toDay: 30, feeRatePercent: 60 },
        { residencyType: 'SHORT_TERM', fromDay: 1, toDay: null, feeRatePercent: null },
      ],
    })
  })

  it('bangDoiTuong_coThoiLuong_suaGiaTri', async () => {
    const value = { count: 3, window: { value: 24, unit: 'HOURS' } }
    api.on('POST', '/parameters/CFG-M05-02/change-value', () => json(200, parameter(), { ETag: '"4"' }))
    render(
      <ParameterForm
        parameter={parameter({
          code: 'CFG-M05-02',
          type: 'TABLE',
          unit: null,
          currentValue: value,
          defaultValue: value,
        })}
        etag={'"3"'}
        onSaved={vi.fn()}
        onStale={vi.fn()}
      />,
    )
    type(screen.getByLabelText('count'), '4')
    type(screen.getByLabelText('window (giờ)'), '12')
    type(screen.getByLabelText('Lý do'), 'x')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu giá trị' }))

    await waitFor(() => expect(api.calls('POST', '/parameters/CFG-M05-02/change-value')).toHaveLength(1))
    expect(body(api.calls('POST', '/parameters/CFG-M05-02/change-value')[0]).value).toEqual({
      count: 4,
      window: { value: 12, unit: 'HOURS' },
    })
  })

  it('CFG_M11_11_chuaCoGiaTri_hienCacOThongTinVien', async () => {
    // 007 T104: dạng giá trị thông tin viện do 000 chốt (common.document.FacilityInfo); chưa có giá trị vẫn nhập được
    api.on('POST', '/parameters/CFG-M11-11/change-value', () => json(200, parameter(), { ETag: '"1"' }))
    render(
      <ParameterForm
        parameter={parameter({ code: 'CFG-M11-11', type: 'TABLE', unit: null, currentValue: null, defaultValue: null })}
        etag={'"0"'}
        onSaved={vi.fn()}
        onStale={vi.fn()}
      />,
    )

    type(screen.getByLabelText('name'), 'Viện dưỡng lão Bình An')
    type(screen.getByLabelText('phone'), '024 3999 8888')
    type(screen.getByLabelText('Lý do'), 'Nhập thông tin viện')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu giá trị' }))

    await waitFor(() => expect(api.calls('POST', '/parameters/CFG-M11-11/change-value')).toHaveLength(1))
    expect(body(api.calls('POST', '/parameters/CFG-M11-11/change-value')[0]).value).toEqual({
      name: 'Viện dưỡng lão Bình An',
      address: null,
      taxCode: null,
      phone: '024 3999 8888',
    })
  })
})

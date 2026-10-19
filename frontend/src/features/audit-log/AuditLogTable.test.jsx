import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { AuditLogTable } from './AuditLogTable'

// 000 T073: màn tra cứu nhật ký toàn viện (FR-049, NFR-10, NFR-11; hợp đồng GET /audit-logs).

function entry(overrides = {}) {
  return {
    id: '1',
    occurredAt: '2026-10-19T02:30:00Z',
    deviceTime: null,
    actor: { type: 'USER', id: 20, fullName: null, role: 'AC-09' },
    action: 'SAMPLE_PROFILE_UPDATED',
    entity: { type: 'M15.SAMPLE_PROFILE', id: '7' },
    dataBefore: { address: '12 Lê Lợi', allergies: { changed: true } },
    dataAfter: { address: '34 Trần Phú', allergies: { changed: true } },
    reason: null,
    reasonHidden: true,
    basis: null,
    details: null,
    ...overrides,
  }
}

function json(status, body, type = 'application/json') {
  return Promise.resolve(new Response(JSON.stringify(body), { status, headers: { 'Content-Type': type } }))
}

let fetchMock

beforeEach(() => {
  fetchMock = vi.fn()
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

function lastUrl() {
  return new URL(fetchMock.mock.calls.at(-1)[0], 'http://localhost')
}

describe('AuditLogTable', () => {
  it('dangTai_hienTrangThaiDangTai', async () => {
    fetchMock.mockReturnValue(new Promise(() => {}))
    render(<AuditLogTable />)
    expect(await screen.findByText('Đang tải nhật ký')).toBeInTheDocument()
    expect(lastUrl().pathname).toBe('/api/v1/audit-logs')
  })

  it('NFR_11_hienDongNhatKy_ngayGioVietNam_truongBiChe_lyDoBiAn', async () => {
    fetchMock.mockReturnValue(json(200, { items: [entry()], nextCursor: null }))
    render(<AuditLogTable />)

    const row = (await screen.findAllByRole('row'))[1]
    expect(row).toHaveTextContent('19/10/2026 09:30')
    expect(row).toHaveTextContent('Tài khoản 20')
    expect(row).toHaveTextContent('Hành chính')
    expect(row).toHaveTextContent('SAMPLE_PROFILE_UPDATED')
    expect(row).toHaveTextContent('M15.SAMPLE_PROFILE 7')
    expect(row).toHaveTextContent('address: 12 Lê Lợi → 34 Trần Phú')
    expect(row).toHaveTextContent('allergies: Đã thay đổi')
    expect(row).toHaveTextContent('Đã ẩn vì có trường không được xem')
  })

  it('US3_12_hienThoiDiemTrenThietBi_vaBoLapLich', async () => {
    fetchMock.mockReturnValue(
      json(200, {
        items: [
          entry({
            deviceTime: '2026-10-19T01:10:00Z',
            actor: { type: 'SCHEDULER', id: null, fullName: null, role: 'AC-11' },
            dataBefore: null,
            dataAfter: null,
            reason: 'Áp dụng yêu cầu đến hạn',
            reasonHidden: false,
          }),
        ],
        nextCursor: null,
      }),
    )
    render(<AuditLogTable />)

    const row = (await screen.findAllByRole('row'))[1]
    expect(row).toHaveTextContent('Trên thiết bị: 19/10/2026 08:10')
    expect(row).toHaveTextContent('Bộ lập lịch hệ thống')
    expect(row).toHaveTextContent('Không đổi dữ liệu')
    expect(row).toHaveTextContent('Áp dụng yêu cầu đến hạn')
  })

  it('FR_049_locTheoDoiTuongNguoiVaKhoangNgay_guiDungThamSo', async () => {
    fetchMock.mockReturnValue(json(200, { items: [], nextCursor: null }))
    render(<AuditLogTable />)
    await screen.findByText('Không có bản ghi nhật ký khớp bộ lọc')

    fireEvent.change(screen.getByLabelText('Loại đối tượng'), { target: { value: 'M15.SAMPLE_PROFILE' } })
    fireEvent.change(screen.getByLabelText('Mã đối tượng'), { target: { value: '7' } })
    fireEvent.change(screen.getByLabelText('Mã người thực hiện'), { target: { value: '20' } })
    fireEvent.change(screen.getByLabelText('Từ ngày'), { target: { value: '2026-10-01' } })
    fireEvent.change(screen.getByLabelText('Đến ngày'), { target: { value: '2026-10-19' } })
    fireEvent.click(screen.getByRole('button', { name: 'Tra cứu' }))

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))
    const params = lastUrl().searchParams
    expect(params.get('entityType')).toBe('M15.SAMPLE_PROFILE')
    expect(params.get('entityId')).toBe('7')
    expect(params.get('actorId')).toBe('20')
    expect(params.get('from')).toBe('2026-10-01T00:00:00+07:00')
    expect(params.get('to')).toBe('2026-10-19T23:59:59.999+07:00')
  })

  it('maNguoiThucHienKhongPhaiSo_baoTaiO_khongGoiApi', async () => {
    fetchMock.mockReturnValue(json(200, { items: [], nextCursor: null }))
    render(<AuditLogTable />)
    await screen.findByText('Không có bản ghi nhật ký khớp bộ lọc')

    fireEvent.change(screen.getByLabelText('Mã người thực hiện'), { target: { value: 'abc' } })
    fireEvent.click(screen.getByRole('button', { name: 'Tra cứu' }))

    expect(screen.getByText('Nhập mã số tài khoản')).toBeInTheDocument()
    expect(screen.getByLabelText('Mã người thực hiện')).toHaveAttribute('aria-invalid', 'true')
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  it('xemThem_guiCursor_noiThemDong', async () => {
    fetchMock
      .mockReturnValueOnce(json(200, { items: [entry({ id: '2' })], nextCursor: 'c-1' }))
      .mockReturnValueOnce(json(200, { items: [entry({ id: '1', action: 'SAMPLE_OLDER' })], nextCursor: null }))
    render(<AuditLogTable />)

    fireEvent.click(await screen.findByRole('button', { name: 'Xem thêm' }))

    expect(await screen.findByText('SAMPLE_OLDER')).toBeInTheDocument()
    expect(lastUrl().searchParams.get('cursor')).toBe('c-1')
    expect(screen.getAllByRole('row')).toHaveLength(3)
    expect(screen.queryByRole('button', { name: 'Xem thêm' })).not.toBeInTheDocument()
  })

  it('FR_049_khongPhaiQuanLyVien_403_hienKhongCoQuyen', async () => {
    fetchMock.mockReturnValue(
      json(
        403,
        { type: 'about:blank', title: 'Không có quyền', status: 403, errorCode: 'PERMISSION_DENIED' },
        'application/problem+json',
      ),
    )
    render(<AuditLogTable />)

    expect(await screen.findByText('Bạn không có quyền xem nhật ký toàn viện')).toBeInTheDocument()
    expect(screen.getByText(/xem ở màn của từng đối tượng/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Tra cứu' })).not.toBeInTheDocument()
  })

  it('loiTai_hienLoiVaThuLai', async () => {
    fetchMock
      .mockReturnValueOnce(
        json(500, { title: 'Lỗi', status: 500, errorCode: 'INTERNAL_SERVER_ERROR' }, 'application/problem+json'),
      )
      .mockReturnValueOnce(json(200, { items: [entry()], nextCursor: null }))
    render(<AuditLogTable />)

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Không tải được nhật ký')
    fireEvent.click(within(alert).getByRole('button', { name: 'Thử lại' }))
    expect(await screen.findByText('SAMPLE_PROFILE_UPDATED')).toBeInTheDocument()
  })
})

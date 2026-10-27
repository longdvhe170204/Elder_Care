import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { PendingReviewPage } from './PendingReviewPage'

// 002 T059 (US4 #14; FR-041c; hợp đồng /pending-review-records): bản ghi ngoại tuyến chờ người dùng quyết định.

function record(overrides = {}) {
  return {
    id: 31,
    recordType: 'M07.KET_QUA_LIEU',
    content: { resultType: 'ADMINISTERED', note: 'Uống hết' },
    recorderId: 12,
    recorderName: '[Họ tên NV 1]',
    deviceTime: '2026-10-27T00:10:00Z',
    syncedAt: '2026-10-28T02:00:00Z',
    lastConnectedAt: '2026-10-26T23:00:00Z',
    pendingReason: 'EXCEEDS_CFG_M15_08',
    floorAreaId: 3,
    floorAreaName: 'Tầng 2',
    status: 'PENDING_REVIEW',
    decidedBy: null,
    decidedAt: null,
    rejectionReason: null,
    reference: null,
    version: 0,
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

const isList = (u) => u.pathname === '/api/v1/pending-review-records'

async function openRow(items = [record()]) {
  api.on('GET', isList, json(200, { items, nextCursor: null }))
  render(<PendingReviewPage />)
  fireEvent.click(await screen.findByRole('button', { name: 'Kết quả liều thuốc' }))
  return screen.findByRole('dialog')
}

describe('PendingReviewPage', () => {
  it('FR_041c_bang_hienLoai_nguoiGhi_tang_lyDoCho', async () => {
    api.on('GET', isList, json(200, { items: [record()], nextCursor: null }))
    render(<PendingReviewPage />)

    expect(await screen.findByRole('button', { name: 'Kết quả liều thuốc' })).toBeInTheDocument()
    expect(screen.getByText('[Họ tên NV 1]')).toBeInTheDocument()
    expect(screen.getByText('Tầng 2')).toBeInTheDocument()
    expect(screen.getByText('Quá CFG-M15-08 kể từ lần kết nối cuối')).toBeInTheDocument()
  })

  it('FR_041c_khongCoBanGhi_hienTrong', async () => {
    api.on('GET', isList, json(200, { items: [], nextCursor: null }))
    render(<PendingReviewPage />)

    expect(await screen.findByText('Không có bản ghi nào chờ bạn xem lại')).toBeInTheDocument()
  })

  it('FR_041c_loaiChuaCoNhan_hienMaLoai_khongGanTang', async () => {
    api.on('GET', isList, json(200, { items: [record({ recordType: 'M04.KET_QUA', floorAreaId: null, floorAreaName: null })], nextCursor: null }))
    render(<PendingReviewPage />)

    expect(await screen.findByRole('button', { name: 'M04.KET_QUA' })).toBeInTheDocument()
    expect(screen.getByText('Không gắn tầng')).toBeInTheDocument()
  })

  it('FR_041c_chapNhan_guiIfMatchTheoVersion_boDongKhoiBang', async () => {
    api.on('POST', '/pending-review-records/31/accept', () =>
      json(200, record({ status: 'ACCEPTED', version: 1, reference: 'dose:5' }), { ETag: '"1"' }),
    )
    const panel = await openRow()

    expect(within(panel).getByText('Uống hết')).toBeInTheDocument()
    fireEvent.click(within(panel).getByRole('button', { name: 'Chấp nhận bản ghi' }))

    await waitFor(() => expect(api.calls('POST', '/pending-review-records/31/accept')).toHaveLength(1))
    expect(api.calls('POST', '/pending-review-records/31/accept')[0][1].headers['If-Match']).toBe('"0"')
    expect(await screen.findByText('Đã chấp nhận bản ghi. Bản ghi có hiệu lực từ lúc chấp nhận.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Kết quả liều thuốc' })).not.toBeInTheDocument()
  })

  it('FR_041c_tuChoiKhongLyDo_baoTaiO_khongGoiApi_coLyDo_guiLyDo', async () => {
    api.on('POST', '/pending-review-records/31/reject', () =>
      json(200, record({ status: 'REJECTED', version: 1, rejectionReason: 'Không khớp' }), { ETag: '"1"' }),
    )
    const panel = await openRow()

    fireEvent.click(within(panel).getByRole('button', { name: 'Từ chối' }))
    fireEvent.click(within(panel).getByRole('button', { name: 'Xác nhận từ chối' }))
    expect(within(panel).getByText('Nhập lý do từ chối')).toBeInTheDocument()
    expect(api.calls('POST', '/pending-review-records/31/reject')).toHaveLength(0)

    fireEvent.change(within(panel).getByLabelText('Lý do từ chối'), { target: { value: 'Không khớp' } })
    fireEvent.click(within(panel).getByRole('button', { name: 'Xác nhận từ chối' }))
    await waitFor(() => expect(api.calls('POST', '/pending-review-records/31/reject')).toHaveLength(1))
    const [, init] = api.calls('POST', '/pending-review-records/31/reject')[0]
    expect(init.headers['If-Match']).toBe('"0"')
    expect(JSON.parse(init.body)).toEqual({ reason: 'Không khớp' })
    expect(await screen.findByText('Đã từ chối bản ghi. Bản ghi được lưu nhưng không có hiệu lực.')).toBeInTheDocument()
  })

  it('FR_041c_nguoiKhacDaQuyetDinh_409_baoVaBoDong', async () => {
    api.on('POST', '/pending-review-records/31/accept', () => problem(409, 'ALREADY_PROCESSED'))
    const panel = await openRow()

    fireEvent.click(within(panel).getByRole('button', { name: 'Chấp nhận bản ghi' }))

    expect(await screen.findByText('Bản ghi đã được người khác quyết định.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Kết quả liều thuốc' })).not.toBeInTheDocument()
  })

  it('FR_041c_coTrangSau_xemThem', async () => {
    api.on('GET', (u) => isList(u) && !u.searchParams.get('cursor'), json(200, { items: [record()], nextCursor: '31' }))
    api.on(
      'GET',
      (u) => isList(u) && u.searchParams.get('cursor') === '31',
      json(200, { items: [record({ id: 32, recorderName: '[Họ tên NV 2]' })], nextCursor: null }),
    )
    render(<PendingReviewPage />)

    fireEvent.click(await screen.findByRole('button', { name: 'Xem thêm' }))
    expect(await screen.findByText('[Họ tên NV 2]')).toBeInTheDocument()
    expect(screen.getByText('[Họ tên NV 1]')).toBeInTheDocument()
  })
})

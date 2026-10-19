import { useState } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { CorrectionBadge } from './CorrectionBadge'
import { CorrectionHistorySheet } from './CorrectionHistorySheet'

// 000 T106: component đính chính dùng chung (FR-028, FR-029, SC-007; hợp đồng api-000 /corrections).

const RECORD = { recordType: 'M15.SAMPLE_RECORD', recordId: 7 }
const FIELDS = [{ name: 'note', label: 'Ghi chú' }]
const URL = '/api/v1/corrections/M15.SAMPLE_RECORD/7'

const actor = { type: 'USER', id: 3, fullName: 'Nguyễn Văn A', role: 'AC-04' }

function correction(sequenceNo, overrides = {}) {
  return {
    id: 100 + sequenceNo,
    sequenceNo,
    type: 'AMEND_CONTENT',
    correctedContent: { note: `Nội dung lần ${sequenceNo}` },
    reason: `Lý do lần ${sequenceNo}`,
    actor,
    creationMethod: 'A',
    approvalRequestId: null,
    recordedAt: '2026-10-19T01:30:00Z',
    ...overrides,
  }
}

function chain(overrides = {}) {
  return {
    recordType: 'M15.SAMPLE_RECORD',
    originalRecordId: 7,
    corrected: false,
    voided: false,
    originalContent: { note: 'Ghi chú ban đầu' },
    currentContent: { note: 'Ghi chú ban đầu' },
    corrections: [],
    ...overrides,
  }
}

function json(status, body, type = 'application/json') {
  return Promise.resolve(new Response(JSON.stringify(body), { status, headers: { 'Content-Type': type } }))
}

function problem(status, errorCode, extra = {}) {
  return json(status, { type: 'about:blank', title: 'Lỗi', status, errorCode, ...extra }, 'application/problem+json')
}

let fetchMock

beforeEach(() => {
  fetchMock = vi.fn()
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

function renderSheet(props = {}) {
  return render(<CorrectionHistorySheet {...RECORD} fields={FIELDS} open onOpenChange={() => {}} {...props} />)
}

function type(input, text) {
  fireEvent.change(input, { target: { value: text } })
}

function postCalls() {
  return fetchMock.mock.calls.filter(([, init]) => init?.method === 'POST')
}

describe('CorrectionBadge', () => {
  it('FR_028_banGhiDaDinhChinh_hienNhan', () => {
    render(<CorrectionBadge corrected voided={false} />)
    expect(screen.getByText('Đã đính chính')).toBeInTheDocument()
  })

  it('FR_029_banGhiDaHuyGhiNhan_hienNhanHuy', () => {
    render(<CorrectionBadge corrected voided />)
    expect(screen.getByText('Đã hủy ghi nhận')).toBeInTheDocument()
    expect(screen.queryByText('Đã đính chính')).not.toBeInTheDocument()
  })

  it('banGhiChuaDinhChinh_khongHienNhan', () => {
    const { container } = render(<CorrectionBadge corrected={false} voided={false} />)
    expect(container).toBeEmptyDOMElement()
  })
})

describe('CorrectionHistorySheet – xem', () => {
  it('dangTai_hienTrangThaiDangTai', async () => {
    fetchMock.mockReturnValue(new Promise(() => {}))
    renderSheet()
    expect(await screen.findByText('Đang tải các lần đính chính')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledWith(URL, expect.objectContaining({ method: 'GET' }))
  })

  it('loiTai_hienLoiVaThuLai', async () => {
    fetchMock.mockReturnValueOnce(problem(500, 'INTERNAL_SERVER_ERROR'))
    fetchMock.mockReturnValueOnce(json(200, chain()))
    renderSheet()
    expect(await screen.findByText('Không tải được các lần đính chính')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }))
    expect(await screen.findByText('Chưa có lần đính chính nào.')).toBeInTheDocument()
  })

  it('FR_028_hienGiaTriHienHanh_cacLanTheoThoiGianTang_vaNoiDungGoc', async () => {
    fetchMock.mockReturnValue(
      json(
        200,
        chain({
          corrected: true,
          currentContent: { note: 'Nội dung lần 2' },
          // Thứ tự nhận về đảo để kiểm component xếp theo số thứ tự tăng dần
          corrections: [correction(2, { creationMethod: 'B' }), correction(1)],
        }),
      ),
    )
    renderSheet()

    const current = await screen.findByRole('region', { name: 'Giá trị hiện hành' })
    expect(within(current).getByText('Nội dung lần 2')).toBeInTheDocument()
    expect(within(current).getByText('Ghi chú')).toBeInTheDocument()

    const items = within(screen.getByRole('region', { name: 'Các lần đính chính' })).getAllByRole('listitem')
    expect(items).toHaveLength(2)
    expect(items[0]).toHaveTextContent('Lần 1')
    expect(items[0]).toHaveTextContent('Sửa nội dung')
    expect(items[0]).toHaveTextContent('Lý do lần 1')
    expect(items[0]).toHaveTextContent('Ghi bởi Nguyễn Văn A, 19/10/2026 08:30, cách A')
    expect(items[1]).toHaveTextContent('Lần 2')
    expect(items[1]).toHaveTextContent('cách B')

    const original = screen.getByRole('region', { name: 'Nội dung gốc' })
    expect(within(original).getByText('Ghi chú ban đầu')).toBeInTheDocument()
    expect(screen.getByText('Đã đính chính')).toBeInTheDocument()
  })

  it('canCorrectMacDinhFalse_khongCoNutDinhChinh', async () => {
    fetchMock.mockReturnValue(json(200, chain()))
    renderSheet()
    await screen.findByText('Chưa có lần đính chính nào.')
    expect(screen.queryByRole('button', { name: 'Lưu đính chính' })).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Lý do')).not.toBeInTheDocument()
  })

  it('FR_029_banGhiDaHuy_khongCoFormDuCoQuyen', async () => {
    fetchMock.mockReturnValue(
      json(
        200,
        chain({
          corrected: true,
          voided: true,
          corrections: [correction(1, { type: 'VOID', correctedContent: null })],
        }),
      ),
    )
    renderSheet({ canCorrect: true })
    expect(await screen.findByText('Bản ghi đã hủy ghi nhận, không đính chính thêm được.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Lưu đính chính' })).not.toBeInTheDocument()
    expect(screen.getAllByText('Đã hủy ghi nhận').length).toBeGreaterThan(0)
    const items = within(screen.getByRole('region', { name: 'Các lần đính chính' })).getAllByRole('listitem')
    expect(items[0]).toHaveTextContent('Lần 1, Hủy ghi nhận')
  })
})

describe('CorrectionHistorySheet – tạo đính chính', () => {
  async function openForm() {
    renderSheet({ canCorrect: true })
    await screen.findByText('Chưa có lần đính chính nào.')
  }

  it('FR_024_lyDoTrongHoacChiKhoangTrang_baoTaiO_khongGoiApi', async () => {
    fetchMock.mockReturnValue(json(200, chain()))
    await openForm()
    type(screen.getByLabelText('Ghi chú'), 'Nội dung đúng')
    type(screen.getByLabelText('Lý do'), '   ')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu đính chính' }))

    expect(screen.getByText('Nhập lý do đính chính')).toBeInTheDocument()
    expect(screen.getByLabelText('Lý do')).toHaveAttribute('aria-invalid', 'true')
    expect(postCalls()).toHaveLength(0)
  })

  it('201_suaNoiDung_guiDungHopDong_taiLaiChuoi', async () => {
    const after = chain({
      corrected: true,
      currentContent: { note: 'Nội dung đúng' },
      corrections: [correction(1, { correctedContent: { note: 'Nội dung đúng' }, reason: 'Ghi nhầm' })],
    })
    fetchMock
      .mockReturnValueOnce(json(200, chain()))
      .mockReturnValueOnce(json(201, after.corrections[0]))
      .mockReturnValueOnce(json(200, after))
    const onChanged = vi.fn()
    renderSheet({ canCorrect: true, onChanged })
    await screen.findByText('Chưa có lần đính chính nào.')

    type(screen.getByLabelText('Ghi chú'), 'Nội dung đúng')
    type(screen.getByLabelText('Lý do'), 'Ghi nhầm')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu đính chính' }))

    await waitFor(() => expect(postCalls()).toHaveLength(1))
    const [url, init] = postCalls()[0]
    expect(url).toBe(URL)
    expect(JSON.parse(init.body)).toEqual({
      type: 'AMEND_CONTENT',
      correctedContent: { note: 'Nội dung đúng' },
      reason: 'Ghi nhầm',
    })

    const current = await screen.findByRole('region', { name: 'Giá trị hiện hành' })
    await waitFor(() => expect(within(current).getByText('Nội dung đúng')).toBeInTheDocument())
    expect(screen.getByText('Đã lưu bản đính chính.')).toBeInTheDocument()
    expect(onChanged).toHaveBeenCalledWith(after)
  })

  it('FR_029_huyGhiNhan_khongGuiNoiDung_anOChoNoiDung', async () => {
    fetchMock
      .mockReturnValueOnce(json(200, chain()))
      .mockReturnValueOnce(json(201, correction(1, { type: 'VOID', correctedContent: null })))
      .mockReturnValueOnce(json(200, chain({ corrected: true, voided: true })))
    await openForm()

    fireEvent.click(screen.getByRole('button', { name: 'Hủy ghi nhận' }))
    expect(screen.queryByLabelText('Ghi chú')).not.toBeInTheDocument()
    type(screen.getByLabelText('Lý do'), 'Ghi cho sai người')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu đính chính' }))

    await waitFor(() => expect(postCalls()).toHaveLength(1))
    expect(JSON.parse(postCalls()[0][1].body)).toEqual({ type: 'VOID', reason: 'Ghi cho sai người' })
    expect(await screen.findByText('Bản ghi đã hủy ghi nhận, không đính chính thêm được.')).toBeInTheDocument()
  })

  it('202_cachC_hienDaGuiDuyet_khongDanhDauDinhChinh', async () => {
    fetchMock
      .mockReturnValueOnce(json(200, chain()))
      .mockReturnValueOnce(
        json(202, { id: 55, displayCode: 'YC-000055', type: 'M15.CORRECTION', status: 'PENDING_APPROVAL' }),
      )
    const onChanged = vi.fn()
    renderSheet({ canCorrect: true, onChanged })
    await screen.findByText('Chưa có lần đính chính nào.')

    type(screen.getByLabelText('Ghi chú'), 'Nội dung đúng')
    type(screen.getByLabelText('Lý do'), 'Ghi nhầm')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu đính chính' }))

    expect(await screen.findByText(/Đã gửi duyệt, mã yêu cầu YC-000055/)).toBeInTheDocument()
    expect(screen.queryByText('Đã đính chính')).not.toBeInTheDocument()
    expect(onChanged).not.toHaveBeenCalled()
  })

  it('409_hienDieuKienChuaThoa', async () => {
    fetchMock.mockReturnValueOnce(json(200, chain())).mockReturnValueOnce(
      problem(409, 'CONDITION_NOT_MET', {
        title: 'Conflict',
        detail: 'Điều kiện chưa thỏa',
        unmetConditions: [{ code: 'BR-M05-21', description: 'Sự cố khẩn cấp chỉ Bác sĩ được hủy ghi nhận' }],
      }),
    )
    await openForm()
    type(screen.getByLabelText('Lý do'), 'Ghi nhầm')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu đính chính' }))

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Chưa thực hiện được')
    expect(alert).toHaveTextContent('Điều kiện chưa thỏa')
    expect(alert).not.toHaveTextContent('Conflict')
    expect(alert).toHaveTextContent('Sự cố khẩn cấp chỉ Bác sĩ được hủy ghi nhận')
  })

  it('403_hienKhongDuocDinhChinh', async () => {
    fetchMock
      .mockReturnValueOnce(json(200, chain()))
      .mockReturnValueOnce(
        problem(403, 'CORRECTION_NOT_ALLOWED', {
          title: 'Forbidden',
          detail: 'Không được đính chính bản ghi này (FR-026)',
        }),
      )
    await openForm()
    type(screen.getByLabelText('Lý do'), 'Ghi nhầm')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu đính chính' }))

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Không có quyền thực hiện')
    expect(alert).toHaveTextContent('Không được đính chính bản ghi này')
    expect(alert).not.toHaveTextContent('Forbidden')
  })

  it('422_lyDo_baoTaiOLyDo', async () => {
    fetchMock
      .mockReturnValueOnce(json(200, chain()))
      .mockReturnValueOnce(
        problem(422, 'REASON_REQUIRED', { title: 'Thiếu lý do', detail: 'Lý do không được chỉ có khoảng trắng' }),
      )
    await openForm()
    type(screen.getByLabelText('Lý do'), 'x')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu đính chính' }))

    expect(await screen.findByText('Lý do không được chỉ có khoảng trắng')).toBeInTheDocument()
    expect(screen.getByLabelText('Lý do')).toHaveAttribute('aria-invalid', 'true')
  })

  it('422_noiDung_baoTaiONoiDung', async () => {
    fetchMock
      .mockReturnValueOnce(json(200, chain()))
      .mockReturnValueOnce(
        problem(422, 'INVALID_VALUE', { title: 'Giá trị không hợp lệ', detail: 'Nội dung đúng cần trường note' }),
      )
    await openForm()
    type(screen.getByLabelText('Lý do'), 'Ghi nhầm')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu đính chính' }))

    expect(await screen.findByText('Nội dung đúng cần trường note')).toBeInTheDocument()
    expect(screen.getByLabelText('Ghi chú')).toHaveAttribute('aria-invalid', 'true')
  })

  it('manChaReRenderSauKhiLuu_khongTaiLaiChuoiThem', async () => {
    const after = chain({ corrected: true, corrections: [correction(1)] })
    fetchMock
      .mockReturnValueOnce(json(200, chain()))
      .mockReturnValueOnce(json(201, after.corrections[0]))
      .mockReturnValue(json(200, after))
    // Màn của module truyền hàm viết tại chỗ và re-render khi nhận chuỗi mới (để cập nhật nhãn)
    function Screen() {
      const [current, setCurrent] = useState(null)
      return (
        <>
          <span>{current?.corrected ? 'nhãn đã đổi' : ''}</span>
          <CorrectionHistorySheet
            {...RECORD}
            fields={FIELDS}
            open
            onOpenChange={() => {}}
            canCorrect
            onChanged={(c) => setCurrent(c)}
          />
        </>
      )
    }
    render(<Screen />)
    await screen.findByText('Chưa có lần đính chính nào.')
    type(screen.getByLabelText('Lý do'), 'Ghi nhầm')
    fireEvent.click(screen.getByRole('button', { name: 'Lưu đính chính' }))

    expect(await screen.findByText('nhãn đã đổi')).toBeInTheDocument()
    await new Promise((resolve) => setTimeout(resolve, 50))
    const gets = fetchMock.mock.calls.filter(([, init]) => init?.method === 'GET')
    expect(gets).toHaveLength(2)
    expect(screen.getByText('Đã lưu bản đính chính.')).toBeInTheDocument()
  })
})

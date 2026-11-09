import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes } from '@/test/fetchRoutes'
import { TransferOutPanel } from './TransferOutPanel'

// 007 T105 (US5; FR-049, FR-050; Q-383, Q-384): đơn vị nhận, thời điểm, người đi cùng bắt buộc; phiếu nguyện vọng có yêu
// cầu hỏi trước thì phải chọn đã hỏi hay chưa, chưa hỏi được (sự cố không Khẩn cấp) thì ghi lý do.

let api

const INFO = { mustAskFirst: true, isEmergency: false, primaryContact: { fullName: 'Lê Văn An', phoneNumber: '0900' } }

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/incidents/11/transfer-out/info', json(200, INFO))
  api.on('POST', '/incidents/11/transfer-out', json(201, { id: 77 }))
})

afterEach(() => vi.unstubAllGlobals())

function fill() {
  fireEvent.change(screen.getByLabelText('Đơn vị nhận'), { target: { value: 'Bệnh viện A' } })
  fireEvent.change(screen.getByLabelText('Thời điểm chuyển'), { target: { value: '2026-11-09T10:30' } })
  fireEvent.change(screen.getByLabelText(/^Người đi cùng/), { target: { value: 'Trần Thu Hà, điều dưỡng' } })
}

describe('TransferOutPanel', () => {
  it('FR_049_thieuTruongBatBuoc_khongGui', async () => {
    render(<TransferOutPanel incidentId={11} onDone={() => {}} onCancel={() => {}} />)
    fireEvent.click(await screen.findByRole('button', { name: 'Xác nhận chuyển viện' }))
    expect(await screen.findByText('Nhập đơn vị nhận')).toBeTruthy()
    expect(screen.getByText('Nhập người đi cùng')).toBeTruthy()
    expect(api.calls('POST', '/incidents/11/transfer-out')).toHaveLength(0)
  })

  it('Q_383_chuaHoiDuoc_canLyDo_roiGuiKemKey', async () => {
    render(<TransferOutPanel incidentId={11} onDone={() => {}} onCancel={() => {}} />)
    await screen.findByText(/Lê Văn An/)
    fill()
    fireEvent.click(screen.getByRole('button', { name: 'Chưa hỏi được' }))
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận chuyển viện' }))
    expect(await screen.findByText('Nhập lý do')).toBeTruthy()
    fireEvent.change(screen.getByLabelText('Lý do chưa hỏi được'), { target: { value: 'Không gọi được' } })
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận chuyển viện' }))

    await waitFor(() => expect(api.calls('POST', '/incidents/11/transfer-out')).toHaveLength(1))
    const [, init] = api.calls('POST', '/incidents/11/transfer-out')[0]
    expect(new Headers(init.headers).get('Idempotency-Key')).toBeTruthy()
    expect(JSON.parse(init.body)).toMatchObject({
      transferUnit: 'Bệnh viện A',
      transferAt: '2026-11-09T10:30:00+07:00',
      companion: 'Trần Thu Hà, điều dưỡng',
      requiredContactReached: false,
      notAskedFirstReason: 'Không gọi được',
    })
    expect(await screen.findByRole('button', { name: 'Tải bản tóm tắt' })).toBeTruthy()
  })

  it('Q_384_suCoKhanCap_khongBatLyDo', async () => {
    api.on('GET', '/incidents/11/transfer-out/info', json(200, { ...INFO, isEmergency: true }))
    render(<TransferOutPanel incidentId={11} onDone={() => {}} onCancel={() => {}} />)
    await screen.findByText(/Lê Văn An/)
    fill()
    fireEvent.click(screen.getByRole('button', { name: 'Chưa hỏi được' }))
    expect(screen.queryByLabelText('Lý do chưa hỏi được')).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận chuyển viện' }))
    await waitFor(() => expect(api.calls('POST', '/incidents/11/transfer-out')).toHaveLength(1))
  })
})

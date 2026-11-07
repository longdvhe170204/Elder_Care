import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { CriticalCarePanel } from './CriticalCarePanel'

// 007 T046 (US11; FR-050a → FR-050d): ghi dấu, xác nhận lại nguyện vọng, thực hiện lựa chọn; ô bắt buộc báo tại ô,
// lỗi 409 hiện điều kiện chưa thỏa trong khung.

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

const ALERT = { id: 20, criticalFlagId: 9 }

describe('CriticalCarePanel', () => {
  it('chưa có cảnh báo: chỉ có form ghi dấu, thiếu ô thì báo tại ô và không gửi', () => {
    render(<CriticalCarePanel residentId={5} alert={null} />)
    fireEvent.click(screen.getByRole('button', { name: 'Ghi dấu nguy kịch' }))
    expect(screen.getByText('Chọn loại dấu')).toBeInTheDocument()
    expect(screen.getByText('Nhập nhận định')).toBeInTheDocument()
    expect(api.fetchMock).not.toHaveBeenCalled()
  })

  it('ghi dấu chính thức gửi loại và nhận định', async () => {
    api.on('POST', '/residents/5/critical-flags', json(201, { flagId: 9, alertId: 20 }))
    render(<CriticalCarePanel residentId={5} alert={null} />)
    fireEvent.click(screen.getByRole('button', { name: 'Dấu chính thức' }))
    fireEvent.change(screen.getByLabelText('Nhận định'), { target: { value: 'Suy hô hấp nặng' } })
    fireEvent.click(screen.getByRole('button', { name: 'Ghi dấu nguy kịch' }))
    await screen.findByText('Đã ghi dấu nguy kịch và tạo cảnh báo Khẩn cấp.')
    const [, init] = api.calls('POST', '/residents/5/critical-flags')[0]
    expect(JSON.parse(init.body)).toEqual({ type: 'OFFICIAL', assessment: 'Suy hô hấp nặng' })
    expect(init.headers['Idempotency-Key']).toBeTruthy()
  })

  it('có cảnh báo: xác nhận dấu tạm gọi đúng mã dấu', async () => {
    api.on('POST', '/critical-flags/9/confirm', json(200, {}))
    render(<CriticalCarePanel residentId={5} alert={ALERT} />)
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận dấu tạm' }))
    await screen.findByText('Đã xác nhận dấu.')
    expect(api.calls('POST', '/critical-flags/9/confirm')).toHaveLength(1)
  })

  it('gỡ dấu bắt buộc lý do', async () => {
    api.on('POST', '/critical-flags/9/remove', json(200, {}))
    render(<CriticalCarePanel residentId={5} alert={ALERT} />)
    fireEvent.click(screen.getByRole('button', { name: 'Gỡ dấu' }))
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận gỡ dấu' }))
    expect(screen.getByText('Nhập lý do gỡ dấu')).toBeInTheDocument()
    expect(api.calls('POST', '/critical-flags/9/remove')).toHaveLength(0)
    fireEvent.change(screen.getByLabelText('Lý do gỡ dấu'), { target: { value: 'Ổn định' } })
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận gỡ dấu' }))
    await waitFor(() => expect(api.calls('POST', '/critical-flags/9/remove')).toHaveLength(1))
  })

  it('ghi kết quả xác nhận lại gửi người liên hệ, cách và kết quả', async () => {
    api.on('POST', '/alerts/20/wish-reconfirmation', json(201, {}))
    render(<CriticalCarePanel residentId={5} alert={ALERT} />)
    fireEvent.change(screen.getByLabelText('Người đã liên hệ'), { target: { value: 'Trần Thị B' } })
    fireEvent.click(screen.getByRole('button', { name: 'Gọi điện' }))
    fireEvent.click(screen.getByRole('button', { name: 'Giữ nguyện vọng' }))
    fireEvent.click(screen.getByRole('button', { name: 'Ghi kết quả xác nhận lại' }))
    await screen.findByText('Đã ghi kết quả xác nhận lại.')
    const body = JSON.parse(api.calls('POST', '/alerts/20/wish-reconfirmation')[0][1].body)
    expect(body).toMatchObject({ contactedPerson: { fullName: 'Trần Thị B' }, channel: 'PHONE_CALL', result: 'KEPT' })
  })

  it('chuyển bệnh viện cần nơi đến và người đi cùng; 409 hiện điều kiện chưa thỏa', async () => {
    api.on(
      'POST',
      '/alerts/20/execute-choice',
      problem(409, 'CONDITION_NOT_MET', {
        detail: 'Chưa thực hiện được',
        unmetConditions: [{ code: 'RECONFIRM', description: 'Chưa xác nhận lại nguyện vọng' }],
      }),
    )
    render(<CriticalCarePanel residentId={5} alert={ALERT} />)
    fireEvent.click(screen.getByRole('button', { name: 'Chuyển bệnh viện' }))
    fireEvent.click(screen.getByRole('button', { name: 'Thực hiện lựa chọn' }))
    expect(screen.getByText('Nhập nơi chuyển đến')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Bệnh viện chuyển đến'), { target: { value: 'Bệnh viện X' } })
    fireEvent.change(screen.getByLabelText(/Người đi cùng/), { target: { value: 'Con gái' } })
    fireEvent.click(screen.getByRole('button', { name: 'Thực hiện lựa chọn' }))
    await screen.findByText('Chưa xác nhận lại nguyện vọng')
    const body = JSON.parse(api.calls('POST', '/alerts/20/execute-choice')[0][1].body)
    expect(body.transferOut).toMatchObject({ transferUnit: 'Bệnh viện X', companion: 'Con gái' })
  })
})

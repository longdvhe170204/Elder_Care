import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { Call115Panel } from './Call115Panel'
import { PressedAlertPanel } from './PressedAlertPanel'

// 007 T102 (tasks ghi GoiCapCuu.test.jsx; FR-047j, FR-047k): ghi lượt gọi trước khi quay số, kết quả một lần; thông tin
// đọc cho tổng đài theo quyền xem sức khỏe; nút "Đã tới nơi", "Gọi 115" theo lệnh được phép.

vi.mock('leaflet', () => ({ default: {} }))
vi.mock('leaflet/dist/leaflet.css', () => ({}))

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

const INFO = {
  facility: { name: 'Viện An Bình', address: '12 Lê Lợi', phone: '0281234567' },
  floor: 'Tầng 2',
  roomOrPlace: 'P11',
  resident: { fullName: 'Nguyễn Thị Lan', age: null, gender: 'FEMALE' },
  allergies: ['Penicillin'],
  chronicConditions: [],
  hiddenFields: [],
}

describe('Call115Panel', () => {
  it('ghi lượt gọi rồi mở trình quay số, sau đó chọn kết quả một lần', async () => {
    const dial = vi.fn()
    api.on('POST', '/alerts/20/call-115', json(201, { id: 7, pressedAt: '2026-11-06T03:00:00Z', result: null }))
    api.on('POST', '/call-115-attempts/7/result', json(200, {}))
    render(<Call115Panel subject="alerts" id={20} dial={dial} />)

    fireEvent.click(screen.getByRole('button', { name: 'Gọi 115' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Đã gọi được' }))
    await screen.findByText('Đã ghi: đã gọi được cấp cứu 115.')

    expect(dial).toHaveBeenCalledWith('tel:115')
    const [, press] = api.calls('POST', '/alerts/20/call-115')[0]
    expect(press.headers['Idempotency-Key']).toBeTruthy()
    const [, result] = api.calls('POST', '/call-115-attempts/7/result')[0]
    expect(JSON.parse(result.body)).toEqual({ result: 'CONNECTED' })
    expect(screen.queryByRole('button', { name: 'Không gọi được' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Gọi 115' })).toBeDisabled()
  })

  it('thông tin cho tổng đài có dị ứng khi được xem', async () => {
    api.on('GET', '/incidents/5/call-115-info', json(200, INFO))
    render(<Call115Panel subject="incidents" id={5} dial={vi.fn()} />)

    fireEvent.click(screen.getByRole('button', { name: 'Xem thông tin đọc cho tổng đài' }))

    expect(await screen.findByText('Viện An Bình, 12 Lê Lợi')).toBeInTheDocument()
    expect(screen.getByText('Tầng 2, P11')).toBeInTheDocument()
    expect(screen.getByText('Nguyễn Thị Lan, nữ')).toBeInTheDocument()
    expect(screen.getByText('Penicillin')).toBeInTheDocument()
    expect(screen.getByText('Chưa ghi nhận')).toBeInTheDocument()
  })

  it('người không được xem sức khỏe thấy dòng báo phần bị ẩn, không có dị ứng', async () => {
    api.on(
      'GET',
      '/alerts/20/call-115-info',
      json(200, { ...INFO, allergies: null, chronicConditions: null, hiddenFields: ['allergies', 'chronicConditions'] }),
    )
    render(<Call115Panel subject="alerts" id={20} dial={vi.fn()} />)

    fireEvent.click(screen.getByRole('button', { name: 'Xem thông tin đọc cho tổng đài' }))

    expect(await screen.findByText(/Bạn không được xem dị ứng, bệnh nền/)).toBeInTheDocument()
    expect(screen.queryByText('Penicillin')).not.toBeInTheDocument()
  })

  it('lỗi 409 khi chọn kết quả lần hai hiện trong khung', async () => {
    api.on('POST', '/alerts/20/call-115', json(201, { id: 7, pressedAt: '2026-11-06T03:00:00Z' }))
    api.on('POST', '/call-115-attempts/7/result', problem(409, 'ALREADY_PROCESSED', { detail: 'Đã ghi kết quả' }))
    render(<Call115Panel subject="alerts" id={20} dial={vi.fn()} />)

    fireEvent.click(screen.getByRole('button', { name: 'Gọi 115' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Không gọi được' }))

    expect(await screen.findByRole('alert')).toBeInTheDocument()
  })
})

describe('PressedAlertPanel', () => {
  const ALERT = {
    id: 20,
    origin: 'ASSIST_BUTTON',
    pressedBy: 'Trần Thu Hà',
    eventType: 'FALL',
    chosenPlace: 'RESTROOM',
    latestLocation: null,
    arrivedAt: null,
    allowedCommands: ['mark-arrived', 'call-115'],
  }

  it('hiện nguồn, người bấm, sự việc, nơi; ghi đã tới nơi', async () => {
    api.on('POST', '/alerts/20/mark-arrived', json(200, {}))
    render(<PressedAlertPanel alert={ALERT} dial={vi.fn()} />)

    expect(screen.getByText('Nút hỗ trợ khẩn cấp của nhân viên')).toBeInTheDocument()
    expect(screen.getByText('Trần Thu Hà')).toBeInTheDocument()
    expect(screen.getByText('Ngã')).toBeInTheDocument()
    expect(screen.getByText('Nhà vệ sinh')).toBeInTheDocument()
    expect(screen.getByText('Chưa có vị trí GPS.')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Đã tới nơi' }))
    expect(await screen.findByText(/Đã tới nơi lúc/)).toBeInTheDocument()
    expect(api.calls('POST', '/alerts/20/mark-arrived')).toHaveLength(1)
  })

  it('không có lệnh thì không có nút', () => {
    render(<PressedAlertPanel alert={{ ...ALERT, allowedCommands: [] }} dial={vi.fn()} />)

    expect(screen.queryByRole('button', { name: 'Đã tới nơi' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Gọi 115' })).not.toBeInTheDocument()
  })
})

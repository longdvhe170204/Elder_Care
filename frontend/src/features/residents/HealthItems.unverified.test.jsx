import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes } from '@/test/fetchRoutes'
import { HealthItems } from './HealthItems'

// 001 T106 (Q-400, BR-M01-17, FR-018b): mục tự khai của gia đình do Hành chính ghi mang nhãn "Tự khai, chưa được Bác sĩ
// kiểm chứng"; Bác sĩ, Điều dưỡng bấm "Kiểm chứng"; Hành chính ghi mục với nguồn mặc định "Người thân".

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

function item(overrides = {}) {
  return {
    id: 9,
    type: 'CHRONIC_CONDITION',
    allergen: null,
    freeText: 'Tăng huyết áp',
    notAutoChecked: false,
    pendingCheck: false,
    severity: null,
    source: 'Người thân',
    recordedBy: 'Hành chính Lan',
    at: '2026-11-06T09:30:00+07:00',
    status: 'ACTIVE',
    exclusion: null,
    unverified: true,
    verifiedBy: null,
    verifiedAt: null,
    ...overrides,
  }
}

function list(items) {
  api.on('GET', '/residents/7/health-items', json(200, items))
}

describe('HealthItems, mục tự khai chưa kiểm chứng', () => {
  it('FR_018b_mucChuaKiemChung_hienNhanTuKhai', async () => {
    list([item()])

    render(<HealthItems residentId={7} allowedCommands={[]} />)

    expect(await screen.findByText('Tự khai, chưa được Bác sĩ kiểm chứng')).toBeTruthy()
  })

  it('FR_018b_mucDaKiemChung_khongHienNhan', async () => {
    list([item({ unverified: false, verifiedBy: 'Bác sĩ Lê', verifiedAt: '2026-11-06T10:00:00+07:00' })])

    render(<HealthItems residentId={7} allowedCommands={['VERIFY_HEALTH_ITEM']} />)

    await screen.findByText('Tăng huyết áp')
    expect(screen.queryByText('Tự khai, chưa được Bác sĩ kiểm chứng')).toBeNull()
    expect(screen.queryByRole('button', { name: 'Kiểm chứng' })).toBeNull()
  })

  it('FR_018b_khongCoQuyenKiemChung_khongCoNut', async () => {
    list([item()])

    render(<HealthItems residentId={7} allowedCommands={['RECORD_HEALTH_ITEM']} />)

    await screen.findByText('Tăng huyết áp')
    expect(screen.queryByRole('button', { name: 'Kiểm chứng' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Loại trừ' })).toBeNull()
  })

  it('FR_018b_kiemChung_guiPostRoiTaiLaiDanhSach', async () => {
    list([item()])
    api.on('POST', '/health-items/9/verify', json(200, item({ unverified: false, verifiedBy: 'Bác sĩ Lê' })))

    render(<HealthItems residentId={7} allowedCommands={['VERIFY_HEALTH_ITEM']} />)
    fireEvent.click(await screen.findByRole('button', { name: 'Kiểm chứng' }))

    await waitFor(() => expect(api.calls('POST', '/health-items/9/verify')).toHaveLength(1))
    await waitFor(() => expect(api.calls('GET', '/residents/7/health-items').length).toBeGreaterThan(1))
  })

  it('FR_018b_hanhChinhGhiMuc_nguonMacDinhLaNguoiThan', async () => {
    list([])
    api.on('GET', '/catalogs/allergens', json(200, []))
    api.on('GET', '/catalogs/allergy-severities', json(200, [{ id: 2, name: 'Nhẹ' }]))
    api.on(
      'GET',
      '/catalogs/information-sources',
      json(200, [
        { id: 4, name: 'Bác sĩ khám' },
        { id: 5, name: 'Người thân' },
      ]),
    )

    render(<HealthItems residentId={7} allowedCommands={['RECORD_HEALTH_ITEM']} />)
    fireEvent.click(await screen.findByRole('button', { name: 'Ghi nhận mục' }))

    const source = await screen.findByLabelText('Nguồn thông tin')
    expect(source.value).toBe('5')
    expect(screen.getByText('Thông tin tự khai của gia đình, chưa được Bác sĩ kiểm chứng')).toBeTruthy()
  })
})

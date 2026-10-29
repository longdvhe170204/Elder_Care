import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes } from '@/test/fetchRoutes'
import { AdminPage } from './AdminPage'

// Màn "Tài khoản, tham số": chi tiết của tab mở trong DetailLayout ở cấp màn (DESIGN.md 5); đổi tab thì đóng khung.

const PARAMETER = {
  code: 'CFG-M15-05',
  description: 'Mốc nhắc người duyệt',
  type: 'DURATION',
  unit: 'HOURS',
  constraints: { min: 1 },
  defaultValue: { value: 48, unit: 'HOURS' },
  currentValue: { value: 48, unit: 'HOURS' },
  version: 0,
  changedAt: null,
}

let api

beforeEach(() => {
  api = mockRoutes()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('AdminPage', () => {
  // Từ 1280px khung cố định bên phải, tab vẫn bấm được khi khung mở
  it('doiTab_dongKhungChiTiet', async () => {
    vi.stubGlobal('matchMedia', () => ({ matches: true, addEventListener() {}, removeEventListener() {} }))
    api.on('GET', '/parameters', json(200, [PARAMETER]))
    api.on('GET', '/parameters/CFG-M15-05', json(200, PARAMETER, { ETag: '"0"' }))
    render(<AdminPage role="NURSING_HOME_MANAGER" />)

    fireEvent.click(await screen.findByRole('button', { name: 'CFG-M15-05' }))
    expect(await screen.findByRole('complementary', { name: 'Chi tiết tham số' })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Tài khoản' }))
    await waitFor(() => expect(screen.queryByRole('complementary', { name: 'Chi tiết tham số' })).toBeNull())
  })
})

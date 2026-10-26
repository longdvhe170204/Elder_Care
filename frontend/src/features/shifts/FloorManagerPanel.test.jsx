import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes } from '@/test/fetchRoutes'
import { FloorManagerPanel } from './FloorManagerPanel'
let api
beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/floor-manager-assignments', json(200, []))
})
afterEach(() => vi.unstubAllGlobals())
it('FR_024_giaoCoThoiHan_khongGuiNgayKetThucRong', async () => {
  let body
  api.on('POST', '/floor-manager-assignments', (_url, options) => {
    body = JSON.parse(options.body)
    return json(201, {})
  })
  render(<FloorManagerPanel canCreate canUpdate staff={[{ id: 10, fullName: '[Trưởng tầng kiểm thử]' }]} />)
  fireEvent.change(screen.getByLabelText('Mã tầng / khu vực được giao'), { target: { value: '5' } })
  fireEvent.change(screen.getByLabelText('Ngày bắt đầu giao'), { target: { value: '2026-11-01' } })
  fireEvent.click(screen.getByRole('button', { name: 'Giao Trưởng tầng' }))
  await waitFor(() => expect(body).toEqual({ staffId: 10, floorAreaId: 5, from: '2026-11-01' }))
})
it('FR_024_rutNganGiaoPhaiCoLyDo', async () => {
  api.on('GET', '/floor-manager-assignments', json(200, [{ id: 8, staffId: 10, floorAreaId: 5, from: '2026-11-01' }]))
  let body
  api.on('POST', '/floor-manager-assignments/8/end', (_url, options) => {
    body = JSON.parse(options.body)
    return json(200, {})
  })
  render(<FloorManagerPanel canUpdate />)
  fireEvent.click(await screen.findByRole('button', { name: 'Kết thúc giao #8' }))
  fireEvent.change(screen.getByLabelText('Ngày kết thúc giao #8'), { target: { value: '2026-11-02' } })
  expect(screen.getByRole('button', { name: 'Xác nhận rút ngắn' })).toBeDisabled()
  fireEvent.change(screen.getByLabelText('Lý do rút ngắn'), { target: { value: 'Nghỉ dài ngày, giao tạm người thay' } })
  fireEvent.click(screen.getByRole('button', { name: 'Xác nhận rút ngắn' }))
  await waitFor(() => expect(body?.reason).toBe('Nghỉ dài ngày, giao tạm người thay'))
})

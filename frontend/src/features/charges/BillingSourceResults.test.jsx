import { expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { BillingSourceResults } from './BillingSourceResults'
import { sourceSelectionKey } from './sourceSelection'

const source = { sourceType: 'OVERNIGHT_STAY', sourceId: 'stay-17', billingUnit: '2026-11-04',
  incurredOn: '2026-11-04', description: 'Lượt ở lại', quantity: 1, unit: 'đêm', flags: [] }
const base = { residentId: 42, sourceType: 'OVERNIGHT_STAY', sources: [source], selection: [],
  onSelectionChange: vi.fn(), onCreate: vi.fn(), canCreate: true }

it('FR_005_nguonDaCoKhoanVanHienVaMoDuocKhoan', () => {
  const open = vi.fn()
  render(<BillingSourceResults {...base} sources={[{ ...source, existingItemId: 91 }]} onOpenItem={open} />)
  fireEvent.click(screen.getByRole('button', { name: 'Xem khoản #91' }))
  expect(open).toHaveBeenCalledWith(91)
  expect(screen.getByText('Lượt ở lại')).toBeVisible()
  expect(screen.queryByText(/chưa lập khoản/i)).not.toBeInTheDocument()
})

it('FR_005_phaiChonNguonVaXemLaiTruocKhiLap', () => {
  const create = vi.fn()
  const props = { ...base, onCreate: create, selection: [sourceSelectionKey(42, source)] }
  render(<BillingSourceResults {...props} />)
  expect(screen.getByRole('button', { name: 'Lập 1 khoản chi phí' })).toBeDisabled()
  fireEvent.click(screen.getByRole('checkbox', { name: 'Tôi đã kiểm tra các nguồn đã chọn' }))
  fireEvent.click(screen.getByRole('button', { name: 'Lập 1 khoản chi phí' }))
  expect(create).toHaveBeenCalledWith({ residentId: 42, sourceType: 'OVERNIGHT_STAY',
    sourceUnits: [{ sourceId: 'stay-17', billingUnit: '2026-11-04' }] })
})

it('FR_043_khongSuyQuyenLapTuVaiTro', () => {
  render(<BillingSourceResults {...base} canCreate={false} selection={[sourceSelectionKey(42, source)]} />)
  expect(screen.getByRole('button', { name: 'Lập 1 khoản chi phí' })).toBeDisabled()
  expect(screen.getByRole('checkbox', { name: /Chọn Lượt ở lại/ })).toBeDisabled()
})

it('FR_005_doiLuaChonThiBoXacNhanCu', () => {
  const change = vi.fn()
  render(<BillingSourceResults {...base} onSelectionChange={change} selection={[sourceSelectionKey(42, source)]} />)
  fireEvent.click(screen.getByRole('checkbox', { name: 'Tôi đã kiểm tra các nguồn đã chọn' }))
  fireEvent.click(screen.getByRole('checkbox', { name: /Chọn Lượt ở lại/ }))
  expect(change).toHaveBeenCalledWith([])
  expect(screen.getByRole('button', { name: 'Lập 1 khoản chi phí' })).toBeDisabled()
})

it('FR_005_nguonDoiLuongSauKhiXemThiPhaiXacNhanLai', () => {
  const props = { ...base, selection: [sourceSelectionKey(42, source)] }
  const { rerender } = render(<BillingSourceResults {...props} />)
  fireEvent.click(screen.getByRole('checkbox', { name: 'Tôi đã kiểm tra các nguồn đã chọn' }))
  expect(screen.getByRole('button', { name: 'Lập 1 khoản chi phí' })).toBeEnabled()
  rerender(<BillingSourceResults {...props} sources={[{ ...source, quantity: 2 }]} />)
  expect(screen.getByRole('button', { name: 'Lập 1 khoản chi phí' })).toBeDisabled()
})

it('FR_005_hienTungNguonBiBoQuaKeCaKetQuaRong', () => {
  render(<BillingSourceResults {...base} sources={[]} skipped={[
    { source: 'stay-17', errorCode: 'CONDITION_NOT_MET', reason: 'Cần chọn đúng đêm.' },
    { source: 'dose-18', errorCode: 'COST_ITEM_ALREADY_EXISTS' },
  ]} />)
  expect(screen.getByText('Không có nguồn trong kết quả tra cứu.')).toBeVisible()
  expect(screen.getByText(/Cần chọn đúng đêm/)).toBeVisible()
  expect(screen.getByText(/Nguồn này đã có khoản đang hiệu lực/)).toBeVisible()
})

it('FR_013_chiHienMoTaThuocDaDuocBackendLocVaKhongCoONhapTien', () => {
  render(<BillingSourceResults {...base} sources={[{ ...source, sourceType: 'DOSE', description: 'Thuốc VT-17',
    billingUnit: '17', quantity: 0.125, unit: 'viên' }]} sourceType="DOSE" />)
  expect(screen.getByText('Thuốc VT-17')).toBeVisible()
  expect(screen.getByText('0,125')).toBeVisible()
  expect(screen.queryByRole('spinbutton')).not.toBeInTheDocument()
})

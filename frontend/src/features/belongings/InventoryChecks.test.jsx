import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { InventoryChecks } from './InventoryChecks'

const catalogs = {
  levels: [{ id: 12, name: 'Trầy xước', order: 2, unusable: false, status: 'ACTIVE' }],
  locationName: (id) => (id === 21 ? 'Kho tầng 2' : `Vị trí #${id}`),
}
const CHECK = {
  id: 40,
  status: 'WAITING_CHECK',
  late: true,
  version: 0,
  content: {
    storageLocationId: 21,
    periodCode: '2026-11',
    dueDate: '2026-11-05',
    lines: [
      { belongingId: 90, residentId: 7, amountAtCreation: 1, conditionLevelIdAtCreation: 11, result: null },
      { belongingId: 91, residentId: 7, amountAtCreation: 2000000, conditionLevelIdAtCreation: 11, result: null },
    ],
    unknownOwnerItems: [],
  },
}

function setup() {
  const api = mockRoutes()
  api.on(
    'GET',
    '/belonging-inventory-checks',
    json(200, {
      periodCode: '2026-11',
      dueDate: '2026-11-05',
      locations: [
        { storageLocationId: 21, valuableCount: 2, checkId: 40 },
        { storageLocationId: 22, valuableCount: 1, checkId: null },
      ],
      checks: [CHECK],
    }),
  )
  api.on('GET', '/belonging-inventory-checks/40', json(200, CHECK))
  render(<InventoryChecks catalogs={catalogs} />)
  return api
}

describe('InventoryChecks (013 T026, BR-M12-07)', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('FR_028a_kyHienTai_viTriCoDoCoGiaTri_khongGoiY', async () => {
    setup()
    expect(await screen.findByText(/Kỳ 2026-11/)).toBeInTheDocument()
    expect(screen.getByText('Kho tầng 2')).toBeInTheDocument()
    expect(screen.getByText('Trễ hạn')).toBeInTheDocument()
  })

  it('FR_028a_lapPhieuChoViTri', async () => {
    const api = setup()
    api.on('POST', '/belonging-inventory-checks', json(201, { ...CHECK, id: 41 }))
    fireEvent.click(await screen.findByRole('button', { name: 'Lập phiếu cho Vị trí #22' }))
    await waitFor(() => expect(api.calls('POST', '/belonging-inventory-checks')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/belonging-inventory-checks')[0][1].body)).toEqual({ storageLocationId: 22 })
  })

  it('FR_028b_hoanThanh_ketQuaTungDo_doKhongRoChu', async () => {
    const api = setup()
    api.on('POST', '/files', json(201, { fileId: 730 }))
    api.on('POST', '/belonging-inventory-checks/40/complete', json(200, { ...CHECK, status: 'COMPLETED' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Mở phiếu Kho tầng 2' }))
    fireEvent.change(await screen.findByLabelText('Kết quả đồ #90'), { target: { value: 'MATCHED' } })
    fireEvent.change(screen.getByLabelText('Kết quả đồ #91'), { target: { value: 'CONDITION_MISMATCH' } })
    fireEvent.change(screen.getByLabelText('Mức mới của đồ #91'), { target: { value: '12' } })
    fireEvent.click(screen.getByRole('checkbox', { name: 'Ghi hư hỏng đồ #91' }))
    fireEvent.click(screen.getByRole('button', { name: 'Thêm đồ không rõ chủ' }))
    fireEvent.change(screen.getByLabelText('Mô tả đồ không rõ chủ 1'), { target: { value: 'Ví da màu nâu' } })
    fireEvent.click(screen.getByRole('button', { name: 'Hoàn thành kiểm kê' }))
    expect(await screen.findByText('Chọn ảnh của đồ không rõ chủ.')).toBeInTheDocument()
    expect(api.calls('POST', '/belonging-inventory-checks/40/complete')).toHaveLength(0)
    const png = new File(['x'], 'vi.png', { type: 'image/png' })
    fireEvent.change(screen.getByLabelText('Ảnh đồ không rõ chủ 1'), { target: { files: [png] } })
    fireEvent.click(screen.getByRole('button', { name: 'Hoàn thành kiểm kê' }))

    await waitFor(() => expect(api.calls('POST', '/belonging-inventory-checks/40/complete')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/belonging-inventory-checks/40/complete')[0][1].body)).toEqual({
      lines: [
        { belongingId: 90, result: 'MATCHED' },
        { belongingId: 91, result: 'CONDITION_MISMATCH', newCondition: { levelId: 12 }, recordDamage: true },
      ],
      found: [],
      unknownOwnerItems: [{ description: 'Ví da màu nâu', photoFileIds: [730] }],
    })
  })

  it('BR_M12_07_thieuKetQua_baoTaiO_huyCanLyDo', async () => {
    const api = setup()
    api.on(
      'POST',
      '/belonging-inventory-checks/40/cancel',
      problem(409, 'BELONGINGS_STILL_IN_CHECK', { detail: 'Còn đồ tại vị trí.' }),
    )
    fireEvent.click(await screen.findByRole('button', { name: 'Mở phiếu Kho tầng 2' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Hoàn thành kiểm kê' }))
    expect(await screen.findAllByText('Chọn kết quả.')).toHaveLength(2)
    expect(api.calls('POST', '/belonging-inventory-checks/40/complete')).toHaveLength(0)

    fireEvent.change(screen.getByLabelText('Lý do hủy phiếu'), { target: { value: 'Đồ đã chuyển hết' } })
    fireEvent.click(screen.getByRole('button', { name: 'Hủy phiếu' }))
    expect(await screen.findByText('Còn đồ tại vị trí.')).toBeInTheDocument()
    expect(JSON.parse(api.calls('POST', '/belonging-inventory-checks/40/cancel')[0][1].body)).toEqual({
      reason: 'Đồ đã chuyển hết',
    })
  })
})

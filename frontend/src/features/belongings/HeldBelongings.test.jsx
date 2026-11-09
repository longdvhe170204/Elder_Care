import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { HeldBelongings } from './HeldBelongings'

const catalogs = {
  typeName: (id) => (id === 2 ? 'Điện thoại' : `Loại #${id}`),
  locationName: (id) => (id === 21 ? 'Kho tầng 2' : 'Không ghi vị trí'),
}

describe('HeldBelongings (013 T015, FR-014a)', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('FR_014a_doCoGiaTriNhanVienDangGiu', async () => {
    const api = mockRoutes()
    api.on(
      'GET',
      '/staff/15/held-belongings',
      json(200, [
        {
          id: 90,
          residentId: 7,
          typeId: 2,
          description: 'Điện thoại Nokia',
          quantity: 1,
          status: 'HELD',
          holder: 'STAFF',
          storageLocationId: 21,
          version: 1,
        },
      ]),
    )
    render(<HeldBelongings catalogs={catalogs} />)
    fireEvent.change(screen.getByLabelText('Mã nhân viên'), { target: { value: '15' } })
    fireEvent.click(screen.getByRole('button', { name: 'Xem đồ đang giữ' }))

    expect(await screen.findByText('Điện thoại Nokia')).toBeInTheDocument()
    expect(screen.getByText('Kho tầng 2')).toBeInTheDocument()
    expect(screen.getByText('Đang giữ')).toBeInTheDocument()
  })

  it('FR_029_403_baoKhongCoQuyen', async () => {
    const api = mockRoutes()
    api.on(
      'GET',
      '/staff/16/held-belongings',
      problem(403, 'FORBIDDEN', { detail: 'Không có quyền xem đồ người khác đang giữ.' }),
    )
    render(<HeldBelongings catalogs={catalogs} />)
    fireEvent.change(screen.getByLabelText('Mã nhân viên'), { target: { value: '16' } })
    fireEvent.click(screen.getByRole('button', { name: 'Xem đồ đang giữ' }))
    expect(await screen.findByText('Không có quyền xem đồ người khác đang giữ.')).toBeInTheDocument()
  })
})

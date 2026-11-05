import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { PortalBelongings } from './PortalBelongings'

describe('PortalBelongings (013 T028, US6)', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('FR_030_doGuiVaLichSu_khongViTriKhongTenNhanVien', async () => {
    const api = mockRoutes()
    api.on(
      'GET',
      '/family-portal/residents/7/belongings',
      json(200, [
        {
          id: 90,
          typeId: 2,
          description: 'Điện thoại Nokia',
          quantity: 1,
          status: 'IN_USE',
          holder: 'RESIDENT',
          history: [
            {
              id: 300,
              kind: 'INTAKE',
              targetStatus: 'HELD',
              occurredAt: '2026-11-06T02:00:00Z',
              quantity: 1,
              conditionDescription: 'Nguyên vẹn',
              photoFileIds: [501],
            },
            {
              id: 301,
              kind: 'GIVE_FOR_USE',
              targetStatus: 'IN_USE',
              occurredAt: '2026-11-06T03:00:00Z',
              quantity: 1,
              photoFileIds: [],
            },
          ],
        },
      ]),
    )
    render(<PortalBelongings residentId={7} />)

    expect(await screen.findByText('Điện thoại Nokia')).toBeInTheDocument()
    expect(screen.getByText('Đang được người cao tuổi sử dụng')).toBeInTheDocument()
    expect(screen.getByText('Tiếp nhận')).toBeInTheDocument()
    expect(screen.getByText('Giao sử dụng')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Xem ảnh 1 của Điện thoại Nokia' })).toBeInTheDocument()
    expect(screen.queryByText(/Vị trí/)).not.toBeInTheDocument()
  })

  it('BR_M12_09_khongCoQuyen_baoRo', async () => {
    const api = mockRoutes()
    api.on('GET', '/family-portal/residents/7/belongings', problem(403, 'FORBIDDEN'))
    render(<PortalBelongings residentId={7} />)
    expect(
      await screen.findByText('Chỉ người đại diện và người được phép đón đang hiệu lực xem được đồ gửi.'),
    ).toBeInTheDocument()
  })
})

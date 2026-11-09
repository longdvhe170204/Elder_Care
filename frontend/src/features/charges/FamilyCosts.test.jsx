import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { FamilyCosts } from './FamilyCosts'
import { AccountingExport } from './AccountingExport'
import * as api from '@/api'

vi.mock('@/api', () => ({
  getResidentsByResidentIdCostsStatements: vi.fn(),
  getResidentsByResidentIdCostsProvisional: vi.fn(),
  postCostsPeriodsByIdExport: vi.fn(),
}))
describe('Chi phí người thân và xuất kế toán', () => {
  beforeEach(() => vi.clearAllMocks())
  it('FR_037_038_hienBangDaChot_vaTongTamTinhThieuGia', async () => {
    api.getResidentsByResidentIdCostsStatements.mockResolvedValue({
      data: [
        {
          id: 1,
          period: '2026-10',
          total: 500,
          totalsByType: [],
          items: [
            {
              id: 2,
              costType: 'MEDICATION',
              description: 'Thuốc VT01',
              amount: 500,
              incurredOn: '2026-10-01',
              quantity: 1,
              unit: 'viên',
            },
          ],
        },
      ],
    })
    api.getResidentsByResidentIdCostsProvisional.mockResolvedValue({
      data: { label: 'Tạm tính, chưa chốt', totalsByType: [{ type: 'SERVICE', total: 200 }], hasUnpricedItems: true },
    })
    render(<FamilyCosts residentId={42} />)
    expect(await screen.findByText(/Thuốc VT01/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Xem tạm tính' }))
    expect(await screen.findByText('Còn khoản chưa có đơn giá')).toBeInTheDocument()
  })
  it('FR_039_guiPhamViDinhDangVaKhoa_lenhTaiFile', async () => {
    api.postCostsPeriodsByIdExport.mockResolvedValue({ data: new Blob(['test']) })
    const download = vi.fn()
    render(<AccountingExport periodId="7" download={download} />)
    fireEvent.change(screen.getByLabelText('Phạm vi xuất'), { target: { value: 'NEVER_EXPORTED' } })
    fireEvent.click(screen.getByRole('button', { name: 'Tải file kế toán' }))
    await waitFor(() => expect(download).toHaveBeenCalled())
    expect(api.postCostsPeriodsByIdExport).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { id: 7 },
        body: { scope: 'NEVER_EXPORTED', format: 'XLSX' },
        responseType: 'blob',
      }),
    )
  })
})

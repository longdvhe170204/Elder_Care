import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { AccountingExport } from './AccountingExport'
import * as api from '@/api'

vi.mock('@/api', () => ({
  postCostsPeriodsByIdExport: vi.fn(),
}))
// Phần bảng chi phí của người thân chuyển sang cổng (family-portal/live/LiveCostStatements, 2026-11-10).
describe('Xuất kế toán', () => {
  beforeEach(() => vi.clearAllMocks())
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

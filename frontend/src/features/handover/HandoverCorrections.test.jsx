import { afterEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes } from '@/test/fetchRoutes'
import { HandoverCorrections } from './HandoverCorrections'

afterEach(() => vi.unstubAllGlobals())
it('FR_046_dinhChinhNoiDungGiuMaNguon_vaKhongGoiXacNhanLanNua', async () => {
 const api = mockRoutes()
 api.on('GET', '/corrections/M09.HANDOVER/41', json(200, { originalContent: { generalAssessment: 'Nhận định gốc' }, corrections: [] }))
 api.on('POST', '/corrections/M09.HANDOVER/41', json(201, { id: 6 }))
 const changed = vi.fn()
 render(<HandoverCorrections handover={{ id: 41, generalAssessment: 'Nhận định gốc', residentAssessments: [], comments: [], items: [{ id: 8, description: 'Nội dung', note: 'Ghi chú cũ' }] }} canCorrect onChanged={changed} />)
 fireEvent.click(screen.getByRole('button', { name: 'Xem và đính chính' }))
 fireEvent.change(await screen.findByLabelText('Ghi chú mục 8'), { target: { value: 'Ghi chú đúng' } })
 fireEvent.change(screen.getByLabelText('Lý do đính chính'), { target: { value: 'Sửa sau đối chiếu' } })
 fireEvent.click(screen.getByRole('button', { name: 'Lưu đính chính' }))
 await waitFor(() => expect(changed).toHaveBeenCalled())
 expect(JSON.parse(api.calls('POST', '/corrections/M09.HANDOVER/41')[0][1].body).correctedContent.items).toEqual([{ id: 8, description: 'Nội dung', note: 'Ghi chú đúng' }])
 expect(api.calls('POST', '/handovers/41/confirm')).toHaveLength(0)
})

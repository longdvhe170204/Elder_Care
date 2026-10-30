import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { json, mockRoutes } from '@/test/fetchRoutes'
import { PublicationForm } from './ScheduleForms'

let api
beforeEach(() => {
  api = mockRoutes()
})
afterEach(() => vi.unstubAllGlobals())

it('FR_036_canhBaoTyLeVaThieuNguonKhongChanCongBo_khongEpThemLyDo', async () => {
  api.on('POST', '/shift-schedules/7/publish', json(200, {}))
  const done = vi.fn()
  render(
    <PublicationForm
      schedule={{ id: 7 }}
      preview={{
        unmetConditions: [],
        invalidAssignments: [],
        warnings: [
          { code: 'BELOW_STAFFING_THRESHOLD', description: 'Ca dưới ngưỡng phục vụ', details: { shiftId: 20 } },
          {
            code: 'STAFFING_RATIO_UNAVAILABLE',
            description: 'Chưa đủ nguồn để tính tỷ lệ phục vụ',
            details: { shiftId: 21 },
          },
        ],
      }}
      onDone={done}
    />,
  )
  expect(screen.getByText('Ca dưới ngưỡng phục vụ')).toBeVisible()
  expect(screen.getByText('Chưa đủ nguồn để tính tỷ lệ phục vụ')).toBeVisible()
  expect(screen.getByRole('button', { name: 'Công bố lịch' })).toBeEnabled()
  fireEvent.click(screen.getByRole('button', { name: 'Công bố lịch' }))
  await waitFor(() => expect(done).toHaveBeenCalledOnce())
  expect(JSON.parse(api.calls('POST', '/shift-schedules/7/publish')[0][1].body)).toEqual({
    confirmWarnings: false,
    assignmentIdsToRemove: [],
  })
})

it('FR_014_canhBaoGioLienTucVanCanXacNhanVaLyDo', () => {
  render(
    <PublicationForm
      schedule={{ id: 7 }}
      preview={{
        unmetConditions: [],
        warnings: [{ code: 'CONTINUOUS_WORK_HOURS', description: 'Chuỗi ca liên tục vượt ngưỡng', details: {} }],
      }}
      onDone={vi.fn()}
    />,
  )
  expect(screen.getByRole('button', { name: 'Công bố lịch' })).toBeDisabled()
})

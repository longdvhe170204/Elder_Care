import { expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { CarePlanItemsForm } from './CarePlanItemsForm'

it('FR_002_nhapNhieuMocGioGiuDauPhayTrongLucNhapVaGuiMangGio', () => {
  const save = vi.fn()
  render(
    <CarePlanItemsForm
      items={[
        {
          id: 1,
          taskTypeId: 8,
          content: 'Nội dung kiểm thử',
          frequencyType: 'TIMES_PER_DAY',
          timesPerDay: 2,
          times: ['08:00'],
          weekdays: [],
          performerRole: 'CAREGIVER',
          resultType: 'PERFORMED',
          importance: 'NORMAL',
          billable: false,
        },
      ]}
      taskTypes={[{ id: 8, name: 'Loại kiểm thử', status: 'ACTIVE', group: 'OTHER' }]}
      busy={false}
      onSave={save}
    />,
  )
  const field = screen.getByLabelText('Các mốc giờ mục 1 (cách nhau bằng dấu phẩy)')
  fireEvent.change(field, { target: { value: '08:00,' } })
  expect(field).toHaveValue('08:00,')
  fireEvent.change(field, { target: { value: '08:00, 14:00' } })
  fireEvent.click(screen.getByRole('button', { name: 'Lưu mục công việc' }))
  expect(save).toHaveBeenCalledWith([expect.objectContaining({ times: ['08:00', '14:00'] })])
  expect(save.mock.calls[0][0][0]).not.toHaveProperty('timesText')
})

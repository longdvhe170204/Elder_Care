import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { json, mockRoutes } from '@/test/fetchRoutes'
import { AssignmentPanel } from './AssignmentPanel'

let api
const shift = {
  id: 20,
  status: 'DRAFT',
  startsAt: '2026-11-01T00:00:00Z',
  endsAt: '2026-11-01T08:00:00Z',
  staff: [{ staffId: 10 }],
  allowedCommands: ['createAssignment', 'copyAssignments', 'deleteAssignment'],
}
beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/shifts/20/assignments', json(200, []))
})
afterEach(() => vi.unstubAllGlobals())

it('FR_031_xemCanhBaoTruocLuu_vaDoiDuLieuThiPhaiXemLai', async () => {
  api.on(
    'POST',
    '/shifts/20/assignments/preview',
    json(200, {
      warnings: [
        {
          code: 'UNQUALIFIED_TASK_TYPES',
          description: 'Chưa đủ chứng chỉ cho một số công việc',
          details: { taskTypeIds: [7] },
        },
      ],
    }),
  )
  let saved
  api.on('POST', '/shifts/20/assignments', (_url, options) => {
    saved = JSON.parse(options.body)
    return json(201, { id: 8, warnings: [] })
  })
  render(<AssignmentPanel shift={shift} staff={[{ id: 10, fullName: '[Nhân viên kiểm thử]' }]} />)
  fireEvent.change(screen.getByLabelText('Mã đối tượng'), { target: { value: '5' } })
  expect(screen.getByRole('button', { name: 'Lưu phân công' })).toBeDisabled()
  fireEvent.click(screen.getByRole('button', { name: 'Kiểm tra phân công' }))
  expect(await screen.findByText('Chưa đủ chứng chỉ cho một số công việc')).toBeVisible()
  fireEvent.click(screen.getByLabelText('Đã xem các công việc không đủ điều kiện'))
  expect(screen.getByRole('button', { name: 'Lưu phân công' })).toBeEnabled()
  fireEvent.change(screen.getByLabelText('Mã đối tượng'), { target: { value: '6' } })
  expect(screen.getByRole('button', { name: 'Lưu phân công' })).toBeDisabled()
  fireEvent.click(screen.getByRole('button', { name: 'Kiểm tra phân công' }))
  await screen.findByText('Chưa đủ chứng chỉ cho một số công việc')
  fireEvent.click(screen.getByLabelText('Đã xem các công việc không đủ điều kiện'))
  fireEvent.click(screen.getByRole('button', { name: 'Lưu phân công' }))
  await waitFor(() => expect(saved?.targetId).toBe(6))
})

it('FR_027_ketThucBatBuocLyDo_vaGuiDungLenh', async () => {
  api.on(
    'GET',
    '/shifts/20/assignments',
    json(200, [
      {
        id: 8,
        staffId: 10,
        targetType: 'ROOM',
        targetId: 5,
        role: 'CAREGIVER',
        assignmentKind: 'PRIMARY',
        status: 'ACTIVE',
      },
    ]),
  )
  let body
  api.on('POST', '/assignments/8/end', (_url, options) => {
    body = JSON.parse(options.body)
    return json(200, {})
  })
  render(<AssignmentPanel shift={{ ...shift, status: 'IN_PROGRESS', allowedCommands: ['endAssignment'] }} />)
  fireEvent.click(await screen.findByRole('button', { name: 'Kết thúc #8' }))
  expect(screen.getByRole('button', { name: 'Xác nhận kết thúc' })).toBeDisabled()
  fireEvent.change(screen.getByLabelText('Lý do kết thúc'), { target: { value: 'Chuyển người phụ trách' } })
  fireEvent.click(screen.getByRole('button', { name: 'Xác nhận kết thúc' }))
  await waitFor(() => expect(body).toEqual({ reason: 'Chuyển người phụ trách' }))
})

it('FR_033_chiXemKhiBackendKhongChoLenhGhi', async () => {
  render(<AssignmentPanel shift={{ ...shift, allowedCommands: [] }} />)
  await screen.findByText('Chưa có phân công trong ca.')
  expect(screen.queryByRole('button', { name: 'Lưu phân công' })).not.toBeInTheDocument()
})

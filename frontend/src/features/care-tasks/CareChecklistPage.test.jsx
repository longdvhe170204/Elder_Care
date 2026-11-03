import { beforeEach, afterEach, it, expect, vi } from 'vitest'
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { CareChecklistPage } from './CareChecklistPage'

let api
const task = (id, status, at) => ({
  id,
  residentId: 77,
  residentCard: { fullName: '[Người kiểm thử]', roomBed: 'Phòng kiểm thử', careNotes: 'Lưu ý kiểm thử' },
  taskType: `Công việc ${id}`,
  content: 'Nội dung đã ghi',
  basis: { type: 'AD_HOC' },
  plannedAt: at,
  windowFrom: at,
  windowTo: '2026-11-01T04:00:00Z',
  importance: 'NORMAL',
  performer: '[Nhân viên kiểm thử]',
  shared: false,
  status,
  allowedCommands: ['notPerform'],
})
const work = [task(5, 'DUE', '2026-11-01T03:00:00Z'), task(6, 'OVERDUE', '2026-11-01T02:00:00Z')]
beforeEach(() => {
  api = mockRoutes()
  api.on(
    'GET',
    '/me/permissions',
    json(200, [
      { function: 'TASK_CHECKLIST', operation: 'VIEW' },
      { function: 'TASK_CHECKLIST', operation: 'CONFIRM' },
    ]),
  )
  api.on(
    'GET',
    '/shifts/9',
    json(200, {
      id: 9,
      startsAt: '2026-11-01T00:00:00Z',
      endsAt: '2026-11-01T08:00:00Z',
      status: 'IN_PROGRESS',
      staff: [],
    }),
  )
  api.on(
    'GET',
    '/checklist',
    json(200, {
      shiftId: 9,
      loadedAt: '2026-11-01T03:30:00Z',
      tasks: work,
      riskAgreements: [
        {
          residentId: 77,
          agreements: [
            {
              id: 1,
              choice: 'Lựa chọn kiểm thử',
              discussedRisks: 'Rủi ro đã trao đổi',
              residentParticipates: true,
              assessingDoctorId: 1,
              selfDeterminationAssessment: {
                canSelfDetermine: true,
                supportMethod: 'Hỗ trợ kiểm thử',
                reviewDate: '2026-11-08',
              },
              decisionMaker: { type: 'RESIDENT' },
              reviewDate: '2026-11-08',
            },
          ],
        },
      ],
      nonPerformanceReasonTypes: [
        { id: 2, name: 'Lý do khác', status: 'ACTIVE', fixed: false },
        { id: 1, name: 'Người cao tuổi từ chối', status: 'ACTIVE', fixed: true },
      ],
      medicationDoses: [],
      openAlerts: [],
      handoverTasks: {},
    }),
  )
  api.on('GET', '/care-tasks/5', json(200, { ...work[0], result: null, handlingHistory: [] }))
})
afterEach(() => vi.unstubAllGlobals())
const view = () =>
  render(
    <PermissionsProvider>
      <CareChecklistPage shiftId={9} />
    </PermissionsProvider>,
  )
it('FR_030_hienViecQuaHanTruocVaLuaChonTrenThe', async () => {
  view()
  await screen.findByText('Công việc 5')
  const blocks = screen.getAllByRole('button').filter((b) => b.textContent.includes('Công việc'))
  expect(blocks[0]).toHaveTextContent('Công việc 6')
  expect(screen.getAllByText(/Lựa chọn kiểm thử/)).toHaveLength(2)
  expect(api.calls('GET', '/checklist')[0][0]).toContain('shiftId=9')
  expect(api.calls('POST', '/care-tasks/5/not-perform')).toHaveLength(0)
})
it('FR_028a_khongThucHienBatBuocChonLoaiLyDoVaGiuGhiChuSauLoi', async () => {
  api.on(
    'POST',
    '/care-tasks/5/not-perform',
    problem(409, 'CARE_TASK_CLOSED', { detail: 'Công việc đã được ghi nhận.' }),
  )
  view()
  fireEvent.click(await screen.findByRole('button', { name: /Công việc 5/ }))
  fireEvent.click(await screen.findByRole('button', { name: 'Không thực hiện được…' }))
  fireEvent.click(screen.getByRole('button', { name: 'Lưu Không thực hiện' }))
  expect(api.calls('POST', '/care-tasks/5/not-perform')).toHaveLength(0)
  fireEvent.click(screen.getByRole('radio', { name: 'Người cao tuổi từ chối' }))
  fireEvent.change(screen.getByLabelText('Ghi chú'), { target: { value: 'Lời người cao tuổi đã nói' } })
  fireEvent.click(screen.getByRole('button', { name: 'Lưu Không thực hiện' }))
  await screen.findByText('Công việc đã được ghi nhận.')
  expect(screen.getByLabelText('Ghi chú')).toHaveValue('Lời người cao tuổi đã nói')
  fireEvent.click(screen.getByRole('button', { name: 'Lưu Không thực hiện' }))
  await waitFor(() => expect(api.calls('POST', '/care-tasks/5/not-perform')).toHaveLength(2))
  const calls = api.calls('POST', '/care-tasks/5/not-perform')
  expect(JSON.parse(calls[0][1].body)).toEqual({ reasonTypeId: 1, note: 'Lời người cao tuổi đã nói' })
  expect(calls[0][1].headers['Idempotency-Key']).toBe(calls[1][1].headers['Idempotency-Key'])
})
it('FR_049_khongTaiDuLieuKhiKhongCoQuyen', async () => {
  api.on('GET', '/me/permissions', json(200, []))
  view()
  expect(await screen.findByText(/không có quyền/i)).toBeInTheDocument()
  expect(api.calls('GET', '/checklist')).toHaveLength(0)
})

it('FR_030_moChiTietGiuViTriVaLuuYDaTai', async () => {
  api.on(
    'GET',
    '/care-tasks/5',
    json(200, {
      ...work[0],
      residentCard: { fullName: '[Người kiểm thử]', roomBed: null, careNotes: null },
      hiddenFields: [],
      result: null,
    }),
  )
  view()
  fireEvent.click(await screen.findByRole('button', { name: /Công việc 5/ }))
  await waitFor(() => expect(api.calls('GET', '/care-tasks/5')).toHaveLength(1))
  expect(await screen.findByText('Lưu ý kiểm thử')).toBeInTheDocument()
})

it('FR_030_hienNguonThuocCanhBaoVaBanGiao', async () => {
  api.on(
    'GET',
    '/checklist',
    json(200, {
      shiftId: 9,
      loadedAt: '2026-11-01T03:30:00Z',
      tasks: [],
      riskAgreements: [],
      nonPerformanceReasonTypes: [],
      medicationDoses: [
        {
          doseKey: 'dose-1',
          residentId: 77,
          medication: { description: 'Liều thuốc kiểm thử' },
          plannedAt: '2026-11-01T03:00:00Z',
        },
      ],
      openAlerts: [{ id: 22, residentId: 77 }],
      handoverTasks: { receiveAtStart: true, prepareAtEnd: true },
    }),
  )
  view()
  expect(await screen.findByText('Liều thuốc kiểm thử')).toBeInTheDocument()
  expect(screen.getByText('Cảnh báo #22')).toBeInTheDocument()
  expect(screen.getByText('Nhận bàn giao đầu ca')).toBeInTheDocument()
  expect(screen.getByText('Lập bàn giao cuối ca')).toBeInTheDocument()
})

it('FR_030_responseTaiLaiCaCuKhongGhiDeCaMoi', async () => {
  api.on(
    'GET',
    '/me/shifts',
    json(200, [
      { id: 9, startsAt: '2026-11-01T00:00:00Z', endsAt: '2026-11-01T08:00:00Z' },
      { id: 10, startsAt: '2026-11-01T08:00:00Z', endsAt: '2026-11-01T16:00:00Z' },
    ]),
  )
  api.on('GET', '/shifts/10', json(200, { id: 10 }))
  let count = 0,
    finishOld
  const content = (id) => ({
    shiftId: id,
    loadedAt: '2026-11-01T03:30:00Z',
    tasks: [task(id, 'DUE', '2026-11-01T03:00:00Z')],
    riskAgreements: [],
    nonPerformanceReasonTypes: [],
  })
  api.on('GET', '/checklist', (url) => {
    if (url.searchParams.get('shiftId') === '10') return json(200, content(10))
    if (++count === 1) return problem(409, 'TEST_CONFLICT', { detail: 'Lỗi kiểm thử để tải lại' })
    return new Promise((resolve) => {
      finishOld = () => resolve(json(200, content(9)))
    })
  })
  render(
    <PermissionsProvider>
      <CareChecklistPage />
    </PermissionsProvider>,
  )
  await screen.findByRole('option', { name: /01\/11\/2026 07:00/ })
  fireEvent.change(screen.getByLabelText('Ca làm việc'), { target: { value: '9' } })
  fireEvent.click(await screen.findByRole('button', { name: 'Tải lại checklist' }))
  await waitFor(() => expect(finishOld).toBeTypeOf('function'))
  fireEvent.change(screen.getByLabelText('Ca làm việc'), { target: { value: '10' } })
  await screen.findByText('Công việc 10')
  await act(async () => finishOld())
  expect(screen.getByText('Công việc 10')).toBeInTheDocument()
  expect(screen.queryByText('Công việc 9')).not.toBeInTheDocument()
})

import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { WellbeingSheet } from './WellbeingSheet'

let api
beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/result-values', json(200, [{ type: 'MOOD', code: 'POSITIVE', name: 'Vui vẻ', status: 'ACTIVE' }]))
})
afterEach(() => vi.unstubAllGlobals())

it('FR_053_hienThamGiaTheoTuanVaNguonSoThichVaNguonCanhBaoChuaCo', async () => {
  api.on(
    'GET',
    '/residents/701/wellbeing-profile',
    json(200, {
      from: '2026-11-01',
      to: '2026-11-08',
      weeklyParticipation: [{ weekStart: '2026-11-02', presentCount: 2 }],
      sessions: [
        {
          sessionId: 11,
          startsAt: '2026-11-03T01:00:00Z',
          result: 'PRESENT',
          participationLevel: 'LEFT_EARLY',
          communicationLevel: 'PROACTIVE',
        },
      ],
      dailyObservations: [
        {
          date: '2026-11-03',
          records: [
            {
              id: 9,
              resultType: 'MOOD',
              performedAt: '2026-11-03T02:00:00Z',
              values: { codeValue: 'POSITIVE' },
              note: null,
            },
          ],
        },
      ],
      preferences: {
        current: [
          {
            id: 1,
            interestGroupId: 3,
            description: 'Đọc truyện',
            level: 'LIKE',
            source: { type: 'RESIDENT' },
            recordedBy: 15,
            recordedAt: '2026-11-01T02:00:00Z',
          },
        ],
        history: [],
      },
      alerts: null,
    }),
  )
  render(<WellbeingSheet residentId={701} residentName="Nguyễn Thị Lan" onClose={() => {}} />)
  expect(await screen.findByText('Đọc truyện')).toBeInTheDocument()
  expect(screen.getByText('Theo lời người cao tuổi')).toBeInTheDocument()
  expect(screen.getByText('Bỏ giữa chừng')).toBeInTheDocument()
  expect(screen.getByText('Chủ động')).toBeInTheDocument()
  expect(await screen.findByText('Tâm trạng, Vui vẻ')).toBeInTheDocument()
  expect(screen.getByText('Chưa có nguồn lịch sử cảnh báo.')).toBeInTheDocument()
  expect(screen.getByLabelText('Từ ngày (bắt buộc)')).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: /Sửa|Xóa/ })).not.toBeInTheDocument()
})

it('FR_053_loiGiuKhoangNgayVaChoThuLai', async () => {
  api.on(
    'GET',
    '/residents/701/wellbeing-profile',
    problem(503, 'CARE_SOURCE_UNAVAILABLE', {
      detail: 'Chưa có nguồn hồ sơ tinh thần',
    }),
  )
  render(<WellbeingSheet residentId={701} residentName="Nguyễn Thị Lan" onClose={() => {}} />)
  expect(await screen.findByText('Chưa có nguồn hồ sơ tinh thần')).toBeInTheDocument()
  fireEvent.change(screen.getByLabelText('Từ ngày (bắt buộc)'), { target: { value: '2026-10-01' } })
  fireEvent.click(screen.getByRole('button', { name: 'Xem theo khoảng' }))
  expect(screen.getByLabelText('Từ ngày (bắt buộc)')).toHaveValue('2026-10-01')
})

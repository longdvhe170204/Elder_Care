import { beforeEach, expect, it } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes } from '@/test/fetchRoutes'
import { LiveActivities } from './LiveActivities'

// 014 FR-068, FR-016a (T026, schema chủ dự án duyệt 2026-11-10): cổng người thân thấy buổi đã đăng ký, chuyến đi, số buổi
// đã tham gia; người đại diện ghi, rút đồng ý về muộn cho buổi đủ điều kiện. Không có mức tham gia, cảnh báo.

let api
const ACTIVITIES = {
  from: '2026-10-12',
  to: '2026-12-11',
  registeredSessions: [
    {
      sessionId: 51,
      activityName: 'Cờ tướng',
      startsAt: '2026-11-12T09:00:00Z',
      endsAt: '2026-11-12T10:00:00Z',
      registrationStatus: 'REGISTERED',
      lateReturnConsentEligible: true,
      activeLateReturnConsentId: null,
    },
    {
      sessionId: 52,
      activityName: 'Hát karaoke',
      startsAt: '2026-11-13T09:00:00Z',
      endsAt: '2026-11-13T10:00:00Z',
      registrationStatus: 'REGISTERED',
      lateReturnConsentEligible: true,
      activeLateReturnConsentId: 77,
    },
  ],
  trips: [
    {
      sessionId: 60,
      destination: 'Chùa Một Cột',
      departureAt: '2026-11-14T01:00:00Z',
      expectedReturnAt: '2026-11-14T05:00:00Z',
      status: 'SCHEDULED',
    },
  ],
  attendedSessionCount: 4,
}

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/family-portal/residents/41/activities', json(200, ACTIVITIES))
  api.on('POST', '/family-portal/late-return-consents', json(201, {}))
  api.on('POST', '/family-portal/late-return-consents/77/withdraw', json(200, {}))
})

it('FR_068_buoiDaDangKy_chuyenDi_soBuoiDaThamGia', async () => {
  render(<LiveActivities residentId={41} representative={false} />)
  const card = await screen.findByRole('region', { name: 'Hoạt động' })
  expect(within(card).getByText('Cờ tướng')).toBeInTheDocument()
  expect(within(card).getByText('Chùa Một Cột')).toBeInTheDocument()
  expect(within(card).getByText('Đã tham gia 4 buổi từ 12/10/2026 đến 11/12/2026')).toBeInTheDocument()
  // Người thân không là người đại diện: không có nút đồng ý về muộn
  expect(within(card).queryByRole('button', { name: /Đồng ý về muộn/ })).not.toBeInTheDocument()
})

it('FR_016a_nguoiDaiDienGhiVaRutDongYVeMuon', async () => {
  render(<LiveActivities residentId={41} representative />)
  const card = await screen.findByRole('region', { name: 'Hoạt động' })
  fireEvent.click(within(card).getByRole('button', { name: 'Đồng ý về muộn cho buổi Cờ tướng' }))
  await waitFor(() => expect(api.calls('POST', '/family-portal/late-return-consents')).toHaveLength(1))
  expect(JSON.parse(api.calls('POST', '/family-portal/late-return-consents')[0][1].body)).toEqual({
    residentId: 41,
    sessionId: 51,
  })
  fireEvent.click(within(card).getByRole('button', { name: 'Rút đồng ý về muộn cho buổi Hát karaoke' }))
  await waitFor(() => expect(api.calls('POST', '/family-portal/late-return-consents/77/withdraw')).toHaveLength(1))
})

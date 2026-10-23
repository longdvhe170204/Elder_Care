import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ReassessmentNotice } from './ReassessmentNotice'

// 001 T066 (US6 #1 → #4; FR-041, FR-042): dải cảnh báo ở đầu tab Đánh giá. Chỉ hiện khi cần hoặc quá hạn đánh giá lại,
// nêu hạn và mọi căn cứ đã báo; hệ thống chỉ báo, không có nút tự lập đánh giá hay yêu cầu (Q-307).

const BASIS = [
  { basis: 'PERIODIC', source: 'ASSESSMENT:4', notifiedAt: '2026-10-20T01:00:00Z' },
  { basis: 'AFTER_INCIDENT', source: 'INCIDENT:77', notifiedAt: '2026-10-21T02:30:00Z' },
]

describe('ReassessmentNotice', () => {
  it('không hiện gì khi không cần đánh giá lại hoặc hồ sơ đã ở trạng thái cuối', () => {
    for (const status of ['NONE', 'NOT_APPLICABLE']) {
      const { container, unmount } = render(
        <ReassessmentNotice reassessment={{ residentId: 5, status, dueAt: null, overdueSince: null, basis: [] }} />,
      )
      expect(container.textContent).toBe('')
      unmount()
    }
    const { container } = render(<ReassessmentNotice reassessment={null} />)
    expect(container.textContent).toBe('')
  })

  it('cần đánh giá lại: nêu hạn và từng căn cứ kèm thời điểm báo, không có nút lập yêu cầu', () => {
    render(
      <ReassessmentNotice
        reassessment={{
          residentId: 5,
          status: 'DUE',
          dueAt: '2026-10-23T01:00:00Z',
          overdueSince: null,
          basis: BASIS,
        }}
      />,
    )

    expect(screen.getByText('Cần đánh giá lại')).toBeTruthy()
    expect(screen.getByText(/Hạn đánh giá lại: 23\/10\/2026 08:00/)).toBeTruthy()
    expect(screen.getByText(/Định kỳ, báo lúc 20\/10\/2026 08:00/)).toBeTruthy()
    expect(screen.getByText(/Sau sự cố, báo lúc 21\/10\/2026 09:30/)).toBeTruthy()
    expect(screen.queryByRole('button')).toBeNull()
  })

  it('quá hạn: tiêu đề nêu thời điểm bắt đầu quá hạn', () => {
    render(
      <ReassessmentNotice
        reassessment={{
          residentId: 5,
          status: 'OVERDUE',
          dueAt: '2026-10-23T01:00:00Z',
          overdueSince: '2026-10-23T01:00:00Z',
          basis: BASIS.slice(0, 1),
        }}
      />,
    )

    expect(screen.getByText('Quá hạn đánh giá lại từ 23/10/2026 08:00')).toBeTruthy()
  })

  it('đang điều trị tại bệnh viện: chưa có hạn, ghi hạn tính từ lúc Ghi nhận trở về', () => {
    render(
      <ReassessmentNotice
        reassessment={{ residentId: 5, status: 'DUE', dueAt: null, overdueSince: null, basis: BASIS.slice(0, 1) }}
      />,
    )

    expect(screen.getByText(/hạn tính từ lúc Ghi nhận trở về/)).toBeTruthy()
    expect(screen.queryByText(/Hạn đánh giá lại:/)).toBeNull()
  })
})

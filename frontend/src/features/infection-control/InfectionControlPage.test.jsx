import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes, problem } from '@/test/fetchRoutes'
import { InfectionControlPage } from './InfectionControlPage'

// 007 US8 (T054 phần web): vùng khoanh và danh sách tiếp xúc theo api-007; tên tầng, khu vực từ api-003. Gỡ, khoanh
// vùng cần lý do; danh sách tiếp xúc lập, tra cứu theo nhóm (không đề xuất, Q-306), thêm từng người, xác nhận.

let api

const TYPES = [
  { id: 40, code: 'SUSPECTED_INFECTION', name: 'Lây nhiễm nghi ngờ', defaultLevel: 'MEDIUM', status: 'ACTIVE' },
]
const INFECTION = {
  id: 21,
  content: {
    residentId: 9,
    incidentTypeId: 40,
    discoveredAt: '2026-11-08T02:00:00Z',
    description: 'Sốt',
    level: 'MEDIUM',
  },
  currentTypeId: 40,
  status: 'IN_PROGRESS',
  level: 'MEDIUM',
}
const ZONE = {
  id: 3,
  scope: { floorIds: [101] },
  incidentIds: [21],
  reason: 'Ho sốt',
  status: 'ACTIVE',
  startedAt: '2026-11-08T03:00:00Z',
}
const PERSON = {
  type: 'RESIDENT',
  subjectId: 10,
  fullName: 'Lê Văn Bình',
  group: 'SAME_ROOM',
  source: 'M03',
  contactFrom: '2026-11-05T00:00:00Z',
  contactTo: '2026-11-08T00:00:00Z',
}

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/isolations', json(200, [ZONE]))
  api.on('GET', '/floors', json(200, [{ id: 101, code: 'T2', name: 'Tầng 2', areaId: 1 }]))
  api.on('GET', '/areas', json(200, [{ id: 1, code: 'A', name: 'Khu A' }]))
  api.on('GET', '/incidents', json(200, [INFECTION]))
  api.on('GET', '/incident-types', json(200, TYPES))
  api.on(
    'GET',
    '/residents/9',
    json(200, { id: 9, profileCode: 'HS-0009', personalInfo: { fullName: 'Trần Thị Mai' } }),
  )
})

afterEach(() => vi.unstubAllGlobals())

describe('InfectionControlPage', () => {
  it('FR_064_vungDangKhoanh_tenTangVaSuCo', async () => {
    render(<InfectionControlPage role="DOCTOR" />)
    const zones = await screen.findByRole('region', { name: 'Vùng đang khoanh' })
    expect(await within(zones).findByText('Tầng 2')).toBeTruthy()
    expect(within(zones).getByText(/Ho sốt/)).toBeTruthy()
  })

  it('FR_065_goKhoanhVung_batBuocLyDo', async () => {
    api.on('POST', '/isolations/3/lift', json(200, {}))
    render(<InfectionControlPage role="DOCTOR" />)
    const zones = await screen.findByRole('region', { name: 'Vùng đang khoanh' })
    fireEvent.click(await within(zones).findByRole('button', { name: 'Gỡ khoanh vùng' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu và gỡ' }))
    expect(await screen.findByText('Nhập lý do')).toBeTruthy()
    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Hết ca nghi nhiễm' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu và gỡ' }))
    await waitFor(() => expect(api.calls('POST', '/isolations/3/lift')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/isolations/3/lift')[0][1].body)).toEqual({ reason: 'Hết ca nghi nhiễm' })
  })

  it('FR_064_khoanhVung_tangVaSuCo', async () => {
    api.on('POST', '/isolations', json(201, ZONE))
    render(<InfectionControlPage role="DOCTOR" />)
    fireEvent.click(await screen.findByRole('button', { name: 'Khoanh vùng' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Tầng 2' }))
    fireEvent.click(screen.getByRole('button', { name: /Trần Thị Mai/ }))
    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Ho sốt nhiều người' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu khoanh vùng' }))
    await waitFor(() => expect(api.calls('POST', '/isolations')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/isolations')[0][1].body)).toEqual({
      scope: { floorIds: [101] },
      incidentIds: [21],
      reason: 'Ho sốt nhiều người',
    })
  })

  it('FR_060_lapDanhSach_traCuu_themNguoi_xacNhan', async () => {
    let list = null
    api.on('GET', '/incidents/21/contact-list', () => (list ? json(200, list) : problem(404, 'NOT_FOUND')))
    api.on('POST', '/incidents/21/contact-list', () => {
      list = { id: 5, incidentId: 21, status: 'DRAFT', contacts: [] }
      return json(201, list)
    })
    api.on('GET', '/incidents/21/contact-lookup', json(200, { group: [{ group: 'SAME_ROOM', people: [PERSON] }] }))
    api.on('POST', '/contact-lists/5/contacts', () => {
      list = { ...list, contacts: [{ ...PERSON, id: 77 }] }
      return json(201, {})
    })
    api.on('POST', '/contact-lists/5/confirm', json(200, {}))
    render(<InfectionControlPage role="NURSING_STAFF" />)
    const incidents = await screen.findByRole('region', { name: 'Sự cố lây nhiễm nghi ngờ' })
    fireEvent.click(await within(incidents).findByRole('button', { name: /Trần Thị Mai/ }))
    fireEvent.click(await screen.findByRole('button', { name: 'Lập danh sách tiếp xúc' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Tra cứu tiếp xúc' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Thêm Lê Văn Bình' }))
    await waitFor(() => expect(api.calls('POST', '/contact-lists/5/contacts')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/contact-lists/5/contacts')[0][1].body)).toEqual(PERSON)
    const listRegion = await screen.findByRole('region', { name: 'Danh sách tiếp xúc' })
    expect(await within(listRegion).findByText('Lê Văn Bình')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận danh sách' }))
    await waitFor(() => expect(api.calls('POST', '/contact-lists/5/confirm')).toHaveLength(1))
  })

  it('FR_080_403_khongCoQuyen', async () => {
    api.on('GET', '/isolations', problem(403, 'PERMISSION_DENIED'))
    render(<InfectionControlPage role="CAREGIVER" />)
    expect(await screen.findByText(/không có quyền/i)).toBeTruthy()
  })
})

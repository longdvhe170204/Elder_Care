import { afterEach, expect, it, vi } from 'vitest'
import * as api from '@/api/api007'

// FR-010/016: client sinh từ hợp đồng dùng cùng đường dẫn tiếng Anh với backend.
afterEach(() => vi.unstubAllGlobals())
it('FR_010_FR_016_goiDungBaOperationTiengAnh', async () => {
  const fetch = vi.fn(() => Promise.resolve(new Response('[]', {
    status: 200, headers: { 'Content-Type': 'application/json' },
  })))
  vi.stubGlobal('fetch', fetch)
  await api.getResidentsByResidentIdVitalSigns({ path: { residentId: 42 } })
  await api.postResidentsByResidentIdVitalSigns({ path: { residentId: 42 }, body: { indicatorId: 2 } })
  await api.postVitalSignRecordsByIdRemeasure({ path: { id: 7 }, body: { values: { VALUE: 98 } } })
  expect(fetch.mock.calls.map(([url, init]) => [init.method, url])).toEqual([
    ['GET', '/api/v1/residents/42/vital-signs'],
    ['POST', '/api/v1/residents/42/vital-signs'],
    ['POST', '/api/v1/vital-sign-records/7/remeasure'],
  ])
})

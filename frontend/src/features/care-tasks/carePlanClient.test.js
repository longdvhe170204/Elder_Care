import { afterEach, expect, it, vi } from 'vitest'
import * as client from '@/api'
import { json, mockRoutes } from '@/test/fetchRoutes'

afterEach(() => vi.unstubAllGlobals())

it('FR_004_clientSinhChoKeHoachGuiIfMatchVaIdempotencyKey', async () => {
  expect(client.putCarePlansByIdItems).toBeTypeOf('function')
  const routes = mockRoutes()
  routes.on('PUT', '/care-plans/7/items', json(200, null))
  await client.putCarePlansByIdItems({
    path: { id: 7 },
    body: [],
    headers: { 'If-Match': '"3"', 'Idempotency-Key': 'care-test' },
  })
  const [, options] = routes.calls('PUT', '/care-plans/7/items')[0]
  expect(options.headers['If-Match']).toBe('"3"')
  expect(options.headers['Idempotency-Key']).toBe('care-test')
  expect(options.body).toBe('[]')
})

it('T017_chiCongBoClientCacOperation005DaDichVaDuyet', () => {
  for (const name of [
    'getResidentsByResidentIdCarePlans',
    'postResidentsByResidentIdCarePlans',
    'getCarePlansById',
    'getCarePlansCompare',
    'putCarePlansByIdRiskAgreements',
    'getTaskTypes',
    'putTaskTypesById',
    'getResultValues',
    'getNonPerformanceReasonTypes',
  ]) {
    expect(client[name], name).toBeTypeOf('function')
  }
  expect(client.getThoiKhoaBieu).toBeUndefined()
})

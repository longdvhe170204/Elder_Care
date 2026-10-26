import { afterEach, expect, it, vi } from 'vitest'
import { request } from '../src/api'
afterEach(() => vi.unstubAllGlobals())
it('FR_001b_clientGiuFormData_vaKhongGanContentType', async () => {
  const form = new FormData()
  form.append('file', new File(['x'], 'staff.xlsx'))
  const fetch = vi
    .fn()
    .mockResolvedValue(
      new Response(JSON.stringify({ canConfirm: true }), { headers: { 'Content-Type': 'application/json' } }),
    )
  vi.stubGlobal('fetch', fetch)
  expect((await request('POST', '/staff/import/preview', { body: form })).data.canConfirm).toBe(true)
  const options = fetch.mock.calls[0][1]
  expect(options.body).toBe(form)
  expect(options.headers['Content-Type']).toBeUndefined()
  expect(options.headers['X-Requested-From']).toBe('web')
})
it('FR_001b_clientTaiFileMauBangBlob', async () => {
  const blob = { size: 3 }
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      headers: new Headers({ 'Content-Type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }),
      blob: async () => blob,
    }),
  )
  expect((await request('GET', '/staff/import/template', { responseType: 'blob' })).data).toBe(blob)
  expect(globalThis.fetch.mock.calls[0][1].headers.Accept).toBe('*/*')
})
it('R_03_clientJSONVanGuiDoc_vaGiuETag', async () => {
  const fetch = vi
    .fn()
    .mockResolvedValue(new Response('{"id":3}', { headers: { 'Content-Type': 'application/json', ETag: '"2"' } }))
  vi.stubGlobal('fetch', fetch)
  const r = await request('POST', '/staff', { body: { fullName: '[Nhân viên]' } })
  expect(r.data).toEqual({ id: 3 })
  expect(r.etag).toBe('"2"')
  expect(fetch.mock.calls[0][1].body).toBe('{"fullName":"[Nhân viên]"}')
  expect(fetch.mock.calls[0][1].headers['Content-Type']).toBe('application/json')
})
it('R_03_taiBlobGapLoiVanDocProblemJSON', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(
      new Response('{"errorCode":"PERMISSION_DENIED"}', {
        status: 403,
        headers: { 'Content-Type': 'application/problem+json' },
      }),
    ),
  )
  await expect(request('GET', '/staff/import/template', { responseType: 'blob' })).rejects.toMatchObject({
    status: 403,
    problem: { errorCode: 'PERMISSION_DENIED' },
  })
})

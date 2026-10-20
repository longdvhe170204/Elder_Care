import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { request } from '@/api'

// 002 T011 (research A-02): web gửi token bằng cookie, mọi lệnh ghi phải kèm X-Requested-From: web để chặn CSRF.

let fetchMock

beforeEach(() => {
  fetchMock = vi.fn(() => Promise.resolve(new Response('{}', { status: 200, headers: { 'Content-Type': 'application/json' } })))
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('request', () => {
  it('A_02_lenhGhi_guiXRequestedFromWeb', async () => {
    await request('POST', '/approval-requests', { body: {} })

    expect(fetchMock.mock.calls[0][1].headers['X-Requested-From']).toBe('web')
  })

  it('A_02_lenhDoc_khongCanHeader', async () => {
    await request('GET', '/me')

    expect(fetchMock.mock.calls[0][1].headers['X-Requested-From']).toBeUndefined()
  })
})

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { IfAllowed, PermissionsProvider, isAllowed } from './permissions'

// 002 T026 (FR-042): client ẩn nút, menu theo GET /me/permissions và allowedCommands; chỉ để hiển thị, backend luôn kiểm lại.

function json(status, body) {
  return Promise.resolve(new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } }))
}

let fetchMock

beforeEach(() => {
  fetchMock = vi.fn()
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  vi.unstubAllGlobals()
})

const PERMISSIONS = [
  { function: 'RESIDENT_PROFILE', operation: 'VIEW', scope: 'ASSIGNED', footnoteCodes: [], source: 'ROLE' },
  { function: 'TASK_CHECKLIST', operation: 'CREATE', scope: 'ASSIGNED', footnoteCodes: [], source: 'ROLE' },
]

describe('IfAllowed', () => {
  it('FR_042_coQuyenTheoMePermissions_hienNut_khongCo_an', async () => {
    fetchMock.mockReturnValue(json(200, PERMISSIONS))
    render(
      <PermissionsProvider>
        <IfAllowed function="TASK_CHECKLIST" operation="CREATE">
          <button>Ghi nhận công việc</button>
        </IfAllowed>
        <IfAllowed function="RESIDENT_PROFILE" operation="CREATE">
          <button>Lập hồ sơ</button>
        </IfAllowed>
      </PermissionsProvider>,
    )

    expect(await screen.findByRole('button', { name: 'Ghi nhận công việc' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Lập hồ sơ' })).not.toBeInTheDocument()
    expect(new URL(fetchMock.mock.calls[0][0], 'http://localhost').pathname).toBe('/api/v1/me/permissions')
  })

  it('FR_042_chuaTaiXongHoacLoi_anHet', async () => {
    fetchMock.mockReturnValue(json(401, { errorCode: 'UNAUTHORIZED' }))
    render(
      <PermissionsProvider>
        <IfAllowed function="TASK_CHECKLIST" operation="CREATE">
          <button>Ghi nhận công việc</button>
        </IfAllowed>
      </PermissionsProvider>,
    )

    expect(screen.queryByRole('button', { name: 'Ghi nhận công việc' })).not.toBeInTheDocument()
    await vi.waitFor(() => expect(fetchMock).toHaveBeenCalled())
    expect(screen.queryByRole('button', { name: 'Ghi nhận công việc' })).not.toBeInTheDocument()
  })

  it('FR_042_theoAllowedCommandsCuaDoiTuong', () => {
    const request = { id: 7, allowedCommands: ['approve'] }

    render(
      <>
        <IfAllowed resource={request} command="approve">
          <button>Duyệt</button>
        </IfAllowed>
        <IfAllowed resource={request} command="cancel">
          <button>Hủy</button>
        </IfAllowed>
      </>,
    )

    expect(screen.getByRole('button', { name: 'Duyệt' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Hủy' })).not.toBeInTheDocument()
    expect(isAllowed({ allowedCommands: null }, 'approve')).toBe(false)
  })
})

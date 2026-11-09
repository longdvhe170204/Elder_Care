import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { json, mockRoutes } from '@/test/fetchRoutes'
import { EndingCondition } from './EndingCondition'

const catalogs = { typeName: () => 'Điện thoại', locationName: () => 'Kho tầng 2' }
const PHONE = {
  id: 90,
  residentId: 7,
  typeId: 2,
  description: 'Điện thoại Nokia',
  quantity: 1,
  status: 'HELD',
  version: 1,
}
const CASH = {
  id: 92,
  residentId: 7,
  typeId: 3,
  description: 'Tiền mặt',
  cashAmount: 500000,
  status: 'HELD',
  version: 1,
}

function setup() {
  const api = mockRoutes()
  api.on('GET', '/me/permissions', json(200, [{ function: 'BELONGINGS', operation: 'CREATE' }]))
  api.on(
    'GET',
    '/residents/7/belongings/ending-condition',
    json(200, {
      met: false,
      blocking: [PHONE, CASH],
      lostWithOpenIncident: [],
      postDeathItem: { status: 'IN_PROGRESS', basis: 'Còn 2 đồ chưa trả' },
    }),
  )
  render(
    <PermissionsProvider>
      <EndingCondition residentId={7} catalogs={catalogs} />
    </PermissionsProvider>,
  )
  return api
}

describe('EndingCondition (013 T026, US5)', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('BR_M12_03_chuaDat_lietKeDoChan_vaMucSauQuaDoi', async () => {
    setup()
    expect(await screen.findByText('Chưa đạt')).toBeInTheDocument()
    expect(screen.getByText(/Điện thoại Nokia/)).toBeInTheDocument()
    expect(screen.getByText(/Còn 2 đồ chưa trả/)).toBeInTheDocument()
  })

  it('BR_M12_08_deNghiXuLy_quaYeuCauPheDuyet', async () => {
    const api = setup()
    api.on('POST', '/files', json(201, { fileId: 801 }))
    api.on('POST', '/approval-requests', json(201, { id: 5, displayCode: 'YC-5' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Lập đề nghị xử lý đồ không người nhận' }))
    fireEvent.click(screen.getByRole('checkbox', { name: 'Xử lý Tiền mặt' }))
    fireEvent.change(screen.getByLabelText('Cách xử lý'), { target: { value: 'TRANSFER_TO_AUTHORITY' } })
    fireEvent.change(screen.getByLabelText('Đơn vị nhận'), { target: { value: 'UBND phường' } })
    fireEvent.change(screen.getByLabelText('Biên bản'), {
      target: { files: [new File(['b'], 'bb.pdf', { type: 'application/pdf' })] },
    })
    fireEvent.change(screen.getByLabelText('Mã người thân đã liên hệ (lần 1)'), { target: { value: '33' } })
    fireEvent.change(screen.getByLabelText('Kênh (lần 1)'), { target: { value: 'PHONE_CALL' } })
    fireEvent.change(screen.getByLabelText('Kết quả (lần 1)'), { target: { value: 'Không nghe máy' } })
    fireEvent.change(screen.getByLabelText('Ngày áp dụng đề nghị'), { target: { value: '2026-12-01' } })
    fireEvent.change(screen.getByLabelText('Lý do đề nghị'), { target: { value: 'Quá hạn nhận đồ' } })
    fireEvent.click(screen.getByRole('button', { name: 'Gửi đề nghị duyệt' }))

    await waitFor(() => expect(api.calls('POST', '/approval-requests')).toHaveLength(1))
    const body = JSON.parse(api.calls('POST', '/approval-requests')[0][1].body)
    expect(body).toMatchObject({
      type: 'M12.UNCLAIMED_BELONGING_DISPOSAL',
      requestedEffectiveDate: '2026-12-01',
      reason: 'Quá hạn nhận đồ',
      submitImmediately: true,
      content: {
        residentId: 7,
        belongingIds: [92],
        method: 'TRANSFER_TO_AUTHORITY',
        receivingUnit: 'UBND phường',
        minutesFileId: 801,
        contactAttempts: [{ familyMemberId: 33, channel: 'PHONE_CALL', result: 'Không nghe máy' }],
      },
    })
    expect(body.content.contactAttempts[0].at).toMatch(/\+07:00$/)
    expect(await screen.findByText('Đã gửi đề nghị duyệt, mã yêu cầu YC-5.')).toBeInTheDocument()
  })
})

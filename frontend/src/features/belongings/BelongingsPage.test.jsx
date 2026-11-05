import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { PermissionsProvider } from '@/common/permissions/permissions'
import { json, mockRoutes } from '@/test/fetchRoutes'
import { BelongingsPage } from './BelongingsPage'

const TYPES = [
  { id: 1, name: 'Quần áo', hasCashAmount: false, requiresConsentForUse: true, valuable: false, status: 'ACTIVE' },
  { id: 2, name: 'Điện thoại', hasCashAmount: false, requiresConsentForUse: true, valuable: true, status: 'ACTIVE' },
]
const PHONE = {
  id: 90,
  residentId: 7,
  typeId: 2,
  description: 'Điện thoại Nokia',
  quantity: 1,
  status: 'HELD',
  holder: 'STAFF',
  storageLocationId: 21,
  lastHandoverAt: '2026-11-06T02:00:00Z',
  version: 1,
}

function setup(permissions = ['BELONGINGS:VIEW', 'BELONGINGS:CREATE', 'BELONGINGS:UPDATE']) {
  const api = mockRoutes()
  api.on(
    'GET',
    '/me/permissions',
    json(
      200,
      permissions.map((k) => ({ function: k.split(':')[0], operation: k.split(':')[1] })),
    ),
  )
  api.on('GET', '/belongings/types', json(200, TYPES))
  api.on(
    'GET',
    '/belongings/condition-levels',
    json(200, [{ id: 11, name: 'Nguyên vẹn', order: 1, unusable: false, status: 'ACTIVE' }]),
  )
  api.on('GET', '/belongings/storage-locations', json(200, [{ id: 21, name: 'Kho tầng 2', status: 'ACTIVE' }]))
  api.on(
    'GET',
    '/residents',
    json(200, {
      items: [{ id: 7, profileCode: 'NCT-0007', personalInfo: { fullName: 'Nguyễn Thị Lan' } }],
      nextCursor: null,
    }),
  )
  api.on('GET', '/residents/7/belongings', json(200, [PHONE]))
  api.on(
    'GET',
    '/belongings/90',
    json(200, {
      belonging: PHONE,
      handovers: [
        {
          id: 300,
          belongingId: 90,
          kind: 'INTAKE',
          targetStatus: 'HELD',
          giverType: 'FAMILY_MEMBER',
          giverName: 'Trần Văn Bình',
          recipientType: 'STAFF',
          recipientName: 'Trần Thu Hà',
          occurredAt: '2026-11-06T02:00:00Z',
          quantity: 1,
          conditionDescription: 'Nguyên vẹn',
          photoFileIds: [501],
        },
      ],
    }),
  )
  render(
    <PermissionsProvider>
      <BelongingsPage role="OFFICE_STAFF" />
    </PermissionsProvider>,
  )
  return api
}

async function openResident() {
  fireEvent.change(screen.getByLabelText('Tìm người cao tuổi'), { target: { value: 'Lan' } })
  fireEvent.click(screen.getByRole('button', { name: 'Tìm' }))
  fireEvent.click(await screen.findByRole('button', { name: 'Nguyễn Thị Lan, NCT-0007' }))
}

describe('BelongingsPage (013 T011, US1)', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('FR_011_danhSachDoGui_loaiTrangThaiNguoiGiuViTri', async () => {
    setup()
    await openResident()
    const row = (await screen.findByText('Điện thoại Nokia')).closest('tr')
    expect(within(row).getByText('Điện thoại')).toBeInTheDocument()
    expect(within(row).getByText('Đang giữ')).toBeInTheDocument()
    expect(within(row).getByText('Nhân viên')).toBeInTheDocument()
    expect(within(row).getByText('Kho tầng 2')).toBeInTheDocument()
  })

  it('FR_012_chiTietDoGui_lichSuBanGiao', async () => {
    setup()
    await openResident()
    fireEvent.click(await screen.findByRole('button', { name: 'Chi tiết Điện thoại Nokia' }))
    expect(await screen.findByText('Tiếp nhận')).toBeInTheDocument()
    expect(screen.getByText(/Trần Văn Bình → Trần Thu Hà/)).toBeInTheDocument()
  })

  it('FR_029_khongCoQuyenTao_khongCoNutTiepNhan', async () => {
    setup(['BELONGINGS:VIEW'])
    await openResident()
    await screen.findByText('Điện thoại Nokia')
    expect(screen.queryByRole('button', { name: 'Tiếp nhận đồ' })).not.toBeInTheDocument()
  })

  it('FR_015a_ghiDongYTheoBanKy_guiMaNguoiDaiDienVaTep', async () => {
    const api = setup()
    api.on('POST', '/files', json(201, { fileId: 640 }))
    api.on('POST', '/self-custody-consents', json(201, { id: 55 }))
    await openResident()
    fireEvent.click(await screen.findByRole('button', { name: 'Chi tiết Điện thoại Nokia' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Ghi đồng ý cho tự giữ' }))
    fireEvent.change(screen.getByLabelText('Mã người đại diện'), { target: { value: '33' } })
    fireEvent.change(screen.getByLabelText('Bản ký của người đại diện'), {
      target: { files: [new File(['p'], 'dong-y.pdf', { type: 'application/pdf' })] },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu đồng ý' }))

    await waitFor(() => expect(api.calls('POST', '/self-custody-consents')).toHaveLength(1))
    const [, init] = api.calls('POST', '/self-custody-consents')[0]
    expect(JSON.parse(init.body)).toEqual({ belongingId: 90, representativeId: 33, signedFormFileId: 640 })
    expect(await screen.findByText('Đã ghi đồng ý cho tự giữ.')).toBeInTheDocument()
  })
})

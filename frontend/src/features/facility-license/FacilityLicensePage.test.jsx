import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { json, mockRoutes } from '@/test/fetchRoutes'
import { FacilityLicensePage } from './FacilityLicensePage'

// 007 US9 (T057 phần web): giấy phép cơ sở và lịch sử, số liên hệ của viện (đổi gửi If-Match theo version), bản ghi khám
// và điều trị của một người cao tuổi theo api-007. Lưu giấy phép, đổi số liên hệ chỉ Quản lý viện (FR-070).

let api

const ACTIVE = {
  id: 2,
  number: 'GP-02',
  issuer: 'Sở Y tế',
  issuedOn: '2026-01-01',
  expiresOn: '2031-01-01',
  coversMedicalPractice: true,
  scopes: ['Khám nội khoa'],
  active: true,
  replaces: 1,
}
const OLD = { ...ACTIVE, id: 1, number: 'GP-01', active: false, replaces: null }
const EXAM = {
  id: 8,
  type: 'EXAMINATION',
  content: 'Khám định kỳ',
  atFacility: true,
  performedAt: '2026-11-07T02:00:00Z',
}

beforeEach(() => {
  api = mockRoutes()
  api.on('GET', '/facility-licenses', json(200, [ACTIVE, OLD]))
  api.on('GET', '/facility-contact', json(200, { phoneNumber: '0281234567', note: 'Trực 24 giờ', version: 3 }))
  api.on(
    'GET',
    '/residents',
    json(200, { items: [{ id: 9, profileCode: 'HS-0009', personalInfo: { fullName: 'Trần Thị Mai' } }] }),
  )
  api.on('GET', '/residents/9/examinations', json(200, [EXAM]))
})

afterEach(() => vi.unstubAllGlobals())

describe('FacilityLicensePage', () => {
  it('FR_070_giayPhepHieuLucVaLichSu_soLienHe', async () => {
    render(<FacilityLicensePage role="NURSING_HOME_MANAGER" />)
    const licenses = await screen.findByRole('region', { name: 'Giấy phép cơ sở' })
    expect(await within(licenses).findByText('GP-02')).toBeTruthy()
    expect(within(licenses).getByText('Đang hiệu lực')).toBeTruthy()
    expect(within(licenses).getAllByText(/Có phạm vi khám bệnh, chữa bệnh/)).toHaveLength(2)
    expect(within(licenses).getByText('GP-01')).toBeTruthy()
    const contact = screen.getByRole('region', { name: 'Số liên hệ của viện' })
    expect(await within(contact).findByText('0281234567')).toBeTruthy()
  })

  it('FR_070_luuGiayPhepMoi', async () => {
    api.on('POST', '/facility-licenses', json(201, {}))
    render(<FacilityLicensePage role="NURSING_HOME_MANAGER" />)
    fireEvent.click(await screen.findByRole('button', { name: 'Lưu giấy phép mới' }))
    fireEvent.change(await screen.findByLabelText('Số giấy phép'), { target: { value: 'GP-03' } })
    fireEvent.change(screen.getByLabelText('Nơi cấp'), { target: { value: 'Sở Y tế' } })
    fireEvent.change(screen.getByLabelText('Ngày cấp'), { target: { value: '2026-11-01' } })
    fireEvent.change(screen.getByLabelText('Ngày hết hạn'), { target: { value: '2031-11-01' } })
    fireEvent.click(screen.getByRole('button', { name: 'Có phạm vi khám bệnh, chữa bệnh' }))
    fireEvent.click(screen.getByRole('button', { name: 'Lưu giấy phép' }))
    await waitFor(() => expect(api.calls('POST', '/facility-licenses')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/facility-licenses')[0][1].body)).toEqual({
      number: 'GP-03',
      issuer: 'Sở Y tế',
      issuedOn: '2026-11-01',
      expiresOn: '2031-11-01',
      coversMedicalPractice: true,
      scopes: [],
      replaces: 2,
    })
  })

  it('FR_070_doiSoLienHe_guiIfMatch', async () => {
    api.on('PUT', '/facility-contact', json(200, { phoneNumber: '0289999999', version: 4 }))
    render(<FacilityLicensePage role="NURSING_HOME_MANAGER" />)
    const contact = await screen.findByRole('region', { name: 'Số liên hệ của viện' })
    fireEvent.click(await within(contact).findByRole('button', { name: 'Đổi số liên hệ' }))
    fireEvent.change(screen.getByLabelText('Số điện thoại'), { target: { value: '0289999999' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu số liên hệ' }))
    await waitFor(() => expect(api.calls('PUT', '/facility-contact')).toHaveLength(1))
    const [, init] = api.calls('PUT', '/facility-contact')[0]
    expect(new Headers(init.headers).get('If-Match')).toBe('"3"')
  })

  it('FR_074_khamVaDieuTri_xemVaGhi', async () => {
    api.on('POST', '/residents/9/examinations', json(201, {}))
    render(<FacilityLicensePage role="DOCTOR" />)
    fireEvent.click(await screen.findByRole('button', { name: 'Trần Thị Mai, HS-0009' }))
    const exams = await screen.findByRole('region', { name: 'Khám và điều trị' })
    expect(await within(exams).findByText('Khám định kỳ')).toBeTruthy()
    fireEvent.click(within(exams).getByRole('button', { name: 'Ghi khám, điều trị' }))
    fireEvent.click(screen.getByRole('button', { name: 'Chẩn đoán' }))
    fireEvent.change(screen.getByLabelText('Nội dung'), { target: { value: 'Tăng huyết áp' } })
    fireEvent.change(screen.getByLabelText('Thời điểm'), { target: { value: '2026-11-08T09:00' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lưu bản ghi' }))
    await waitFor(() => expect(api.calls('POST', '/residents/9/examinations')).toHaveLength(1))
    expect(JSON.parse(api.calls('POST', '/residents/9/examinations')[0][1].body)).toEqual({
      type: 'DIAGNOSIS',
      content: 'Tăng huyết áp',
      atFacility: true,
      performedAt: '2026-11-08T09:00:00+07:00',
    })
  })

  it('FR_070_khongPhaiQuanLy_khongCoNutLuu', async () => {
    render(<FacilityLicensePage role="DOCTOR" />)
    await screen.findByText('GP-02')
    expect(screen.queryByRole('button', { name: 'Lưu giấy phép mới' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Đổi số liên hệ' })).toBeNull()
  })
})

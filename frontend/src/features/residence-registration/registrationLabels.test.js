import { describe, expect, it } from 'vitest'
import {
  CHANNEL_LABELS,
  CORRECTION_FIELDS,
  RESULT_LABELS,
  STATUS_LABELS,
  STATUS_TONES,
  TASK_LABELS,
  TASK_TONES,
  TYPE_LABELS,
  compareTasks,
} from './registrationLabels'

// Nhãn và tông của 018 theo api-018 và DESIGN.md (bố cục K5): mọi giá trị enum của hợp đồng đều có nhãn tiếng Việt.
describe('registrationLabels (018)', () => {
  it('K5_moiTrangThaiCuaHopDong_coNhanVaTong_hetHanLaUrgent', () => {
    const statuses = ['TO_SUBMIT', 'SUBMITTED', 'CONFIRMED', 'EXPIRED', 'DEREGISTERED', 'SUPERSEDED', 'CANCELLED']
    for (const status of statuses) {
      expect(STATUS_LABELS[status]).toBeTruthy()
      expect(STATUS_TONES[status]).toBeTruthy()
    }
    expect(STATUS_TONES.EXPIRED).toBe('urgent')
    expect(STATUS_TONES.TO_SUBMIT).toBe('attention')
    expect(STATUS_TONES.CONFIRMED).toBe('settled')
    for (const status of ['SUPERSEDED', 'CANCELLED', 'DEREGISTERED']) {
      expect(STATUS_TONES[status]).toBe('neutral-outline')
    }
  })

  it('K2_moiLoaiViec_coNhan_quaHanLaUrgent_canLapLaAttention', () => {
    for (const task of ['TO_CREATE', 'OVERDUE', 'EXPIRING_SOON', 'EXPIRED', 'TO_DEREGISTER']) {
      expect(TASK_LABELS[task]).toBeTruthy()
    }
    expect(TASK_TONES.OVERDUE).toBe('urgent')
    expect(TASK_TONES.TO_CREATE).toBe('attention')
    expect(TASK_LABELS.OVERDUE).toBe('Quá hạn')
    expect(TASK_LABELS.TO_DEREGISTER).toBe('Cần khai báo xóa')
  })

  it('nhanLoaiKenhKetQua', () => {
    expect(TYPE_LABELS.TEMPORARY_RESIDENCE_REGISTRATION).toBe('Đăng ký tạm trú')
    expect(TYPE_LABELS.STAY_NOTIFICATION).toBe('Thông báo lưu trú')
    expect(CHANNEL_LABELS.PUBLIC_SERVICE_PORTAL).toBe('Cổng dịch vụ công')
    expect(CHANNEL_LABELS.IN_PERSON).toBe('Trực tiếp')
    expect(RESULT_LABELS).toEqual({ PENDING: 'Chờ kết quả', ACCEPTED: 'Được chấp nhận', REJECTED: 'Bị từ chối' })
  })

  it('K2_quaHanDungTruoc_roiTheoHan_khongHanDungCuoi', () => {
    const tasks = [
      { residentId: 1, taskType: 'TO_DEREGISTER' },
      { residentId: 2, taskType: 'TO_CREATE', dueAt: '2026-11-10T00:00:00Z' },
      { residentId: 3, taskType: 'OVERDUE', dueAt: '2026-11-05T00:00:00Z' },
      { residentId: 4, taskType: 'OVERDUE', dueAt: '2026-11-01T00:00:00Z' },
    ]
    expect(tasks.toSorted(compareTasks).map((t) => t.residentId)).toEqual([4, 3, 2, 1])
  })

  it('dinhChinh_coCacOGiongNoiDungGocCuaBackend', () => {
    expect(CORRECTION_FIELDS.map((f) => f.name)).toEqual([
      'localPermanentResident',
      'basisNote',
      'submittedDate',
      'channel',
      'receivingAuthority',
      'dossierCode',
      'result',
      'rejectionReason',
      'expiryDate',
      'evidenceFileId',
    ])
  })
})

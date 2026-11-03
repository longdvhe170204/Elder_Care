import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { formatDateTime } from '@/common/dateTime'
import { CareField, CareCheck } from './CareField'

const SOURCES = { RESIDENT: 'Người cao tuổi', FAMILY_MEMBER: 'Người thân', OBSERVING_STAFF: 'Nhân viên quan sát' }
const MAKERS = { RESIDENT: 'Người cao tuổi', PATIENT_REPRESENTATIVE: 'Người đại diện của người bệnh', DOCTOR: 'Bác sĩ' }
const blank = () => ({
  choice: '',
  discussedRisks: '',
  residentParticipates: false,
  participants: [],
  assessingDoctorId: '',
  selfDeterminationAssessment: { canSelfDetermine: '', supportMethod: '', reviewDate: '' },
  decisionMaker: { type: '' },
  opinions: [],
  reviewDate: '',
})

export function CareRiskAgreementsForm({ agreements, busy, onSave }) {
  const [rows, setRows] = useState(agreements)
  const [error, setError] = useState('')
  const set = (index, patch) => setRows((all) => all.map((row, i) => (i === index ? { ...row, ...patch } : row)))
  function submit(event) {
    event.preventDefault()
    if (rows.some((row) => row.selfDeterminationAssessment.canSelfDetermine === '')) {
      setError('Chọn nhận định tự quyết cho từng thỏa thuận.')
      return
    }
    setError('')
    onSave(
      rows.map((row) => ({
        id: row.id,
        sourceAgreementId: row.sourceAgreementId,
        choice: row.choice,
        discussedRisks: row.discussedRisks,
        residentParticipates: row.residentParticipates,
        participants: (row.participants ?? []).map((p) => ({ type: p.type, id: Number(p.id) })),
        assessingDoctorId: Number(row.assessingDoctorId),
        selfDeterminationAssessment: {
          canSelfDetermine: String(row.selfDeterminationAssessment.canSelfDetermine) === 'true',
          supportMethod: row.selfDeterminationAssessment.supportMethod,
          reviewDate: row.selfDeterminationAssessment.reviewDate,
        },
        decisionMaker: {
          type: row.decisionMaker.type,
          familyMemberId:
            row.decisionMaker.type === 'PATIENT_REPRESENTATIVE' ? Number(row.decisionMaker.familyMemberId) : null,
          doctorId: row.decisionMaker.type === 'DOCTOR' ? Number(row.decisionMaker.doctorId) : null,
          reason: row.decisionMaker.type === 'DOCTOR' ? row.decisionMaker.reason : null,
        },
        opinions: (row.opinions ?? []).map((opinion) => ({
          source: {
            type: opinion.source.type,
            familyMemberId: opinion.source.type === 'FAMILY_MEMBER' ? Number(opinion.source.familyMemberId) : null,
          },
          content: opinion.content,
        })),
        reviewDate: row.reviewDate,
      })),
    )
  }
  return (
    <form onSubmit={submit} className="flex flex-col gap-4">
      <h3 className="type-title">Thỏa thuận lựa chọn có rủi ro</h3>
      <p className="type-body-sm text-muted-foreground">
        Thỏa thuận đi theo phiên bản kế hoạch. Thỏa thuận không tạo hoặc chặn công việc.
      </p>
      {error && (
        <p role="alert" className="text-destructive">
          {error}
        </p>
      )}
      {rows.map((row, index) => {
        const assessment = row.selfDeterminationAssessment
        const maker = row.decisionMaker
        return (
          <fieldset
            key={row.id ?? `new-${index}`}
            disabled={busy}
            className="flex min-w-0 flex-col gap-4 rounded-lg border p-4"
          >
            <legend className="type-label px-1">Thỏa thuận {index + 1}</legend>
            <CareField
              label={`Lựa chọn ${index + 1}`}
              value={row.choice}
              onChange={(value) => set(index, { choice: value })}
              type="textarea"
              maxLength={2000}
              required
            />
            <CareField
              label={`Rủi ro đã trao đổi ${index + 1}`}
              value={row.discussedRisks}
              onChange={(value) => set(index, { discussedRisks: value })}
              type="textarea"
              maxLength={2000}
              required
            />
            <CareCheck
              label={`Người cao tuổi tham gia trao đổi ${index + 1}`}
              checked={row.residentParticipates}
              onChange={(value) => set(index, { residentParticipates: value })}
            />
            <h4 className="type-label">Người tham gia trao đổi</h4>
            {(row.participants ?? []).map((p, pi) => (
              <div className="grid gap-3 sm:grid-cols-2" key={pi}>
                <CareField
                  label={`Loại người tham gia ${index + 1}.${pi + 1}`}
                  value={p.type}
                  onChange={(value) =>
                    set(index, {
                      participants: row.participants.map((entry, j) => (j === pi ? { ...entry, type: value } : entry)),
                    })
                  }
                  options={{ FAMILY_MEMBER: 'Người thân', STAFF: 'Nhân viên' }}
                  required
                />
                <CareField
                  label={`Mã người tham gia ${index + 1}.${pi + 1}`}
                  value={p.id}
                  onChange={(value) =>
                    set(index, {
                      participants: row.participants.map((entry, j) => (j === pi ? { ...entry, id: value } : entry)),
                    })
                  }
                  type="number"
                  min={1}
                  required
                />
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => set(index, { participants: row.participants.filter((_, j) => j !== pi) })}
                >
                  Bỏ người tham gia {pi + 1}
                </Button>
              </div>
            ))}
            <Button
              type="button"
              variant="outline"
              onClick={() => set(index, { participants: [...(row.participants ?? []), { type: '', id: '' }] })}
            >
              Thêm người tham gia thỏa thuận {index + 1}
            </Button>
            <CareField
              label={`Mã bác sĩ nhận định ${index + 1}`}
              value={row.assessingDoctorId}
              onChange={(value) => set(index, { assessingDoctorId: value })}
              type="number"
              min={1}
              required
            />
            <CareField
              label={`Nhận định tự quyết ${index + 1}`}
              value={String(assessment.canSelfDetermine)}
              onChange={(value) =>
                set(index, { selfDeterminationAssessment: { ...assessment, canSelfDetermine: value } })
              }
              options={{ true: 'Có thể tự quyết', false: 'Chưa thể tự quyết' }}
              required
            />
            <CareField
              label={`Cách đã hỗ trợ ${index + 1}`}
              value={assessment.supportMethod}
              onChange={(value) => set(index, { selfDeterminationAssessment: { ...assessment, supportMethod: value } })}
              type="textarea"
              maxLength={2000}
              required
            />
            <CareField
              label={`Ngày xem lại nhận định ${index + 1}`}
              value={assessment.reviewDate}
              onChange={(value) => set(index, { selfDeterminationAssessment: { ...assessment, reviewDate: value } })}
              type="date"
              required
            />
            {assessment.at && (
              <p className="type-body-sm text-muted-foreground">Nhận định ghi lúc {formatDateTime(assessment.at)}</p>
            )}
            <CareField
              label={`Người quyết định ${index + 1}`}
              value={maker.type}
              onChange={(value) => set(index, { decisionMaker: { type: value } })}
              options={MAKERS}
              required
            />
            {maker.type === 'PATIENT_REPRESENTATIVE' && (
              <CareField
                label={`Mã người thân đại diện ${index + 1}`}
                value={maker.familyMemberId}
                onChange={(value) => set(index, { decisionMaker: { ...maker, familyMemberId: value } })}
                type="number"
                min={1}
                required
              />
            )}
            {maker.type === 'DOCTOR' && (
              <>
                <CareField
                  label={`Mã bác sĩ quyết định ${index + 1}`}
                  value={maker.doctorId}
                  onChange={(value) => set(index, { decisionMaker: { ...maker, doctorId: value } })}
                  type="number"
                  min={1}
                  required
                />
                <CareField
                  label={`Căn cứ bác sĩ quyết định ${index + 1}`}
                  value={maker.reason}
                  onChange={(value) => set(index, { decisionMaker: { ...maker, reason: value } })}
                  type="textarea"
                  maxLength={1000}
                  required
                />
                <p className="type-body-sm text-muted-foreground">
                  Ghi căn cứ không có văn bản nguyện vọng trước và không có người đại diện của người bệnh.
                </p>
              </>
            )}
            <h4 className="type-label">Ý kiến có nguồn</h4>
            {(row.opinions ?? []).map((opinion, oi) => (
              <fieldset key={oi} className="flex flex-col gap-3 rounded-lg border p-3">
                <legend>Ý kiến {oi + 1}</legend>
                <CareField
                  label={`Nguồn ý kiến ${index + 1}.${oi + 1}`}
                  value={opinion.source.type}
                  onChange={(value) =>
                    set(index, {
                      opinions: row.opinions.map((entry, j) =>
                        j === oi ? { ...entry, source: { type: value } } : entry,
                      ),
                    })
                  }
                  options={SOURCES}
                  required
                  disabled={!!opinion.at}
                />
                {opinion.source.type === 'FAMILY_MEMBER' && (
                  <CareField
                    label={`Mã người thân nêu ý kiến ${index + 1}.${oi + 1}`}
                    value={opinion.source.familyMemberId}
                    onChange={(value) =>
                      set(index, {
                        opinions: row.opinions.map((entry, j) =>
                          j === oi ? { ...entry, source: { ...entry.source, familyMemberId: value } } : entry,
                        ),
                      })
                    }
                    type="number"
                    min={1}
                    required
                    disabled={!!opinion.at}
                  />
                )}
                <CareField
                  label={`Nội dung ý kiến ${index + 1}.${oi + 1}`}
                  value={opinion.content}
                  onChange={(value) =>
                    set(index, {
                      opinions: row.opinions.map((entry, j) => (j === oi ? { ...entry, content: value } : entry)),
                    })
                  }
                  type="textarea"
                  maxLength={2000}
                  required
                  disabled={!!opinion.at}
                />
                {opinion.at && (
                  <p className="type-body-sm text-muted-foreground">
                    {opinion.recordedBy} · {formatDateTime(opinion.at)}
                  </p>
                )}
              </fieldset>
            ))}
            <Button
              type="button"
              variant="outline"
              onClick={() => set(index, { opinions: [...(row.opinions ?? []), { source: { type: '' }, content: '' }] })}
            >
              Thêm ý kiến thỏa thuận {index + 1}
            </Button>
            <CareField
              label={`Ngày xem lại thỏa thuận ${index + 1}`}
              value={row.reviewDate}
              onChange={(value) => set(index, { reviewDate: value })}
              type="date"
              required
            />
            <Button type="button" variant="outline" onClick={() => setRows((all) => all.filter((_, i) => i !== index))}>
              Bỏ thỏa thuận {index + 1}
            </Button>
          </fieldset>
        )
      })}
      <div className="flex flex-wrap gap-3">
        <Button type="button" variant="outline" disabled={busy} onClick={() => setRows((all) => [...all, blank()])}>
          Thêm thỏa thuận
        </Button>
        <Button type="submit" disabled={busy}>
          Lưu thỏa thuận
        </Button>
      </div>
    </form>
  )
}

import { useEffect, useState } from 'react'
import {
  getResidents,
  getResidentsByResidentIdRelationships,
  getResidentsByResidentIdVisitPickupWishes,
  postFeedback,
} from '@/api'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Sheet, SheetContent, SheetDescription, SheetTitle } from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { ProblemAlert } from '@/common/ProblemAlert'
import { formatDate } from '@/common/dateTime'
import { problemOf } from '@/common/problem'
import { RecordWishSheet } from '../right-change-requests/VisitPickupWishes'
import { describeWish } from '../right-change-requests/requestLabels'
import { FEEDBACK_GROUPS, FEEDBACK_TYPES, WISH, WISH_TOPICS } from './feedbackLabels'

// Phản hồi của người cao tuổi trên màn nhân viên (012 T072; US9, FR-058a, FR-058b, BR-M10-12; bố cục duyệt 2026-11-05):
// form ghi lời người cao tuổi nói trực tiếp, khối gắn mục ý kiến của phản hồi Mong muốn.

const SELECT = 'h-11 w-full rounded-md border border-input bg-transparent px-3'

function Choices({ label, options, value, onChange, error }) {
  return (
    <Field data-invalid={!!error || undefined}>
      <FieldLabel>{label}</FieldLabel>
      <ToggleGroup
        variant="outline"
        value={value ? [value] : []}
        onValueChange={(next) => next[0] && onChange(next[0])}
        aria-label={label}
        className="flex-wrap"
      >
        {options.map(([code, text]) => (
          <ToggleGroupItem key={code} value={code} className="h-11 px-4">
            {text}
          </ToggleGroupItem>
        ))}
      </ToggleGroup>
      {error && <FieldError>{error}</FieldError>}
    </Field>
  )
}

/** Ghi phản hồi theo lời người cao tuổi nói trực tiếp (FR-058a (a)): Hành chính, Điều dưỡng có phạm vi. */
export function OnBehalfSheet({ onClose, onDone }) {
  const [key] = useState(() => crypto.randomUUID())
  const [residents, setResidents] = useState(null)
  const [residentId, setResidentId] = useState('')
  const [type, setType] = useState('')
  const [topic, setTopic] = useState('')
  const [group, setGroup] = useState('')
  const [content, setContent] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)

  useEffect(() => {
    let cancelled = false
    getResidents({ query: { status: ['IN_RESIDENCE'] } })
      .then(({ data }) => !cancelled && setResidents(data.items))
      .catch((e) => {
        if (!cancelled) {
          setResidents([])
          setFailure(problemOf(e))
        }
      })
    return () => {
      cancelled = true
    }
  }, [])

  const wish = type === 'WISH'
  const errors = {
    resident: !residentId && 'Chọn người cao tuổi',
    type: !type && 'Chọn loại',
    topic: wish && !topic && 'Chọn mong muốn về việc gì',
    group: !wish && !group && 'Chọn nội dung về việc gì',
    content: !content.trim() && 'Nhập nội dung người cao tuổi nói',
  }
  const show = (name) => submitted && errors[name]

  async function submit(event) {
    event.preventDefault()
    setSubmitted(true)
    if (Object.values(errors).some(Boolean)) return
    setBusy(true)
    setFailure(null)
    try {
      // Mong muốn: nhóm đặt theo chủ đề như backend (FR-059): người đến thăm, người đón → khác; việc khác → chăm sóc
      const body = {
        residentId: Number(residentId),
        senderType: 'RESIDENT',
        type,
        group: wish ? (topic === 'VISITORS_PICKERS' ? 'OTHER' : 'CARE') : group,
        content: content.trim(),
      }
      if (wish) body.wishTopic = topic
      await postFeedback({ headers: { 'Idempotency-Key': key }, body })
      onDone('Đã ghi phản hồi theo lời người cao tuổi')
    } catch (e) {
      setFailure(problemOf(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Sheet open onOpenChange={(next) => !next && onClose()}>
      <SheetContent className="overflow-y-auto p-6 sm:max-w-2xl">
        <SheetTitle className="type-title">Ghi phản hồi theo lời người cao tuổi</SheetTitle>
        <SheetDescription className="type-body-sm text-muted-foreground">
          Ghi đúng điều người cao tuổi nói trực tiếp với bạn.
        </SheetDescription>
        <form onSubmit={submit} noValidate className="mt-4 flex flex-col gap-4">
          <FieldGroup>
            <Field data-invalid={!!show('resident') || undefined}>
              <FieldLabel htmlFor="on-behalf-resident">Người cao tuổi</FieldLabel>
              <select
                id="on-behalf-resident"
                value={residentId}
                onChange={(e) => setResidentId(e.target.value)}
                aria-invalid={!!show('resident') || undefined}
                className={SELECT}
              >
                <option value="">{residents === null ? 'Đang tải…' : 'Chọn người'}</option>
                {(residents ?? []).map((r) => (
                  <option key={r.id} value={r.id}>
                    {`${r.personalInfo.fullName}, ${r.profileCode}`}
                  </option>
                ))}
              </select>
              {show('resident') && <FieldError>{errors.resident}</FieldError>}
            </Field>
            <Choices
              label="Loại"
              options={[...FEEDBACK_TYPES, WISH]}
              value={type}
              onChange={setType}
              error={show('type')}
            />
            {wish ? (
              <Choices
                label="Mong muốn về"
                options={WISH_TOPICS}
                value={topic}
                onChange={setTopic}
                error={show('topic')}
              />
            ) : (
              <Choices
                label="Nội dung về"
                options={FEEDBACK_GROUPS}
                value={group}
                onChange={setGroup}
                error={show('group')}
              />
            )}
            <Field data-invalid={!!show('content') || undefined}>
              <FieldLabel htmlFor="on-behalf-content">Nội dung</FieldLabel>
              <Textarea
                id="on-behalf-content"
                rows={3}
                value={content}
                onChange={(e) => setContent(e.target.value)}
                aria-invalid={!!show('content') || undefined}
              />
              {show('content') && <FieldError>{errors.content}</FieldError>}
            </Field>
          </FieldGroup>
          <ProblemAlert problem={failure} />
          <span className="flex flex-wrap gap-2">
            <Button type="submit" className="h-11" disabled={busy}>
              Lưu phản hồi
            </Button>
            <Button type="button" variant="ghost" className="h-11" onClick={onClose}>
              Quay lại
            </Button>
          </span>
        </form>
      </SheetContent>
    </Sheet>
  )
}

/**
 * Mục ý kiến của phản hồi Mong muốn (BR-M10-12): về người đến thăm, người đón thì gắn mong muốn đã ghi có nguồn người
 * cao tuổi hoặc ghi mới; việc khác là sở thích ghi ở hồ sơ hoạt động (014), mục tự gắn khi ghi.
 */
export function WishOpinions({ feedback, residentName, onLink }) {
  const [mode, setMode] = useState(null)
  const [wishes, setWishes] = useState(null)
  const [relationships, setRelationships] = useState([])
  const [chosen, setChosen] = useState('')
  const [error, setError] = useState('')
  const [failure, setFailure] = useState(null)
  const residentId = feedback.residentId

  function loadWishes() {
    return getResidentsByResidentIdVisitPickupWishes({ path: { residentId } })
      .then(({ data }) => setWishes(data.effective.filter((w) => w.source?.type === 'RESIDENT')))
      .catch((e) => {
        setWishes([])
        setFailure(problemOf(e))
      })
  }

  useEffect(() => {
    if (mode === null) return
    loadWishes()
    getResidentsByResidentIdRelationships({ path: { residentId } })
      .then(({ data }) => setRelationships(data))
      .catch(() => setRelationships([]))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mode, residentId])

  if (feedback.wishTopic !== 'VISITORS_PICKERS') {
    return (
      <p className="rounded-lg bg-muted px-3 py-2 type-body-sm">
        Ghi sở thích ở hồ sơ hoạt động; mục sẽ tự gắn vào phản hồi khi ghi xong, rồi mới trả lời được.
      </p>
    )
  }

  return (
    <div className="flex flex-col gap-3">
      <p className="rounded-lg bg-muted px-3 py-2 type-body-sm">
        Ghi mong muốn vào hồ sơ với nguồn &quot;Người cao tuổi nói&quot; rồi gắn vào đây trước khi trả lời.
      </p>
      {mode === null && (
        <span className="flex flex-wrap gap-2">
          <Button variant="outline" className="h-11" onClick={() => setMode('link')}>
            Gắn mong muốn đã ghi
          </Button>
          <Button variant="ghost" className="h-11" onClick={() => setMode('record')}>
            Ghi mong muốn mới
          </Button>
        </span>
      )}
      {mode === 'link' && (
        <form
          noValidate
          className="flex flex-col gap-3 rounded-lg border p-3"
          onSubmit={(e) => {
            e.preventDefault()
            if (!chosen) setError('Chọn mong muốn cần gắn')
            else onLink(Number(chosen)).then((ok) => ok && setMode(null))
          }}
        >
          <ProblemAlert problem={failure} />
          <Field data-invalid={!!error || undefined}>
            <FieldLabel htmlFor="wish-opinion">Mong muốn của người cao tuổi đã ghi</FieldLabel>
            <select
              id="wish-opinion"
              value={chosen}
              onChange={(e) => {
                setChosen(e.target.value)
                setError('')
              }}
              aria-invalid={!!error || undefined}
              className={SELECT}
            >
              <option value="">{wishes === null ? 'Đang tải…' : 'Chọn mong muốn'}</option>
              {(wishes ?? []).map((w) => (
                <option key={w.id} value={w.id}>
                  {`${describeWish(w, relationships)}, ghi ${formatDate(w.at)}`}
                </option>
              ))}
            </select>
            {error && <FieldError>{error}</FieldError>}
          </Field>
          <span className="flex flex-wrap gap-2">
            <Button type="submit" className="h-11">
              Gắn vào phản hồi
            </Button>
            <Button type="button" variant="ghost" className="h-11" onClick={() => setMode(null)}>
              Quay lại
            </Button>
          </span>
        </form>
      )}
      {mode === 'record' && (
        <RecordWishSheet
          residentId={residentId}
          residentName={residentName ?? `người cao tuổi mã ${residentId}`}
          relationships={relationships}
          defaultSource="RESIDENT"
          onClose={() => setMode(null)}
          onDone={() => {
            loadWishes()
            setMode('link')
          }}
        />
      )}
    </div>
  )
}

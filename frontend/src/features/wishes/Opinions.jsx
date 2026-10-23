import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Field, FieldDescription, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { formatDateTime } from '@/common/dateTime'
import { POSITIVE_INTEGER } from './wishUtils'
import { OPINION_SOURCE_LABELS } from './wishLabels'

// Ý kiến khác với lựa chọn trên phiếu (001 FR-023c, FR-023g, BR-M01-14): mỗi ý kiến một dòng có nguồn, không sửa, không
// xóa sau khi lưu. Người cao tuổi và người thân nói khác nhau thì hai dòng riêng, không gộp, không chọn hộ.

export function OpinionEditor({ idPrefix, items, onChange, error }) {
  const [type, setType] = useState('FAMILY_MEMBER')
  const [familyMemberId, setFamilyMemberId] = useState('')
  const [content, setContent] = useState('')
  const [problem, setProblem] = useState(null)

  function add() {
    if (!content.trim()) return setProblem('Nhập nội dung ý kiến')
    if (type === 'FAMILY_MEMBER' && !POSITIVE_INTEGER.test(familyMemberId.trim())) {
      return setProblem('Nhập mã người thân nêu ý kiến')
    }
    onChange([...items, { type, familyMemberId: familyMemberId.trim(), content }])
    setContent('')
    setFamilyMemberId('')
    setProblem(null)
  }

  return (
    <fieldset className="flex flex-col gap-3 rounded-lg border p-3">
      <legend className="type-label px-1">Ý kiến khác với lựa chọn (không bắt buộc)</legend>
      <FieldDescription>
        Mỗi ý kiến một mục có nguồn. Lựa chọn trên phiếu ghi theo người quyết định; ý kiến khác không đổi lựa chọn và không
        sửa được sau khi lưu.
      </FieldDescription>
      {items.length > 0 && (
        <ul aria-label="Ý kiến khác đã thêm" className="flex flex-col gap-2">
          {items.map((item, index) => (
            <li key={index} className="flex items-start justify-between gap-2 rounded-lg border p-2">
              <span className="type-body-sm">
                <strong>
                  {OPINION_SOURCE_LABELS[item.type]}
                  {item.type === 'FAMILY_MEMBER' && ` (mã ${item.familyMemberId})`}
                </strong>
                : {item.content}
              </span>
              <button
                type="button"
                className="min-h-8 min-w-8 rounded-full text-muted-foreground"
                aria-label={`Bỏ ý kiến ${index + 1}`}
                onClick={() => onChange(items.filter((_, i) => i !== index))}
              >
                ×
              </button>
            </li>
          ))}
        </ul>
      )}
      <div className="flex flex-wrap gap-2" role="radiogroup" aria-label="Nguồn ý kiến">
        {Object.entries(OPINION_SOURCE_LABELS).map(([value, label]) => (
          <Button
            key={value}
            type="button"
            role="radio"
            aria-checked={type === value}
            variant={type === value ? 'default' : 'outline'}
            className="h-10"
            onClick={() => setType(value)}
          >
            {label}
          </Button>
        ))}
      </div>
      {type === 'FAMILY_MEMBER' && (
        <Field>
          <FieldLabel htmlFor={`${idPrefix}-opinion-member`}>Mã người thân nêu ý kiến</FieldLabel>
          <Input
            id={`${idPrefix}-opinion-member`}
            inputMode="numeric"
            value={familyMemberId}
            onChange={(e) => setFamilyMemberId(e.target.value)}
          />
        </Field>
      )}
      <Field data-invalid={!!problem || !!error || undefined}>
        <FieldLabel htmlFor={`${idPrefix}-opinion-content`}>Nội dung ý kiến</FieldLabel>
        <Textarea
          id={`${idPrefix}-opinion-content`}
          rows={2}
          value={content}
          onChange={(e) => setContent(e.target.value)}
        />
        {(problem || error) && <FieldError>{problem ?? error}</FieldError>}
      </Field>
      <Button type="button" variant="outline" className="h-10 self-start" onClick={add}>
        Thêm ý kiến
      </Button>
    </fieldset>
  )
}

export function OpinionList({ opinions }) {
  if (!opinions?.length) return null
  return (
    <div className="flex flex-col gap-2">
      <h4 className="type-label">Ý kiến khác</h4>
      <ul aria-label="Ý kiến khác" className="flex flex-col gap-2">
        {opinions.map((opinion, index) => (
          <li key={index} className="rounded-lg border p-2">
            <p className="type-caption text-muted-foreground">
              {OPINION_SOURCE_LABELS[opinion.source.type]}
              {opinion.source.familyMemberId != null && ` (mã ${opinion.source.familyMemberId})`} · {opinion.recordedBy} ·{' '}
              {formatDateTime(opinion.at)}
            </p>
            <p className="type-body-sm whitespace-pre-wrap">{opinion.content}</p>
          </li>
        ))}
      </ul>
    </div>
  )
}

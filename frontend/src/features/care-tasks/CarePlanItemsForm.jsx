import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { CareField, CareCheck } from './CareField'
import { CARE_ROLES, FREQUENCIES, IMPORTANCE, RESULT_TYPES } from './carePlanLabels'

export function CarePlanItemsForm({ items, taskTypes, busy, onSave }) {
  const [rows, setRows] = useState(() => items.map((row) => ({ ...row, timesText: (row.times ?? []).join(', ') })))
  const set = (index, key, value) =>
    setRows((all) => all.map((row, i) => (i === index ? { ...row, [key]: value } : row)))
  function selectType(index, value) {
    const type = taskTypes.find((row) => String(row.id) === value)
    setRows((all) =>
      all.map((row, i) =>
        i === index
          ? {
              ...row,
              taskTypeId: Number(value),
              performerRole: type?.defaultRole ?? '',
              resultType: type?.resultType ?? '',
              importance: type?.defaultImportance ?? '',
              billable: type?.billable ?? false,
            }
          : row,
      ),
    )
  }
  function submit(event) {
    event.preventDefault()
    onSave(
      rows.map(({ timesText, ...row }) => ({
        ...row,
        taskTypeId: Number(row.taskTypeId),
        times: timesText
          .split(',')
          .map((at) => at.trim())
          .filter(Boolean),
        weekdays: row.weekdays ?? [],
        ...Object.fromEntries(
          ['intervalHours', 'timesPerDay', 'windowBeforeMinutes', 'windowAfterMinutes', 'waterTargetMl'].map((key) => [
            key,
            row[key] === '' || row[key] == null ? null : Number(row[key]),
          ]),
        ),
        startsAt: row.startsAt || null,
      })),
    )
  }
  return (
    <form onSubmit={submit} className="flex flex-col gap-4">
      <h3 className="type-title">Mục công việc</h3>
      {rows.map((row, index) => (
        <fieldset
          className="flex min-w-0 flex-col gap-4 rounded-lg border p-4"
          key={row.id ?? `new-${index}`}
          disabled={busy}
        >
          <legend className="type-label px-1">Mục {index + 1}</legend>
          <CareField
            label={`Loại công việc ${index + 1}`}
            value={row.taskTypeId}
            onChange={(value) => selectType(index, value)}
            options={Object.fromEntries(
              taskTypes
                .filter((type) => type.status === 'ACTIVE' && type.group !== 'MEDICATION')
                .map((type) => [type.id, type.name]),
            )}
            required
          />
          <CareField
            label={`Nội dung mục ${index + 1}`}
            value={row.content}
            onChange={(value) => set(index, 'content', value)}
            type="textarea"
            maxLength={1000}
            required
          />
          <div className="grid gap-4 sm:grid-cols-2">
            <CareField
              label={`Tần suất mục ${index + 1}`}
              value={row.frequencyType}
              onChange={(value) => set(index, 'frequencyType', value)}
              options={FREQUENCIES}
              required
            />
            {row.frequencyType === 'EVERY_N_HOURS' && (
              <CareField
                label={`Mỗi N giờ mục ${index + 1}`}
                value={row.intervalHours}
                onChange={(value) => set(index, 'intervalHours', value)}
                type="number"
                min={1}
                required
              />
            )}
            {row.frequencyType === 'TIMES_PER_DAY' && (
              <CareField
                label={`Số lần trong ngày mục ${index + 1}`}
                value={row.timesPerDay}
                onChange={(value) => set(index, 'timesPerDay', value)}
                type="number"
                min={1}
                required
              />
            )}
            <CareField
              label={`Giờ bắt đầu mục ${index + 1}`}
              value={row.startsAt}
              onChange={(value) => set(index, 'startsAt', value)}
              type="time"
              required={row.frequencyType === 'EVERY_N_HOURS'}
            />
            <CareField
              label={`Các mốc giờ mục ${index + 1} (cách nhau bằng dấu phẩy)`}
              value={row.timesText}
              onChange={(value) => set(index, 'timesText', value)}
            />
            <CareField
              label={`Khung trước mục ${index + 1} (phút)`}
              value={row.windowBeforeMinutes}
              onChange={(value) => set(index, 'windowBeforeMinutes', value)}
              type="number"
              min={0}
            />
            <CareField
              label={`Khung sau mục ${index + 1} (phút)`}
              value={row.windowAfterMinutes}
              onChange={(value) => set(index, 'windowAfterMinutes', value)}
              type="number"
              min={0}
            />
            <CareField
              label={`Vai trò thực hiện mục ${index + 1}`}
              value={row.performerRole}
              onChange={(value) => set(index, 'performerRole', value)}
              options={CARE_ROLES}
              required
            />
            <CareField
              label={`Kết quả cần ghi mục ${index + 1}`}
              value={row.resultType}
              onChange={(value) => set(index, 'resultType', value)}
              options={RESULT_TYPES}
              required
            />
            <CareField
              label={`Mức quan trọng mục ${index + 1}`}
              value={row.importance}
              onChange={(value) => set(index, 'importance', value)}
              options={IMPORTANCE}
              required
            />
            <CareField
              label={`Mục tiêu nước mục ${index + 1} (ml/ngày)`}
              value={row.waterTargetMl}
              onChange={(value) => set(index, 'waterTargetMl', value)}
              type="number"
              min={1}
            />
          </div>
          {row.frequencyType === 'DAYS_OF_WEEK' && (
            <fieldset className="flex flex-wrap gap-x-4">
              <legend className="type-label">Ngày trong tuần mục {index + 1}</legend>
              {[1, 2, 3, 4, 5, 6, 7].map((day) => (
                <CareCheck
                  key={day}
                  label={day === 7 ? 'Chủ nhật' : `Thứ ${day + 1}`}
                  checked={(row.weekdays ?? []).includes(day)}
                  onChange={(checked) =>
                    set(
                      index,
                      'weekdays',
                      checked ? [...(row.weekdays ?? []), day] : row.weekdays.filter((d) => d !== day),
                    )
                  }
                />
              ))}
            </fieldset>
          )}
          <p className="type-body-sm text-muted-foreground">
            Để trống giờ là trong ca. Để trống khung để dùng cấu hình khi gửi duyệt. Mỗi phiên bản chỉ đặt một mục tiêu
            nước.
          </p>
          <CareCheck
            label={`Có tính phí mục ${index + 1}`}
            checked={row.billable ?? false}
            onChange={(value) => set(index, 'billable', value)}
          />
          <Button type="button" variant="outline" onClick={() => setRows((all) => all.filter((_, i) => i !== index))}>
            Bỏ mục {index + 1}
          </Button>
        </fieldset>
      ))}
      <div className="flex flex-wrap gap-3">
        <Button
          type="button"
          variant="outline"
          disabled={busy}
          onClick={() =>
            setRows((all) => [
              ...all,
              {
                taskTypeId: '',
                content: '',
                frequencyType: '',
                performerRole: '',
                resultType: '',
                importance: '',
                times: [],
                timesText: '',
                weekdays: [],
                billable: false,
              },
            ])
          }
        >
          Thêm mục công việc
        </Button>
        <Button type="submit" disabled={busy}>
          Lưu mục công việc
        </Button>
      </div>
    </form>
  )
}

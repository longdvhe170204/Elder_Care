import { Plus, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Input } from '@/components/ui/input'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { UNIT_LABELS, isDuration } from './parameterValue'

// Sửa tham số dạng bảng theo đúng cấu trúc của giá trị hiện hành (000 T081; chủ dự án duyệt 2026-10-19):
// mảng đối tượng thành bảng có thêm, xóa dòng; đối tượng thành danh sách ô; thời lượng {value, unit} giữ đơn vị.
// Cấu trúc chi tiết do module sở hữu tham số quyết; backend kiểm (FR-015). Tên ô là khóa trong giá trị.

// Ô chữ: số khi cột/ô vốn là số và chữ nhập là số; trống là null
function parseCell(text, numeric) {
  if (text.trim() === '') return null
  if (numeric && /^-?\d+(\.\d+)?$/.test(text.trim())) return Number(text.trim())
  return text
}

function cellText(value) {
  return value === null || value === undefined ? '' : String(value)
}

function Leaf({ label, value, numeric, onChange, disabled }) {
  if (typeof value === 'boolean') {
    return (
      <label className="flex items-center gap-2">
        <Checkbox
          checked={value}
          onCheckedChange={(checked) => onChange(checked === true)}
          disabled={disabled}
          aria-label={label}
        />
        <span>{label}</span>
      </label>
    )
  }
  return (
    <Input
      aria-label={label}
      value={cellText(value)}
      inputMode={numeric ? 'decimal' : undefined}
      onChange={(e) => onChange(parseCell(e.target.value, numeric))}
      disabled={disabled}
      className="h-10 min-w-24"
    />
  )
}

function RowsTable({ name, rows, onChange, disabled }) {
  const columns = [...new Set(rows.flatMap((row) => Object.keys(row)))]
  const numericColumn = (column) => rows.some((row) => typeof row[column] === 'number')
  const setCell = (index, column, value) =>
    onChange(rows.map((row, i) => (i === index ? { ...row, [column]: value } : row)))
  const addRow = () => onChange([...rows, Object.fromEntries(columns.map((c) => [c, null]))])
  const removeRow = (index) => onChange(rows.filter((_, i) => i !== index))

  return (
    <div className="flex flex-col gap-2">
      <Table>
        <TableHeader>
          <TableRow>
            {columns.map((column) => (
              <TableHead key={column}>{column}</TableHead>
            ))}
            <TableHead className="w-12">
              <span className="sr-only">Xóa</span>
            </TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {rows.map((row, index) => (
            <TableRow key={index}>
              {columns.map((column) => (
                <TableCell key={column}>
                  <Leaf
                    label={`${name} dòng ${index + 1} ${column}`}
                    value={row[column] ?? null}
                    numeric={numericColumn(column)}
                    onChange={(value) => setCell(index, column, value)}
                    disabled={disabled}
                  />
                </TableCell>
              ))}
              <TableCell>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  aria-label={`Xóa dòng ${index + 1} ${name}`}
                  onClick={() => removeRow(index)}
                  disabled={disabled}
                >
                  <Trash2 aria-hidden="true" />
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
      <Button type="button" variant="outline" className="self-start" onClick={addRow} disabled={disabled}>
        <Plus aria-hidden="true" />
        Thêm dòng <span className="sr-only">{name}</span>
      </Button>
    </div>
  )
}

function Node({ name, value, onChange, disabled }) {
  if (isDuration(value)) {
    const label = `${name} (${UNIT_LABELS[value.unit] ?? value.unit})`
    return (
      <label className="flex flex-col gap-1">
        <span className="type-label">{label}</span>
        <Leaf
          label={label}
          value={value.value}
          numeric
          onChange={(v) => onChange({ ...value, value: v })}
          disabled={disabled}
        />
      </label>
    )
  }
  if (Array.isArray(value)) {
    if (value.every((item) => item !== null && typeof item === 'object' && !Array.isArray(item))) {
      return (
        <fieldset className="flex flex-col gap-2">
          <legend className="type-label">{name}</legend>
          <RowsTable name={name} rows={value} onChange={onChange} disabled={disabled} />
        </fieldset>
      )
    }
    return (
      <fieldset className="flex flex-col gap-2">
        <legend className="type-label">{name}</legend>
        {value.map((item, index) => (
          <div key={index} className="flex items-center gap-2">
            <Leaf
              label={`${name} mục ${index + 1}`}
              value={item}
              numeric={typeof item === 'number'}
              onChange={(v) => onChange(value.map((x, i) => (i === index ? v : x)))}
              disabled={disabled}
            />
            <Button
              type="button"
              variant="ghost"
              size="icon"
              aria-label={`Xóa mục ${index + 1} ${name}`}
              onClick={() => onChange(value.filter((_, i) => i !== index))}
              disabled={disabled}
            >
              <Trash2 aria-hidden="true" />
            </Button>
          </div>
        ))}
        <Button
          type="button"
          variant="outline"
          className="self-start"
          onClick={() => onChange([...value, ''])}
          disabled={disabled}
        >
          <Plus aria-hidden="true" />
          Thêm mục <span className="sr-only">{name}</span>
        </Button>
      </fieldset>
    )
  }
  if (value !== null && typeof value === 'object') {
    return (
      <div className="flex flex-col gap-3">
        {Object.entries(value).map(([key, child]) => (
          <Node
            key={key}
            name={key}
            value={child}
            onChange={(v) => onChange({ ...value, [key]: v })}
            disabled={disabled}
          />
        ))}
      </div>
    )
  }
  return (
    <label className="flex flex-col gap-1">
      <span className="type-label">{name}</span>
      <Leaf label={name} value={value} numeric={typeof value === 'number'} onChange={onChange} disabled={disabled} />
    </label>
  )
}

export function TableValueEditor({ value, onChange, disabled }) {
  if (value === null || typeof value !== 'object') {
    return (
      <p className="text-muted-foreground">
        Tham số này chưa có cấu trúc giá trị; module sở hữu sẽ chốt khi triển khai.
      </p>
    )
  }
  return <Node name="" value={value} onChange={onChange} disabled={disabled} />
}

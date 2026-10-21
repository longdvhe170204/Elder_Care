import { useCallback, useEffect, useState } from 'react'
import { Search } from 'lucide-react'
import { getResidents } from '@/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Empty, EmptyDescription, EmptyHeader, EmptyTitle } from '@/components/ui/empty'
import { InputGroup, InputGroupAddon, InputGroupInput } from '@/components/ui/input-group'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ProblemAlert } from '@/common/ProblemAlert'
import { problemOf } from '@/common/problem'
import { GENDER_LABELS, STATUS_LABELS } from './residentLabels'

// Tra cứu hồ sơ thật trong phạm vi (001 T022; US1 #6, #7; FR-007; hợp đồng GET /residents). Kết quả đã lọc
// theo phạm vi ở backend; vai trò không được xem nhận lỗi 403 và màn hiện lỗi đó.

export function ResidentSearch({ reloadKey }) {
  const [text, setText] = useState('')
  const [items, setItems] = useState(null)
  const [nextCursor, setNextCursor] = useState(null)
  const [failure, setFailure] = useState(null)
  const [applied, setApplied] = useState('')

  const load = useCallback(async (q, cursor) => {
    try {
      const { data } = await getResidents({ query: { q: q || undefined, cursor } })
      setFailure(null)
      setItems((current) => (cursor && current ? [...current, ...data.items] : data.items))
      setNextCursor(data.nextCursor)
    } catch (error) {
      setItems([])
      setFailure(problemOf(error))
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- nạp danh sách khi vào màn, đổi từ khóa hoặc vừa tạo hồ sơ
    load(applied)
  }, [applied, reloadKey, load])

  function submit(event) {
    event.preventDefault()
    setApplied(text.trim())
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="type-title">Hồ sơ trong phạm vi của bạn</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <form onSubmit={submit} className="flex flex-col gap-3 sm:flex-row">
          <InputGroup className="h-11 bg-popover sm:max-w-sm">
            <InputGroupAddon>
              <Search aria-hidden="true" />
            </InputGroupAddon>
            <InputGroupInput
              value={text}
              onChange={(e) => setText(e.target.value)}
              placeholder="Họ tên, mã hồ sơ hoặc số giấy tờ"
              aria-label="Tìm hồ sơ"
            />
          </InputGroup>
          <Button type="submit" className="h-11">
            Tìm
          </Button>
        </form>
        <ProblemAlert problem={failure} />
        {items && items.length === 0 && !failure && (
          <Empty>
            <EmptyHeader>
              <EmptyTitle>Chưa có hồ sơ nào</EmptyTitle>
              <EmptyDescription>Dùng nút "Tạo hồ sơ" hoặc đổi từ khóa tìm.</EmptyDescription>
            </EmptyHeader>
          </Empty>
        )}
        {items && items.length > 0 && (
          <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead>Mã hồ sơ</TableHead>
                <TableHead>Họ tên</TableHead>
                <TableHead>Ngày sinh</TableHead>
                <TableHead>Giới tính</TableHead>
                <TableHead>Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {items.map((p) => (
                <TableRow key={p.id}>
                  <TableCell className="tabular-nums">{p.profileCode}</TableCell>
                  <TableCell className="font-medium">{p.personalInfo.fullName}</TableCell>
                  <TableCell className="tabular-nums">{p.personalInfo.dateOfBirth}</TableCell>
                  <TableCell>{GENDER_LABELS[p.personalInfo.gender]}</TableCell>
                  <TableCell>{STATUS_LABELS[p.status] ?? p.status}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
        {nextCursor && (
          <Button variant="outline" className="h-11 self-start" onClick={() => load(applied, nextCursor)}>
            Xem thêm
          </Button>
        )}
      </CardContent>
    </Card>
  )
}

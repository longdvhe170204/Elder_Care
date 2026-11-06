import { useState } from 'react'
import { getStaffByStaffIdHeldBelongings } from '@/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ProblemAlert } from '@/common/ProblemAlert'
import { StatusBadge } from '@/common/StatusBadge'
import { problemOf } from '@/common/problem'
import { amountText, statusLabel } from './belongingLabels'

// Đồ có giá trị một nhân viên đang giữ (013 T015; FR-014a; api-013 GET /staff/{staffId}/held-belongings), dùng khi lập
// bàn giao ca. GET /me của 002 không trả mã hồ sơ nhân viên nên nhập mã; backend cho xem đồ của chính mình hoặc khi có
// quyền "Đồ gửi".
export function HeldBelongings({ catalogs }) {
  const [staffId, setStaffId] = useState('')
  const [state, setState] = useState({ status: 'idle', items: [] })

  async function load(event) {
    event.preventDefault()
    if (!/^[1-9]\d*$/.test(staffId)) return
    setState({ status: 'loading', items: [] })
    try {
      const { data } = await getStaffByStaffIdHeldBelongings({ path: { staffId } })
      setState({ status: 'ready', items: data })
    } catch (error) {
      setState({ status: 'error', items: [], problem: problemOf(error) })
    }
  }

  return (
    <Card>
      <CardContent className="flex flex-col gap-4">
        <form className="flex flex-wrap items-end gap-2" onSubmit={load}>
          <label className="flex flex-col gap-1">
            <span className="type-caption text-muted-foreground">Mã nhân viên</span>
            <Input
              aria-label="Mã nhân viên"
              inputMode="numeric"
              className="h-11 w-full sm:w-40"
              value={staffId}
              onChange={(e) => setStaffId(e.target.value)}
            />
          </label>
          <Button type="submit" variant="outline" className="h-11">
            Xem đồ đang giữ
          </Button>
        </form>
        {state.status === 'error' && <ProblemAlert problem={state.problem} />}
        {state.status === 'ready' && state.items.length === 0 && (
          <p className="text-muted-foreground">Nhân viên này không giữ đồ có giá trị nào.</p>
        )}
        {state.items.length > 0 && (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Đồ gửi</TableHead>
                <TableHead>Loại</TableHead>
                <TableHead>Số lượng, số tiền</TableHead>
                <TableHead>Trạng thái</TableHead>
                <TableHead>Vị trí</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {state.items.map((b) => (
                <TableRow key={b.id}>
                  <TableCell className="whitespace-normal">{b.description}</TableCell>
                  <TableCell>{catalogs.typeName(b.typeId)}</TableCell>
                  <TableCell className="tabular-nums">{amountText(b)}</TableCell>
                  <TableCell>
                    <StatusBadge status={statusLabel(b.status)} />
                  </TableCell>
                  <TableCell>{catalogs.locationName(b.storageLocationId)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  )
}

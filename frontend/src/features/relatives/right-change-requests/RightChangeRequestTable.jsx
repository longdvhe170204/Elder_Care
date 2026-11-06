import { Card } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { StatusBadge } from '@/common/StatusBadge'
import { REQUEST_SOURCE_LABELS, REQUEST_STATUS_LABELS, describeRequest } from './requestLabels'

// Bảng yêu cầu thay đổi quyền của một người cao tuổi (012 T017; FR-013 → FR-020). Bấm nội dung mở khung chi tiết.
export function RightChangeRequestTable({ requests, relationships, selectedId, onSelect }) {
  return (
    <Card className="py-0">
      <Table aria-label="Yêu cầu thay đổi quyền">
        <TableHeader>
          <TableRow className="hover:bg-transparent">
            <TableHead className="pl-4">Yêu cầu</TableHead>
            <TableHead>Lý do</TableHead>
            <TableHead className="pr-4">Trạng thái</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {requests.map((q) => (
            <TableRow key={q.id} className={selectedId === q.id ? 'bg-accent' : undefined}>
              <TableCell className="pl-4">
                <button
                  type="button"
                  className="flex min-h-11 flex-col items-start text-left"
                  aria-pressed={selectedId === q.id}
                  onClick={() => onSelect(q.id)}
                >
                  <span className="type-label whitespace-normal">{describeRequest(q, relationships)}</span>
                  <span className="type-caption text-muted-foreground">{REQUEST_SOURCE_LABELS[q.source]}</span>
                </button>
              </TableCell>
              <TableCell className="whitespace-normal">{q.reason}</TableCell>
              <TableCell className="pr-4">
                <div className="flex flex-col items-start gap-0.5">
                  <StatusBadge status={REQUEST_STATUS_LABELS[q.status] ?? q.status} />
                  {q.status === 'PENDING_CONFIRMATION' && q.appliedOnCreation && (
                    <span className="type-caption text-muted-foreground">Đã áp dụng khi lập</span>
                  )}
                  {q.status === 'PENDING_CONFIRMATION' && q.conflictingWishId != null && (
                    <span className="type-caption text-warning-foreground">Chờ Quản lý viện duyệt</span>
                  )}
                </div>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Card>
  )
}

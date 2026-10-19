import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'

// Báo lỗi lệnh trong khung đang thao tác (DESIGN.md 7.4; 000 T099). `problem` lấy từ problemOf; 412 thì màn truyền
// giá trị mới qua children vì cách hiển thị tùy tài nguyên.
export function ProblemAlert({ problem, title, children }) {
  if (!problem) return null
  const hasBody = problem.detail || problem.unmetConditions.length > 0 || children
  return (
    <Alert variant="attention">
      <AlertTitle>{title || problem.title}</AlertTitle>
      {hasBody && (
        <AlertDescription>
          {problem.detail && <p>{problem.detail}</p>}
          {problem.unmetConditions.length > 0 && (
            <ul className="ml-4 list-disc">
              {problem.unmetConditions.map((c) => (
                <li key={c.code}>{c.description}</li>
              ))}
            </ul>
          )}
          {children}
        </AlertDescription>
      )}
    </Alert>
  )
}

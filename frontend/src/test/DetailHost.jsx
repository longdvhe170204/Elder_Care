import { useState } from 'react'
import { DetailLayout } from '@/common/DetailLayout'

// Đóng vai màn cha (AdminPage, NotificationsPage) cho test danh sách mở chi tiết qua onOpen: giữ mục đang mở và vẽ
// DetailLayout. children nhận { openedKey, onOpen, onClose } để truyền cho danh sách.
export function DetailHost({ children }) {
  const [detail, setDetail] = useState(null)
  const onClose = () => setDetail(null)
  return (
    <DetailLayout
      open={detail !== null}
      detailKey={detail?.key}
      onClose={onClose}
      label={detail?.label}
      renderDetail={(titles) => detail.render(titles)}
    >
      {children({ openedKey: detail?.key, onOpen: setDetail, onClose })}
    </DetailLayout>
  )
}

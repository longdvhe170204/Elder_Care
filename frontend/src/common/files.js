import { ApiError } from '@/api'

// Tải tệp lên và xuống (001 T004, T059; contracts/api-001.yaml, tag files). Client sinh từ hợp đồng chỉ gửi JSON nên hai
// thao tác này gọi fetch riêng. Tải xuống đi qua backend kèm cookie phiên, backend kiểm quyền theo bản ghi chủ sở hữu
// mỗi lần (NFR-08); không có đường dẫn công khai hay URL ký sẵn, nội dung chỉ nằm trong bộ nhớ trình duyệt.

const BASE_URL = '/api/v1/files'

async function failure(response) {
  const type = response.headers.get('Content-Type') || ''
  const body = type.includes('json') ? await response.json() : null
  return new ApiError(response.status, body)
}

// Trả mã tệp (fileId); tệp chưa gắn bản ghi nào sẽ bị dọn sau 24 giờ
export async function uploadFile(file) {
  const form = new FormData()
  form.append('file', file)
  const response = await fetch(BASE_URL, {
    method: 'POST',
    credentials: 'include',
    headers: { Accept: 'application/json', 'X-Requested-From': 'web' },
    body: form,
  })
  if (!response.ok) throw await failure(response)
  return (await response.json()).fileId
}

// Mở nội dung tệp ở thẻ mới
export async function openFile(id) {
  const response = await fetch(`${BASE_URL}/${encodeURIComponent(id)}`, { credentials: 'include' })
  if (!response.ok) throw await failure(response)
  const url = URL.createObjectURL(await response.blob())
  window.open(url, '_blank', 'noopener')
  // đủ thời gian để thẻ mới nạp xong rồi giải phóng bộ nhớ
  setTimeout(() => URL.revokeObjectURL(url), 60_000)
}

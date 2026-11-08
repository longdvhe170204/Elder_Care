// Lưu nội dung file (Blob) từ API về máy: chứng từ và file xuất của 017 trả đúng file đã dựng (FR-037, FR-027).
export function saveBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

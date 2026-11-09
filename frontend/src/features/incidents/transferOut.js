import {
  getIncidentsByIdTransferOutInfo,
  getTransferSummariesByIdDocument,
  postIncidentsByIdTransferOut,
  postTransferSummariesByIdReprint,
} from '@/api/api007'

// Chuyển viện từ sự cố qua client sinh từ hợp đồng api-007 (transfer-out, transfer-out/info, transfer-summaries).

export async function loadTransferInfo(incidentId) {
  const { data } = await getIncidentsByIdTransferOutInfo({ path: { id: incidentId } })
  return data
}

export async function submitTransferOut(incidentId, body) {
  const { data } = await postIncidentsByIdTransferOut({
    path: { id: incidentId },
    headers: { 'Idempotency-Key': crypto.randomUUID() },
    body,
  })
  return data
}

// Tải bản tóm tắt (PDF); in lại ghi nhật ký in (POST reprint).
export async function downloadTransferDocument(summaryId, reprint) {
  const call = reprint ? postTransferSummariesByIdReprint : getTransferSummariesByIdDocument
  const { data } = await call({ path: { id: summaryId }, responseType: 'blob' })
  const url = URL.createObjectURL(data)
  const link = document.createElement('a')
  link.href = url
  link.download = `transfer-summary-${summaryId}.pdf`
  link.click()
  URL.revokeObjectURL(url)
}

import { request } from '@/api/client'

/**
 * Tra danh mục thuốc (FR-001, FR-003).
 */
export async function getMedications(params = {}) {
  const { q, hieuLuc } = params
  return request('GET', '/thuoc', {
    query: {
      ...(q && { q }),
      ...(hieuLuc !== undefined && { hieuLuc }),
    },
  })
}

/**
 * Xem chi tiết một thuốc kèm ETag phiên bản.
 */
export async function getMedication(id) {
  return request('GET', '/thuoc/{id}', { path: { id } })
}

/**
 * Thêm mới thuốc vào danh mục (FR-001).
 */
export async function createMedication(data) {
  return request('POST', '/thuoc', { body: data })
}

/**
 * Cập nhật thông tin thuốc với If-Match ETag (FR-002).
 */
export async function updateMedication(id, ifMatch, data) {
  return request('PUT', '/thuoc/{id}', {
    path: { id },
    headers: { 'If-Match': ifMatch },
    body: data,
  })
}

/**
 * Ngừng hiệu lực thuốc kèm lý do (FR-002).
 */
export async function deactivateMedication(id, reason) {
  return request('POST', '/thuoc/{id}/ngung-hieu-luc', {
    path: { id },
    body: { lyDo: reason },
  })
}

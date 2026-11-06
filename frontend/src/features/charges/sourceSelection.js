// Khóa lựa chọn có phạm vi người và loại nguồn; unit hiển thị không thay billingUnit.
export function sourceSelectionKey(residentId, source) {
  if (residentId == null || typeof source.sourceType !== 'string' || typeof source.sourceId !== 'string'
    || typeof source.billingUnit !== 'string') return null
  return JSON.stringify([String(residentId), source.sourceType, source.sourceId, source.billingUnit])
}

export function sourceCreationBody({ residentId, sourceType, sources, selection, dateRange }) {
  if (residentId == null) return null
  if (sourceType === 'RESIDENCY_DAY') {
    const { from, to } = dateRange || {}
    const plainDate = /^\d{4}-\d{2}-\d{2}$/
    if (typeof from !== 'string' || typeof to !== 'string' || !plainDate.test(from) || !plainDate.test(to) || from > to) return null
    return { residentId, sourceType, dateRange: { from, to } }
  }
  const keys = new Set(selection)
  const selected = sources.filter((source) => {
    const key = sourceSelectionKey(residentId, source)
    return key !== null && keys.has(key)
  })
  if (!selected.length || selected.some((source) => source.sourceType !== sourceType)) return null
  const units = new Map(selected.map((source) => [sourceSelectionKey(residentId, source),
    { sourceId: source.sourceId, billingUnit: source.billingUnit }]))
  return { residentId, sourceType, sourceUnits: [...units.values()] }
}

const skipLabels = {
  COST_ITEM_ALREADY_EXISTS: 'Nguồn này đã có khoản đang hiệu lực.',
  CONDITION_NOT_MET: 'Nguồn chưa đủ điều kiện để lập khoản.',
  NOT_A_BILLABLE_EVENT: 'Nguồn này không phải sự kiện tính phí trong phạm vi đã chọn.',
  OUTSIDE_BILLING_PERIOD: 'Nguồn nằm ngoài thời gian tính phí.',
  DAY_NOT_ENDED: 'Ngày lưu trú chưa kết thúc.',
  STATEMENT_FINALIZED: 'Bảng chi phí đã chốt.',
}

export function sourceSkipMessage(skipped) {
  return skipped.reason || skipLabels[skipped.errorCode] || 'Chưa thể lập khoản từ nguồn này.'
}

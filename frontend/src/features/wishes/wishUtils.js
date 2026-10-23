// Hàm dùng chung của tab Nguyện vọng (001 T070, T080).

export const POSITIVE_INTEGER = /^[1-9]\d{0,17}$/

export function today() {
  return new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date())
}

export function uploadProblem(problem) {
  if (problem.errorCode === 'FILE_TYPE_NOT_ALLOWED') return 'Tệp phải là PDF, JPEG hoặc PNG, tối đa 10 MB'
  if (problem.errorCode === 'FILE_TOO_LARGE') return 'Tệp quá 10 MB'
  return null
}

export function toRequestOpinion(item) {
  return {
    source: {
      type: item.type,
      familyMemberId: item.type === 'FAMILY_MEMBER' ? Number(item.familyMemberId) : undefined,
    },
    content: item.content.trim(),
  }
}

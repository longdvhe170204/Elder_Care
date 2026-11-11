import { AllergyNote } from '@/common/CareNotes'
import { formatDate, formatDateTime } from '@/common/dateTime'

// Sáu phần thông tin của một người tham gia chuyến (014 FR-036, FR-038, FR-039; api-014 TripParticipantInfo, chủ dự án
// duyệt 2026-11-10). Mỗi phần nói rõ có dữ liệu, không có dữ liệu, chưa lấy được từ nguồn, hay người xem không có quyền;
// không suy thuốc đã giao từ "đã chuẩn bị", không suy an toàn từ kết quả đánh giá (đề xuất 08/11). Dị ứng là khối hổ
// phách theo DESIGN.md 7.2 care-notes; còn lại là chữ có nhãn, không nền.

const UNAVAILABLE = {
  NOT_PERMITTED: 'Không có quyền xem',
  SOURCE_UNAVAILABLE: 'Chưa lấy được dữ liệu từ nguồn',
}

const TIME = new Intl.DateTimeFormat('en-GB', {
  timeZone: 'Asia/Ho_Chi_Minh',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
})

const WISHES = { NOT_RECORDED: 'Chưa ghi nguyện vọng cuối đời' }

// render chỉ gọi khi phần có dữ liệu, để không đọc trường của phần trống
function Part({ title, section, empty, render }) {
  const status = section?.availability ?? 'SOURCE_UNAVAILABLE'
  return (
    <div className="flex flex-col gap-0.5">
      <span className="type-caption text-muted-foreground">{title}</span>
      {status === 'AVAILABLE' ? (
        <span className="type-body-sm">{render(section)}</span>
      ) : (
        <span className="type-body-sm text-muted-foreground">{status === 'NO_DATA' ? empty : UNAVAILABLE[status]}</span>
      )}
    </div>
  )
}

const doses = (s) =>
  s.items
    .map((d) => `${d.medicationName}${d.strength ? ` ${d.strength}` : ''}, ${TIME.format(new Date(d.scheduledTime))}`)
    .join('; ')

const carried = (s) =>
  s.notCarriedReason
    ? `Không mang thuốc: ${s.notCarriedReason}`
    : `Đã giao ${formatDateTime(s.handedAt)} cho ${s.recipientName}: ${(s.items ?? [])
        .map((i) => `${i.medicationName} ${i.quantity}${i.unit ? ` ${i.unit}` : ''}`)
        .join(', ')}`

const health = (s) =>
  [
    s.suspectedInfection && 'Đang nghi nhiễm',
    s.openAlerts?.length > 0 && `${s.openAlerts.length} cảnh báo chưa đóng`,
    s.openIncidents?.length > 0 && `${s.openIncidents.length} sự cố chưa đóng`,
  ]
    .filter(Boolean)
    .join(', ') || 'Không có cảnh báo, sự cố đang mở'

const card = (s) =>
  [
    s.fullName,
    s.dateOfBirth && `sinh ${formatDate(s.dateOfBirth)}`,
    s.roomBed,
    s.identificationOnly && 'chỉ phần nhận dạng',
    s.endOfLifeWish && (WISHES[s.endOfLifeWish] ?? `nguyện vọng cuối đời: ${s.endOfLifeWish}`),
    s.criticalFlag && 'đang có dấu nguy kịch',
    s.chronicConditions?.length > 0 && `bệnh mạn tính: ${s.chronicConditions.join(', ')}`,
    s.currentMedications?.length > 0 && `thuốc hiện hành: ${s.currentMedications.join(', ')}`,
  ]
    .filter(Boolean)
    .join(', ')

export function TripParticipantInfo({ info, name }) {
  if (!info) return null
  const emergency = info.emergencyCard
  return (
    <section aria-label={`Thông tin cho chuyến của ${name}`} className="mt-2 flex flex-col gap-2 border-t pt-2">
      {emergency?.availability === 'AVAILABLE' && !emergency.identificationOnly && (
        <AllergyNote allergies={emergency.allergies} />
      )}
      <Part title="Liều trong khoảng đi" section={info.medicationDoses} empty="Không có liều trong khoảng đi" render={doses} />
      <Part title="Thuốc mang theo" section={info.carriedMedication} empty="Chưa giao thuốc mang theo" render={carried} />
      <Part title="Sức khỏe đang mở" section={info.health} empty="Không có" render={health} />
      <Part title="Lưu ý chăm sóc" section={info.careNotes} empty="Chưa ghi nhận" render={(s) => s.text} />
      <Part
        title="Liên hệ chính"
        section={info.primaryContact}
        empty="Chưa có người liên hệ chính"
        render={(s) => [s.fullName, s.phoneNumber].filter(Boolean).join(', ')}
      />
      <Part title="Thẻ khẩn cấp" section={emergency} empty="Chưa có" render={card} />
    </section>
  )
}

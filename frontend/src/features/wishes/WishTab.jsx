import { EndOfLifeWish } from './EndOfLifeWish'
import { TreatmentWish } from './TreatmentWish'

// Tab "Nguyện vọng" trong khung chi tiết hồ sơ (001 T070, T080; US7): hai khối xếp dọc, "Nguyện vọng cuối đời" rồi
// "Nguyện vọng khi điều trị". Cờ hasEndOfLifeWish, hasTreatmentWish lấy từ hồ sơ để vai trò không xem được nội dung
// (Hành chính, Kế toán) vẫn biết đã có phiếu hay chưa (FR-023e, FR-023m).
export function WishTab({ residentId, status, allowedCommands = [], profile, onChanged }) {
  return (
    <div className="flex flex-col gap-6">
      <EndOfLifeWish
        residentId={residentId}
        status={status}
        allowedCommands={allowedCommands}
        hasWish={profile?.hasEndOfLifeWish}
        onChanged={onChanged}
      />
      <TreatmentWish
        residentId={residentId}
        status={status}
        allowedCommands={allowedCommands}
        hasWish={profile?.hasTreatmentWish}
        onChanged={onChanged}
      />
    </div>
  )
}

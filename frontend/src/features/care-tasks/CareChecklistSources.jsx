import { careTime } from './careChecklistView'

// Nguồn điều dưỡng được trả riêng, không biến thành care_task hoặc nút ghi thuốc của M04.
export function CareChecklistSources({ data }) {
  const doses = data.medicationDoses ?? [],
    alerts = data.openAlerts ?? [],
    handover = data.handoverTasks ?? {}
  return (
    <div className="flex flex-col gap-6">
      {doses.length > 0 && (
        <section aria-label="Liều thuốc" className="flex flex-col gap-3">
          <h2 className="type-title">Liều thuốc</h2>
          {doses.map((dose) => (
            <div key={dose.doseKey} className="rounded-lg border bg-card p-4">
              <p>{dose.medication?.description ?? `Liều thuốc ${dose.doseKey}`}</p>
              {dose.plannedAt && (
                <p className="type-body-sm text-muted-foreground">Dự kiến {careTime(dose.plannedAt)}</p>
              )}
              {dose.awaitingHandoverConfirmation && <p>Chờ xác nhận bàn giao</p>}
              {dose.awaitingRecordingAfterReturn && <p>Chờ ghi nhận sau khi trở về</p>}
              {dose.onlineRequired && <p>Cần kết nối mạng để ghi nhận liều thuốc.</p>}
            </div>
          ))}
        </section>
      )}
      {alerts.length > 0 && (
        <section aria-label="Cảnh báo đang mở" className="flex flex-col gap-3">
          <h2 className="type-title">Cảnh báo đang mở</h2>
          {alerts.map((alert) => (
            <p key={alert.id}>Cảnh báo #{alert.id}</p>
          ))}
        </section>
      )}
      {(handover.receiveAtStart || handover.prepareAtEnd) && (
        <section aria-label="Bàn giao ca" className="flex flex-col gap-3">
          <h2 className="type-title">Bàn giao ca</h2>
          {handover.receiveAtStart && <p>Nhận bàn giao đầu ca</p>}
          {handover.prepareAtEnd && <p>Lập bàn giao cuối ca</p>}
        </section>
      )}
    </div>
  )
}

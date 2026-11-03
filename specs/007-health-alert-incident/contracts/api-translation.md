# Bản dịch phần còn lại của api-007 (T062, đã duyệt 2026-11-03)

Soạn 2026-11-03 (thay D) để duyệt trước khi sửa `api-007.yaml`, controller và client sinh (CLAUDE.md "Khi vào triển khai" bước 4, research R-03). Phần danh mục và hai đường dẫn chỉ số đã dịch ở `catalog-translation.md` và glossary mục 13, không nhắc lại ở đây.

Giữ nguyên phương thức, số endpoint, trường, kiểu, required/readOnly, security, header và mã phản hồi. Chỉ đổi tên. Tên lấy theo glossary khi đã có (ghi nguồn trong ngoặc); tên đã có trong mã, bảng 007 (`AlertStatus`, `VitalSignLevel`, cột `alert.*`…) được giữ để không phải đổi mã.

## 1. Tag, security, tham số dùng chung

| Gốc | Dịch |
| --- | --- |
| tag `danh-muc`, `lich-do`, `chi-so`, `nguong`, `canh-bao`, `su-co`, `khan-cap`, `nguy-kich`, `lay-nhiem`, `giay-phep-kham` | `catalogs`, `measurement-schedules`, `vital-signs`, `thresholds`, `alerts`, `incidents`, `emergency`, `critical-condition`, `infection`, `licenses-examinations` |
| security `phien` | `session` (7.1) |
| security `tokenThietBi`, header `X-Token-Thiet-Bi` | `deviceToken` (7.2), header `X-Device-Token` |
| tham số `NctId` (`nctId`) | gộp vào `ResidentId` (`residentId`) đã có |
| query `tu`, `den`, `chiSoId`, `phamVi`, `trangThai`, `tangKhuVucId`, `nhom` | `from`, `to`, `indicatorId`, `scope`, `status`, `floorAreaId` (14.3), `group` |
| query `leoThangToiDa` | `maxEscalationReached` |
| path `nguoiId` (người trong danh sách tiếp xúc) | `contactId` |
| requestBody `LyDo`; schema `LyDo` | dùng `Reason` đã có (6.2), bỏ bản tiếng Việt |
| response `Loi`, `LoiChan`, `LoiTiepNhan` | `Problem`, `Blocked`, `AlreadyReceived` |

## 2. Đường dẫn (tiền tố `/api/v1`)

| Gốc | Dịch |
| --- | --- |
| `/nguoi-cao-tuoi/{nctId}/lich-do` | `/residents/{residentId}/measurement-schedules` |
| `/lich-do/{id}/moc-do`, `…/thay-doi`, `…/ngung` | `/measurement-schedules/{id}/measurement-times`, `…/change`, `…/stop` |
| `/chi-so/chua-co-nguong-ca-nhan` | `/vital-signs/missing-personal-threshold` |
| `/nguong`, `/nguong/{id}/huy`, `/nguong/{id}/ngung` | `/thresholds`, `/thresholds/{id}/cancel`, `/thresholds/{id}/stop` |
| `/canh-bao`, `/canh-bao/{id}` | `/alerts`, `/alerts/{id}` |
| `…/tiep-nhan`, `…/giao-nguoi-phu-trach`, `…/bat-dau-xu-ly` | `…/receive`, `…/assign`, `…/start-handling` |
| `…/doi-muc`, `…/dong`, `…/chuyen-su-co`, `…/kich-hoat-khan-cap` | `…/change-level`, `…/close`, `…/convert-to-incident`, `…/activate-emergency` |
| `…/ket-qua-xac-nhan-lai`, `…/lua-chon-thuc-hien` | `…/wish-reconfirmation`, `…/execute-choice` |
| `…/da-toi-noi`, `…/thong-tin-goi-115`, `…/goi-115` | `…/mark-arrived`, `…/call-115-info`, `…/call-115` |
| `/su-co`, `/su-co/{id}` | `/incidents`, `/incidents/{id}` |
| `…/tiep-nhan`, `…/giao-nguoi-xu-ly`, `…/tham-gia-xu-ly`, `…/dien-bien` | `…/receive`, `…/assign-handler`, `…/join-handling`, `…/progressions` |
| `…/doi-muc`, `…/chuyen-theo-doi`, `…/mo-lai-xu-ly`, `…/dong` | `…/change-level`, `…/move-to-monitoring`, `…/reopen`, `…/close` |
| `…/ket-thuc-theo-doi-sau-nga` | `…/end-post-fall-monitoring` |
| `…/the-khan-cap`, `…/xac-nhan-doi-chieu-nguyen-vong` | `…/emergency-card`, `…/confirm-wish-check` |
| `…/chuyen-vien`, `…/chuyen-vien/thong-tin` | `…/transfer-out` (như 8.1), `…/transfer-out/info` |
| `…/thong-tin-goi-115`, `…/goi-115` | `…/call-115-info`, `…/call-115` |
| `…/tra-cuu-tiep-xuc`, `…/danh-sach-tiep-xuc`, `…/nguoi-than-can-lien-he` | `…/contact-lookup`, `…/contact-list`, `…/family-contacts-to-notify` |
| `/ban-tom-tat-chuyen-vien/{id}`, `…/phieu`, `…/in-lai` | `/transfer-summaries/{id}`, `…/document`, `…/reprint` |
| `/nguoi-cao-tuoi/{nctId}/dau-nguy-kich` | `/residents/{residentId}/critical-flags` |
| `/dau-nguy-kich/{id}/xac-nhan`, `…/go` | `/critical-flags/{id}/confirm`, `…/remove` |
| `/danh-sach-tiep-xuc/{id}/nguoi` | `/contact-lists/{id}/contacts` |
| `…/nguoi/{nguoiId}/bo`, `…/nguoi/{nguoiId}/loai`, `/danh-sach-tiep-xuc/{id}/xac-nhan` | `…/contacts/{contactId}/remove`, `…/contacts/{contactId}/exclude` (như 8.1 `exclude`), `/contact-lists/{id}/confirm` |
| `/khoanh-vung`, `…/{id}/gan-su-co`, `…/{id}/go` | `/isolations`, `…/{id}/attach-incident`, `…/{id}/lift` |
| `/giay-phep-co-so`, `/so-lien-he-vien` | `/facility-licenses`, `/facility-contact` |
| `/nguoi-cao-tuoi/{nctId}/kham-dieu-tri` | `/residents/{residentId}/examinations` |
| `/sos`, `/sos/hien-tai`, `/sos/{id}/noi-dang-o`, `/sos/{id}/vi-tri` | `/sos`, `/sos/current`, `/sos/{id}/place`, `/sos/{id}/location` |
| `/ho-tro-khan-cap`, `…/hien-tai`, `…/{id}/chi-tiet`, `…/{id}/vi-tri` | `/emergency-assist`, `…/current`, `…/{id}/details`, `…/{id}/location` |
| `/luot-goi-115/{id}/ket-qua` | `/call-115-attempts/{id}/result` |

## 3. Thân lệnh viết thẳng trong đường dẫn

| Lệnh | Gốc | Dịch |
| --- | --- | --- |
| tạo cảnh báo | `nctId`, `loaiSuCoId`, `muc`, `moTa` | `residentId`, `incidentTypeId`, `level`, `description` |
| giao người phụ trách cảnh báo | `nhanVienId`, `ghiChu` | `staffId`, `note` |
| giao người xử lý sự cố | `nguoiDuocGiaoId`, `ghiChu` | `assigneeId`, `note` |
| đổi mức | `mucMoi`, `lyDo` | `newLevel`, `reason` |
| đóng cảnh báo, sự cố | `ketQua`, `lyDo`, `ketQuaLayNhiem` | `resolution` (cột `alert.resolution`), `reason`, `infectionOutcome` |
| ghi diễn biến | `loaiDienBienId`, `noiDung`, `dienRaLuc` | `progressionTypeId`, `content`, `occurredAt` |
| dấu nguy kịch | `loai`, `nhanDinh` | `type`, `assessment` |
| lựa chọn thực hiện | `luaChon`, `lyDo`, `chuyenVien` | `choice`, `reason`, `transferOut` |
| xác nhận danh sách tiếp xúc | `khongCoNguoiTiepXuc`, `lyDo` | `noContacts`, `reason` |
| gắn sự cố vào vùng | `suCoId` | `incidentId` |
| đổi số liên hệ viện | `soDienThoai`, `ghiChu` | `phoneNumber`, `note` |
| nơi đang ở (SOS) | `noi` | `place` |
| chi tiết nút hỗ trợ | `loaiSuViec`, `tangKhuVucId`, `noi`, `nctId` | `eventType`, `floorAreaId`, `place`, `residentId` |
| kết quả gọi 115 | `ketQua` | `result` |

## 4. Schema và trường

| Gốc | Dịch |
| --- | --- |
| `TrangThaiSos` (`lanBamId`, `canhBaoId`, `trangThai`, `nguoiTiepNhan`, `daToiLuc`, `guiViTri`, `chuKyGuiViTriGiay`, `lienHe`) | `SosStatus` (`pressId`, `alertId`, `status`, `receiverName`, `arrivedAt`, `sendLocation`, `locationIntervalSeconds`, `contacts`) |
| `LienHeKhiKhongGuiDuoc` (`soLienHeVien`, `soCapCuu`) | `FallbackContacts` (`facilityPhone`, `emergencyNumber`) |
| `LoiSos` (`maLoi`, `detail`, `lienHe`) | `SosProblem` (`errorCode`, `detail`, `contacts`) |
| `ViTriGui` (`viDo`, `kinhDo`, `saiSoMet`, `doLuc`) | `LocationReport` (`latitude`, `longitude`, `accuracyMeters`, `measuredAt`) |
| `ViTriHienThi` (+ `coTheDaCu`, `laViTriChinh`) | `LocationView` (+ `possiblyStale`, `isPrimary`) |
| `ThongTinGoi115` (`vien` {`ten`, `diaChi`, `dienThoai`}, `tang`, `phongHoacNoi`, `viTri`, `nguoiCaoTuoi` {`hoTen`, `tuoi`, `gioiTinh`}, `diUng`, `benhNen`, `truongBiAn`) | `Call115Info` (`facility` {`name`, `address`, `phone`}, `floor`, `roomOrPlace`, `location`, `resident` {`fullName`, `age`, `gender`}, `allergies`, `chronicConditions`, `hiddenFields`) |
| `LuotGoi115` (`id`, `bamLuc`, `nguoiBam`, `ketQua`) | `Call115Attempt` (`id`, `pressedAt`, `pressedBy`, `result`) |
| `ThongTinChuyenVien` (`nguyenVongDieuTri`, `nguyenVongCuoiDoi`, `nguoiLienHeChinh`, `canHoiTruoc`, `laCapCuu`) | `TransferOutInfo` (`treatmentWish`, `endOfLifeWish`, `primaryContact` (13), `mustAskFirst`, `isEmergency`) |
| `Muc` | dùng `IncidentLevel` đã có |
| `TrangThaiCanhBao`, `TrangThaiSuCo` | `AlertStatus`, `IncidentStatus` |
| `Loi` (+ `khoangHopLe`, `suCoDangMo`, `nguyenVong`) | `Problem` (6.2) (+ `validRange`, `openIncidentId`, `endOfLifeWish`); `LoiTiepNhan` thêm `receivedBy`, `receivedAt` |
| `LichDoMoi` (`chiSoIds`, `tanSuat` {`kieu`, `cacMoc`, `nGio`, `mocGoc`, `thu`}, `batDau`, `ketThuc`, `mucQuanTrong`, `ghiChu`) | `NewMeasurementSchedule` (`indicatorIds`, `frequency` {`type`, `times`, `everyHours`, `anchorAt`, `weekdays`}, `startAt`, `endAt`, `priority`, `notes`) |
| `LichDo` (+ `id`, `trangThai`, `thayTheCho`, `ngungLuc`, `lyDoNgung`) | `MeasurementSchedule` (+ `id`, `status`, `replaces`, `stoppedAt`, `stopReason`) |
| `GiaTriThanhPhan` (`giaTri`, `thoiDiemDo`, `ghiChu`) | `ComponentValues` (`values`, `measuredAt`, `notes`) |
| `GhiChiSo` (+ `chiSoId`, `xuLyNgay`) | `RecordVitalSign` (+ `indicatorId`, `immediateAction`) |
| `BanGhiChiSo` (`id`, `chiSoId`, `giaTri`, `giaTriGoc`, `thoiDiemDo`, `nguoiDo`, `nguong` {`phienBanId`, `phamVi`}, `muc`, `trangThaiXacNhan`, `hanDoLai`, `nhan`, `doLaiCho`, `truongBiAn`) | `VitalSignRecord` (`id`, `indicatorId`, `values`, `originalValues`, `measuredAt`, `measuredBy`, `threshold` {`versionId`, `scope`}, `level`, `confirmationStatus`, `remeasurementDeadline`, `labels`, `remeasurementFor`, `hiddenFields`) — trùng `VitalSignRecordResponse` hiện có |
| `PhienBanNguongMoi` (`phamVi`, `nctId`, `chiSoId`, `hieuLucTu`, `lyDo`, `thanhPhan[]` {`ma`, `duoiNguyHiem`, `duoiCanhBao`, `trenCanhBao`, `trenNguyHiem`}) | `NewThresholdVersion` (`scope`, `residentId`, `indicatorId`, `effectiveFrom`, `reason`, `components[]` {`code`, `dangerBelow`, `warningBelow`, `warningAbove`, `dangerAbove`}) |
| `PhienBanNguong` (+ `id`, `trangThai`, `hieuLucDen`, `nguoiThietLap`) | `ThresholdVersion` (+ `id`, `status`, `effectiveUntil`, `setBy`) |
| `CanhBao` (`id`, `nctId`, `loai`, `khoaLoai`, `muc`, `soLan`, `lanDauLuc`, `ganNhatLuc`, `trangThai`, `capLeoThang`, `leoThangToiDa`, `hanTiepNhan`, `nguoiPhuTrach`, `tamNhanBanGiao`, `khongCoBacSiTruc`, `suCoId`, `gop`) | `Alert` (`id`, `residentId`, `type`, `typeKey`, `level`, `occurrenceCount`, `firstAt`, `latestAt`, `status`, `escalationLevel`, `maxEscalationReached`, `acknowledgementDeadline`, `assignee`, `handoverAcknowledged`, `noOnDutyDoctor`, `incidentId`, `merged`) — theo cột `alert` |
| `CanhBao` (`nguonTao`, `nguoiBam`, `loaiSuViec`, `noiChon`, `viTriMoiNhat`, `daToiNoiLuc`, `khongGhiMocToiNoi`, `nguon[]` {`loaiNguon`, `nguonId`, `muc`, `thoiDiem`, `daXuLyLuc`}, `lenhDuocPhep`) | (`origin`, `pressedBy`, `eventType`, `chosenPlace`, `latestLocation`, `arrivedAt`, `skipArrivalTime`, `sources[]` {`sourceType`, `sourceId`, `level`, `occurredAt`, `resolvedAt`}, `allowedCommands` (6.2)) |
| `SuCoTuCanhBao` (`loaiSuCoId`, `moTa`, `xuLyBanDau`, `nctId`) | `IncidentFromAlert` (`incidentTypeId`, `description`, `initialResponse`, `residentId`) |
| `GhiNhanSuCo` (`nctId`, `loaiSuCoId`, `nguonPhatSinh`, `xayRaLuc`, `phatHienLuc`, `tangKhuVucId`, `diaDiem`, `hoatDongDangDienRa`, `moTa`, `muc`, `lyDoMucThap`, `xuLyBanDau`, `taoMoiDuDaCo`) | `RecordIncident` (`residentId`, `incidentTypeId`, `context`, `occurredAt`, `discoveredAt`, `floorAreaId`, `location`, `ongoingActivity`, `description`, `level`, `lowerLevelReason`, `initialResponse`, `createDespiteOpen`) |
| `SuCo` (`id`, `noiDung`, `loaiHienHanh`, `trangThai`, `muc`, `daTungKhanCap`, `capLeoThang`, `nguoiPhuTrach`, `tiepNhanLuc`, `ngoaiPhamVi`, `khongCoBacSiTruc`, `dienBien`, `doiMuc`, `nguoiXuLy`, `nguoiDuocThongBao[]` {`nguoiNhan`, `kenh`, `guiLuc`, `daXemLuc`}, `ketQua`, `lenhDuocPhep`, `truongBiAn`) | `Incident` (`id`, `content`, `currentTypeId`, `status`, `level`, `wasEmergency`, `escalationLevel`, `assignee`, `receivedAt`, `outOfScope`, `noOnDutyDoctor`, `progressions`, `levelChanges`, `handlers`, `notifiedRecipients[]` {`recipient`, `channel`, `sentAt`, `viewedAt`} (13 `IncidentNotificationView`), `resolution`, `allowedCommands`, `hiddenFields`) |
| `KetQuaKichHoat` (`suCo`, `theKhanCap`, `chiNhanDang`) | `ActivationResult` (`incident`, `emergencyCard`, `identificationOnly`) |
| `TheKhanCap` (`nhanDang` {`hoTen`, `anh`, `ngaySinh`, `phongGiuong`}, `nguyenVong`, `dauNguyKich`, `diUng`, `thuocDangDung`, `benhNen`, `luuYChamSoc`, `nguoiLienHeChinh` {`hoTen`, `soDienThoai`}, `khongLayDuoc`, `banChupLuc`) | `EmergencyCard` (`identification` {`fullName`, `photo`, `dateOfBirth`, `roomBed`}, `endOfLifeWish`, `criticalFlag`, `allergies`, `currentMedications`, `chronicConditions`, `careNotes`, `primaryContact` {`fullName`, `phoneNumber`}, `unavailable`, `snapshotAt`); trong `endOfLifeWish`, `chuaGhiNhan` → `notRecorded` |
| `ChuyenVien` (`donViDuaDi`, `thoiDiem`, `duKienTroLai`, `nhanVienBanGiaoId`, `nguoiDiKem`, `daLienLacNguoiCanBao`, `lyDoKhongHoiTruoc`, `tepDinhKemIds`) | `TransferOut` (`transferUnit` (như lệnh Chuyển viện 001), `transferAt`, `expectedReturnAt`, `handoverStaffId`, `companion`, `requiredContactReached`, `notAskedFirstReason`, `attachmentIds`) |
| `BanTomTat` (`id`, `coSoTiepNhan`, `diUng`, `thuocDangDung`, `chiSoGanNhat`, `dienBien`, `nguyenVong`, `nguyenVongDieuTri`, `nguoiLienHeChinh`, `nguoiDiKem`, `phieuTepId`, `nguoiLap`, `thoiDiem`) | `TransferSummary` (`id`, `receivingFacility`, `allergies`, `currentMedications`, `latestVitalSigns`, `progressions`, `endOfLifeWish`, `treatmentWish`, `primaryContact`, `companion`, `documentFileId`, `preparedBy`, `preparedAt`) |
| `KetQuaXacNhanLai` (`nguoiDuocLienHe` {`nguoiThanId`, `hoTen`}, `kenh`, `thoiDiem`, `ketQua`, `nguyenVongMoi`) | `WishReconfirmation` (`contactedPerson` {`familyMemberId`, `fullName`}, `channel`, `contactedAt`, `result`, `newWish`) |
| `KetQuaTraCuuTiepXuc` (`truyVetTu`, `truyVetDen`, `khoangVangCuaNguoiNghiNhiem`, `nhom[]` {`nhom`, `nguoi`, `chuaCoDuLieu`}) | `ContactLookupResult` (`traceFrom`, `traceTo`, `suspectedPersonAbsences`, `groups[]` {`group`, `people`, `noData`}) |
| `NguoiTiepXuc` (`id`, `loai`, `doiTuongId`, `hoTen`, `quanHe`, `soDienThoai`, `nhom`, `nguon`, `tiepXucTu`, `tiepXucDen`, `lyDo`, `theoDoiTu`, `theoDoiDen`, `biLoaiLuc`) | `Contact` (`id`, `type`, `subjectId`, `fullName`, `relationship`, `phoneNumber`, `group`, `source`, `contactFrom`, `contactTo`, `reason`, `monitoredFrom`, `monitoredTo`, `excludedAt`) |
| `DanhSachTiepXuc` (`id`, `suCoId`, `trangThai`, `nguoi`, `khongCoNguoiTiepXuc`) | `ContactList` (`id`, `incidentId`, `status`, `contacts`, `noContacts`) |
| `KhoanhVungMoi` (`phamVi` {`tangIds`, `khuVucId`}, `suCoIds`, `lyDo`) | `NewIsolation` (`scope` {`floorIds`, `areaId`} (9), `incidentIds`, `reason`) |
| `KhoanhVung` (+ `id`, `trangThai`, `batDau`, `ketThuc`) | `Isolation` (+ `id`, `status`, `startedAt`, `endedAt`) |
| `GiayPhepCoSo` (`id`, `so`, `coQuanCap`, `ngayCap`, `ngayHetHan`, `coKhamChuaBenh`, `phamVi`, `tepScanId`, `thayTheCho`, `thayTheDoSaiSot`, `lyDo`, `hieuLuc`) | `FacilityLicense` (`id`, `number`, `issuer`, `issuedOn`, `expiresOn`, `coversMedicalPractice`, `scopes`, `scanFileId`, `replaces`, `replacesDueToError`, `reason`, `active`) |
| `BanGhiKham` (`id`, `loai`, `noiDung`, `taiVien`, `tenCoSoNgoai`, `nguoiThucHienId`, `tenNguoiThucHienNgoai`, `thucHienLuc`, `tepIds`) | `ExaminationRecord` (`id`, `type`, `content`, `atFacility`, `externalFacilityName`, `performerId`, `externalPerformerName`, `performedAt`, `fileIds` (16.3)) |

## 5. Giá trị enum

| Gốc | Dịch |
| --- | --- |
| trạng thái SOS `DA_GUI`, `DA_TIEP_NHAN`, `DA_TOI` | `SENT`, `RECEIVED`, `ARRIVED` |
| trạng thái cảnh báo `MOI`, `LEO_THANG`, `DA_TIEP_NHAN`, `DANG_XU_LY`, `DA_DONG`, `CHUYEN_SU_CO` | `NEW`, `ESCALATED`, `RECEIVED`, `IN_PROGRESS`, `CLOSED`, `CONVERTED_TO_INCIDENT` (đã có ở `AlertStatus`, cột `alert.status`) |
| trạng thái sự cố `MOI`, `DANG_XU_LY`, `DANG_THEO_DOI`, `DA_DONG`, `DA_HUY` | `NEW`, `IN_PROGRESS`, `MONITORING`, `CLOSED`, `VOIDED` |
| nguồn tạo cảnh báo `HE_THONG`, `THU_CONG`, `SOS`, `NUT_HO_TRO` | `SYSTEM`, `MANUAL`, `SOS`, `ASSIST_BUTTON` |
| kiểu tần suất `MOC_GIO`, `MOI_N_GIO`, `THEO_THU` | `CLOCK_TIMES`, `EVERY_N_HOURS`, `WEEKDAYS` (đã có ở `MeasurementFrequency`) |
| mức quan trọng `THUONG`, `QUAN_TRONG`, `BAT_BUOC` | `NORMAL`, `IMPORTANT`, `MANDATORY` |
| trạng thái lịch đo `CHO_HIEU_LUC`, `HIEU_LUC`, `DA_NGUNG`, `HET_HAN` | `PENDING`, `EFFECTIVE`, `STOPPED`, `EXPIRED` |
| trạng thái ngưỡng `CHO_HIEU_LUC`, `HIEU_LUC`, `HET_HIEU_LUC`, `DA_HUY` | `PENDING`, `EFFECTIVE`, `EXPIRED`, `CANCELLED` (đã có ở cột `monitoring_setting.status`) |
| phạm vi ngưỡng `CO_SO`, `CA_NHAN` | `FACILITY`, `PERSONAL` (đã có ở cột `monitoring_setting.scope`) |
| mức chỉ số `BINH_THUONG`, `CANH_BAO`, `NGUY_HIEM`, `CHUA_CO_NGUONG` | `NORMAL`, `WARNING`, `DANGEROUS`, `NO_THRESHOLD` (`VitalSignLevel`) |
| xác nhận `CHO_DO_LAI`, `DA_XAC_NHAN`, `DA_DO_LAI` | `AWAITING_REMEASUREMENT`, `CONFIRMED`, `REMEASURED` (`VitalSignConfirmationStatus`) |
| nhãn `XU_LY_NGAY`, `CHUA_DO_LAI`, `GHI_NHAN_MUON`, `GHI_NGOAI_TUYEN` | `IMMEDIATE_ACTION`, `NOT_REMEASURED`, `LATE_RECORDED`, `OFFLINE_RECORDED` (`VitalSignLabel`) |
| nguồn phát sinh `CHAM_SOC`, `AN_UONG`, `THUOC`, `VAN_DONG`, `HOAT_DONG`, `DI_LAI`, `DIEU_TRI`, `HOAT_DONG_NGOAI_VIEN`, `PHUC_VU_SAI_SUAT_AN`, `DO_GUI` | `CARE`, `EATING`, `MEDICATION`, `MOBILITY`, `ACTIVITY`, `TRANSPORT`, `TREATMENT`, `OFFSITE_ACTIVITY`, `WRONG_MEAL_SERVED`, `BELONGINGS` |
| kết quả lây nhiễm `XAC_NHAN_NHIEM`, `LOAI_TRU`, `KHONG_XAC_DINH` | `CONFIRMED_INFECTION`, `RULED_OUT`, `UNDETERMINED` |
| loại dấu nguy kịch `CHINH_THUC`, `TAM` | `OFFICIAL`, `PROVISIONAL` |
| lựa chọn thực hiện `CHUYEN_BENH_VIEN`, `VE_NHA`, `GIAM_NHE` | `TRANSFER_TO_HOSPITAL`, `RETURN_HOME`, `STAY_PALLIATIVE` (như lựa chọn cuối đời 8.3) |
| kênh, kết quả xác nhận lại `GOI_DIEN`, `TRUC_TIEP`; `GIU`, `MOI`, `KHONG_LIEN_LAC_DUOC` | `PHONE_CALL`, `IN_PERSON`; `KEPT`, `CHANGED`, `UNREACHABLE` |
| nhóm tiếp xúc `CUNG_PHONG`, `HOAT_DONG`, `NHAN_VIEN_CHAM_SOC`, `NGUOI_THAN_THAM`, `KHAC` | `SAME_ROOM`, `ACTIVITY`, `CARE_STAFF`, `FAMILY_VISIT`, `OTHER` |
| loại người tiếp xúc `NGUOI_CAO_TUOI`, `NHAN_VIEN`, `NGUOI_THAN`, `NGUOI_DI_CUNG` | `RESIDENT`, `STAFF`, `FAMILY_MEMBER`, `COMPANION` |
| danh sách tiếp xúc `NHAP`, `DA_XAC_NHAN`, `DA_KET_THUC` | `DRAFT`, `CONFIRMED`, `ENDED` |
| khoanh vùng `DANG_KHOANH_VUNG`, `DA_GO` | `ACTIVE`, `LIFTED` |
| loại bản ghi khám `KHAM`, `DANH_GIA`, `CHAN_DOAN`, `CHI_DINH`, `DIEU_TRI`, `KET_QUA` | `EXAMINATION`, `ASSESSMENT`, `DIAGNOSIS`, `ORDER`, `TREATMENT`, `RESULT` |
| nơi `PHONG`, `NHA_VE_SINH`, `HANH_LANG`, `PHONG_SINH_HOAT_CHUNG`, `SAN`, `KHAC` | `ROOM`, `RESTROOM`, `CORRIDOR`, `COMMON_ROOM`, `YARD`, `OTHER` |
| loại sự việc nút hỗ trợ `NGUNG_TIM_NGAT`, `NGA`, `KICH_DONG_BAO_LUC`, `KHAC` | `CARDIAC_ARREST_OR_FAINTING`, `FALL`, `AGITATION_OR_VIOLENCE`, `OTHER` |
| kết quả gọi 115 `DA_GOI_DUOC`, `KHONG_GOI_DUOC` | `CONNECTED`, `NOT_CONNECTED` |
| loại đính chính `M06.BAN_GHI_CHI_SO`, `M06.BAN_GHI_KHAM`, `M05.SU_CO`, `M05.DIEN_BIEN_SU_CO`, `M05.KET_QUA_XAC_NHAN_LAI`, `M05.LUA_CHON_THUC_HIEN` | `M06.VITAL_SIGN_RECORD` (13), `M06.EXAMINATION_RECORD`, `M05.INCIDENT`, `M05.INCIDENT_PROGRESSION`, `M05.WISH_RECONFIRMATION`, `M05.CHOICE_EXECUTION` |
| loại đồng bộ ngoại tuyến `M06.BAN_GHI_CHI_SO` | `M06.VITAL_SIGN_RECORD` |
| loại sự cố trong mô tả `CO_SO_VAT_CHAT`; diễn biến `CHUYEN_VIEN_BI_TU_CHOI`, `GOI_CAP_CUU` | `FACILITY`, `TRANSFER_REFUSED`, `CALL_EMERGENCY` (13) |
| nhóm trường `SUC_KHOE` | `HEALTH` (8.3) |
| quyền `NUT_HO_TRO_KHAN_CAP` | `EMERGENCY_ASSIST_BUTTON` (đã ghi trong hợp đồng) |

## 6. Mã lỗi

| Gốc | Dịch |
| --- | --- |
| `DIEU_KIEN_CHUA_THOA`, `DIEU_KIEN_PHAP_LY`, `CHUYEN_KHONG_HOP_LE` | `CONDITION_NOT_MET`, `LEGAL_CONDITION_NOT_MET` (5), `INVALID_TRANSITION` (8.4) |
| `NGOAI_KHOANG_VAT_LY`, `CHUA_CO_KHOANG_HOP_LE`, `NGUONG_KHONG_HOP_LE` | `OUT_OF_PHYSICAL_RANGE`, `VALID_RANGE_MISSING`, `INVALID_THRESHOLD` |
| `DA_XAC_NHAN_KHONG_DO_LAI` | `RECORD_NOT_AWAITING_REMEASUREMENT` (mã đang dùng) |
| `DA_CO_NGUOI_TIEP_NHAN`, `DA_CO_SU_CO_KHAN_CAP_MO` | `ALREADY_RECEIVED`, `OPEN_EMERGENCY_INCIDENT_EXISTS` |
| `CHUA_DOI_CHIEU_NGUYEN_VONG`, `CHUA_XAC_NHAN_LAI`, `CHUA_CO_LUA_CHON` | `WISH_NOT_CHECKED`, `WISH_NOT_RECONFIRMED`, `CHOICE_NOT_RECORDED` |
| `DA_CO_DAU_NGUY_KICH`, `DANG_CO_BAC_SI_TRUC` | `CRITICAL_FLAG_EXISTS`, `ON_DUTY_DOCTOR_AVAILABLE` |
| `CON_VUNG_KHOANH_CHI_GAN_SU_CO`, `CHUA_DU_DIEN_BIEN` | `ISOLATION_ONLY_FOR_THIS_INCIDENT`, `INSUFFICIENT_PROGRESSIONS` |
| `NGUOI_CAO_TUOI_TRANG_THAI_CUOI` | `FINAL_STATE` (12.5, 15.4) |
| `THIET_BI_KHONG_HOP_LE`, `KHONG_O_VIEN`, `KHONG_CO_MAT`, `KHONG_TRONG_CA` | `INVALID_DEVICE`, `NOT_AT_FACILITY`, `NOT_PRESENT`, `NOT_ON_SHIFT` |
| `DUNG_GUI_VI_TRI`, `CAN_CHON_NGUOI_CAO_TUOI` | `STOP_SENDING_LOCATION`, `RESIDENT_REQUIRED` |

## 7. Điểm chủ dự án đã quyết định (áp theo đề xuất; điểm 4 sửa mã theo hợp đồng)

1. **Loại cảnh báo `NV_YEU_CAU_HO_TRO`** đang nằm trong cột sinh và `CHECK` của `V007_03` (bảng `alert`). Đề xuất dịch thành `STAFF_ASSIST_REQUEST` bằng một migration mới, sửa lại hai cột sinh `open_assist_lock`, `CHECK chk_alert_resident` và đổi giá trị các dòng đã có. Phương án khác: giữ nguyên giá trị trong cơ sở dữ liệu, chỉ dịch ở hợp đồng.
2. **Mã đang phát tiếng Việt trong mã nguồn**: `NGOAI_KHOANG_VAT_LY` (`VitalSignMeasurementService`, `DefaultVitalSignPort`) và phạm vi ngưỡng `"CO_SO"` trong `VitalSignRecordResponse`. Khi áp bản dịch thì đổi sang `OUT_OF_PHYSICAL_RANGE`, `FACILITY`; test HTTP của T018 sửa theo.
3. **Bí danh tiếng Việt trong thân lệnh ghi chỉ số**: `RecordVitalSignRequest`, `RemeasureVitalSignRequest` nhận cả `chiSoId`/`indicatorId`, `giaTri`/`values`… và trường `taskId`/`congViecId` không có trong hợp đồng (đường của 005 đi qua `ChiSoPort`, không qua REST). Đề xuất bỏ bí danh tiếng Việt và bỏ `taskId`; giữ thì phải thêm `taskId` vào hợp đồng, tức là thêm trường (không được làm khi dịch).
4. **`BanGhiChiSo.nguoiDo`** trong hợp đồng là chuỗi (họ tên), còn mã trả `measuredBy` là id số và có thêm `residentId`. Cần chọn: sửa mã theo hợp đồng, hoặc ghi lệch ở plan 007.
5. **`/sos` dùng header `X-Device-Token`** trong khi 002 đã có `X-Device-Id` cùng phiên kênh `RESIDENT` sau quyết định QR 2026-11-02. Bản dịch chỉ đổi tên header. Việc có còn cần token riêng cho SOS hay không là quyết định khác, chưa đổi ở đây.

## 8. Sau khi duyệt

- Sửa `api-007.yaml` theo mục 1 → 6. Chạy `npm run gen:api` để sinh lại client.
- Sửa các controller đã cài nếu có tên bị đổi (mục 7.2 → 7.4).
- Bật so operation 007 trong `ApiContractIT`. Các operation chưa cài vẫn đăng ký là chưa cài.
- Thêm bảng dịch vào glossary mục 13.
- T062 còn phần "mở rộng `NhomGhiNhanCoTriggerIT` cho bảng nhóm 3 của 007". Nhóm 3 của 007 nằm trong bảng gộp (`vital_sign_record` MULTIPLE) và bảng chung. Phần này kiểm ở `RecordGroupTriggerIT` F-04 và trigger theo dòng.

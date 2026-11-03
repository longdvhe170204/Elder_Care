# Hợp đồng Phase 5 — đã duyệt, còn gate tích hợp

Ngày 2026-11-01; phạm vi T027–T031. Chủ dự án đã trả lời “1” duyệt toàn bộ mục1–3; bốn HTTP, SPI và callback đã có trong checkout. Các gate nghiệp vụ/tích hợp bên dưới chưa nghiệm thu.

## 1. Bốn operation HTTP T031

Chỉ dịch, giữ method/status/required/header hiện có:

| Cũ | Tiếng Anh |
| --- | --- |
| GET /co-mat-ban-tru?ngay= | GET /day-care-attendances?date= |
| POST /co-mat-ban-tru/diem-danh-den | POST /day-care-attendances/check-in |
| POST /co-mat-ban-tru/{id}/diem-danh-ve | POST /day-care-attendances/{id}/check-out |
| POST /diem-danh-ban-tru/{id}/dinh-chinh | POST /day-care-attendance-records/{id}/correct |

Check-in: nctId → residentId (required), thoiDiem → at (optional). Check-out giữ required banGhiDonId → pickupRecordId, thoiDiem → at; nguồn 012 xác định PICKUP/SELF_RETURN/INSTITUTE_RETURN từ bản ghi thật, không thêm field type để client bỏ kiểm nguồn.

CoMat → DayCareAttendance: id, nctId→residentId, ngay→date, trangThai→status, gioDenDuKien→expectedArrival, gioVeDuKien→expectedReturn, gioVeDuKienHienHanh→currentExpectedReturn, gioDen→arrivedAt, gioVe→leftAt, loaiVe→checkoutType, buoiPhatSinh→adHocSession, baoVangMuon→lateAbsenceReported. Enum dùng AttendanceStatus và CheckoutType đã công bố. Checkout response: coMat→attendance, congViecDaHuy→cancelledTaskIds. Không thêm trường ngoài schema.

Đính chính chỉ dịch request riêng của Phase5: kieu→type (AMEND_CONTENT/VOID theo CorrectionType hiện có), noiDungDung→correctedContent, lyDo→reason. correctedContent mở như hợp đồng cũ; provider giới hạn đính chính giờ bằng {at: ISO instant}, không sửa bản gốc. Giữ POST201 và cơ chế CorrectionApi cách A/B/C. Không đổi request DinhChinh dùng bởi story chưa dịch.

## 2. Nguồn lịch nghỉ/báo vắng còn thiếu

ContractReadApi.attendanceOn đã có giờ đến/về, nhưng không có ngày khu nghỉ hoặc báo vắng đủ/muộn hạn. Không được mặc định không nghỉ/không báo vắng để chạy T030. Đề xuất SPI do005 sở hữu, B004 cài:

```java
interface DayCareCalendarPort {
    CalendarView onDate(long residentId, LocalDate date);
    record CalendarView(boolean holiday, Long timelyAbsenceId, Long lateAbsenceId) {}
}
```

Chỉ đọc các quyết định/bản ghi đã có của004. Provider chưa có thì503, không default hoặc fake production. Fake duy nhất do005 khai báo tại support/fake/care, fixture chưa đặt báo lỗi.

## 3. Callback cập nhật lịch

Checkout hiện không có lớp sự kiện công khai BaoVangBanTruDaGhi/NgayNghiBanTruDaDoi/PhuLucDaApDung của004; không tự khai hộ event B. Đề xuất thêm vào DayCareAttendanceApi, giữ mọi hàm cũ:

```java
@Transactional(propagation = Propagation.MANDATORY)
void refreshCalendar(long residentId, LocalDate date);
```

004 gọi trong transaction lệnh thay lịch/ngày nghỉ/báo vắng;005 đọc lại ContractReadApi, ContractApi và DayCareCalendarPort để áp bảng FR-013/FR-014. Đây là callback thay đường tích hợp listener chưa có kiểu nguồn, không thêm nghiệp vụ. Chưa có provider TrangThaiCoMatBanTruPort do004 khai báo trong checkout; bàn giao B công bố trước khi005 cài, không tự sửa module B.

Đổi api cần duyệt theo AGENTS.md mục2.5 và phầnBackend. Nếu duyệt sẽ cập nhật hợp đồng module, glossary, fake hiện có và team-plan mục8 để B/E review nguồn. Không thêm thư viện, không sửa common/mobile ApiClient đã chọn chờ ở Phase4.



## 4. Điều chỉnh cách ghép nguồn theo C-02

Giữ chiều mã `care → admission` theo research C-02 và ModuleBoundaryTest. Phần mô tả để B cài trực tiếp care.spi/call care.api từ module admission cần điều chỉnh về adapter/listener thuộc care: B công bố API/sự kiện của admission; C đọc API đó và nghe sự kiện đồng bộ trong transaction rồi gọi refreshCalendar. B không import care để cài DayCareCalendarPort khi care đã import ContractReadApi, vì như vậy tạo vòng. Không thay chữ ký SPI/callback đã duyệt; nguồn production còn chờ hợp đồng/types của B, fake ở care chỉ phục vụ test.

## 5. Gate còn mở sau bản cài

- VOID: chủ dự án đã chọn triển khai sửa giờ trước, giữ VOID chờ chốt nghiệp vụ. Endpoint005 hiện từ chối409 trước khi tạo yêu cầu cáchC; provider chặn khi áp dụng. Generic000 vẫn cần cơ chế từ chối loại chưa hỗ trợ trước tạo approval; không coi VOID đã triển khai.
- LOAI_LUU_TRU DAY_CARE→nội trú: thiếu quy tắc trạng thái/tính phí ngày đổi; chủ dự án đã chọn chờ thống nhất với B. Callback hiện409 và có thể rollback transaction/job, chưa tự suy trạng thái hủy.
- Retention bảng chung: cần A nối enumeration chuỗi attendance/record/correction và kiểm thời hạn trước xóa. Liên kết raw rows với M01.RESIDENT chưa đủ bảo đảm chuỗi đính chính.
- TrangThaiCoMatBanTruPort và nguồn lịch/ngày nghỉ/báo vắng production chờ B; adapter mobile đọc mảng chờ A theo quyết định Phase4.


**Ghi chú khi merge — ưu tiên listener của B/E (chủ dự án đã chốt):** nếu nhánh ghép đã có listener và kiểu sự kiện nguồn tương ứng do B/E cung cấp, ưu tiên dùng bản của B/E thay cho đường tích hợp tạm đã làm ở005. Đối chiếu và hợp nhất adapter/callback hiện có để mỗi sự kiện chỉ được xử lý một lần; bỏ phần tích hợp tạm bị thay thế khi đã kiểm tương đương. Giữ chữ ký API cũ và bàn giao B/E review trước khi ghép. Chỗ đặt lớp và chiều phụ thuộc vẫn theo C-02; nếu hợp đồng khác nhau thì ghi rõ chênh lệch để review/duyệt, không tự đổi chữ ký hoặc giữ hai listener chạy trùng. Ghi chú này là quy tắc cho lần merge sau, chưa xác nhận B/E đã có listener và chưa thay mã nguồn hiện tại.

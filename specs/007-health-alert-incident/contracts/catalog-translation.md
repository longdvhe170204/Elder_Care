# Bản dịch phần danh mục T006 đã áp dụng

Giữ nguyên phương thức, số endpoint, trường, kiểu, required/readOnly, security, header và mã phản hồi của `api-007.yaml`. Bản dịch này đã được áp vào phần danh mục của hợp đồng và controller sau yêu cầu hoàn tất T006 ngày 2026-11-02.

Đường dẫn (tiền tố `/api/v1` giữ nguyên):

- `/chi-so-danh-muc` → `/vital-sign-catalog`; GET/POST giữ nguyên.
- `/chi-so-danh-muc/{id}` → `/vital-sign-catalog/{id}`; PUT giữ nguyên.
- `/chi-so-danh-muc/{id}/ngung-hieu-luc` → `/vital-sign-catalog/{id}/deactivate`; POST giữ nguyên.
- `/quy-tac-xu-huong` → `/trend-rules`; GET giữ nguyên.
- `/quy-tac-xu-huong/{ma}` → `/trend-rules/{code}`; PUT giữ nguyên.
- `/loai-su-co` → `/incident-types`; GET/POST giữ nguyên.
- `/loai-su-co/{id}` → `/incident-types/{id}`; PUT giữ nguyên.
- `/loai-dien-bien` → `/progression-types`; GET/POST giữ nguyên.

Schema và trường:

- `ChiSo` → `VitalSignIndicator`: `id`, `ma` → `code`, `ten` → `name`, `donVi` → `unit`, `sinhHieu` → `vitalSign`, `thanhPhan` → `components`, `khoangHopLe` → `validRange`, `chuaCoKhoangHopLe` → `missingValidRange`, `hieuLuc` → `active`. Thành phần: `ma` → `code`, `ten` → `name`, `soChuSoThapPhan` → `decimalPlaces`.
- `QuyTacXuHuong` → `TrendRule`: `ma` → `code`, `ten` → `name`, `bat` → `enabled`, `muc` → `level`, `maThamSo` → `parameterCode`. Lệnh PUT giữ ba trường `enabled`, `level`, `reason` (từ `lyDo`).
- `LoaiSuCo` → `IncidentType`: `id`, `ma` → `code`, `ten` → `name`, `mucMacDinh` → `defaultLevel`, `khongThuocSucKhoe` → `nonHealth`, `choPhepKhongNct` → `allowsNoResident` (readOnly), `hieuLuc` → `active`.
- `LoaiDienBien` → `ProgressionType`: `id`, `ma` → `code`, `ten` → `name`, `nhom` → `group`, `bienPhapHoiSuc` → `resuscitation`.
- `Muc`: `NHE/TRUNG_BINH/KHAN_CAP` → `LOW/MEDIUM/CRITICAL`.
- Nhóm diễn biến: `DANH_GIA/HANH_DONG/THONG_BAO/THEO_DOI/HE_THONG` → `EVALUATION/ACTION/NOTIFICATION/MONITORING/SYSTEM`.
- Mã quy tắc: `SUT_CAN/BO_LO_LAN_DO/MAT_NGU_KEO_DAI/TU_CHOI_THUOC` → `WEIGHT_LOSS/MISSED_MEASUREMENT/PROLONGED_INSOMNIA/MEDICATION_REFUSAL`.
- `LyDo` → `Reason`, `phien` → `session`, `Loi` → `Problem`; không thay hình dạng nội dung/luật bảo mật. Không thêm `version` hoặc `sortOrder` vào HTTP; phiên bản nội bộ dùng ETag.

Điểm hợp đồng cần quyết định riêng, không tự sửa: chưa có endpoint Ngừng hiệu lực cho loại sự cố/diễn biến, và PUT loại sự cố chỉ có `active` để biểu diễn hiệu lực. Hiện không thêm endpoint mới; bảo vệ các mã có quy tắc riêng ở dịch vụ khi cập nhật `active`.

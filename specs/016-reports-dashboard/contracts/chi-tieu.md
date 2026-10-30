# Danh mục chỉ tiêu: Feature 016

**Ngày**: 2026-10-10 | **Plan**: [../plan.md](../plan.md)

Mã chỉ tiêu dùng trong REST (`/bao-cao/{nhom}`, `/chi-tiet/{maChiTieu}`) và trong test SC-001. Định nghĩa đầy đủ là FR của spec ghi ở cột cuối (FR-003); bảng này chỉ gán mã, nhóm quyền (bảng FR-010) và giao diện nguồn.

| Mã | Nhóm quyền (FR-010) | Giao diện nguồn | FR |
| --- | --- | --- | --- |
| `NCT.DANG_LUU_TRU` | Người cao tuổi | 001 `NguoiCaoTuoiThongKeApi.trangThaiTaiNgay`; 004 loại lưu trú; 005 có mặt bán trú | FR-020 |
| `NCT.TAI_NGAY` | Người cao tuổi | 001, 004 | FR-022 |
| `NCT.BIEN_DONG` | Người cao tuổi | 001 `bienDong` | FR-023 |
| `NCT.DANH_SACH_CHO` | Danh sách chờ | 004 `LuuTruThongKeApi.danhSachCho` | FR-025 |
| `GIUONG.TRANG_THAI` | Phòng, giường | 003 `TinhTrangGiuongApi.demTheoTrangThai`, `GiuongThongKeApi.trangThaiTaiNgay`; 019 dấu chờ chuyển người | FR-020, FR-024 |
| `GIUONG.CO_THE_DUNG` *(bổ sung 2026-10-27)* | Phòng, giường | 003 `GiuongThongKeApi.trangThaiTaiNgay` | FR-020 (tổng giường có thể dùng, Q-246) |
| `GIUONG.PHONG_CACH_LY` *(bổ sung 2026-10-27)* | Phòng, giường | 003 `GiuongThongKeApi.phongCachLy` | FR-020 |
| `GIUONG.SO_NGUOI` *(bổ sung 2026-10-27)* | Phòng, giường | 003 `PhanBoGiuongApi.nguoiTrongPhamVi`, `dangHieuLuc` | FR-024 (số người theo tầng, phòng) |
| `GIUONG.TRONG_CHO_CHUYEN` *(bổ sung 2026-10-27, chưa cài: chờ 019)* | Phòng, giường | 019 dấu chờ chuyển người | FR-020 |
| `GIUONG.CHO_SAN_SANG` | Thời gian giường Chờ sẵn sàng | 003 `GiuongThongKeApi.choSanSang` | FR-033 (trung bình theo tầng; mốc xếp khoảng Q-390) |
| `GIUONG.CHO_SAN_SANG_DAI_NHAT`, `GIUONG.CHO_SAN_SANG_VUOT_HAN` *(bổ sung 2026-10-27)* | Thời gian giường Chờ sẵn sàng | 003 `GiuongThongKeApi.choSanSang` | FR-033 (dài nhất; số lần vượt CFG-M03-04) |
| `CV.HOM_NAY`, `CV.QUA_HAN` | Công việc | 005 `CongViecThongKeApi` | FR-020 |
| `CV.THEO_TRANG_THAI` | Công việc | 005 | FR-030 |
| `CV.TY_LE_DUNG_HAN_BAT_BUOC` | Công việc | 005 | FR-031 |
| `CV.GHI_NHAN_MUON` | Công việc | 005; 014 (điểm danh, tách riêng) | FR-032 |
| `CV.THEO_NHAN_VIEN` | Chỉ tiêu theo nhân viên | 005, 014 | FR-030, FR-037 |
| `BUA.PHIEU_TRE`, `BUA.PHIEU_SAI_LECH` | Phiếu bữa ăn | 011 `DinhDuongThongKeApi.phieu` | FR-034 |
| `HD.BUOI`, `HD.TY_LE_THAM_GIA`, `HD.CHUYEN_QUA_GIO`, `HD.THIEU_NGUOI` | Hoạt động | 014 `HoatDongThongKeApi` | FR-020, FR-035 |
| `HD.CO_LAP` | Hoạt động | 014 `coLap` | FR-036 |
| `KT.TY_LE_DAT`, `KT.THEO_VAI_TRO_KIEM_TRA` | Kiểm tra chất lượng | 014 `kiemTra` | FR-037 |
| `SK.CHI_SO` | Chỉ số, cảnh báo, sự cố | 007 `ChiSoThongKeApi` | FR-040 |
| `SK.CANH_BAO`, `SK.CANH_BAO_THOI_GIAN_TIEP_NHAN`, `SK.CANH_BAO_LEO_THANG` | Chỉ số, cảnh báo, sự cố | 007 `CanhBaoThongKeApi` | FR-020, FR-041 |
| `SK.CANH_BAO_SOS_HO_TRO` *(bổ sung 2026-10-22; gồm chỉ tiêu SOS của 2026-10-18)* | Chỉ số, cảnh báo, sự cố | 007 `CanhBaoThongKeApi` (`nguon`, `daToiNoiLuc`, `khongGhiMocToiNoi`) | FR-041 |
| `SK.SU_CO`, `SK.SU_CO_LEO_THANG`, `SK.SU_CO_THOI_GIAN_TIEP_NHAN`, `SK.THEO_DOI` | Chỉ số, cảnh báo, sự cố | 007 `SuCoApi`, `CanhBaoThongKeApi` | FR-020, FR-042 |
| `SK.SU_CO_KHONG_THUOC_SK` | Sự cố không thuộc sức khỏe (Hành chính) | 007, danh mục loại sự cố | FR-042a |
| `SK.DANH_GIA_LAI_QUA_HAN` | Đánh giá lại quá hạn | 001 `danhGiaLaiQuaHan` | FR-044 |
| `THUOC.TY_LE_BO_LO`, `THUOC.TY_LE_TU_CHOI` | Liều thuốc | 006 `ThuocThongKeApi` | FR-043 |
| `THUOC.DOI_CHIEU_QUA_HAN` | Phiếu đối chiếu | 006 | FR-044 |
| `SK.KHOANH_VUNG` | Khu đang khoanh vùng | 007 `KhoanhVungApi.dangKhoanh` | FR-020 |
| `SK.NGUY_KICH` | Dấu nguy kịch | 007 | FR-020 |
| `CA.BAN_GIAO_CHUA_XAC_NHAN` | Bàn giao | 008 `NhanSuThongKeApi.banGiao`; *(2026-10-27)* `banGiaoDangMo` (`openHandovers`) | FR-020 |
| `CA.TY_LE_PHUC_VU` | Tỷ lệ phục vụ | 008 `tyLePhucVu` | FR-020, FR-070 |
| ~~`CA.THIEU_PHU`, `CA.THIEU_CA`~~ | *(Bỏ theo Q-397)* | — | FR-071 |
| `CA.YEU_CAU` | Nhân sự và ca trực | 015 `yeuCau` | FR-072 |
| ~~`CA.LOI_MOI`~~ | *(Bỏ theo Q-397)* | — | FR-072 |
| `CA.GIO_LAM` | Nhân sự và ca trực (QL, TT được giao) | 015 `gioLamTheoThang` | FR-073 |
| `GP.CO_SO`, `GP.NHAN_VIEN` | Giấy phép | 007 `GiayPhepCoSoApi`; 008 `chungChiSapHetHan` | FR-020 |
| `CP.BANG`, `CP.TONG_HOP`, `CP.DIEU_CHINH_SAU_CHOT`, `CP.TY_LE_THEO_SU_KIEN` | Chi phí phát sinh | 010 `ChiPhiThongKeApi` | FR-020, FR-050 → FR-055 |
| `CP.MUA_SAM` | Tổng chi mua sắm | 019 `MuaSamThongKeApi` | FR-055a |
| `SD.THAP_CON_NO` | Số dư | 017 `SoDuThongKeApi.tinhTrang` | FR-020 |
| `KB.CAN_LAM` | Khai báo tạm trú | 018 `KhaiBaoThongKeApi` | FR-020 |
| `TS.QUA_HAN` | Tài sản | 019 `TaiSanThongKeApi.quaHan` | FR-020 |
| ~~`KHO.DUOI_TOI_THIEU`, `KHO.LO_SAP_HET_HAN`~~ | *(Bỏ theo Q-392)* | — | — |

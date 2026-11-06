# Kiểm toàn bộ task 008 và thao tác Git — 04/11/2026

Đã fetch và kiểm origin/develop ở `62fdb4f2`; develop cục bộ ở `55807c24`, chậm 5 commit. Không checkout, merge, rebase, reset, stash/pop, commit hoặc push trong lượt kiểm này. Các worktree chức năng giữ nguyên.

**Kết luận: 008 chưa hoàn tất toàn feature.** Trên origin/develop có 57 dòng task: 49 [x], 8 [ ]. Một số phần đã cài và kiểm ở worktree riêng nhưng chưa commit/tích hợp. Không cộng checkbox hoặc số test giữa các worktree có nền khác nhau.

## 1. Đối chiếu đủ 57 dòng task

Nguồn là `git show origin/develop:specs/008-staff-shift-handover/tasks.md`. Bảng ghi trạng thái của từng dòng task và đối chiếu phần còn mở với mã, nhật ký, stash và lịch sử Git. Với 49 dòng [x], đây là trạng thái đã được ghi nhận/merge, không phải kết quả chạy lại tất cả test trong phiên này. Mô tả dài được rút gọn; đường dẫn BE/BT trong tasks là tên logic.

| Task | Trên origin/develop | Nội dung | Kết quả rà |
| --- | --- | --- | --- |
| T001 | [x] | Tạo khung M09 theo backend/README.md, R-02: dùng tên tiếng Anh, chỉ tạo gói khi có lớp; giữ @ApplicationModule ở gốc, @NamedInterface cho các gói API được tạo. ArchUnit BT/staffing/M09ArchTe… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T002 | [x] | Khai báo tối thiểu các SPI đã có trong hợp đồng để tạo bản giả: DoGuiBanGiaoPort (013), ChuyenDiNhiemVuPort (014), KiemTraCongBoMoRong (015); bản giả và @TestConfiguration ở BT/support/fake/… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T003 | [x] | Tạo giao diện BE/staffing/api/: CaTrucApi, NhanSuThongKeApi và sự kiện mục 1.6 (cài CoYeuCauThongBao khi gửi 009). Hai SPI DoGuiBanGiaoPort, ChuyenDiNhiemVuPort đã được đưa lên T002 để bản g… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T004 | [x] | Tạo MIG/V008_01__nhan_vien.sql: nhan_vien (tai_khoan_id BIGINT NULL UNIQUE; trang_thai ENUM('DANG_LAM_VIEC','NGHI_VIEC') chỉ đổi khi @lenh_trang_thai_nv; trigger chặn DELETE), bo_phan (nạp Y… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T005 | [x] | Cài hàm thuần BE/staffing/giayphep/internal/HieuLucChungChi.java (hiệu lực tới hết ngày hết hạn theo ThoiGianNghiepVu; thu hồi; thay thế) và test BT/staffing/HieuLucChungChiTest.java (resear… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T006 | [x] | Cài tạo, sửa hồ sơ nhân viên (Quản lý viện; trường liên hệ @NhomTruong(HAN_CHE_NHAN_VIEN)) và thêm chứng chỉ trong BE/staffing/nhanvien/internal/NhanVienService.java, BE/staffing/giayphep/in… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T007 | [x] | Cài SPI cho 002 trong BE/staffing/nhiemvu/internal/: HoSoNhanVienApi (trangThai, chucDanhCoVaiTro), GiayPhepApi (hieuLuc, sapHetHan), DanhMucPhamViHanhNgheApi (contracts mục 1.1)… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T008 | [x] | Cài khung job BE/staffing/lich/ViecTheoPhutM09.java, ViecHangGioM09.java, ViecHangNgayM09.java (00:10) (ShedLock, lan_chay_m09, chạy bù) – việc đăng ký ở các story; test BT/staffing/ChayBuM0… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T009 | [x] | Cài provider cho 000/002: PhanLoaiTruongProvider (HAN_CHE_NHAN_VIEN, LY_DO_NHAN_SU, nội dung bàn giao), LuuTruProvider theo data-model mục 10 cho bảng đã có trong T004, GanPhamViProvider hồ … | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T010 | [x] | Test đơn vị BT/staffing/ChuoiCaLienTucTest.java | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T011 | [x] | Test BT/staffing/BR_M09_03_ChongCaIT.java, ChongCaDongThoiIT.java, FR_016_CongBoIT.java, FR_022_TrangThaiCaIT.java, BR_M09_12_Ca24GioIT.java (giờ bắt đầu bằng giờ kết thúc là ca 24 giờ, kết … | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T012 | [x] | Tạo MIG/V008_02__lich_ca.sql: mau_ca (loai_pham_vi TANG\|KHU_VUC\|TOAN_VIEN; yeu_cau_ban_giao), lich_ca (trang_thai NHAP\|DA_CONG_BO; UNIQUE (thang, loai_pham_vi, pham_vi_id)), ca (trang_thai E… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T013 | [x] | Cài BE/staffing/lichca/internal/LichCaService.java: mẫu ca (AC-01), lịch Nháp, thêm ca và nhân viên (khóa nhan_vien, chồng giờ, ChuoiCaLienTuc, chứng chỉ), người phụ trách ca, Bác sĩ trực, x… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T014 | [x] | Đăng ký việc theo phút: ca DA_CONG_BO → DANG_DIEN_RA → DA_DONG/CHO_BAN_GIAO | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T015 | [x] | Cài CaTrucApi, NhanSuCaPort (005), DieuDuongCaPort (006), NhanSuCanhBaoPort (007), ThamChieuViTri (003) trong BE/staffing/nhiemvu/internal/TraCuuCaService.java (người phụ trách ca theo thời … | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T016 | [x] | Tạo BE/staffing/lichca/web/LichCaController.java: /mau-ca, /{id}, /lich-ca, /lich-ca/{id}/ca, /ca/{id}, /lich-ca/{id}/xem-truoc-cong-bo, /cong-bo, GET /toi/ca, GET /bac-si-truc; màn web fron… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T016b | [ ] | Mobile Ca của tôi | Đã cài riêng ở 008-my-shifts-mobile, task nhánh đó [x], log ghi 77/77 Flutter xanh. Chưa vào develop; còn tích hợp và kiểm thiết bị/backend thật. |
| T017 | [x] | Test đơn vị BT/staffing/XacDinhNhanVienChinhTest.java, SC_005_XepCaPhanCongTest.java (200 lần). **Tên/chỗ cài**: PrimaryAssigneeResolverTest kiểm hàm thuần; bộ 200 trường hợp SC-005 nằm tron… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T018 | [x] | Test BT/staffing/DBR_21_ChungChiPhanCongIT.java, BR_M09_01_ChanThieuChungChiIT.java, FR_028_NhanVienChinhIT.java, PhanCongDongThoiIT.java, FR_032_SaoChepIT.java, FR_024_GiaoTruongTangIT.java… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T019 | [x] | Tạo MIG/V008_03__phan_cong.sql: tang_ca_khoa (tang_khu_vuc_id khóa chính), giao_truong_tang (trang_thai HIEU_LUC\|DA_KET_THUC), phan_cong (loai_doi_tuong TANG\|KHU_VUC\|PHONG\|NGUOI_CAO_TUOI; ch… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T020 | [x] | Cài BE/staffing/phancong/internal/PhanCongService.java: giao/kết thúc Trưởng tầng, lập (yêu cầu loại công việc qua LoaiCongViecApi.yeuCau; nghiệp vụ "phát thuốc" qua NghiepVuChuyenMonApi.loa… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T021 | [x] | Cài PhanCongApi, NhiemVuApi (002; gồm nguoiThayTruongTang, boPhanCua, laPhuTrachBoPhan) trong BE/staffing/nhiemvu/internal/ Nối kiểm/gỡ/chuyển hiệu lực/snapshot phân công lúc công bố, Assign… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T022 | [x] | Tạo BE/staffing/phancong/web/PhanCongController.java: /giao-truong-tang, /{id}/ket-thuc, /ca/{id}/phan-cong, /phan-cong/{id}, /huy, /ket-thuc, /ca/{id}/sao-chep-phan-cong; màn web frontend/s… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T022b | [ ] | Mobile Phân công | Đã có màn/backend/hợp đồng bổ sung ở 008-mobile-assignments; log Flutter 75/75 xanh. Task vẫn [ ] vì còn review, nguồn thật, thiết bị và tích hợp. |
| T023 | [x] | Test đơn vị BT/staffing/XacDinhCaSauTest.java, MucNghiemTrongTest.java | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T024 | [x] | Test BT/staffing/BR_M09_06_DanhSachNguonIT.java, FR_040_MucBanNhapIT.java, FR_041_HoanTatIT.java, BR_M09_07_ChoBanGiaoIT.java, FR_043_NhacBanGiaoIT.java: danh sách nguồn đọc song song 003 → … | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T025 | [x] | Test BT/staffing/BR_M09_08_XacNhanBanGiaoIT.java, FR_044_XacNhanNguyenTuIT.java, FR_042_LenhDongThoiIT.java, DBR_20_MotBanGiaoMoiCaIT.java, BanGiaoTriggerIT.java, FR_047_ChamSocXemBanGiaoIT.… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T026 | [x] | Tạo MIG/V008_04__ban_giao.sql: ban_giao (ca_id UNIQUE; trang_thai ENUM('BAN_NHAP','DA_LAP','CO_Y_KIEN','DA_XAC_NHAN'); trigger chặn đổi khi DA_XAC_NHAN, chặn đổi chot_luc đã có), muc_ban_gia… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T027 | [x] | Cài BE/staffing/bangiao/internal/BanGhiNguonBanGiaoService.java: đọc song song CongViecBanGiaoApi, LieuBanGiaoApi, ChiSoApi.vuotNguongTrongKhoang, CanhBaoBanGiaoApi.dangMo, LuotVangApi, Tran… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T028 | [x] | Cài BE/staffing/bangiao/internal/BanGiaoService.java: lập (người phụ trách ca hoặc Trưởng tầng từ mốc FR-039), thêm/xóa mục ở Bản nháp (If-Match), nhận định, hoàn tất (chot_luc; ca CHO_BAN_G… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T029 | [x] | Tạo BE/staffing/bangiao/web/BanGiaoController.java: POST/GET /ca/{id}/ban-giao, GET /ban-giao/{id}, /ban-ghi-nguon, /muc, /muc/{mucId}, /nhan-dinh, POST /hoan-tat, /xac-nhan, /y-kien, /bo-su… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T030 | [ ] | Mobile xem bàn giao ngoại tuyến | Chưa xong; chưa thấy Dart/test tương ứng trong các worktree đang đăng ký hoặc bốn stash đã kiểm. |
| T031 | [x] | Test BT/staffing/BR_M09_04_NghiViecIT.java, FR_003_NhanLaiIT.java, FR_001a_PhuTrachBoPhanIT.java, DBR_36_MotBoPhanIT.java, NhanVienTriggerIT.java: nghỉ việc gọi TaiKhoanApi.khoaDoNghiViec và… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T032 | [x] | Test BT/staffing/BR_M09_05_CanhBaoDaoTaoIT.java, FR_009_ThuHoiIT.java: cảnh báo một lần; thu hồi làm mất hiệu lực, kết thúc phân công cần chứng chỉ (Q14, Q15) | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T033 | [x] | Cài lệnh Cho nghỉ việc (hiệu lực ngay hoặc tương lai qua việc theo phút), Nhận lại làm việc, đổi bộ phận, chỉ định Phụ trách bộ phận trong NhanVienService.java (research N-04) | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T034 | [x] | Cài gia hạn, thu hồi, sửa sai chứng chỉ trong ChungChiService.java; việc theo phút chứng chỉ hết hiệu lực theo mốc; việc hằng ngày cảnh báo đào tạo sắp hết hạn (CFG-M09-03), bộ phận chưa có … | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T035 | [x] | Bổ sung endpoint POST /nhan-vien/{id}/cho-nghi-viec, /nhan-lai-lam-viec, /doi-bo-phan, GET /bo-phan, POST /bo-phan/{id}/chi-dinh-phu-trach, POST /chung-chi/{id}/gia-han, /thu-hoi, /sua-sai; … | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T046 | [x] | Test BT/staffing/FR_001b_ImportNhanVienIT.java, KiemTraDongImportTest.java, ImportHieuNangIT.java (quickstart I1 → I7): xem trước không ghi; mọi loại lỗi đúng dòng, cột, lý do; trùng hồ sơ m… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T047 | [x] | Tạo MIG/V008_xx__import_nhan_vien.sql (số theo thứ tự migration 008 lúc làm): lan_import_nhan_vien; cột nhan_vien.dien_thoai_chuan (chỉ mục, điền cho dữ liệu cũ), nguon, lan_import_id; cài B… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T048 | [x] | Tạo endpoint GET /nhan-vien/import/mau, POST /nhan-vien/import/xem-truoc, POST /nhan-vien/import/xac-nhan trong NhanVienController.java và màn web import trong frontend/src/tinh-nang/nhan-vi… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T036 | [x] | Test đơn vị BT/staffing/TinhTyLePhucVuTest.java và BT/staffing/BR_M09_02_TyLePhucVuIT.java, FR_036_CanhBaoMotLanIT.java, BR_M09_14_TinhTheoCaIT.java (người có ca chưa có phân công vẫn được t… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T037 | [x] | Tạo bảng lan_tinh_ty_le (nhóm 3; su_kien 10 giá trị; tong_trong_so DECIMAL(8,2)) trong MIG/V008_07__ty_le.sql; cài BE/staffing/tyle/internal/TyLePhucVuService.java (TinhTyLePhucVu; listener … | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T038 | [x] | Tạo endpoint GET /ca/{id}/ty-le-phuc-vu và màn web frontend/src/tinh-nang/ty-le-phuc-vu/ | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T039 | [x] | Test các quy tắc FR-017 → FR-021, FR-052/053, FR-058/059 và BR-M09-13 trong BT/staffing/PostPublicationShiftIT.java, PostPublicationPortsIT.java, PostPublicationNotificationsTest.java, Absen… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T040 | [x] | Cài lệnh bổ sung nhân viên, chuyển người phụ trách ca, ghi nhận vắng, hủy ca trong BE/staffing/shift/internal/service/PostPublicationService.java; port shift/internal/service/port/ShiftComma… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T041 | [x] | Bổ sung đúng endpoint đã dịch: POST /shifts/{id}/add-staff, /change-lead, /record-absence, /cancel, GET /duty-basis-audit-logs; nối các lệnh và tra cứu vào web features/shifts theo giai đoạn… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T041b | [ ] | Mobile ghi nhận vắng ca | Chưa xong; chưa thấy Dart/test tương ứng trong các worktree đang đăng ký hoặc bốn stash đã kiểm. |
| T042 | [x] | Chốt đối chiếu đầy đủ ApiContractIT với contracts/api-008.yaml (pending và pendingResponses rỗng, checkOperations=true); bổ sung BT/staffing/M09RecordProtectionIT.java kiểm đủ BEFORE UPDATE/… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T043 | [x] | Kiểm các SPI của 002, 005–007 bằng bean thật của 008 trong BT/staffing/StaffModuleIntegrationIT.java, không nhập FakeM15Config: nối ScopeApi thật của A, kiểm lịch sử phụ trách/chuyển người/v… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T043b | [ ] | Kiểm tích hợp với nghiệp vụ 005–007 thật | Còn mở; nguồn thật và kiểm liên module C/D chưa đủ. API/fake/callback test riêng không thay được nghiệm thu tích hợp. |
| T044 | [x] | Chạy nền ./mvnw clean verify, npm test, lint/build và flutter test --no-pub; đối chiếu tự động các Q/SC của quickstart theo validation.md. Flutter hiện chỉ kiểm phần người cao tuổi đã mở; ph… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T044b | [ ] | Quickstart đầu–cuối, thiết bị và đo SC | Còn mở: nguồn/demo, Q38 trên thiết bị, SC-002/010 đo thao tác thật và SC-003 vận hành thử một tháng. |
| T045 | [x] | Rà truy vết data-model mục 11 và validation.md: DBR-20, 21, 23, 25, 36; BR-M09-01 → 08, 12 → 17, BR-M04-13, BR-M05-05 (phần 008); FR-048 → FR-060 đều có task/test hoặc task nghiệm thu còn mở… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T049 | [x] | *(Bổ sung 2026-10-22, 007 FR-047i)* Chốt ba hàm AlertStaffPort.onShift, floorDuringShift, floorsWithShift đã cài trong AlertStaffLookup/ShiftQueries; BT/staffing/AlertStaffPortIT.java kiểm K… | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |
| T049b | [ ] | H1–H9 với AlertStaffPort thật | Còn chờ D/007 US13 T063/T099 và kiểm nguồn ca thật. |
| T050 | [ ] | R-15 gộp bảng 008 | Đã có V008_94/V008_95, phần Store/JSON/job/cấu trúc trong 008-q262. Còn test đỏ lưu giữ tỷ lệ mồ côi; chờ A bổ sung RecordEventApi.scanBefore, review và tích hợp. |
| T051 | [x] | Sửa hồi quy FR-057: thông báo đính chính lấy người xác nhận qua AccountApi.byId, báo Người phụ trách ca hiện tại bằng nhóm ShiftLead; kiểm trường hợp M09 không có liên kết tài khoản cục bộ, … | Đã ghi hoàn thành trên origin/develop; xem giới hạn nghiệm thu bên dưới. |

Backend/web các Phase 1–9 đã có mã và các checkpoint kiểm trước đây. Điều đó không đóng các task mobile/tích hợp/nghiệm thu/R-15 đã tách riêng. Ngoài task còn mở, các dòng review/sửa hồi quy ở team-plan vẫn phải xử lý khi converge; ví dụ Q-262 và adapter ShiftStaffPort thật cho 005.

## 2. Những phần có mã nhưng chưa vào develop

| Worktree | Nhánh | Trạng thái Git khi kiểm | Kết luận |
| --- | --- | --- | --- |
| .worktrees/008-q262 | impl/008-q262 | 241 dòng status, không staged, không conflict | Có Q-262, T050a cấu trúc và phần lớn R-15; chưa commit, một test lưu giữ còn đỏ. Nhiều D là file đã chuyển chỗ, phải đối chiếu cả file mới. |
| .worktrees/008-my-shifts-mobile | impl/008-my-shifts-mobile | 11 dòng status, không staged | Ca của tôi đã cài, nhật ký ghi 77/77 Flutter xanh; chưa vào develop. |
| .worktrees/008-mobile-assignments | impl/008-mobile-assignments | 33 dòng status, không staged | Phân công mobile và năm thao tác bổ sung đã cài; task T022b còn mở, chưa vào develop. |
| .worktrees/008-api-review | docs/008-api-review | 5 dòng status, không staged | Biên bản review riêng, chưa tích hợp. |
| .worktrees/008-governance-fixture | impl/008-governance-fixture | 5 dòng status, không staged | Bản sửa fixture riêng, cần so với develop mới trước khi lấy. |
| .worktrees/008-shift-query-fixture | impl/008-shift-query-fixture | 6 dòng status, không staged | Bản sửa fixture riêng; develop đã có bản sửa ShiftQueriesIT của #105, tránh ghép trùng. |
| .worktrees/010-incurred-costs | impl/008-staff-shift-handover | Sạch, HEAD b2a9466d | Tên thư mục là 010 nhưng nhánh hiện tại là 008 cũ; không đại diện cho tiến độ 010 mới nhất. |

Các nhánh worktree riêng hầu hết còn nền 685dea63 hoặc d2bd7de0; phần mã mới nằm ở working tree. Kiểm `origin/develop..impl/008-q262` không có commit riêng: chưa có commit chứa phần đang sửa đó. Chỉ push tên nhánh hiện tại sẽ không gửi phần mã chưa commit.

Số task trùng giữa nhánh: T051 trên develop là FR-057; T051 ở q262 là sửa Q-262; một worktree fixture cũng từng dùng T051. Khi tích hợp phải so nội dung, không ghép checkbox theo số một cách máy móc.

## 3. T050 vẫn chờ một API cụ thể của A

**Đính chính kết luận trước:** nền 000 T108, migration, API bảng chung và fake đã có; nhưng điều đó chưa đủ đóng T050 của E.

Đã đọc RecordEventApi trên chính origin/develop 62fdb4f2. Interface hiện có add/updateDraft/confirm/require/findByUniqueKey/list(kind,parent), **chưa có**:

```java
<T> List<RecordEvent<T>> scanBefore(
    SharedKind<T> kind, Instant cutoff, long afterId, int limit);
```

Nguồn đề xuất: [shared-event-scan-proposal.md của worktree Q-262](../../.worktrees/008-q262/specs/008-staff-shift-handover/shared-event-scan-proposal.md). Người dùng trước đó đã chọn chờ A cung cấp API, E không tự đổi phần A.

Lý do: ca Nháp có lịch sử tính tỷ lệ rồi bị người dùng xóa. Lịch sử vẫn phải giữ đến hạn; duyệt ca đang còn và gọi list(kind,parent) sẽ bỏ sót sự kiện không còn cha. SharedRowRetention không thay thế bước quét ứng viên này.

Báo cáo XML còn lưu tại `.worktrees/008-q262/backend/target/failsafe-reports/TEST-com.eldercare.backend.staffing.StaffingRatioSchemaIT.xml`, ghi ngày 31/10/2026 11:34:53, có một failure:
`FR_037_retentionFindsExpiredRatioEvenWhenDraftShiftHasBeenDeleted`, không tìm thấy ứng viên hết hạn. Đây là bằng chứng lịch sử; không chạy lại test trong phiên này. Các XML target ở gốc kho còn từ tháng 10 và namespace cũ, không dùng chúng làm kết quả kiểm HEAD mới.

Dòng yêu cầu A có ở team-plan trong worktree Q-262, **chưa có trong team-plan trên origin/develop**. Phiên này bổ sung dòng chờ vào docs/team-plan.md cục bộ, liên kết báo cáo này; chưa commit/push hoặc gửi thông báo cho A.

## 4. Bash: đọc được gì và không đọc được gì

Đã kiểm `C:/Users/PHAN THANH LONG/.bash_history`: 500 dòng, lần ghi cuối **20/10/2026 23:18:39 +07:00**; lệnh cuối là clone Elder_Care. Phần lớn lịch sử trước đó thuộc dự án khác, không dùng làm bằng chứng cho Elder_Care. Không có timestamp riêng của các lệnh commit/push/merge 02–03/11 trong file này. Không thấy cấu hình HISTFILE khác trong các file cấu hình shell đã kiểm.

Có các tiến trình bash đang chạy từ 04/11, nhưng lịch sử chưa ghi xuống của phiên terminal đang mở không có trong file trên. Vì vậy không khẳng định đã đọc được mọi lệnh Bash hôm qua; kết luận về thao tác Git dựa vào reflog, commit graph và trạng thái worktree/remote.

Theo ngày hiện tại 04/11, “hôm qua” là 03/11. Những thao tác commit/reset/stash/checkout cục bộ khớp mô tả tìm được trong reflog máy này mang ngày **02/11**. Các cập nhật origin ngày 03/11 là fetch; không thấy commit cục bộ mới ngày 03/11 trong phần reflog đã kiểm.

## 5. Các thao tác đã xác minh và tác động

| Thời điểm +07:00 | Dấu vết | Tác động được xác minh |
| --- | --- | --- |
| 02/11 14:54:17 | 34de8e39, feat(handover): complete FR-057 and unit test | Commit phần thông báo đính chính bàn giao, code/test/tài liệu, 12 file. |
| 02/11 15:13:06 | bf6e571d, feat(costs): update | Commit nền 010, 64 file; không phải các worktree mobile/Q-262. |
| 02/11 15:13:18 | origin/impl/008-fr057-handover-notifications, update by push | Xác nhận push tới bf6e571d. |
| 02/11 22:36:50 | develop cũ d2bd7de0 → c8a8addd, pull origin develop | Cập nhật develop cục bộ bằng fast-forward. |
| 02/11 23:22:56 | impl/010-incurred-costs reset về HEAD^: 73cc9d61 → e77df60d | Lùi con trỏ nhánh một commit tài liệu. Commit 73cc9d61 vẫn nằm trong origin/develop. Reflog không cho biết đầy đủ cờ của lệnh reset. |
| 02/11 23:33:58 | stash b66a592a, WIP on impl/010-incurred-costs | Stash hiện còn; có ba parent, gồm bản lưu file untracked. Kiểm cả tracked/untracked thấy 67 đường dẫn. |
| 02/11 23:34:06 | worktree 010 checkout impl/008-staff-shift-handover | Giải thích vì sao mở thư mục 010 lại thấy nhánh 008 và task cũ. |
| 02/11 23:39:17 | deb5f5a8, Merge develop vào impl/008-fr057-handover-notifications | Gộp develop vào nhánh FR-057, chuyển gói và xử lý xung đột; commit này có trên remote, không đồng nghĩa mọi worktree riêng được gộp. |
| 02/11 23:42:30 | 87d4266a, sửa câu chữ ngày Giải phóng giường | Cập nhật tài liệu 010 theo Q-395 trên nhánh sau merge. |
| 02/11 23:47:31 | e8891c60, PR #93 | Đưa FR-057 và nền 010 vào develop. Kiểm ancestry xác nhận 34de8e39 và bf6e571d đều đã nằm trong origin/develop. |
| 03/11 | PR #105 trên develop | Sửa AssignmentIT/ShiftQueriesIT của 008; không đưa các màn mobile hoặc Q-262 riêng vào. |
| 04/11, phiên chat trước | develop c8a8addd → 55807c24 | Pull của phiên kiểm trước; ba tài liệu đang sửa ở gốc kho là thay đổi báo cáo của phiên chat, không phải mã chức năng người dùng vừa commit. |

Bốn stash còn tồn tại. Đã kiểm danh sách file tracked và untracked của cả bốn; không thấy mã mobile bàn giao/vắng ca hoặc migration V008_94/95 trong stash. Các file đó, nơi có triển khai, vẫn ở worktree riêng nêu trên.

Các nhánh cũ impl/008-fr057-handover-notifications, impl/008-staff-shift-handover và impl/008-q262 không có commit riêng chưa được origin/develop chứa (đã kiểm nhánh liên quan); nhưng working tree của các nhánh phụ vẫn có thay đổi chưa commit. Nhãn ahead/behind so với upstream nhánh cũ không tự cho biết mã nào đã vào develop: phải kiểm ancestry như trên.

## 6. Nên làm tiếp

1. Giữ các worktree/stash hiện có, đối chiếu và tích hợp mã Q-262/cấu trúc/mobile đã viết với develop mới. Không viết lại chỉ vì task trên develop còn trống; không cộng task xong của các nhánh.
2. A cung cấp scanBefore trên API thật, fake và test; E nối lưu giữ tỷ lệ, kiểm ca đã xóa, hoàn tất kiểm migration/trigger/quyền rồi mới đóng T050.
3. Hoàn thiện T016b/T022b trên nền mới; làm T030/T041b còn thiếu. Phần đọc bàn giao ngoại tuyến phải giữ đúng phạm vi ca, lệnh chỉ trực tuyến.
4. Phối hợp C/D cho T043b/T049b, nguồn thật và adapter ShiftStaffPort; sau đó T044b với dữ liệu/thiết bị/người dùng thật.

Lượt này chỉ kiểm tra và cập nhật tài liệu. Không chạy lại Maven/npm/Flutter, không thay đổi trạng thái task, không sửa worktree chức năng hoặc thực hiện Git tích hợp. Kiểm tài liệu bằng git diff --check.


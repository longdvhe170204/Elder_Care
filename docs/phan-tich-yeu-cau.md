# Phân tích yêu cầu

Tab này chứa các sản phẩm phân tích được rút ra từ tab nghiệp vụ: Context Diagram, Main Business Flow, Conceptual Data Model và User Requirements. Mọi mục đều ghi nguồn (mục nghiệp vụ, mã BR) để truy vết; khi nghiệp vụ thay đổi, cập nhật tab nghiệp vụ trước rồi mới sửa ở đây.

## 1. Context Diagram

Hệ thống là một khối duy nhất ở trung tâm. Các thực thể bên ngoài là 10 actor theo bảng vai trò 2.3 của tài liệu nghiệp vụ; mỗi actor có luồng dữ liệu đưa vào và nhận ra từ hệ thống. Người cao tuổi không trực tiếp dùng hệ thống nên không xuất hiện.

```mermaid
flowchart LR
    SYS(("Hệ thống quản lý<br/>viện dưỡng lão"))

    QL["Quản lý viện"] -- "Cấu hình, tham số, phê duyệt, chốt kỳ" --> SYS
    SYS -- "Dashboard, báo cáo, yêu cầu chờ duyệt, cảnh báo giấy phép" --> QL

    TT["Trưởng tầng"] -- "Phân công, xử lý việc quá hạn, kiểm tra chất lượng, xác nhận bàn giao" --> SYS
    SYS -- "Tình trạng tầng, việc quá hạn, cảnh báo leo thang" --> TT

    BS["Bác sĩ"] -- "Đánh giá, ngưỡng, đơn thuốc, duyệt kế hoạch chăm sóc" --> SYS
    SYS -- "Hồ sơ sức khỏe, yêu cầu đánh giá lại, cảnh báo, thẻ khẩn cấp" --> BS

    DD["Điều dưỡng"] -- "Xác nhận liều, xử lý cảnh báo, kế hoạch chăm sóc, bàn giao" --> SYS
    SYS -- "Lịch liều, cảnh báo, bản nháp bàn giao, checklist" --> DD

    CS["Nhân viên chăm sóc"] -- "Kết quả chăm sóc, chỉ số đo, sự cố" --> SYS
    SYS -- "Checklist ca, nhắc việc, cờ nguy cơ" --> CS

    SYS -- "Dị ứng, hạn chế ăn, cảnh báo xung đột" --> DDV["Dinh dưỡng viên"]
    DDV -- "Chế độ ăn, món ăn, thực đơn" --> SYS

    SYS -- "Số suất theo chế độ ăn, yêu cầu đặc biệt" --> BEP["Nhân viên bếp"]
    BEP -- "Xác nhận chuẩn bị, phân phối suất ăn" --> SYS

    SYS -- "Công việc vệ sinh được phân công" --> VS["Nhân viên vệ sinh"]
    VS -- "Kết quả công việc vệ sinh" --> SYS

    SYS -- "Đề xuất hồ sơ chờ, nhắc hợp đồng, bảng chi phí" --> HC["Nhân viên hành chính"]
    HC -- "Hồ sơ, hợp đồng, tạm vắng, đón, đồ gửi, kiểm tra chi phí" --> SYS

    SYS -- "Bản tin, thông báo, chi phí, lịch sinh hoạt" --> NT["Người thân"]
    NT -- "Đăng ký thăm, đồng ý, phản hồi, xác nhận thay đổi" --> SYS
```

| Actor                | Dữ liệu vào hệ thống                                                                     | Dữ liệu ra từ hệ thống                                                      | Nguồn                   |
| -------------------- | ---------------------------------------------------------------------------------------- | --------------------------------------------------------------------------- | ----------------------- |
| Quản lý viện         | Tham số cấu hình, phê duyệt, chốt kỳ chi phí, cấu hình phân quyền                        | Dashboard, báo cáo, yêu cầu chờ duyệt, cảnh báo giấy phép sắp hết hạn       | 2.3, M11, M14, M15      |
| Trưởng tầng          | Phân công, điều phối, xử lý việc quá hạn, kết quả kiểm tra chất lượng, xác nhận bàn giao | Tình trạng người cao tuổi trong tầng, việc quá hạn, cảnh báo leo thang      | 2.3, 13.3               |
| Bác sĩ               | Đánh giá, ngưỡng cảnh báo, đơn thuốc, đối chiếu thuốc, duyệt kế hoạch chăm sóc           | Hồ sơ sức khỏe, yêu cầu đánh giá lại, cảnh báo, thẻ thông tin khẩn cấp      | 2.3, M01, M06, M07      |
| Điều dưỡng           | Xác nhận liều, xử lý cảnh báo, phiên bản kế hoạch chăm sóc, bàn giao ca                  | Lịch liều, cảnh báo, bản nháp bàn giao, checklist ca                        | 2.3, M04, M05, M07, M09 |
| Nhân viên chăm sóc   | Kết quả công việc chăm sóc, chỉ số đo, sự cố                                             | Checklist ca, nhắc việc quá hạn, cờ nguy cơ                                 | 2.3, M04                |
| Dinh dưỡng viên      | Chế độ ăn, món ăn, thực đơn                                                              | Dị ứng và hạn chế ăn, cảnh báo xung đột, yêu cầu xem lại chế độ ăn          | 2.3, M08                |
| Nhân viên bếp        | Xác nhận chuẩn bị và phân phối suất ăn                                                   | Số suất theo chế độ ăn, yêu cầu đặc biệt, thay đổi phát sinh sau chốt       | 2.3, 12.3               |
| Nhân viên vệ sinh    | Kết quả công việc vệ sinh                                                                | Công việc vệ sinh phòng/khu vực được phân công                              | 2.3, 8.3                |
| Nhân viên hành chính | Hồ sơ, hợp đồng, đặt cọc, tạm vắng, đón, đồ gửi, người thân, kiểm tra chi phí            | Đề xuất hồ sơ chờ, nhắc hợp đồng, bảng chi phí kỳ, file chi phí cho kế toán | 2.3, M02, M10–M12       |
| Người thân           | Đăng ký thăm, bản đồng ý, xác nhận thay đổi, phản hồi                                    | Bản tin, thông báo, chi phí, lịch sinh hoạt                                 | 2.3, M10                |

## 2. Main Business Flows

Ba luồng chính, tương ứng mẫu "End-to-End" và "Self-Service": BF-01 là hành trình lưu trú trọn vòng, BF-02 là vòng ca chăm sóc lặp lại mỗi ngày bên trong BF-01, BF-03 là người thân tự phục vụ qua cổng. Làn "Hệ thống" thể hiện các bước tự động theo quy tắc nghiệp vụ.

### BF-01 – Hành trình lưu trú trọn vòng (End-to-End Resident Journey)

```mermaid
flowchart TD
    subgraph NT[Người thân]
        A1["Liên hệ, đăng ký tiếp nhận"]
        A2["Ký hợp đồng, bản đồng ý, đặt cọc"]
    end
    subgraph HC[Hành chính]
        B1["Tạo hồ sơ đăng ký"]
        B2{"Có giường phù hợp?"}
        B3["Lập hợp đồng"]
        B4["Xác nhận đặt cọc, phân bổ giường"]
        B5["Kết thúc lưu trú: chốt chi phí, trả đồ gửi"]
    end
    subgraph BS[Bác sĩ và Điều dưỡng]
        C1["Đánh giá đầu vào"]
        C2["Chấp nhận mức chăm sóc"]
        C3["Đối chiếu thuốc, lập và duyệt kế hoạch chăm sóc"]
    end
    subgraph SYS[Hệ thống]
        D1["Quy đổi điểm, đề xuất mức chăm sóc và cờ nguy cơ"]
        D2["Đưa vào danh sách chờ, tính điểm ưu tiên"]
        D3["Giường trống: đề xuất hồ sơ chờ phù hợp"]
        D4["Kiểm tra điều kiện, chuyển Đang lưu trú"]
        D5["Chăm sóc hằng ngày (BF-02)"]
        D6{"Sự kiện trong lưu trú"}
        D7["Kiểm tra điều kiện kết thúc, giải phóng giường, đóng hồ sơ"]
    end
    subgraph QL[Quản lý viện]
        E1["Duyệt thay đổi lưu trú"]
    end

    A1 --> B1 --> C1 --> D1 --> C2 --> B2
    B2 -- Có --> B3
    B2 -- Không --> D2 --> D3 --> B3
    B3 --> A2 --> B4 --> D4 --> C3 --> D5 --> D6
    D6 -- "Tạm vắng / bệnh viện, trở về" --> D5
    D6 -- "Đánh giá lại đổi mức chăm sóc" --> E1 --> D5
    D6 -- "Kết thúc / qua đời" --> B5 --> D7
```

| Bước | Vai trò                | Hoạt động                                                                                     | Tham chiếu               |
| ---- | ---------------------- | --------------------------------------------------------------------------------------------- | ------------------------ |
| 1    | Người thân, Hành chính | Đăng ký, tạo hồ sơ                                                                            | 6.1                      |
| 2    | Bác sĩ, Hệ thống       | Đánh giá bằng thang điểm, hệ thống đề xuất mức chăm sóc; bác sĩ chấp nhận hoặc điều chỉnh     | 5.3, BR-M01-09           |
| 3    | Hành chính, Hệ thống   | Có giường thì lập hợp đồng; không có thì vào danh sách chờ, hệ thống đề xuất khi giường trống | 6.2, BR-M02-01, 02, 10   |
| 4    | Người thân, Hành chính | Ký hợp đồng, bản đồng ý chia sẻ dữ liệu, xác nhận đặt cọc, phân bổ giường                     | 5.1, 6.3, 6.5, BR-M03-01 |
| 5    | Hệ thống               | Kiểm tra điều kiện rồi chuyển Đang lưu trú                                                    | 5.6                      |
| 6    | Bác sĩ, Điều dưỡng     | Đối chiếu thuốc, lập và duyệt kế hoạch chăm sóc                                               | 11.5, BR-M04-19          |
| 7    | Hệ thống, Nhân viên    | Vòng ca hằng ngày (BF-02)                                                                     | M04–M09                  |
| 8    | Hệ thống, Quản lý      | Tạm vắng, nằm viện, đánh giá lại, thay đổi lưu trú có duyệt                                   | 6.6, 6.7, BR-M01-02, 03  |
| 9    | Hành chính, Hệ thống   | Kết thúc lưu trú hoặc qua đời: kiểm tra điều kiện, chốt chi phí, trả đồ, giải phóng giường    | 6.8, 5.6, BR-M11-08      |

### BF-02 – Vòng ca chăm sóc hằng ngày

```mermaid
flowchart TD
    subgraph SYS[Hệ thống]
        S1["Sinh công việc, liều thuốc, chốt suất ăn"]
        S2["Đánh giá kết quả theo quy tắc"]
        S3{"Bất thường?"}
        S4["Tạo cảnh báo, sinh chi phí nháp"]
        S5["Liều/việc quá hạn: nhắc, leo thang"]
        S6["Lập bản nháp bàn giao"]
    end
    subgraph DD[Điều dưỡng]
        N1["Nhận và xác nhận bàn giao đầu ca"]
        N2["Phát thuốc, xác nhận từng liều"]
        N3["Tiếp nhận, xử lý cảnh báo"]
        N4["Bổ sung nhận định, lập bàn giao"]
    end
    subgraph CS[Nhân viên chăm sóc]
        C1["Mở checklist ca"]
        C2["Thực hiện, ghi nhận kết quả"]
    end
    subgraph PT[Người phụ trách ca]
        P1["Xử lý việc quá hạn, cảnh báo leo thang"]
        P2["Kích hoạt khẩn cấp khi cần"]
    end

    S1 --> N1 --> C1 --> C2 --> S2
    N1 --> N2 --> S2
    S2 --> S3
    S3 -- Có --> S4 --> N3
    N3 -- "Khẩn cấp" --> P2
    S3 -- Không --> S5
    S5 --> P1
    N3 --> S6
    P1 --> S6
    S5 --> S6 --> N4 --> N1
```

| Bước | Vai trò                        | Hoạt động                                                                 | Tham chiếu                           |
| ---- | ------------------------------ | ------------------------------------------------------------------------- | ------------------------------------ |
| 1    | Hệ thống                       | Sinh công việc, liều thuốc theo ca; chốt suất ăn trước bữa                | BR-M04-01, BR-M07-01, BR-M08-01      |
| 2    | Điều dưỡng                     | Nhận bàn giao đã tổng hợp sẵn, xác nhận; việc tồn vào checklist           | BR-M09-06 → 08                       |
| 3    | Nhân viên chăm sóc             | Mở checklist, thực hiện, ghi nhận kết quả                                 | 8.5, 8.6                             |
| 4    | Điều dưỡng                     | Phát thuốc, xác nhận từng liều trong cửa sổ thời gian                     | BR-M07-02                            |
| 5    | Hệ thống                       | Đánh giá kết quả theo ngưỡng và xu hướng; tạo cảnh báo; sinh chi phí nháp | BR-M04-08 → 10, BR-M05-04, BR-M11-01 |
| 6    | Điều dưỡng, Người phụ trách ca | Xử lý cảnh báo; quá hạn thì leo thang; khẩn cấp thì kích hoạt quy trình   | BR-M05-01, 9.5                       |
| 7    | Hệ thống, Điều dưỡng           | Lập bản nháp bàn giao, bổ sung nhận định, chuyển sang ca sau              | BR-M09-06                            |

### BF-03 – Người thân tự phục vụ (Family Self-Service)

```mermaid
flowchart TD
    subgraph NT[Người thân]
        F1["Đăng nhập cổng người thân"]
        F2["Xem bản tin, lịch sinh hoạt, chi phí"]
        F3["Đăng ký thăm"]
        F4["Đến đón người cao tuổi"]
        F5["Gửi phản hồi / khiếu nại"]
        F6["Xác nhận kết quả xử lý"]
    end
    subgraph SYS[Hệ thống]
        G1["Lọc dữ liệu theo quyền và bản đồng ý"]
        G2{"Khung giờ, sức chứa, khoanh vùng, trạng thái hợp lệ?"}
        G3{"Người đón có trong danh sách?"}
        G4["Gán người phụ trách, đặt hạn xử lý, leo thang khi quá hạn"]
    end
    subgraph HC[Hành chính / Nhân viên]
        H1["Ghi nhận giờ vào, giờ ra"]
        H2["Bàn giao người cao tuổi, ghi nhận tạm vắng"]
        H3["Xử lý phản hồi"]
    end

    F1 --> G1 --> F2
    F1 --> F3 --> G2
    G2 -- "Hợp lệ" --> H1
    G2 -- "Không" --> F3
    F4 --> G3
    G3 -- Có --> H2
    G3 -- "Không: chặn, cần người đại diện hoặc quản lý duyệt" --> F4
    F5 --> G4 --> H3 --> F6
```

| Bước | Vai trò                         | Hoạt động                                                                                                | Tham chiếu           |
| ---- | ------------------------------- | -------------------------------------------------------------------------------------------------------- | -------------------- |
| 1    | Người thân, Hệ thống            | Xem bản tin, lịch, chi phí; dữ liệu sức khỏe chỉ hiện khi có đồng ý                                      | BR-M10-01, BR-M01-08 |
| 2    | Người thân, Hệ thống            | Đăng ký thăm; tự từ chối khi ngoài khung giờ, hết chỗ, khu khoanh vùng hoặc người cao tuổi đang nằm viện | BR-M10-02            |
| 3    | Người thân, Hành chính          | Đón người cao tuổi; chặn người không có trong danh sách                                                  | 14.3, BR-M10-03      |
| 4    | Người thân, Hệ thống, Nhân viên | Phản hồi có hạn xử lý, leo thang khi quá hạn, người thân xác nhận đóng                                   | 14.7, BR-M10-05      |

## 3. Conceptual Data Model

### 3.1. Entity Relationship Diagram

Đây là mô hình khái niệm: thể hiện thực thể và quan hệ nghiệp vụ, chưa có kiểu dữ liệu, khóa hay bảng kỹ thuật. Mô hình dữ liệu vật lý thuộc giai đoạn thiết kế sau này và phải khớp với mô hình này.

**ERD tổng quan (thực thể cốt lõi)**

```mermaid
erDiagram
    NGUOI_CAO_TUOI ||--o{ QUAN_HE_NGUOI_THAN : "có"
    NGUOI_THAN ||--o{ QUAN_HE_NGUOI_THAN : "tham gia"
    NGUOI_CAO_TUOI ||--o{ DANH_GIA : "được đánh giá"
    NGUOI_CAO_TUOI ||--o{ HOP_DONG : "là bên hưởng"
    NGUOI_CAO_TUOI ||--o{ PHAN_BO_GIUONG : "được phân bổ"
    GIUONG ||--o{ PHAN_BO_GIUONG : "được dùng trong"
    NGUOI_CAO_TUOI ||--o{ KE_HOACH_CHAM_SOC : "có"
    KE_HOACH_CHAM_SOC ||--o{ CONG_VIEC : "sinh ra"
    NGUOI_CAO_TUOI ||--o{ DON_THUOC : "được kê"
    DON_THUOC ||--o{ LIEU_THUOC : "sinh ra"
    NGUOI_CAO_TUOI ||--o{ CANH_BAO : "phát sinh"
    NGUOI_CAO_TUOI ||--o{ SU_CO : "liên quan"
    NHAN_VIEN ||--o{ PHAN_CONG : "nhận"
    CA_TRUC ||--o{ PHAN_CONG : "gồm"
    CA_TRUC ||--|| BAN_GIAO : "kết thúc bằng"
    NGUOI_CAO_TUOI ||--o{ CHI_PHI : "phát sinh"
    NGUOI_CAO_TUOI ||--o{ DO_GUI : "gửi"
```

**Miền A – Người cao tuổi, người thân, đánh giá**

```mermaid
erDiagram
    NGUOI_CAO_TUOI ||--o{ QUAN_HE_NGUOI_THAN : "có"
    NGUOI_THAN ||--o{ QUAN_HE_NGUOI_THAN : "tham gia"
    NGUOI_CAO_TUOI ||--o{ BAN_DONG_Y : "có"
    NGUOI_THAN |o--o{ BAN_DONG_Y : "ký thay"
    BAN_DONG_Y }o--o{ NGUOI_THAN : "cho phép xem"
    NGUOI_CAO_TUOI ||--o{ MUC_SUC_KHOE : "có"
    NGUOI_CAO_TUOI ||--o{ DANH_GIA : "được đánh giá"
    DANH_GIA ||--|{ KET_QUA_THANG_DIEM : "gồm"
    DANH_GIA ||--o{ CO_NGUY_CO : "đề xuất"
    NGUOI_CAO_TUOI ||--o{ CO_NGUY_CO : "mang"
    NGUOI_CAO_TUOI ||--o{ LICH_SU_TRANG_THAI : "có"
    NHAN_VIEN ||--o{ DANH_GIA : "thực hiện"
```

**Miền B – Lưu trú, hợp đồng, phòng giường**

```mermaid
erDiagram
    NGUOI_CAO_TUOI ||--o| HO_SO_CHO : "đăng ký chờ"
    NGUOI_CAO_TUOI ||--o{ HOP_DONG : "là bên hưởng"
    NGUOI_THAN ||--o{ HOP_DONG : "ký với tư cách đại diện"
    HOP_DONG ||--o{ PHU_LUC_HOP_DONG : "có"
    YEU_CAU_PHE_DUYET |o--o| PHU_LUC_HOP_DONG : "tạo ra"
    HOP_DONG }o--o{ DICH_VU : "bao gồm"
    DICH_VU ||--|{ PHIEN_BAN_DON_GIA : "có"
    KHU_VUC ||--|{ TANG : "gồm"
    TANG ||--|{ PHONG : "gồm"
    PHONG ||--|{ GIUONG : "gồm"
    GIUONG ||--o{ PHAN_BO_GIUONG : "được dùng trong"
    NGUOI_CAO_TUOI ||--o{ PHAN_BO_GIUONG : "được phân bổ"
    NGUOI_CAO_TUOI ||--o{ LUOT_VANG : "có"
    NGUOI_CAO_TUOI ||--o{ CO_MAT_BAN_TRU : "có"
```

**Miền C – Chăm sóc, thuốc, sức khỏe, sự cố**

```mermaid
erDiagram
    NGUOI_CAO_TUOI ||--o{ KE_HOACH_CHAM_SOC : "có (theo phiên bản)"
    KE_HOACH_CHAM_SOC ||--|{ MUC_KE_HOACH : "gồm"
    MUC_KE_HOACH ||--o{ CONG_VIEC : "sinh ra"
    CONG_VIEC ||--o| KET_QUA_GHI_NHAN : "có"
    NHAN_VIEN ||--o{ KET_QUA_GHI_NHAN : "ghi nhận"
    NGUOI_CAO_TUOI ||--o{ DON_THUOC : "được kê"
    DON_THUOC |o--o| DON_THUOC : "thay thế cho"
    DON_THUOC ||--o{ LIEU_THUOC : "sinh ra"
    THUOC_GIA_DINH_GUI |o--o{ LIEU_THUOC : "là nguồn"
    NGUOI_CAO_TUOI ||--o{ PHIEU_DOI_CHIEU : "có"
    PHIEU_DOI_CHIEU }o--o{ DON_THUOC : "quyết định"
    NGUOI_CAO_TUOI ||--o{ CHI_SO : "được đo"
    NGUOI_CAO_TUOI ||--o{ NGUONG_CANH_BAO : "có"
    CHI_SO |o--o{ CANH_BAO : "kích hoạt"
    CANH_BAO |o--o| SU_CO : "chuyển thành"
    KHU_VUC ||--o{ KHOANH_VUNG : "bị"
    SU_CO |o--o{ KHOANH_VUNG : "dẫn đến"
```

**Miền D – Nhân sự, ca trực, hoạt động**

```mermaid
erDiagram
    NHAN_VIEN ||--o{ CHUNG_CHI : "có"
    NHAN_VIEN ||--o| TAI_KHOAN : "sử dụng"
    NGUOI_THAN ||--o| TAI_KHOAN : "sử dụng"
    TAI_KHOAN }o--|{ VAI_TRO : "được gán"
    CA_TRUC ||--|{ PHAN_CONG : "gồm"
    NHAN_VIEN ||--o{ PHAN_CONG : "nhận"
    PHAN_CONG }o--o{ NGUOI_CAO_TUOI : "phụ trách"
    CA_TRUC ||--|| BAN_GIAO : "kết thúc bằng"
    HOAT_DONG ||--o{ BUOI_HOAT_DONG : "sinh ra"
    BUOI_HOAT_DONG ||--o{ DIEM_DANH : "có"
    NGUOI_CAO_TUOI ||--o{ DIEM_DANH : "được điểm danh"
```

**Miền E – Chi phí, dinh dưỡng, đồ gửi, tương tác người thân**

```mermaid
erDiagram
    NGUOI_CAO_TUOI ||--o{ CHI_PHI : "phát sinh"
    KY_CHI_PHI ||--o{ CHI_PHI : "gồm"
    CHI_PHI |o--o| CHI_PHI : "điều chỉnh cho"
    PHIEN_BAN_DON_GIA ||--o{ CHI_PHI : "định giá"
    NGUOI_CAO_TUOI }o--|| CHE_DO_AN : "áp dụng"
    THUC_DON ||--|{ MON_AN : "gồm"
    THUC_DON ||--o{ SUAT_AN : "chốt thành"
    NGUOI_CAO_TUOI ||--o{ DO_AN_GIA_DINH : "nhận"
    NGUOI_CAO_TUOI ||--o{ DO_GUI : "gửi"
    DO_GUI ||--|{ BAN_GIAO_DO_GUI : "có"
    NGUOI_THAN ||--o{ LUOT_THAM : "đăng ký"
    NGUOI_THAN ||--o{ PHAN_HOI : "gửi"
    NGUOI_CAO_TUOI ||--o{ BAN_TIN : "có"
```

Quan hệ nguồn của CHI_PHI là quan hệ đa hình: mỗi khoản trỏ tới đúng một bản ghi nguồn thuộc CONG_VIEC, LIEU_THUOC, DIEM_DANH, LUOT_VANG, CO_MAT_BAN_TRU hoặc là khoản nhập tay (DBR-15). Các thực thể nền tảng THAM_SO, YEU_CAU_PHE_DUYET, NHAT_KY, DINH_CHINH, THONG_BAO dùng chung cho mọi miền nên không vẽ quan hệ riêng.

### 3.2. Entity Descriptions

Cột Nhóm theo phân loại ở mục 1.5 tab nghiệp vụ: **1** danh mục (được sửa), **2** nghiệp vụ có trạng thái (đổi qua lệnh hoặc phiên bản), **3** ghi nhận (chỉ ghi thêm).

| Thực thể                  | Mô tả                                                     | Thuộc tính chính                                                                                              | Nhóm           | Nguồn           |
| ------------------------- | --------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- | -------------- | --------------- |
| NGUOI_CAO_TUOI            | Người được chăm sóc tại viện                              | Mã hồ sơ, họ tên, ngày sinh, giới tính, CCCD, ảnh, loại lưu trú, mức chăm sóc, trạng thái                     | 2              | 5.1, 5.5        |
| NGUOI_THAN                | Người thân hoặc người đại diện của người cao tuổi         | Họ tên, số điện thoại, địa chỉ                                                                                | 1              | 14.1            |
| QUAN_HE_NGUOI_THAN        | Liên kết người cao tuổi – người thân kèm vai trò và quyền | Quan hệ, là đại diện, là liên hệ chính, được phép đón, các quyền xem                                          | 2              | 14.1, BR-M10-07 |
| BAN_DONG_Y                | Đồng ý xử lý và chia sẻ dữ liệu                           | Người đồng ý, phạm vi, người thân được xem, thời điểm, bằng chứng, trạng thái                                 | 2              | 5.1, BR-M01-08  |
| MUC_SUC_KHOE              | Một mục dị ứng, bệnh nền hoặc tiền sử                     | Loại, nội dung, mức độ, nguồn, người ghi, trạng thái, lý do loại trừ                                          | 2              | 5.2, BR-M01-07  |
| DANH_GIA                  | Một lần đánh giá đầu vào hoặc đánh giá lại                | Loại, lý do, thời điểm, người thực hiện, mức chăm sóc đề xuất, mức được chấp nhận, lý do khác đề xuất         | 3              | 5.3, 5.4        |
| KET_QUA_THANG_DIEM        | Điểm của một thang trong lần đánh giá                     | Thang (Barthel, Braden, Morse, MMSE), điểm, phân loại                                                         | 3              | 5.3             |
| CO_NGUY_CO                | Cờ nguy cơ đang gắn cho người cao tuổi                    | Loại (ngã, loét, đi lạc), đánh giá gắn cờ, đánh giá gỡ cờ                                                     | 2              | BR-M01-10       |
| LICH_SU_TRANG_THAI        | Mỗi lần chuyển trạng thái                                 | Trạng thái từ, đến, lệnh, thời điểm, người thực hiện, lý do                                                   | 3              | BR-M01-04       |
| HO_SO_CHO                 | Hồ sơ trong danh sách chờ                                 | Ngày đăng ký, loại lưu trú, mức chăm sóc, điểm ưu tiên, trạng thái, hạn giữ chỗ                               | 2              | 6.2, BR-M02-10  |
| HOP_DONG                  | Hợp đồng lưu trú gốc                                      | Số hợp đồng, loại lưu trú, thời hạn, mức chăm sóc, chính sách phí và giữ giường, trạng thái                   | 2              | 6.3             |
| PHU_LUC_HOP_DONG          | Thay đổi đã duyệt gắn với hợp đồng                        | Nội dung thay đổi (trước/sau), ngày hiệu lực, trạng thái áp dụng                                              | 2              | 6.6, BR-M02-09  |
| DICH_VU                   | Dịch vụ viện cung cấp                                     | Tên, đơn vị tính, có trong gói, trạng thái hiệu lực                                                           | 1              | 6.4             |
| PHIEN_BAN_DON_GIA         | Đơn giá của dịch vụ/vật phẩm theo thời gian               | Đơn giá, hiệu lực từ, hiệu lực đến                                                                            | 1              | 6.4             |
| KHU_VUC, TANG, PHONG      | Cơ cấu vị trí                                             | Mã, tên; phòng có loại, mức chăm sóc cho phép, chính sách giới tính, trạng thái cách ly                       | 1              | 7.1             |
| GIUONG                    | Giường trong phòng                                        | Mã, trạng thái, lý do giữ chỗ, hạn giữ chỗ                                                                    | 2              | 7.2             |
| PHAN_BO_GIUONG            | Người cao tuổi dùng giường trong một khoảng thời gian     | Bắt đầu, kết thúc, lý do, người thực hiện                                                                     | 2              | 7.3, BR-M03-07  |
| LUOT_VANG                 | Một lần tạm vắng hoặc nằm viện                            | Loại vắng, rời lúc, dự kiến về, về lúc, người đón, người bàn giao, chính sách áp dụng                         | 2              | 6.7             |
| CO_MAT_BAN_TRU            | Trạng thái có mặt của người bán trú theo ngày             | Ngày, trạng thái, giờ đến, giờ về                                                                             | 2              | 3.4             |
| KE_HOACH_CHAM_SOC         | Phiên bản kế hoạch chăm sóc                               | Số phiên bản, hiệu lực từ, trạng thái, người lập, người duyệt                                                 | 2              | 8.1, BR-M04-19  |
| MUC_KE_HOACH              | Một hoạt động chăm sóc trong kế hoạch                     | Loại công việc, tần suất, khung giờ, mức quan trọng, vai trò thực hiện, có tính phí                           | 2              | 8.1             |
| CONG_VIEC                 | Công việc cụ thể trong một ca                             | Thời điểm dự kiến, khung cho phép, người phụ trách, trạng thái, lý do không thực hiện                         | 2              | 8.3, BR-M04-04  |
| KET_QUA_GHI_NHAN          | Kết quả thực hiện công việc                               | Giá trị kết quả, thời điểm thực hiện, thời điểm ghi, ghi nhận muộn                                            | 3              | 8.6             |
| DON_THUOC                 | Đơn thuốc của người cao tuổi                              | Thuốc, hoạt chất, liều, đường dùng, tần suất, loại (định kỳ/PRN), nguồn thuốc, người kê, cơ sở kê, trạng thái | 2              | 11.1            |
| LIEU_THUOC                | Một liều cụ thể                                           | Thời điểm dự kiến, cửa sổ, trạng thái, thời điểm xác nhận, người xác nhận, phản ứng                           | 3              | 11.2, BR-M07-02 |
| PHIEU_DOI_CHIEU           | Đối chiếu thuốc khi tiếp nhận/trở về                      | Lý do, quyết định từng thuốc, người thực hiện, người xác nhận                                                 | 3              | 11.5            |
| THUOC_GIA_DINH_GUI        | Thuốc gia đình gửi                                        | Tên, hàm lượng, số lượng còn, hạn dùng, có toa, trạng thái                                                    | 2              | 11.4            |
| CHI_SO                    | Một lần đo chỉ số                                         | Loại chỉ số, giá trị, thời điểm, người đo                                                                     | 3              | 10.1            |
| NGUONG_CANH_BAO           | Ngưỡng của một chỉ số                                     | Mức cảnh báo, mức nguy hiểm, hiệu lực, người thiết lập                                                        | 2              | 10.3            |
| CANH_BAO                  | Tín hiệu cần xử lý                                        | Loại, mức độ, nguồn, trạng thái, số lần gộp, người phụ trách, hạn tiếp nhận                                   | 2              | 9.4, BR-M05-01  |
| SU_CO                     | Sự việc đã xảy ra                                         | Loại, mức độ, thời gian, địa điểm, mô tả, xử lý, người phát hiện, đã đối chiếu nguyện vọng                    | 3              | 9.2, 9.5        |
| KHOANH_VUNG               | Khu vực bị khoanh vùng lây nhiễm                          | Khu vực, bắt đầu, kết thúc, người khoanh, người gỡ, danh sách tiếp xúc                                        | 2              | 9.6, BR-M05-11  |
| NHAN_VIEN                 | Nhân viên của viện                                        | Họ tên, chức danh, chuyên môn, trạng thái làm việc                                                            | 1              | 13.1            |
| CHUNG_CHI                 | Giấy phép hành nghề hoặc đào tạo                          | Loại, số, phạm vi, ngày cấp, ngày hết hạn                                                                     | 1              | 13.1, BR-M09-01 |
| TAI_KHOAN, VAI_TRO        | Đăng nhập và vai trò hệ thống                             | Tên đăng nhập, trạng thái; vai trò, quyền                                                                     | 1              | 19.1, 19.2      |
| CA_TRUC                   | Một ca cụ thể                                             | Ngày, giờ bắt đầu, giờ kết thúc, khu vực, người phụ trách ca, trạng thái                                      | 2              | 13.2            |
| PHAN_CONG                 | Nhân viên được giao người cao tuổi/khu vực trong ca       | Vai trò (chính/hỗ trợ), phạm vi                                                                               | 2              | 13.4            |
| BAN_GIAO                  | Bàn giao cuối ca                                          | Nội dung tự tổng hợp, nhận định, người lập, người xác nhận, trạng thái                                        | 3              | 13.5, BR-M09-06 |
| HOAT_DONG, BUOI_HOAT_DONG | Hoạt động và từng buổi cụ thể                             | Loại, mẫu lặp, số lượng tối đa, có phí; ngày giờ buổi                                                         | 1 / 2          | 8.8             |
| DIEM_DANH                 | Điểm danh người tham gia buổi hoạt động                   | Có mặt, mức độ tham gia, tình trạng sau                                                                       | 3              | 8.8             |
| CHE_DO_AN, MON_AN         | Chế độ ăn và món ăn                                       | Tên; thành phần gây dị ứng, chế độ phù hợp                                                                    | 1              | 12.1            |
| THUC_DON                  | Thực đơn tuần                                             | Kỳ, trạng thái                                                                                                | 2              | 12.2, BR-M08-06 |
| SUAT_AN                   | Số suất đã chốt cho một bữa                               | Bữa, chế độ ăn, số lượng, phát sinh sau chốt                                                                  | 3              | BR-M08-01       |
| DO_AN_GIA_DINH            | Đồ ăn gia đình mang vào                                   | Loại, số lượng, kết quả đối chiếu, trạng thái                                                                 | 2              | 12.4            |
| CHI_PHI                   | Một khoản chi phí phát sinh                               | Loại, số lượng, đơn giá, số tiền, nguồn, tham chiếu nguồn, trạng thái, khoản điều chỉnh cho                   | 2 → 3 khi chốt | 15.3            |
| KY_CHI_PHI                | Kỳ chi phí                                                | Từ ngày, đến ngày, trạng thái chốt, người chốt                                                                | 2              | 15.6            |
| DO_GUI, BAN_GIAO_DO_GUI   | Đồ gửi và từng lần bàn giao                               | Vật phẩm, số lượng, vị trí, trạng thái; người giao, người nhận, thời điểm, tình trạng                         | 2 / 3          | 16              |
| LUOT_THAM                 | Một lượt thăm                                             | Người thăm, thời gian đăng ký, giờ vào, giờ ra, trạng thái                                                    | 2              | 14.2            |
| PHAN_HOI                  | Phản hồi, khiếu nại                                       | Nội dung, loại, ưu tiên, hạn, người phụ trách, kết quả, trạng thái                                            | 2              | 14.7            |
| BAN_TIN                   | Bản tin định kỳ gửi người thân                            | Kỳ, nội dung tổng hợp, nhận xét, người duyệt, thời điểm gửi                                                   | 3              | BR-M10-08       |
| THONG_BAO                 | Một thông báo đã gửi                                      | Nguồn, người nhận, kênh, thời điểm gửi, thời điểm xem                                                         | 3              | 17              |
| YEU_CAU_PHE_DUYET         | Yêu cầu phê duyệt dùng chung                              | Loại, nội dung, người yêu cầu, người duyệt, trạng thái                                                        | 2              | 1.5             |
| THAM_SO                   | Tham số cấu hình                                          | Mã CFG, giá trị, kiểu, mô tả                                                                                  | 1              | Phụ lục 25      |
| NHAT_KY, DINH_CHINH       | Nhật ký thay đổi và bản ghi đính chính                    | Đối tượng, hành động, trước/sau, lý do; bản ghi gốc, nội dung sửa                                             | 3              | 19.4, 1.5       |

### 3.3. Data Business Rules

Quy tắc dữ liệu là các ràng buộc luôn đúng trên dữ liệu (tính duy nhất, bội số, bất biến), khác với BR là quy tắc hành vi. Mỗi DBR cần xuất hiện trong spec của feature tương ứng dưới dạng yêu cầu có kịch bản chấp nhận.

| Mã     | Quy tắc                                                                                                                                                       | Thực thể                       | Nguồn             |
| ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------ | ----------------- |
| DBR-01 | Mã hồ sơ người cao tuổi là duy nhất; CCCD là duy nhất nếu có. Mỗi người cao tuổi có đúng một trạng thái hiện tại                                              | NGUOI_CAO_TUOI                 | 5.1, 5.5          |
| DBR-02 | Người cao tuổi đang lưu trú có đúng một người liên hệ chính và ít nhất một người đại diện                                                                     | QUAN_HE_NGUOI_THAN             | 14.1              |
| DBR-03 | Người thân chỉ có quyền xem sức khỏe khi nằm trong phạm vi một bản đồng ý đang hiệu lực                                                                       | BAN_DONG_Y, QUAN_HE_NGUOI_THAN | BR-M01-08         |
| DBR-04 | Mục dị ứng, bệnh nền không bao giờ bị xóa; chỉ chuyển Đã loại trừ kèm lý do và người thực hiện                                                                | MUC_SUC_KHOE                   | BR-M01-07         |
| DBR-05 | Lần đánh giá có mức chăm sóc được chấp nhận khác mức đề xuất thì bắt buộc có lý do                                                                            | DANH_GIA                       | BR-M01-09         |
| DBR-06 | Mỗi người cao tuổi có tối đa một hợp đồng hiệu lực tại một thời điểm; hợp đồng hiệu lực không bị sửa                                                          | HOP_DONG                       | BR-M02-08, 09     |
| DBR-07 | Phụ lục hợp đồng phải gắn với một yêu cầu phê duyệt đã duyệt; ngày hiệu lực không sớm hơn ngày bắt đầu hợp đồng                                               | PHU_LUC_HOP_DONG               | 6.6               |
| DBR-08 | Các phiên bản đơn giá của cùng một dịch vụ không chồng khoảng hiệu lực                                                                                        | PHIEN_BAN_DON_GIA              | 6.4               |
| DBR-09 | Tại một thời điểm, mỗi giường có tối đa một phân bổ đang mở; mỗi người cao tuổi nội trú có tối đa một phân bổ đang mở                                         | PHAN_BO_GIUONG                 | 7.3               |
| DBR-10 | Phân bổ chỉ được tạo khi giường Trống (hoặc đang giữ chỗ cho chính người đó), phòng khớp mức chăm sóc, giới tính và không bị cách ly                          | PHAN_BO_GIUONG, GIUONG, PHONG  | BR-M03-01         |
| DBR-11 | Mỗi người cao tuổi có tối đa một phiên bản kế hoạch chăm sóc hiệu lực tại một ngày                                                                            | KE_HOACH_CHAM_SOC              | BR-M04-19         |
| DBR-12 | Công việc là duy nhất theo (mục kế hoạch, thời điểm dự kiến), để sinh lại không tạo trùng                                                                     | CONG_VIEC                      | BR-M04-01, NFR-04 |
| DBR-13 | Liều thuốc là duy nhất theo (đơn thuốc, thời điểm dự kiến) và chỉ có một lần xác nhận                                                                         | LIEU_THUOC                     | BR-M07-12         |
| DBR-14 | Đơn thuốc hiệu lực không đổi thuốc, liều, tần suất. Đơn thay thế trỏ về đơn cũ, và đơn cũ chuyển Đã ngừng trong cùng giao dịch                                | DON_THUOC                      | 11.1              |
| DBR-15 | Mỗi khoản chi phí có đúng một nguồn: một bản ghi nguồn (công việc, liều thuốc, điểm danh, lượt vắng, có mặt bán trú) hoặc "nhập tay" kèm lý do và người duyệt | CHI_PHI                        | BR-M11-01, 04     |
| DBR-16 | Đơn giá của khoản chi phí là phiên bản hiệu lực tại ngày phát sinh; khoản thuộc gói có số tiền 0                                                              | CHI_PHI                        | BR-M11-02, 03     |
| DBR-17 | Khi kỳ đã chốt, các khoản thuộc kỳ không đổi; sai sót được ghi bằng khoản điều chỉnh mới trỏ về khoản gốc                                                     | KY_CHI_PHI, CHI_PHI            | 15.6              |
| DBR-18 | Cùng một người cao tuổi không có hai cảnh báo cùng loại đang mở; cảnh báo mới được gộp vào cảnh báo cũ                                                        | CANH_BAO                       | BR-M05-02         |
| DBR-19 | Sự cố khẩn cấp không sửa, không xóa; nếu người cao tuổi có nguyện vọng cuối đời thì phải có xác nhận đã đối chiếu                                             | SU_CO                          | BR-M05-08, 13     |
| DBR-20 | Mỗi ca kết thúc có đúng một bàn giao; bàn giao đã xác nhận không sửa được                                                                                     | CA_TRUC, BAN_GIAO              | BR-M09-07, 08     |
| DBR-21 | Phân công không được gán nhân viên có chứng chỉ bắt buộc đã hết hạn; một nhân viên không có hai ca chồng giờ                                                  | PHAN_CONG, CHUNG_CHI           | BR-M09-01, 03     |
| DBR-22 | Mỗi đồ gửi có ít nhất một lần bàn giao (lần tiếp nhận); trạng thái hiện tại xác định theo lần bàn giao gần nhất                                               | DO_GUI, BAN_GIAO_DO_GUI        | 16                |
| DBR-23 | Mọi thay đổi dữ liệu nhóm 2 và 3 có bản ghi nhật ký; mỗi bản đính chính trỏ đúng một bản ghi gốc                                                              | NHAT_KY, DINH_CHINH            | 19.4, 1.5         |
| DBR-24 | Mỗi mã tham số có đúng một giá trị hiện hành; các giá trị cũ được lưu lịch sử                                                                                 | THAM_SO                        | BR-M15-04         |
| DBR-25 | Mọi mốc thời gian theo múi giờ Asia/Ho_Chi_Minh; bản ghi ngoại tuyến lưu cả thời điểm trên thiết bị và thời điểm đồng bộ                                      | Tất cả                         | NFR-09, 8.6       |

## 4. User Requirements

### 4.1. Actors

| Mã    | Actor                | Loại               | Mô tả                                                                                                                | Kênh sử dụng                             |
| ----- | -------------------- | ------------------ | -------------------------------------------------------------------------------------------------------------------- | ---------------------------------------- |
| AC-00 | Nhân viên            | Chính (trừu tượng) | Actor cha của AC-01 → AC-09: đăng nhập, tra cứu hồ sơ trong phạm vi, nhận thông báo, ghi nhận sự cố                  | —                                        |
| AC-01 | Quản lý viện         | Chính              | Cấu hình, phê duyệt, chốt kỳ chi phí, xem báo cáo                                                                    | Web quản trị                             |
| AC-02 | Trưởng tầng          | Chính              | Điều phối tầng, phân công, xử lý việc quá hạn, kiểm tra chất lượng; thường là người phụ trách ca                     | App nhân viên, web                       |
| AC-03 | Bác sĩ               | Chính              | Đánh giá, ngưỡng, đơn thuốc (trong phạm vi giấy phép), duyệt kế hoạch chăm sóc                                       | App nhân viên, web                       |
| AC-04 | Điều dưỡng           | Chính              | Thuốc, cảnh báo, kế hoạch chăm sóc, bàn giao ca                                                                      | App nhân viên                            |
| AC-05 | Nhân viên chăm sóc   | Chính              | Thực hiện và ghi nhận công việc chăm sóc, đo chỉ số                                                                  | App nhân viên                            |
| AC-06 | Dinh dưỡng viên      | Chính              | Chế độ ăn, món ăn, thực đơn                                                                                          | Web                                      |
| AC-07 | Nhân viên bếp        | Chính              | Xem số suất đã chốt và yêu cầu đặc biệt                                                                              | App hoặc màn hình bếp                    |
| AC-08 | Nhân viên vệ sinh    | Chính              | Thực hiện công việc vệ sinh được phân công                                                                           | App nhân viên                            |
| AC-09 | Nhân viên hành chính | Chính              | Tiếp nhận, hợp đồng, tạm vắng, đón, đồ gửi, kiểm tra chi phí                                                         | Web                                      |
| AC-10 | Người thân           | Chính              | Xem thông tin được phép, đăng ký thăm, phản hồi, xác nhận thay đổi                                                   | Cổng người thân (trình duyệt điện thoại) |
| AC-11 | Bộ lập lịch hệ thống | Hệ thống           | Khởi phát các use case theo thời gian: sinh công việc/liều/buổi, kiểm tra quá hạn, leo thang, nhắc hạn, sinh bản tin | —                                        |
| AC-12 | Hệ thống kế toán     | Phụ (bên ngoài)    | Nhận file chi phí đã chốt                                                                                            | File Excel/CSV                           |
| AC-13 | Nhà cung cấp SMS     | Phụ (bên ngoài)    | Gửi tin nhắn thông báo                                                                                               | Cổng SMS                                 |

Người cao tuổi không phải actor vì không trực tiếp dùng hệ thống. Bộ lập lịch (AC-11) được mô hình hóa thành actor để các use case tự động (sinh công việc, leo thang…) xuất hiện rõ trên diagram; đây là phần làm hệ thống khác với CRUD.

### 4.2. Danh sách Use Case

Tên use case là **lệnh nghiệp vụ** (động từ + đối tượng), không gộp thành "Quản lý X" theo kiểu Thêm/Sửa/Xóa. Cột Feature là nhánh Spec Kit tương ứng.

| Mã    | Use case                              | Actor chính                             | Feature  | Quy tắc chính       |
| ----- | ------------------------------------- | --------------------------------------- | -------- | ------------------- |
| UC-01 | Tạo hồ sơ người cao tuổi              | Hành chính                              | 001      | 5.1, DBR-01         |
| UC-02 | Tra cứu hồ sơ                         | Nhân viên                               | 001      | 5.1, BR-M15-01      |
| UC-03 | Ghi nhận bản đồng ý chia sẻ dữ liệu   | Hành chính                              | 001      | 5.1, BR-M01-08      |
| UC-04 | Ghi nhận / loại trừ dị ứng, bệnh nền  | Bác sĩ, Điều dưỡng                      | 001      | BR-M01-07           |
| UC-05 | Đánh giá đầu vào                      | Bác sĩ                                  | 001      | 5.3, BR-M01-09, 10  |
| UC-06 | Đánh giá lại                          | Bác sĩ                                  | 001      | 5.4, BR-M01-03      |
| UC-07 | Tạo yêu cầu đánh giá lại              | Bộ lập lịch                             | 001      | BR-M01-02           |
| UC-08 | Hoàn tất / hủy tiếp nhận              | Hành chính                              | 001, 004 | 5.6, BR-M01-06      |
| UC-09 | Đăng ký tiếp nhận                     | Hành chính                              | 004      | 6.1                 |
| UC-10 | Đề xuất hồ sơ chờ khi có giường trống | Bộ lập lịch, Hành chính                 | 004      | BR-M02-01, 02, 10   |
| UC-11 | Lập hợp đồng lưu trú                  | Hành chính                              | 004      | 6.3, BR-M02-08      |
| UC-12 | Xác nhận đặt cọc                      | Hành chính                              | 004      | 6.5                 |
| UC-13 | Yêu cầu thay đổi lưu trú              | Hành chính, Bác sĩ                      | 004      | 6.6                 |
| UC-14 | Duyệt yêu cầu                         | Quản lý viện                            | 000, 004 | 1.5, BR-M02-04      |
| UC-15 | Cho tạm vắng / ghi nhận trở về        | Hành chính, Trưởng tầng                 | 004      | 6.7, 5.6, BR-M02-06 |
| UC-16 | Chuyển viện                           | Điều dưỡng, Bác sĩ                      | 004, 007 | 5.6, BR-M05-14      |
| UC-17 | Kết thúc lưu trú                      | Hành chính                              | 004      | 6.8, 5.6            |
| UC-18 | Ghi nhận qua đời                      | Bác sĩ                                  | 004      | 6.8                 |
| UC-19 | Cấu hình khu, tầng, phòng, giường     | Quản lý viện                            | 003      | 7.1                 |
| UC-20 | Phân bổ giường                        | Hành chính                              | 003      | BR-M03-01, 02       |
| UC-21 | Chuyển giường                         | Trưởng tầng, Hành chính                 | 003      | BR-M03-04, 07       |
| UC-22 | Lập phiên bản kế hoạch chăm sóc       | Điều dưỡng                              | 005      | 8.1                 |
| UC-23 | Duyệt kế hoạch chăm sóc               | Bác sĩ                                  | 005      | BR-M04-19           |
| UC-24 | Sinh công việc theo ca                | Bộ lập lịch                             | 005      | BR-M04-01 → 04      |
| UC-25 | Xem checklist ca                      | Nhân viên chăm sóc, Điều dưỡng, Vệ sinh | 005      | 8.5                 |
| UC-26 | Ghi nhận thực hiện công việc          | Nhân viên chăm sóc, Điều dưỡng, Vệ sinh | 005      | 8.6, BR-M04-12, 14  |
| UC-27 | Xử lý công việc quá hạn               | Trưởng tầng                             | 005      | BR-M04-05, 06       |
| UC-28 | Điểm danh bán trú đến/về              | Hành chính, Nhân viên chăm sóc          | 005      | 3.4, BR-M04-02      |
| UC-29 | Tổ chức hoạt động và điểm danh        | Trưởng tầng, Nhân viên chăm sóc         | 014      | 8.8, BR-M04-15, 21  |
| UC-30 | Tổ chức hoạt động ngoài viện          | Trưởng tầng                             | 014      | 8.9, BR-M04-16, 17  |
| UC-31 | Kiểm tra chất lượng ngẫu nhiên        | Trưởng tầng                             | 014      | BR-M04-23           |
| UC-32 | Ghi nhận chỉ số sức khỏe              | Nhân viên chăm sóc, Điều dưỡng          | 007      | 10.1, BR-M06-03, 04 |
| UC-33 | Thiết lập ngưỡng cảnh báo             | Bác sĩ                                  | 007      | 10.3, BR-M06-01     |
| UC-34 | Tiếp nhận và xử lý cảnh báo           | Điều dưỡng                              | 007      | 9.4, BR-M05-01, 02  |
| UC-35 | Ghi nhận sự cố                        | Nhân viên                               | 007      | 9.2                 |
| UC-36 | Kích hoạt quy trình khẩn cấp          | Nhân viên                               | 007      | 9.5, BR-M05-06, 13  |
| UC-37 | Khoanh vùng lây nhiễm                 | Bác sĩ                                  | 007      | 9.6, BR-M05-10 → 12 |
| UC-38 | Leo thang cảnh báo quá hạn            | Bộ lập lịch                             | 007      | BR-M05-01           |
| UC-39 | Nhập đơn thuốc / đơn thay thế         | Bác sĩ                                  | 006      | 11.1, BR-M07-05, 06 |
| UC-40 | Ngừng đơn thuốc                       | Bác sĩ                                  | 006      | BR-M07-07           |
| UC-41 | Phát thuốc và xác nhận liều           | Điều dưỡng                              | 006      | BR-M07-02, 12       |
| UC-42 | Dùng thuốc khi cần (PRN)              | Điều dưỡng                              | 006      | BR-M07-04           |
| UC-43 | Đối chiếu thuốc                       | Bác sĩ, Điều dưỡng                      | 006      | 11.5, BR-M07-09     |
| UC-44 | Tiếp nhận thuốc gia đình gửi          | Điều dưỡng                              | 006      | 11.4, BR-M07-10, 11 |
| UC-45 | Gán chế độ ăn / duyệt chế độ bệnh lý  | Dinh dưỡng viên, Bác sĩ                 | 011      | BR-M08-03           |
| UC-46 | Lập và công bố thực đơn               | Dinh dưỡng viên                         | 011      | BR-M08-06 → 08      |
| UC-47 | Xem số suất đã chốt                   | Nhân viên bếp                           | 011      | BR-M08-01           |
| UC-48 | Đối chiếu đồ ăn gia đình              | Điều dưỡng, Dinh dưỡng viên             | 011      | BR-M08-04           |
| UC-49 | Quản lý hồ sơ nhân viên và chứng chỉ  | Quản lý viện                            | 008      | 13.1, BR-M09-05     |
| UC-50 | Lập và công bố lịch ca                | Quản lý viện, Trưởng tầng               | 008, 015 | 13.2, BR-M09-09     |
| UC-51 | Phân công chăm sóc                    | Trưởng tầng                             | 008      | 13.4, BR-M09-01, 02 |
| UC-52 | Lập bàn giao cuối ca                  | Điều dưỡng                              | 008      | BR-M09-06, 07       |
| UC-53 | Xác nhận bàn giao                     | Điều dưỡng, Trưởng tầng                 | 008      | BR-M09-08           |
| UC-54 | Yêu cầu đổi ca                        | Nhân viên                               | 015      | BR-M09-10           |
| UC-55 | Quản lý người thân và quyền           | Hành chính                              | 012      | 14.1, BR-M10-07     |
| UC-56 | Xem thông tin trên cổng               | Người thân                              | 012      | BR-M10-01           |
| UC-57 | Đăng ký thăm                          | Người thân                              | 012      | BR-M10-02           |
| UC-58 | Đón người cao tuổi                    | Hành chính                              | 012      | 14.3, BR-M10-03     |
| UC-59 | Gửi phản hồi / khiếu nại              | Người thân                              | 012      | 14.7, BR-M10-05     |
| UC-60 | Duyệt và gửi bản tin                  | Điều dưỡng                              | 012      | BR-M10-08, 09       |
| UC-61 | Kiểm tra chi phí phát sinh            | Hành chính                              | 010      | 15.6                |
| UC-62 | Duyệt và chốt kỳ chi phí              | Quản lý viện                            | 010      | BR-M11-06           |
| UC-63 | Tạo khoản điều chỉnh                  | Hành chính                              | 010      | BR-M11-05           |
| UC-64 | Xuất dữ liệu cho kế toán              | Hành chính                              | 010      | 23                  |
| UC-65 | Tiếp nhận, bàn giao, trả đồ gửi       | Hành chính                              | 013      | 16, BR-M12-01 → 05  |
| UC-66 | Nhận và xác nhận thông báo            | Nhân viên, Người thân                   | 009      | BR-M13-01, 02       |
| UC-67 | Xem dashboard và báo cáo              | Quản lý viện, Trưởng tầng               | 016      | 18                  |
| UC-68 | Quản lý tài khoản và phân quyền       | Quản lý viện                            | 002      | 19.1, 19.2          |
| UC-69 | Cấu hình tham số                      | Quản lý viện                            | 000      | BR-M15-04           |
| UC-70 | Đăng nhập                             | Nhân viên, Người thân                   | 002      | BR-M15-05           |

### 4.3. Use Case Diagrams

Mermaid không có ký hiệu use case chuẩn UML, nên các diagram dưới đây dùng quy ước: hình chữ nhật là actor, hình viên thuốc là use case, khung ngoài là ranh giới hệ thống. Mũi tên nét đứt `«include»` đi từ use case gốc sang use case được bao gồm; `«extend»` đi từ use case mở rộng về use case gốc. Khi đưa vào báo cáo, có thể vẽ lại bằng PlantUML theo đúng các quan hệ này.

**Tổng quan theo gói chức năng**

```mermaid
flowchart LR
    QL["Quản lý viện"]
    TT["Trưởng tầng"]
    BS["Bác sĩ"]
    DD["Điều dưỡng"]
    CS["Nhân viên chăm sóc"]
    DDV["Dinh dưỡng viên / Bếp"]
    HC["Hành chính"]
    NT["Người thân"]
    SCH["Bộ lập lịch hệ thống"]
    KT["Hệ thống kế toán"]
    subgraph SYS[Hệ thống quản lý viện dưỡng lão]
        P1(["Hồ sơ, tiếp nhận, lưu trú, giường<br/>UC-01 → 21"])
        P2(["Chăm sóc và hoạt động<br/>UC-22 → 31"])
        P3(["Sức khỏe, sự cố, thuốc<br/>UC-32 → 44"])
        P4(["Dinh dưỡng<br/>UC-45 → 48"])
        P5(["Nhân sự, ca, bàn giao<br/>UC-49 → 54"])
        P6(["Người thân<br/>UC-55 → 60"])
        P7(["Chi phí, đồ gửi<br/>UC-61 → 65"])
        P8(["Thông báo, báo cáo, quản trị<br/>UC-66 → 70"])
    end
    HC --- P1
    BS --- P1
    QL --- P1
    DD --- P2
    CS --- P2
    TT --- P2
    BS --- P3
    DD --- P3
    CS --- P3
    DDV --- P4
    QL --- P5
    TT --- P5
    DD --- P5
    NT --- P6
    HC --- P6
    HC --- P7
    QL --- P7
    P7 --- KT
    QL --- P8
    SCH --- P1
    SCH --- P2
    SCH --- P3
```

**D1 – Hồ sơ, tiếp nhận và lưu trú**

```mermaid
flowchart LR
    HC["Hành chính"]
    BS["Bác sĩ"]
    TT["Trưởng tầng"]
    QL["Quản lý viện"]
    SCH["Bộ lập lịch"]
    subgraph SYS[Hồ sơ, tiếp nhận, lưu trú]
        U01(["UC-01 Tạo hồ sơ"])
        U03(["UC-03 Ghi nhận bản đồng ý"])
        U05(["UC-05 Đánh giá đầu vào"])
        U07(["UC-07 Tạo yêu cầu đánh giá lại"])
        U08(["UC-08 Hoàn tất tiếp nhận"])
        U09(["UC-09 Đăng ký tiếp nhận"])
        U10(["UC-10 Đề xuất hồ sơ chờ"])
        U11(["UC-11 Lập hợp đồng"])
        U12(["UC-12 Xác nhận đặt cọc"])
        U13(["UC-13 Yêu cầu thay đổi lưu trú"])
        U14(["UC-14 Duyệt yêu cầu"])
        U15(["UC-15 Cho tạm vắng / trở về"])
        U17(["UC-17 Kết thúc lưu trú"])
        U18(["UC-18 Ghi nhận qua đời"])
        U20(["UC-20 Phân bổ giường"])
        U21(["UC-21 Chuyển giường"])
        U43(["UC-43 Đối chiếu thuốc"])
        U65(["UC-65 Trả đồ gửi"])
    end
    HC --- U01
    HC --- U03
    HC --- U08
    HC --- U09
    HC --- U11
    HC --- U12
    HC --- U13
    HC --- U15
    HC --- U17
    HC --- U20
    BS --- U05
    BS --- U13
    BS --- U18
    TT --- U15
    TT --- U21
    QL --- U14
    SCH --- U07
    SCH --- U10
    U08 -. "«include»" .-> U20
    U08 -. "«include»" .-> U43
    U13 -. "«include»" .-> U14
    U17 -. "«include»" .-> U65
```

**D2 – Chăm sóc hằng ngày và hoạt động**

```mermaid
flowchart LR
    DD["Điều dưỡng"]
    BS["Bác sĩ"]
    CS["Nhân viên chăm sóc"]
    VS["Nhân viên vệ sinh"]
    TT["Trưởng tầng"]
    HC["Hành chính"]
    SCH["Bộ lập lịch"]
    subgraph SYS[Chăm sóc và hoạt động]
        U22(["UC-22 Lập phiên bản kế hoạch"])
        U23(["UC-23 Duyệt kế hoạch"])
        U24(["UC-24 Sinh công việc theo ca"])
        U25(["UC-25 Xem checklist ca"])
        U26(["UC-26 Ghi nhận thực hiện"])
        U27(["UC-27 Xử lý việc quá hạn"])
        U28(["UC-28 Điểm danh bán trú"])
        U29(["UC-29 Tổ chức hoạt động"])
        U30(["UC-30 Hoạt động ngoài viện"])
        U31(["UC-31 Kiểm tra chất lượng"])
        U36(["UC-36 Kích hoạt khẩn cấp"])
    end
    DD --- U22
    BS --- U23
    SCH --- U24
    CS --- U25
    DD --- U25
    VS --- U25
    CS --- U26
    DD --- U26
    VS --- U26
    TT --- U27
    HC --- U28
    CS --- U28
    TT --- U29
    CS --- U29
    TT --- U30
    TT --- U31
    U22 -. "«include»" .-> U23
    U26 -. "«include»" .-> U25
    U36 -. "«extend» thiếu người khi về" .-> U30
```

**D3 – Sức khỏe, sự cố và thuốc**

```mermaid
flowchart LR
    NV["Nhân viên"]
    CS["Nhân viên chăm sóc"]
    DD["Điều dưỡng"]
    BS["Bác sĩ"]
    SCH["Bộ lập lịch"]
    subgraph SYS[Sức khỏe, sự cố, thuốc]
        U32(["UC-32 Ghi nhận chỉ số"])
        U33(["UC-33 Thiết lập ngưỡng"])
        U34(["UC-34 Xử lý cảnh báo"])
        U35(["UC-35 Ghi nhận sự cố"])
        U36(["UC-36 Kích hoạt khẩn cấp"])
        U16(["UC-16 Chuyển viện"])
        U37(["UC-37 Khoanh vùng lây nhiễm"])
        U38(["UC-38 Leo thang cảnh báo"])
        U39(["UC-39 Nhập đơn / đơn thay thế"])
        U40(["UC-40 Ngừng đơn thuốc"])
        U41(["UC-41 Phát thuốc, xác nhận liều"])
        U42(["UC-42 Dùng thuốc khi cần"])
        U43(["UC-43 Đối chiếu thuốc"])
        U44(["UC-44 Tiếp nhận thuốc gia đình gửi"])
    end
    CS --- U32
    DD --- U32
    BS --- U33
    DD --- U34
    NV --- U35
    NV --- U36
    DD --- U16
    BS --- U16
    BS --- U37
    SCH --- U38
    BS --- U39
    BS --- U40
    DD --- U41
    DD --- U42
    BS --- U43
    DD --- U43
    DD --- U44
    U36 -. "«extend» mức khẩn cấp" .-> U35
    U16 -. "«extend» vượt khả năng xử lý" .-> U36
    U44 -. "«include» khi đưa vào sử dụng" .-> U43
```

**D4 – Dinh dưỡng, nhân sự, ca và bàn giao**

```mermaid
flowchart LR
    QL["Quản lý viện"]
    TT["Trưởng tầng"]
    DD["Điều dưỡng"]
    NV["Nhân viên"]
    DDV["Dinh dưỡng viên"]
    BS["Bác sĩ"]
    BEP["Nhân viên bếp"]
    subgraph SYS[Dinh dưỡng, nhân sự, ca]
        U45(["UC-45 Gán / duyệt chế độ ăn"])
        U46(["UC-46 Lập và công bố thực đơn"])
        U47(["UC-47 Xem số suất đã chốt"])
        U48(["UC-48 Đối chiếu đồ ăn gia đình"])
        U49(["UC-49 Hồ sơ nhân viên, chứng chỉ"])
        U50(["UC-50 Lập và công bố lịch ca"])
        U51(["UC-51 Phân công chăm sóc"])
        U52(["UC-52 Lập bàn giao cuối ca"])
        U53(["UC-53 Xác nhận bàn giao"])
        U54(["UC-54 Yêu cầu đổi ca"])
    end
    DDV --- U45
    BS --- U45
    DDV --- U46
    BEP --- U47
    DD --- U48
    DDV --- U48
    QL --- U49
    QL --- U50
    TT --- U50
    TT --- U51
    DD --- U52
    DD --- U53
    TT --- U53
    NV --- U54
    U54 -. "«include» duyệt" .-> U50
```

**D5 – Người thân, chi phí, đồ gửi và quản trị**

```mermaid
flowchart LR
    NT["Người thân"]
    HC["Hành chính"]
    DD["Điều dưỡng"]
    QL["Quản lý viện"]
    NV["Nhân viên"]
    KT["Hệ thống kế toán"]
    SMS["Nhà cung cấp SMS"]
    subgraph SYS[Người thân, chi phí, quản trị]
        U55(["UC-55 Quản lý người thân và quyền"])
        U56(["UC-56 Xem thông tin trên cổng"])
        U57(["UC-57 Đăng ký thăm"])
        U58(["UC-58 Đón người cao tuổi"])
        U59(["UC-59 Gửi phản hồi"])
        U60(["UC-60 Duyệt và gửi bản tin"])
        U61(["UC-61 Kiểm tra chi phí"])
        U62(["UC-62 Duyệt và chốt kỳ"])
        U63(["UC-63 Tạo khoản điều chỉnh"])
        U64(["UC-64 Xuất dữ liệu kế toán"])
        U65(["UC-65 Tiếp nhận, trả đồ gửi"])
        U66(["UC-66 Nhận, xác nhận thông báo"])
        U67(["UC-67 Xem dashboard, báo cáo"])
        U68(["UC-68 Tài khoản, phân quyền"])
        U69(["UC-69 Cấu hình tham số"])
        U70(["UC-70 Đăng nhập"])
    end
    HC --- U55
    NT --- U56
    NT --- U57
    NT --- U59
    NT --- U66
    NT --- U70
    HC --- U58
    DD --- U60
    HC --- U61
    HC --- U63
    HC --- U64
    HC --- U65
    QL --- U62
    QL --- U67
    QL --- U68
    QL --- U69
    NV --- U66
    NV --- U70
    U64 --- KT
    U66 --- SMS
    U56 -. "«include»" .-> U70
    U62 -. "«include» khi còn sai sót" .-> U63
```

### 4.4. Permission Matrix

Ký hiệu: **C** cấu hình; **T** thực hiện; **D** duyệt; **X** xem; **P** xem trong phạm vi được phân công; **—** không có quyền. Mọi quyền của nhân viên còn bị giới hạn bởi phạm vi dữ liệu và điều kiện pháp lý (BR-M15-01); ma trận này là quyền tối đa của vai trò.

Viết tắt cột: QL Quản lý viện, TT Trưởng tầng, BS Bác sĩ, ĐD Điều dưỡng, CS Nhân viên chăm sóc, DDV Dinh dưỡng viên, Bếp Nhân viên bếp, VS Nhân viên vệ sinh, HC Hành chính, NT Người thân.

| Chức năng                           | Use case          | QL   | TT   | BS   | ĐD  | CS  | DDV | Bếp | VS  | HC  | NT  |
| ----------------------------------- | ----------------- | ---- | ---- | ---- | --- | --- | --- | --- | --- | --- | --- |
| Hồ sơ người cao tuổi                | UC-01, 02         | X    | P    | X    | P   | P   | P   | —   | —   | T   | X   |
| Bản đồng ý chia sẻ dữ liệu          | UC-03             | X    | —    | —    | —   | —   | —   | —   | —   | T   | X   |
| Dị ứng, bệnh nền                    | UC-04             | X    | P    | T    | T   | P   | X   | —   | —   | —   | X¹  |
| Đánh giá, quy đổi mức chăm sóc      | UC-05, 06         | X    | P    | T, D | T   | —   | —   | —   | —   | —   | —   |
| Tiếp nhận, hợp đồng, đặt cọc        | UC-08, 09, 11, 12 | X    | —    | —    | —   | —   | —   | —   | —   | T   | X   |
| Danh sách chờ, điểm ưu tiên         | UC-10             | D    | —    | —    | —   | —   | —   | —   | —   | T   | —   |
| Yêu cầu thay đổi lưu trú            | UC-13, 14         | D    | —    | T    | —   | —   | —   | —   | —   | T   | T²  |
| Tạm vắng, trở về                    | UC-15             | D    | T    | —    | X   | —   | —   | —   | —   | T   | X   |
| Kết thúc lưu trú, qua đời           | UC-17, 18         | D    | —    | T    | —   | —   | —   | —   | —   | T   | X   |
| Cấu hình phòng, giường              | UC-19             | C    | X    | —    | —   | —   | —   | —   | —   | X   | —   |
| Phân bổ, chuyển giường              | UC-20, 21         | X    | T    | —    | —   | —   | —   | —   | —   | T   | —   |
| Kế hoạch chăm sóc                   | UC-22, 23         | X    | X    | D    | T   | P   | —   | —   | —   | —   | —   |
| Checklist, ghi nhận công việc       | UC-25, 26         | X    | T    | —    | T   | T   | —   | —   | T   | —   | —   |
| Xử lý việc quá hạn                  | UC-27             | X    | T    | —    | —   | —   | —   | —   | —   | —   | —   |
| Hoạt động, ngoài viện               | UC-29, 30         | X    | T    | —    | —   | T   | —   | —   | —   | —   | X   |
| Kiểm tra chất lượng                 | UC-31             | X    | T    | —    | —   | —   | —   | —   | —   | —   | —   |
| Chỉ số sức khỏe                     | UC-32             | X    | P    | X    | T   | T   | —   | —   | —   | —   | X¹  |
| Ngưỡng cảnh báo                     | UC-33             | X    | —    | T    | X   | —   | —   | —   | —   | —   | —   |
| Xử lý cảnh báo                      | UC-34             | X    | T    | T    | T   | X   | —   | —   | —   | —   | —   |
| Sự cố, khẩn cấp                     | UC-35, 36         | X    | T    | T    | T   | T   | T   | T   | T   | T   | X¹  |
| Khoanh vùng lây nhiễm               | UC-37             | T    | X    | T    | X   | —   | —   | —   | —   | X   | —   |
| Đơn thuốc                           | UC-39, 40         | X    | —    | T³   | T⁴  | —   | —   | —   | —   | —   | X¹  |
| Phát thuốc, thuốc khi cần           | UC-41, 42         | X    | X    | X    | T   | —   | —   | —   | —   | —   | —   |
| Đối chiếu thuốc, thuốc gia đình gửi | UC-43, 44         | X    | —    | T    | T   | —   | —   | —   | —   | —   | X   |
| Chế độ ăn, thực đơn                 | UC-45, 46         | X    | —    | D    | X   | —   | T   | X   | —   | —   | X   |
| Suất ăn đã chốt                     | UC-47             | X    | —    | —    | —   | —   | X   | X   | —   | —   | —   |
| Hồ sơ nhân viên, chứng chỉ          | UC-49             | T    | X    | —    | —   | —   | —   | —   | —   | —   | —   |
| Lịch ca, phân công                  | UC-50, 51         | T, D | T, D | P    | P   | P   | P   | P   | P   | P   | —   |
| Bàn giao ca                         | UC-52, 53         | X    | T    | X    | T   | X   | —   | —   | —   | —   | —   |
| Yêu cầu đổi ca                      | UC-54             | X    | D    | T    | T   | T   | T   | T   | T   | T   | —   |
| Người thân và quyền                 | UC-55             | D    | —    | —    | —   | —   | —   | —   | —   | T   | T²  |
| Đăng ký thăm, đón                   | UC-57, 58         | X    | X    | —    | —   | —   | —   | —   | —   | T   | T   |
| Phản hồi, khiếu nại                 | UC-59             | D    | T    | —    | T   | —   | —   | —   | —   | T   | T   |
| Bản tin định kỳ                     | UC-60             | X    | X    | —    | T   | —   | —   | —   | —   | —   | X   |
| Chi phí, khoản điều chỉnh           | UC-61, 63         | D    | —    | —    | —   | —   | —   | —   | —   | T   | X   |
| Chốt kỳ, xuất kế toán               | UC-62, 64         | D    | —    | —    | —   | —   | —   | —   | —   | T   | —   |
| Đồ gửi                              | UC-65             | X    | X    | —    | T   | —   | —   | —   | —   | T   | X   |
| Dashboard, báo cáo                  | UC-67             | X    | P    | P    | P   | —   | —   | —   | —   | P   | —   |
| Tài khoản, phân quyền, tham số      | UC-68, 69         | C    | —    | —    | —   | —   | —   | —   | —   | —   | —   |

¹ Chỉ khi có bản đồng ý chia sẻ dữ liệu đang hiệu lực bao gồm người thân đó (BR-M01-08). ² Chỉ người đại diện; thao tác là gửi yêu cầu hoặc xác nhận, không trực tiếp thay đổi. ³ Kê đơn nội bộ chỉ khi cơ sở và bác sĩ có giấy phép còn hiệu lực (BR-M06-05); nếu không, chỉ nhập đơn từ cơ sở bên ngoài. ⁴ Chỉ nhập đơn đã được kê tại cơ sở y tế bên ngoài, bắt buộc có thông tin cơ sở kê (BR-M07-05).

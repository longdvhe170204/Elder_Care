package com.eldercare.backend.common.document;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.PdfStamper;
import org.openpdf.text.pdf.PdfWriter;

/**
 * Chứng từ PDF dùng chung (tên logic {@code PdfBieuMau}; 007 research S-25): khổ A4, đầu trang thông tin viện
 * (CFG-M11-11), đoạn chữ, bảng tự ngắt trang, ô ký, chân trang "Trang x/y". Font Be Vietnam Pro nhúng nên đủ dấu tiếng
 * Việt. Mỗi đối tượng dựng một file, không dùng lại sau {@link #toBytes()}.
 *
 * <p>Có hai kiểu trình bày: mặc định ({@link #a4}) và gọn ({@link #a4Compact}, lề hẹp, chữ nhỏ hơn, có hàng nhiều cột
 * {@link #fields}) cho chứng từ phải vừa một trang như phiếu tóm tắt chuyển viện (BF-06). Kiểu mặc định giữ nguyên.
 */
public final class PdfDocument {

    private static final float MARGIN = 50f;
    private static final BaseFont REGULAR = font("BeVietnamPro-Regular.ttf");
    private static final BaseFont BOLD = font("BeVietnamPro-Bold.ttf");

    /** Cỡ chữ, lề và khoảng cách của một kiểu trình bày. */
    private record Style(
            float margin,
            Font text,
            Font bold,
            Font small,
            Font title,
            Font heading,
            float titleSpacing,
            float headingBefore,
            float headingAfter,
            float fieldAfter,
            float cellPadding,
            float tableAfter,
            float signatureBefore,
            float signatureHeight) {}

    private static final Style DEFAULT = new Style(
            MARGIN,
            new Font(REGULAR, 10),
            new Font(BOLD, 10),
            new Font(REGULAR, 8),
            new Font(BOLD, 14),
            new Font(BOLD, 11),
            12,
            8,
            6,
            6,
            4,
            10,
            16,
            90);

    private static final Style COMPACT = new Style(
            34f,
            new Font(REGULAR, 9),
            new Font(BOLD, 9),
            new Font(REGULAR, 7.5f),
            new Font(BOLD, 13),
            new Font(BOLD, 9.5f),
            5,
            6,
            3,
            3,
            3,
            4,
            6,
            44);

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final Style style;
    private final Document document;

    private PdfDocument(Style style) {
        this.style = style;
        this.document = new Document(PageSize.A4, style.margin(), style.margin(), style.margin(), style.margin());
        PdfWriter.getInstance(document, out);
        document.open();
    }

    /**
     * Trang A4 có đầu trang thông tin viện và tiêu đề. {@code facility} {@code null} (CFG-M11-11 chưa đặt) thì bỏ đầu
     * trang; {@code logo} là nội dung ảnh PNG, JPEG của tệp {@code logoFileId}, {@code null} thì không in logo.
     */
    public static PdfDocument a4(FacilityInfo facility, byte[] logo, String title) {
        return create(DEFAULT, facility, logo, title);
    }

    /** Như {@link #a4} nhưng trình bày gọn (lề 34pt, chữ 9pt) để chứng từ ngắn vừa một trang. */
    public static PdfDocument a4Compact(FacilityInfo facility, byte[] logo, String title) {
        return create(COMPACT, facility, logo, title);
    }

    private static PdfDocument create(Style style, FacilityInfo facility, byte[] logo, String title) {
        PdfDocument pdf = new PdfDocument(style);
        if (facility != null) {
            pdf.header(facility, logo);
        }
        Paragraph heading = new Paragraph(title, style.title());
        heading.setAlignment(Element.ALIGN_CENTER);
        heading.setSpacingBefore(style.titleSpacing());
        heading.setSpacingAfter(style.titleSpacing());
        pdf.add(heading);
        return pdf;
    }

    public PdfDocument paragraph(String text) {
        Paragraph paragraph = new Paragraph(text, style.text());
        paragraph.setSpacingAfter(style.fieldAfter());
        add(paragraph);
        return this;
    }

    /** Tiêu đề mục: chữ đậm cỡ vừa, cách đoạn trước. */
    public PdfDocument heading(String text) {
        Paragraph paragraph = new Paragraph(text, style.heading());
        paragraph.setSpacingBefore(style.headingBefore());
        paragraph.setSpacingAfter(style.headingAfter());
        paragraph.setKeepTogether(true);
        add(paragraph);
        return this;
    }

    /** Một dòng "Nhãn: giá trị", nhãn in đậm. */
    public PdfDocument field(String label, String value) {
        Paragraph paragraph = new Paragraph();
        paragraph.add(new Chunk(label + ": ", style.bold()));
        paragraph.add(new Chunk(value == null ? "" : value, style.text()));
        paragraph.setSpacingAfter(style.fieldAfter());
        add(paragraph);
        return this;
    }

    /**
     * Nhiều "Nhãn: giá trị" xếp thành lưới {@code columns} cột đều nhau, điền lần lượt từng hàng; {@code labelValuePairs}
     * là nhãn rồi giá trị xen kẽ. Mỗi cặp nằm trọn trong một ô nên chữ không bị xé giữa nhãn và giá trị.
     */
    public PdfDocument fields(int columns, String... labelValuePairs) {
        if (labelValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Cần cặp nhãn, giá trị");
        }
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        table.setSpacingAfter(style.fieldAfter());
        for (int i = 0; i < labelValuePairs.length; i += 2) {
            Paragraph paragraph = new Paragraph();
            paragraph.add(new Chunk(labelValuePairs[i] + ": ", style.bold()));
            paragraph.add(new Chunk(labelValuePairs[i + 1] == null ? "" : labelValuePairs[i + 1], style.text()));
            PdfPCell cell = new PdfPCell(paragraph);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPaddingLeft(0);
            cell.setPaddingTop(0);
            cell.setPaddingRight(10);
            cell.setPaddingBottom(style.fieldAfter());
            table.addCell(cell);
        }
        table.completeRow();
        add(table);
        return this;
    }

    /** Dòng chữ nhỏ canh giữa (phụ đề, nguồn thông tin). */
    public PdfDocument note(String text, boolean centered) {
        Paragraph paragraph = new Paragraph(text, style.small());
        paragraph.setSpacingAfter(4);
        if (centered) paragraph.setAlignment(Element.ALIGN_CENTER);
        add(paragraph);
        return this;
    }

    /** Bảng có dòng tiêu đề lặp lại ở mỗi trang; mỗi dòng có đủ số ô như {@code headers}. */
    public PdfDocument table(List<String> headers, List<List<String>> rows) {
        PdfPTable table = new PdfPTable(headers.size());
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSpacingAfter(style.tableAfter());
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, style.bold()));
            cell.setBackgroundColor(new Color(0xEE, 0xEE, 0xEE));
            cell.setPadding(style.cellPadding());
            table.addCell(cell);
        }
        for (List<String> row : rows) {
            if (row.size() != headers.size()) {
                throw new IllegalArgumentException("Dòng có " + row.size() + " ô, bảng có " + headers.size() + " cột");
            }
            for (String value : row) {
                PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, style.text()));
                cell.setPadding(style.cellPadding());
                table.addCell(cell);
            }
        }
        add(table);
        return this;
    }

    /**
     * Ảnh PNG, JPEG canh giữa trong thân trang, giữ tỉ lệ, cạnh lớn nhất bằng {@code sizePt} điểm (ví dụ mã QR VietQR
     * của phiếu báo phí, 017 FR-032).
     */
    public PdfDocument image(byte[] content, float sizePt) {
        Image image = image(content);
        image.scaleToFit(sizePt, sizePt);
        image.setAlignment(Element.ALIGN_CENTER);
        add(image);
        return this;
    }

    /** Ô ký tay đặt cạnh nhau: chức danh, dòng hướng dẫn, khoảng trống để ký. */
    public PdfDocument signatures(String... titles) {
        PdfPTable table = new PdfPTable(titles.length);
        table.setWidthPercentage(100);
        table.setSpacingBefore(style.signatureBefore());
        table.setKeepTogether(true);
        for (String title : titles) {
            Paragraph content = new Paragraph(title, style.bold());
            content.add(new Phrase("\n(Ký, ghi rõ họ tên)", style.small()));
            content.setAlignment(Element.ALIGN_CENTER);
            PdfPCell cell = new PdfPCell();
            cell.addElement(content);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setMinimumHeight(style.signatureHeight());
            table.addCell(cell);
        }
        add(table);
        return this;
    }

    /** Đóng file và đánh số trang. */
    public byte[] toBytes() {
        document.close();
        try {
            PdfReader reader = new PdfReader(out.toByteArray());
            ByteArrayOutputStream numbered = new ByteArrayOutputStream();
            PdfStamper stamper = new PdfStamper(reader, numbered);
            int pages = reader.getNumberOfPages();
            for (int i = 1; i <= pages; i++) {
                ColumnText.showTextAligned(
                        stamper.getOverContent(i),
                        Element.ALIGN_CENTER,
                        new Phrase("Trang " + i + "/" + pages, style.small()),
                        reader.getPageSize(i).getWidth() / 2,
                        style.margin() / 2,
                        0);
            }
            stamper.close();
            reader.close();
            return numbered.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Đóng dấu chữ (ví dụ "Bản in lại", FR-050h) ở góc trên mỗi trang. Dấu ghi nối vào sau file (chế độ append), nên
     * các byte của file gốc giữ nguyên; file đã lưu không đổi, chỉ bản trả về có dấu.
     */
    public static byte[] stamp(byte[] pdf, String text) {
        try {
            PdfReader reader = new PdfReader(pdf);
            ByteArrayOutputStream stamped = new ByteArrayOutputStream();
            PdfStamper stamper = new PdfStamper(reader, stamped, null, true);
            Font font = new Font(BOLD, 12, Font.NORMAL, new Color(0xB0, 0x20, 0x20));
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                Rectangle page = reader.getPageSize(i);
                PdfContentByte over = stamper.getOverContent(i);
                ColumnText.showTextAligned(
                        over,
                        Element.ALIGN_RIGHT,
                        new Phrase(text, font),
                        page.getWidth() - MARGIN,
                        page.getHeight() - MARGIN / 2,
                        0);
            }
            stamper.close();
            reader.close();
            return stamped.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void header(FacilityInfo facility, byte[] logo) {
        PdfPTable table = new PdfPTable(logo == null ? new float[] {1} : new float[] {1, 5});
        table.setWidthPercentage(100);
        if (logo != null) {
            PdfPCell cell = new PdfPCell(image(logo), true);
            cell.setBorder(Rectangle.BOTTOM);
            cell.setFixedHeight(48);
            table.addCell(cell);
        }
        Paragraph lines = new Paragraph();
        line(lines, facility.name(), style.bold(), "");
        line(lines, facility.address(), style.text(), "Địa chỉ: ");
        line(lines, facility.taxCode(), style.text(), "Mã số thuế: ");
        line(lines, facility.phone(), style.text(), "Điện thoại: ");
        PdfPCell cell = new PdfPCell();
        cell.addElement(lines);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setPaddingBottom(6);
        table.addCell(cell);
        add(table);
    }

    private static void line(Paragraph lines, String value, Font font, String label) {
        if (value != null && !value.isBlank()) {
            lines.add(new Phrase((lines.isEmpty() ? "" : "\n") + label + value, font));
        }
    }

    private static Image image(byte[] content) {
        try {
            return Image.getInstance(content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void add(Element element) {
        try {
            document.add(element);
        } catch (DocumentException e) {
            throw new IllegalStateException(e);
        }
    }

    private static BaseFont font(String file) {
        try (InputStream in = PdfDocument.class.getResourceAsStream("/fonts/" + file)) {
            if (in == null) {
                throw new IllegalStateException("Thiếu font /fonts/" + file);
            }
            return BaseFont.createFont(file, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, in.readAllBytes(), null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

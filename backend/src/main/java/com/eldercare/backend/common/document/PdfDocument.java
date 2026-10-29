package com.eldercare.backend.common.document;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
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
 */
public final class PdfDocument {

    private static final float MARGIN = 50f;
    private static final BaseFont REGULAR = font("BeVietnamPro-Regular.ttf");
    private static final BaseFont BOLD = font("BeVietnamPro-Bold.ttf");
    private static final Font TEXT = new Font(REGULAR, 10);
    private static final Font TEXT_BOLD = new Font(BOLD, 10);
    private static final Font SMALL = new Font(REGULAR, 8);
    private static final Font TITLE = new Font(BOLD, 14);

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final Document document = new Document(PageSize.A4, MARGIN, MARGIN, MARGIN, MARGIN);

    private PdfDocument() {
        PdfWriter.getInstance(document, out);
        document.open();
    }

    /**
     * Trang A4 có đầu trang thông tin viện và tiêu đề. {@code facility} {@code null} (CFG-M11-11 chưa đặt) thì bỏ đầu
     * trang; {@code logo} là nội dung ảnh PNG, JPEG của tệp {@code logoFileId}, {@code null} thì không in logo.
     */
    public static PdfDocument a4(FacilityInfo facility, byte[] logo, String title) {
        PdfDocument pdf = new PdfDocument();
        if (facility != null) {
            pdf.header(facility, logo);
        }
        Paragraph heading = new Paragraph(title, TITLE);
        heading.setAlignment(Element.ALIGN_CENTER);
        heading.setSpacingBefore(12);
        heading.setSpacingAfter(12);
        pdf.add(heading);
        return pdf;
    }

    public PdfDocument paragraph(String text) {
        Paragraph paragraph = new Paragraph(text, TEXT);
        paragraph.setSpacingAfter(6);
        add(paragraph);
        return this;
    }

    /** Bảng có dòng tiêu đề lặp lại ở mỗi trang; mỗi dòng có đủ số ô như {@code headers}. */
    public PdfDocument table(List<String> headers, List<List<String>> rows) {
        PdfPTable table = new PdfPTable(headers.size());
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSpacingAfter(10);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, TEXT_BOLD));
            cell.setBackgroundColor(new Color(0xEE, 0xEE, 0xEE));
            cell.setPadding(4);
            table.addCell(cell);
        }
        for (List<String> row : rows) {
            if (row.size() != headers.size()) {
                throw new IllegalArgumentException("Dòng có " + row.size() + " ô, bảng có " + headers.size() + " cột");
            }
            for (String value : row) {
                PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, TEXT));
                cell.setPadding(4);
                table.addCell(cell);
            }
        }
        add(table);
        return this;
    }

    /** Ô ký tay đặt cạnh nhau: chức danh, dòng hướng dẫn, khoảng trống để ký. */
    public PdfDocument signatures(String... titles) {
        PdfPTable table = new PdfPTable(titles.length);
        table.setWidthPercentage(100);
        table.setSpacingBefore(16);
        table.setKeepTogether(true);
        for (String title : titles) {
            Paragraph content = new Paragraph(title, TEXT_BOLD);
            content.add(new Phrase("\n(Ký, ghi rõ họ tên)", SMALL));
            content.setAlignment(Element.ALIGN_CENTER);
            PdfPCell cell = new PdfPCell();
            cell.addElement(content);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setMinimumHeight(90);
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
                        new Phrase("Trang " + i + "/" + pages, SMALL),
                        reader.getPageSize(i).getWidth() / 2,
                        MARGIN / 2,
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
        line(lines, facility.name(), TEXT_BOLD, "");
        line(lines, facility.address(), TEXT, "Địa chỉ: ");
        line(lines, facility.taxCode(), TEXT, "Mã số thuế: ");
        line(lines, facility.phone(), TEXT, "Điện thoại: ");
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

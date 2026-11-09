package com.eldercare.backend.billing.balance;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

/** Đọc lại chữ trong PDF, ô trong Excel do 017 dựng, để so số liệu giữa hai định dạng (FR-032, FR-025). */
final class DocumentReaders {

    private DocumentReaders() {}

    static String pdfText(byte[] pdf) {
        try {
            PdfReader reader = new PdfReader(pdf);
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page)).append('\n');
            }
            reader.close();
            // ô bảng dài bị ngắt dòng khi đọc lại: gộp mọi khoảng trắng thành một dấu cách
            return text.toString().replaceAll("\\s+", " ");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Mọi ô có chữ của trang tính đầu, theo thứ tự dòng rồi cột. */
    static List<String> excelCells(byte[] xlsx) {
        try (ReadableWorkbook workbook = new ReadableWorkbook(new ByteArrayInputStream(xlsx));
                Stream<Row> rows = workbook.getFirstSheet().openStream()) {
            List<String> cells = new ArrayList<>();
            rows.forEach(row -> row.forEach(cell -> {
                if (cell != null && !cell.getText().isBlank()) {
                    cells.add(cell.getText());
                }
            }));
            return cells;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

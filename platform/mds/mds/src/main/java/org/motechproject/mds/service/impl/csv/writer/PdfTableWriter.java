package org.motechproject.mds.service.impl.csv.writer;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.motechproject.mds.ex.csv.DataExportException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * An implementation of the table writer that writes the table data in PDF format.
 * Uses the iText PDF library underneath.
 */
public class PdfTableWriter implements TableWriter {

    private static final String ROW_NUMBER_HEADER = "No";
    private static final float MARGIN = 36f;
    private static final float PAGE_SIZE = PageSize.A4.getWidth() - 2*MARGIN;
    private static final float MAX_COLUMN_WIDTH = PageSize.A4.getHeight() - 2*MARGIN;

    private final PdfWriter pdfWriter;
    private final Document pdfDocument;
    private final PdfContentByte pdfCanvas;
    private PdfPTable dataTable;
    private Map<String, Float> columnsWidths;
    private int rows = 0;

    public PdfTableWriter(OutputStream outputStream) {
        pdfDocument = new Document(new Rectangle(PageSize.A4.getHeight(), PageSize.A4.getWidth()));
        try {
            pdfWriter = PdfWriter.getInstance(pdfDocument, outputStream);
        } catch (DocumentException e) {
            throw new DataExportException("Unable to create a PDF writer instance", e);
        }
        pdfDocument.open();
        pdfCanvas = pdfWriter.getDirectContent();
    }

    @Override
    public void writeRow(Map<String, String> row, String[] headers) throws IOException {

        if (dataTable == null) {
            writeHeader(headers);
        }

        writeCell(ROW_NUMBER_HEADER, Integer.toString(rows++));

        for (String header : headers) {
            writeCell(header, row.get(header));
        }
    }

    @Override
    public void writeHeader(String[] headers) throws IOException {

        dataTable = new PdfPTable(headers.length + 1);
        columnsWidths = new LinkedHashMap<>();

        writeHeaderCell(ROW_NUMBER_HEADER);

        for (String header : headers) {
            writeHeaderCell(header);
        }
    }

    @Override
    public void close() {
        try {
            float[] relativeWidths = getRelativeWidths();

            List<Integer> pages = calculatePages(relativeWidths);
            dataTable.setWidths(relativeWidths);
            dataTable.setLockedWidth(true);
            dataTable.setTotalWidth(calculateTotalTableWidth(relativeWidths));

            int currentRow = 1;

            while (tableHasMoreRows(currentRow)) {
                currentRow = writePage(pages, relativeWidths, currentRow);
            }

            pdfDocument.close();
        } catch (DocumentException e) {
            throw new DataExportException("Unable to add a table to the PDF file", e);
        } finally {
            pdfWriter.close();
        }
    }

    private List<Integer> calculatePages(float[] relativeWidths) {
        List<Integer> pages = new LinkedList<>();
        pages.add(1);

        float pageSize = 0;

        for (int i = 1; i < relativeWidths.length; i++) {
            if (pageSize + relativeWidths[i] > MAX_COLUMN_WIDTH - relativeWidths[0]) {
                pages.add(i);
                resizeColumns(relativeWidths, pages, pageSize);
                pageSize = 0;
            }
            pageSize += relativeWidths[i];
        }

        pages.add(-1);
        return pages;
    }

    private void resizeColumns(float[] relativeWidths, List<Integer> pages, float pageSize) {
        for (int i = pages.get(pages.size() - 2); i < pages.get(pages.size() - 1); i++) {
            relativeWidths[i] = (MAX_COLUMN_WIDTH - relativeWidths[0]) * (relativeWidths[i] / pageSize);
        }
    }

    private float calculateTotalTableWidth(float[] widths) {
        float totalWidth = 0;
        for (float width : widths) {
            totalWidth += width;
        }
        return totalWidth;
    }

    private void updateWidthIfNeeded(String header, PdfPCell cell) {
        Float width = calculateCellWidth(cell);

        if (columnsWidths.get(header) < width) {
            columnsWidths.put(header, width > MAX_COLUMN_WIDTH ? MAX_COLUMN_WIDTH : width);
        }
    }

    private Float calculateCellWidth(PdfPCell cell) {
        return cell.getBorderWidthLeft()
                + cell.getEffectivePaddingLeft()
                + ColumnText.getWidth(cell.getPhrase())
                + cell.getEffectivePaddingRight()
                + cell.getBorderWidthRight();
    }

    private int writePage(List<Integer> pages, float[] widths, int startRow) {
        float y = writeHeaders(pdfCanvas, pages.get(0), pages.get(1), widths[0]);
        int endRow = startRow;
        do {
            if (endRow == dataTable.getRows().size()) {
                break;
            }
            dataTable.writeSelectedRows(0, 1, endRow, endRow + 1, 36, y, pdfCanvas);
            y = dataTable.writeSelectedRows(pages.get(0), pages.get(1), endRow, endRow+1, 36 + widths[0], y, pdfCanvas);
            endRow++;
        } while (y - dataTable.getRowHeight(endRow + 2) > 36);
        pdfDocument.newPage();
        for (int i = 2; i < pages.size(); i++) {
            y = writeHeaders(pdfCanvas, pages.get(i - 1), pages.get(i), widths[0]);
            dataTable.writeSelectedRows(0, 1, startRow, endRow, 36, y, pdfCanvas);
            dataTable.writeSelectedRows(pages.get(i - 1), pages.get(i), startRow, endRow, 36 + widths[0], y, pdfCanvas);
            pdfDocument.newPage();
        }
        return endRow;
    }

    private boolean tableHasMoreRows(int row) {
        return row < dataTable.getRows().size();
    }

    private Float writeHeaders(PdfContentByte canvas, Integer from, Integer to, float indexSize) {
        dataTable.writeSelectedRows(0, 1, 0, 1, 36, PAGE_SIZE + MARGIN, canvas);
        return dataTable.writeSelectedRows(from, to, 0, 1, MARGIN + indexSize, PAGE_SIZE + MARGIN, canvas);
    }

    private void writeCell(String column, String value) {
        // we want blank cells to display, even if they are the only ones
        Chunk chunk = StringUtils.isBlank(value) ? Chunk.NEWLINE : new Chunk(value);
        // add as a cell to the table
        PdfPCell cell = new PdfPCell(new Phrase(chunk));
        dataTable.addCell(cell);
        updateWidthIfNeeded(column, cell);
    }

    private void writeHeaderCell(String column) {
        PdfPCell cell = new PdfPCell(new Phrase(column));
        cell.setBackgroundColor(BaseColor.GRAY);
        dataTable.addCell(cell);
        columnsWidths.put(column, calculateCellWidth(cell));
    }

    private float[] getRelativeWidths() {
        return ArrayUtils.toPrimitive(columnsWidths.values().toArray(new Float[0]));
    }
}

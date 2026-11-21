package org.ajeet.sheets;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * Google Sheets API v4 implementation. Credentials are caller-supplied (see
 * {@link SheetsAuth}); with a service account the spreadsheet must be shared
 * with its client_email, with OAuth the signed-in user needs edit access.
 */
public class GoogleSheetsClient implements SheetsClient {

    private final Sheets service;
    private final String spreadsheetId;
    private final String tabName;
    private final int sheetId;

    public GoogleSheetsClient(HttpRequestInitializer credentials, String spreadsheetId)
            throws IOException, GeneralSecurityException {
        this.service = new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                credentials)
                .setApplicationName("attendance-extraction")
                .build();
        this.spreadsheetId = spreadsheetId;

        // Resolve the first tab's name (for A1 ranges) and numeric id (for deletes).
        List<Sheet> sheets = service.spreadsheets().get(spreadsheetId)
                .setFields("sheets(properties(sheetId,title))")
                .execute().getSheets();
        if (sheets == null || sheets.isEmpty()) {
            throw new IOException("Spreadsheet has no sheets");
        }
        this.tabName = sheets.getFirst().getProperties().getTitle();
        this.sheetId = sheets.getFirst().getProperties().getSheetId();
    }

    @Override
    public List<List<Object>> readAll() throws IOException {
        ValueRange vr = service.spreadsheets().values()
                .get(spreadsheetId, quoteTab(tabName))
                .setValueRenderOption("FORMATTED_VALUE")
                .execute();
        return vr.getValues() == null ? List.of() : vr.getValues();
    }

    @Override
    public void appendRow(List<Object> values) throws IOException {
        service.spreadsheets().values()
                .append(spreadsheetId, range(tabName, "A:A"),
                        new ValueRange().setValues(List.of(values)))
                .setValueInputOption("USER_ENTERED")
                .setInsertDataOption("INSERT_ROWS")
                .execute();
    }

    @Override
    public void updateRow(int rowNumber, List<Object> values) throws IOException {
        service.spreadsheets().values()
                .update(spreadsheetId,
                        range(tabName, "A" + rowNumber + ":" + colLetter(values.size()) + rowNumber),
                        new ValueRange().setValues(List.of(values)))
                .setValueInputOption("USER_ENTERED")
                .execute();
    }

    @Override
    public void deleteRow(int rowNumber) throws IOException {
        DimensionRange dimension = new DimensionRange()
                .setSheetId(sheetId)
                .setDimension("ROWS")
                .setStartIndex(rowNumber - 1)   // 0-based
                .setEndIndex(rowNumber);        // exclusive
        Request request = new Request()
                .setDeleteDimension(new DeleteDimensionRequest().setRange(dimension));
        service.spreadsheets().batchUpdate(spreadsheetId,
                new BatchUpdateSpreadsheetRequest().setRequests(List.of(request)))
                .execute();
    }

    @Override
    public String describe() {
        return "Google Sheet (" + tabName + ")";
    }

    private static String quoteTab(String title) {
        return "'" + title.replace("'", "\\'") + "'";
    }

    private static String range(String tabName, String a1) {
        return quoteTab(tabName) + "!" + a1;
    }

    /** 1 -> A, 26 -> Z, 27 -> AA. */
    private static String colLetter(int n) {
        StringBuilder sb = new StringBuilder();
        while (n > 0) {
            n--;
            sb.insert(0, (char) ('A' + n % 26));
            n /= 26;
        }
        return sb.toString();
    }
}

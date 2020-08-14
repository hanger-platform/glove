/*
 * Copyright (c) 2020 Dafiti Group
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package br.com.dafiti.googlesheets.export.api;

import br.com.dafiti.googlesheets.export.GoogleSheetsExport;
import br.com.dafiti.googlesheets.export.model.SheetDetails;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.InsertDimensionRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manage google sheets API.
 *
 * @author Helio Leal
 */
public class GoogleSheetsApi {

    private final String sheetName;
    private Sheets service;
    private Spreadsheet spreadsheet;

    /**
     * Credentials file.
     *
     * @param sheetName
     */
    public GoogleSheetsApi(String sheetName) {
        this.sheetName = sheetName;
    }

    /**
     * Authenticate on Googel Spreadsheet API.
     *
     * @param credentials
     * @param spreadSheetId
     * @return GoogleSheetsApi
     */
    public GoogleSheetsApi authenticate(String credentials, String spreadSheetId) {
        try {
            //Build a new authorized API client service.
            NetHttpTransport netHttpTransport
                    = GoogleNetHttpTransport.newTrustedTransport();

            //Json instance.
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

            //Load client secrets.
            GoogleClientSecrets googleClientSecrets
                    = GoogleClientSecrets.load(
                            jsonFactory,
                            new InputStreamReader(
                                    new FileInputStream(
                                            credentials)));

            //Build flow and trigger user authorization request.
            GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow
                    = new GoogleAuthorizationCodeFlow.Builder(
                            netHttpTransport,
                            jsonFactory,
                            googleClientSecrets,
                            Collections.singletonList(SheetsScopes.SPREADSHEETS))
                            .setDataStoreFactory(
                                    new FileDataStoreFactory(
                                            new java.io.File("tokens")))
                            .setAccessType("offline")
                            .build();

            //Instance of sheets service.
            this.service = new Sheets.Builder(
                    netHttpTransport,
                    jsonFactory,
                    new AuthorizationCodeInstalledApp(
                            googleAuthorizationCodeFlow,
                            new LocalServerReceiver()).authorize("user"))
                    .setApplicationName("Google Sheets Export API")
                    .build();

            //Inits spreadsheet object.
            this.spreadsheet = this.service
                    .spreadsheets()
                    .get(spreadSheetId).execute();

        } catch (GeneralSecurityException
                | IOException ex) {
            Logger
                    .getLogger(GoogleSheetsExport.class.getName())
                    .log(Level.WARNING, "Error on authenticate: {0}", new Object[]{ex.getMessage()});
        }

        return this;
    }

    /**
     * Clean cells range.
     *
     * @param sheet SheetDetails
     * @param startIndex Start index
     */
    public void cleanCells(SheetDetails sheet, int startIndex) {
        try {
            String start = this.sheetName + "!A" + startIndex;
            String end = this.getColumnName(sheet.getColumnCount()) + sheet.getRowCount();
            String range = start + ":" + end;

            Logger.getLogger(GoogleSheetsApi.class.getName())
                    .log(Level.INFO,
                            "Clearing cells range: {0}.",
                            new Object[]{range});

            //Execute request
            this.service
                    .spreadsheets()
                    .values()
                    .clear(this.spreadsheet.getSpreadsheetId(), range, new ClearValuesRequest())
                    .execute();

        } catch (IOException ex) {
            Logger.getLogger(GoogleSheetsExport.class.getName())
                    .log(Level.WARNING, "Error cleaning cells: {0}", new Object[]{ex.getMessage()});
        }
    }

    /**
     * Add new columns.
     *
     * @param sheet Sheet Details
     * @param endIndex Add column until index
     */
    public void addColumns(SheetDetails sheet, int endIndex) {
        Logger.getLogger(GoogleSheetsApi.class.getName())
                .log(Level.INFO,
                        "Adding columns from {0} to {1} because process need more columns.",
                        new Object[]{this.getColumnName(sheet.getColumnCount()), this.getColumnName(endIndex)});

        try {
            Request request = new Request()
                    .setInsertDimension(new InsertDimensionRequest()
                            .setRange(new DimensionRange()
                                    .setSheetId(sheet.getId())
                                    .setDimension("COLUMNS")
                                    .setStartIndex(sheet.getColumnCount() - 1)
                                    .setEndIndex(endIndex - 1)
                            )
                    );

            List<Request> requests = new ArrayList();
            requests.add(request);

            Sheets.Spreadsheets.BatchUpdate batchUpdate = this.service
                    .spreadsheets()
                    .batchUpdate(this.spreadsheet.getSpreadsheetId(), new BatchUpdateSpreadsheetRequest().setRequests(requests));
            batchUpdate.execute();
        } catch (IOException ex) {
            Logger.getLogger(GoogleSheetsApi.class.getName())
                    .log(Level.WARNING, "Error addings columns: {0}", new Object[]{ex.getMessage()});
        }
    }

    /**
     * Remove unused columns.
     *
     * @param sheet Sheet details
     * @param startIndex Remove column from index
     */
    public void removeColumns(SheetDetails sheet, int startIndex) {

        Logger.getLogger(GoogleSheetsApi.class.getName())
                .log(Level.INFO,
                        "Deleting columns from {0} because they won't be used.",
                        new Object[]{this.getColumnName(startIndex + 1)});

        try {
            Request request = new Request()
                    .setDeleteDimension(new DeleteDimensionRequest()
                            .setRange(new DimensionRange()
                                    .setSheetId(sheet.getId())
                                    .setDimension("COLUMNS")
                                    .setStartIndex(startIndex)
                            )
                    );

            List<Request> requests = new ArrayList();
            requests.add(request);

            Sheets.Spreadsheets.BatchUpdate batchUpdate = service
                    .spreadsheets()
                    .batchUpdate(this.spreadsheet.getSpreadsheetId(), new BatchUpdateSpreadsheetRequest().setRequests(requests));
            batchUpdate.execute();
        } catch (IOException ex) {
            Logger.getLogger(GoogleSheetsApi.class.getName())
                    .log(Level.WARNING, "Error removing columns: {0}", new Object[]{ex.getMessage()});
        }
    }

    /**
     * Update data.
     *
     * @param values
     * @param start
     * @param columns
     * @param rows
     */
    public void update(List<List<Object>> values, int start, int columns, int rows) {

        try {
            String rangeStart = this.sheetName + "!A" + start;
            String rangeEnd = getColumnName(columns) + rows;

            ValueRange body = new ValueRange().setValues(values);

            this.service
                    .spreadsheets()
                    .values()
                    .update(this.spreadsheet.getSpreadsheetId(), rangeStart + ":" + rangeEnd, body)
                    .setValueInputOption("USER_ENTERED")
                    .execute();

        } catch (IOException ex) {
            Logger.getLogger(GoogleSheetsApi.class.getName())
                    .log(Level.WARNING, "Error updating data: {0}", new Object[]{ex.getMessage()});
        }
    }

    /**
     * Append New data.
     *
     * @param values
     * @param start
     * @param columns
     * @param rows
     */
    public void append(List<List<Object>> values, int start, int columns, int rows) {

        try {
            String rangeStart = this.sheetName + "!A" + start;
            String rangeEnd = getColumnName(columns) + rows;

            ValueRange body = new ValueRange().setValues(values);

            this.service
                    .spreadsheets()
                    .values()
                    .append(this.spreadsheet.getSpreadsheetId(), rangeStart + ":" + rangeEnd, body)
                    .setValueInputOption("USER_ENTERED")
                    .execute();

        } catch (IOException ex) {
            Logger.getLogger(GoogleSheetsApi.class.getName())
                    .log(Level.WARNING, "Error appending data: {0}", new Object[]{ex.getMessage()});
        }
    }

    /**
     * Get details about a sheet.
     *
     * @return
     */
    public SheetDetails getSheetDetails() {
        SheetDetails details = new SheetDetails();
        List<Sheet> sheetList = this.spreadsheet.getSheets();

        //Read sheets to get id, name and column count of a sheet.
        for (Sheet sheet : sheetList) {
            details.setName(sheet.getProperties().getTitle());

            if (details.getName().equalsIgnoreCase(this.sheetName)) {
                details.setId(sheet.getProperties().getSheetId());
                details.setColumnCount(sheet.getProperties().getGridProperties().getColumnCount());
                details.setRowCount(sheet.getProperties().getGridProperties().getRowCount());
                break;
            }
        }

        return details;
    }

    /**
     * Based on a column count, return the column name to use on Google
     * Spreadsheet.
     *
     * Logic taken from:
     * https://stackoverflow.com/questions/181596/how-to-convert-a-column-number-e-g-127-into-an-excel-column-e-g-aa?page=1&tab=votes#tab-top
     *
     * @param columnNumber
     * @return
     */
    public String getColumnName(int columnNumber) {
        int dividend = columnNumber;
        int i;
        String columnName = "";
        int modulo;
        while (dividend > 0) {
            modulo = (dividend - 1) % 26;
            i = 65 + modulo;
            columnName = new Character((char) i).toString() + columnName;
            dividend = (int) ((dividend - modulo) / 26);
        }
        return columnName;
    }

    /**
     * Identify if a sheet has values.
     *
     * @param sheet SheetDetails
     * @param startIndex Start index.
     * @return
     */
    public boolean hasValues(SheetDetails sheet, int startIndex) {
        try {
            String start = this.sheetName + "!A" + startIndex;
            String end = this.getColumnName(sheet.getColumnCount()) + sheet.getRowCount();

            //Get filled cells.
            ValueRange result = this.service
                    .spreadsheets()
                    .values()
                    .get(this.spreadsheet.getSpreadsheetId(), start + ":" + end)
                    .execute();
            return result.getValues() != null;

        } catch (IOException ex) {
            Logger.getLogger(GoogleSheetsApi.class.getName())
                    .log(Level.WARNING, "Error getting cells values: {0}", new Object[]{ex.getMessage()});
        }

        return false;
    }
}

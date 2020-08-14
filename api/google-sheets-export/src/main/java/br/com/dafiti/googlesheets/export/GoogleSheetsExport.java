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
package br.com.dafiti.googlesheets.export;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
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
import com.google.api.services.sheets.v4.Sheets.Spreadsheets.BatchUpdate;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.Clear;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.ClearValuesResponse;
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.InsertDimensionRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.univocity.parsers.common.routine.InputDimension;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.CsvRoutines;
import java.io.File;
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
 * Method getSpreadSheetColumnName was taken from:
 * https://stackoverflow.com/questions/181596/how-to-convert-a-column-number-e-g-127-into-an-excel-column-e-g-aa?page=1&tab=votes#tab-top
 *
 * @author Helio Leal
 */
public class GoogleSheetsExport {

    public static final int CELLS_LIMIT = 5000000;
    public static final int POOL = 5;

    /**
     * @param args the command line arguments
     *
     * @throws br.com.dafiti.mitt.exception.DuplicateEntityException
     * @throws java.security.GeneralSecurityException
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws
            DuplicateEntityException,
            GeneralSecurityException,
            IOException {

        Logger.getLogger(GoogleSheetsExport.class.getName())
                .info("Google sheets export started.");

        //Define the mitt.
        Mitt mitt = new Mitt();

        //Define parameters. 
        mitt.getConfiguration()
                .addParameter("c", "credentials", "Credentials file", "", true, false)
                .addParameter("s", "spreadsheet", "Id of the spreadsheet", "", true, false)
                .addParameter("i", "input", "Input file path and file name", "", true, false)
                .addParameter("t", "sheet", "Define sheet name", "", true, false)
                .addParameter("sl", "sleep", "(Optional) Sleep time in seconds at one request and another; 0 is default", "0")
                .addParameter("m", "method", "(Optional) Update method, 0 for full, 1 append; 0 is default", "0")
                .addParameter("d", "debug", "(Optional) Identify if it is debug mode; false is default", "false");

        //Read the command line interface. 
        CommandLineInterface cli = mitt.getCommandLineInterface(args);

        //Identifies if spreadsheet id is not empty.
        if (!cli.getParameter("spreadsheet").isEmpty()) {

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
                                            cli.getParameter("credentials"))));

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
            Sheets service = new Sheets.Builder(
                    netHttpTransport,
                    jsonFactory,
                    new AuthorizationCodeInstalledApp(
                            googleAuthorizationCodeFlow,
                            new LocalServerReceiver()).authorize("user"))
                    .setApplicationName("Google Sheets Export API")
                    .build();

            //Define the input file.
            File file = new File(cli.getParameter("input"));

            //Get CSV informations.
            InputDimension inputDimension = new CsvRoutines(getSettings(true)).getInputDimension(file);

            //Calculate the number of cells that will be written.
            long cells = (inputDimension.columnCount() * inputDimension.rowCount());

            Logger.getLogger(GoogleSheetsExport.class.getName())
                    .log(Level.INFO, "[{0}] rows: {1}, cols: {2} ({3} cells will be written)",
                            new Object[]{file.getName(), inputDimension.rowCount(), inputDimension.columnCount(), cells});

            //Identify if cells count is over the suppported on Google Sheets.
            if (cells <= CELLS_LIMIT) {
                int sheetId = -1;
                int sheetColumnCount = 0;
                int sheetRowCount = 0;
                String sheetName = null;

                Spreadsheet spreadsheet = service.spreadsheets().get(cli.getParameter("spreadsheet")).execute();
                List<Sheet> sheetList = spreadsheet.getSheets();

                //Read sheets to get id, name and column count of a sheet.
                for (Sheet sheet : sheetList) {
                    sheetName = sheet.getProperties().getTitle();

                    if (sheetName.equalsIgnoreCase(cli.getParameter("sheet"))) {
                        sheetId = sheet.getProperties().getSheetId();
                        sheetColumnCount = sheet.getProperties().getGridProperties().getColumnCount();
                        sheetRowCount = sheet.getProperties().getGridProperties().getRowCount();
                        break;
                    }
                }

                //Identify if sheet was found.
                if (sheetId >= 0) {
                    Request request = null;

                    //Identify if should remove unused columns.
                    if (sheetColumnCount > inputDimension.columnCount()) {
                        Logger.getLogger(GoogleSheetsExport.class.getName())
                                .log(Level.INFO,
                                        "Deleting columns from {0} to {1} because they won't be used.",
                                        new Object[]{getSpreadSheetColumnName(inputDimension.columnCount() + 1), getSpreadSheetColumnName(sheetColumnCount)});

                        request = new Request()
                                .setDeleteDimension(new DeleteDimensionRequest()
                                        .setRange(new DimensionRange()
                                                .setSheetId(sheetId)
                                                .setDimension("COLUMNS")
                                                .setStartIndex(inputDimension.columnCount())
                                        )
                                );
                    }

                    //Identify if should add new columns.
                    if (sheetColumnCount < inputDimension.columnCount()) {
                        Logger.getLogger(GoogleSheetsExport.class.getName())
                                .log(Level.INFO,
                                        "Adding columns from {0} to {1} because process need more columns.",
                                        new Object[]{getSpreadSheetColumnName(sheetColumnCount), getSpreadSheetColumnName(inputDimension.columnCount())});

                        request = new Request()
                                .setInsertDimension(new InsertDimensionRequest()
                                        .setRange(new DimensionRange()
                                                .setSheetId(sheetId)
                                                .setDimension("COLUMNS")
                                                .setStartIndex(sheetColumnCount - 1)
                                                .setEndIndex(inputDimension.columnCount() - 1)
                                        )
                                );
                    }

                    //Identify if should execute some request.
                    if (request != null) {
                        executeRequest(request, service, spreadsheet.getSpreadsheetId());
                    }                    
                    
                    //Define a csv parser.
                    CsvParser csvParser;
                    
                    //Identify update method.                    
                    switch(cli.getParameterAsInteger("method")) {                        
                        // Append Method
                        case 1:                           
                            csvParser = new CsvParser(getSettings(sheetRowCount > 0));
                            break;
                        // Full method
                        default:
                            csvParser = new CsvParser(getSettings(false));
                            break;
                    }                    

                    //Init a parser.
                    csvParser.beginParsing(file);
                    
                    //Range init.
                    int startIndex = 1;
                    int pool = (inputDimension.rowCount() < POOL) ? (int) inputDimension.rowCount() : POOL;

                    //Value being processed.
                    String[] line;

                    //Values that will fill spreadsheet.
                    List<List<Object>> values = new ArrayList();

                    //Count records.
                    int count = 0;

                    // If method is full, clear all cells.
                    if (cli.getParameterAsInteger("method") == 0) {
                        String rangeStart = sheetName + "!A" + startIndex;
                        String rangeEnd = getSpreadSheetColumnName(sheetColumnCount) + sheetRowCount;
                        String range = rangeStart + ":" + rangeEnd;
                        
                        Logger.getLogger(GoogleSheetsExport.class.getName())
                            .log(Level.INFO,
                                    "Clearing cells range: {0}",
                                    new Object[]{spreadsheet.getProperties().getTitle(), spreadsheet.getSpreadsheetUrl()});
                        

                        Clear request2 = service.spreadsheets().values().clear(spreadsheet.getSpreadsheetId(), range, new ClearValuesRequest());

                        ClearValuesResponse response = request2.execute();
                    }

                    Logger.getLogger(GoogleSheetsExport.class.getName())
                            .log(Level.INFO,
                                    "Updating spreadsheet {0} [{1}]",
                                    new Object[]{spreadsheet.getProperties().getTitle(), spreadsheet.getSpreadsheetUrl()});

                    //Process each csv record.
                    while ((line = csvParser.parseNext()) != null) {
                        List<Object> record = new ArrayList();

                        for (String column : line) {
                            record.add(column);
                        }
                        values.add(record);
                        count++;

                        //Identify if should flush data into google spreadsheet.
                        if (count == pool) {
                            flush(sheetName, startIndex, inputDimension, pool, values, cli, service, spreadsheet);

                            //Increment ranges.
                            startIndex += values.size();
                            pool += values.size();

                            //Clears values already saved.
                            values.clear();

                            //Identify if has sleep time until next API call.
                            if (cli.getParameterAsInteger("sleep") > 0) {
                                try {
                                    Logger.getLogger(GoogleSheetsExport.class.getName())
                                            .log(Level.INFO, "Sleeping {0} seconds until next API call", cli.getParameterAsInteger("sleep"));

                                    Thread.sleep(Long.valueOf(cli.getParameterAsInteger("sleep") * 1000));
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(GoogleSheetsExport.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }
                    }

                    if (values.size() > 0) {
                        flush(sheetName, startIndex, inputDimension, pool, values, cli, service, spreadsheet);
                    }

                } else {
                    Logger.getLogger(GoogleSheetsExport.class.getName())
                            .log(Level.WARNING, "Sheet {0} was not found on spreadsheet {1}", new Object[]{cli.getParameter("sheet"), spreadsheet.getSpreadsheetId()});
                }
            } else {
                Logger.getLogger(GoogleSheetsExport.class.getName())
                        .log(Level.WARNING, "Cells count is over the limit of {0}", new Object[]{CELLS_LIMIT});
            }
        } else {
            Logger.getLogger(GoogleSheetsExport.class.getName())
                    .warning("Parameter spreadsheet is empty, please set it up.");
        }

        mitt.close();

        Logger.getLogger(GoogleSheetsExport.class.getName())
                .info("Google sheets export finalized.");
    }

    /**
     *
     * @param sheetName
     * @param startIndex
     * @param inputDimension
     * @param pool
     * @param values
     * @param cli
     * @param service
     * @param spreadsheet
     * @throws IOException
     */
    private static void flush(String sheetName, int startIndex, InputDimension inputDimension, int pool, List<List<Object>> values, CommandLineInterface cli, Sheets service, Spreadsheet spreadsheet) throws IOException {
        String rangeStart = sheetName + "!A" + startIndex;
        String rangeEnd = getSpreadSheetColumnName(inputDimension.columnCount()) + pool;
        String range = rangeStart + ":" + rangeEnd;

        ValueRange body = new ValueRange().setValues(values);

        //Identify if it is update or append.
        if (cli.getParameterAsInteger("method") == 0) {
            UpdateValuesResponse result = service
                    .spreadsheets()
                    .values()
                    .update(spreadsheet.getSpreadsheetId(), range, body)
                    .setValueInputOption("USER_ENTERED")
                    .execute();
        } else {
            AppendValuesResponse result = service
                    .spreadsheets()
                    .values()
                    .append(spreadsheet.getSpreadsheetId(), range, body)
                    .setValueInputOption("USER_ENTERED")
                    .execute();
        }
    }

    /**
     * Based on a column count, return the column name to use on Google
     * Spreadsheet.
     *
     * @param columnNumber
     * @return
     */
    public static String getSpreadSheetColumnName(int columnNumber) {
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
     * Define CSV settings.
     *
     * @param headerExtraction extract header from CSV.
     * @return
     */
    public static CsvParserSettings getSettings(boolean headerExtraction) {
        //Define a settings.
        CsvParserSettings parserSettings = new CsvParserSettings();
        parserSettings.setNullValue("");
        parserSettings.setMaxCharsPerColumn(-1);

        //Define format settings.
        parserSettings.getFormat().setDelimiter(';');
        parserSettings.getFormat().setQuote('"');
        parserSettings.getFormat().setQuoteEscape('"');

        //Define the input buffer.
        parserSettings.setInputBufferSize(2 * (1024 * 1024));
        parserSettings.setHeaderExtractionEnabled(headerExtraction);

        return parserSettings;
    }

    /**
     * Execute a request on Google sheets API.
     *
     * @param request
     * @param service
     * @param spreadsheetId
     * @throws IOException
     */
    public static void executeRequest(
            Request request,
            Sheets service,
            String spreadsheetId)
            throws IOException {
        List<Request> requests = new ArrayList();
        requests.add(request);

        BatchUpdateSpreadsheetRequest content = new BatchUpdateSpreadsheetRequest();
        content.setRequests(requests);

        BatchUpdate batchUpdate = service.spreadsheets().batchUpdate(spreadsheetId, content);
        batchUpdate.execute();
    }
}

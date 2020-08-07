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
package br.com.dafiti.googlesheets.writer;

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
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
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
 *
 * @author Helio Leal
 */
public class GoogleSheetsWriter {

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

        Logger.getLogger(GoogleSheetsWriter.class.getName())
                .info("Google Sheets Writer started.");

        //Define the mitt.
        Mitt mitt = new Mitt();

        //Define parameters. 
        mitt.getConfiguration()
                .addParameter("c", "credentials", "Credentials file", "", true, false)
                .addParameter("s", "spreadsheet", "Id of the spreadsheet", "", true, false)
                .addParameter("i", "input", "Input file path and file name", "", true, false)
                .addParameter("t", "tab", "Define tab name", "", true, false)
                .addParameter("sl", "sleep", "(Optional) Sleep time in seconds at one request and another; 0 is default", "0");

        //Read the command line interface. 
        CommandLineInterface cli = mitt.getCommandLineInterface(args);

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
        Sheets sheets = new Sheets.Builder(
                netHttpTransport,
                jsonFactory,
                new AuthorizationCodeInstalledApp(
                        googleAuthorizationCodeFlow,
                        new LocalServerReceiver()).authorize("user"))
                .setApplicationName("Google Sheets Writer API")
                .build();

        //Identifies if spreadsheet is not empty.
        if (!cli.getParameter("spreadsheet").isEmpty()) {

            //Define a settings.
            CsvParserSettings parserSettings = new CsvParserSettings();
            parserSettings.setNullValue("");
            parserSettings.setMaxCharsPerColumn(-1);

            //Define format settings
            parserSettings.getFormat().setDelimiter(';');
            parserSettings.getFormat().setQuote('"');
            parserSettings.getFormat().setQuoteEscape('"');

            //Define the input buffer.
            parserSettings.setInputBufferSize(2 * (1024 * 1024));

            //Define a csv parser.
            CsvParser csvParser = new CsvParser(parserSettings);

            File file = new File(cli.getParameter("input"));

            //Init a parser.
            csvParser.beginParsing(file);

            //Value being processed.
            String[] line;

            List<List<Object>> values = new ArrayList();

            //Process each csv record.
            /*while ((line = csvParser.parseNext()) != null) {
                List<Object> record = new ArrayList();

                for (String column : line) {
                    record.add(column);
                }
                values.add(record);
            }*/
            int x = 1;
            int y = 100000;

            for (int i = 0; i < 5000000; i++) {
                List<Object> record = new ArrayList();
                record.add("A" + i);
                record.add("B" + i);

                values.add(record);

                if (values.size() == 100000) {

                    String rangeInit = cli.getParameter("tab") + "!A" + x;
                    String rangeEnd = "B" + y;
                    String range = rangeInit + ":" + rangeEnd;

                    y = y + values.size();
                    x = x + values.size();

                    ValueRange body = new ValueRange().setValues(values);

                    /*UpdateValuesResponse result = sheets
                            .spreadsheets()
                            .values()
                            .update(cli.getParameter("spreadsheet"), range, body)
                            .setValueInputOption("USER_ENTERED")
                            .execute();*/
                    AppendValuesResponse result = sheets
                            .spreadsheets()
                            .values()
                            .append(cli.getParameter("spreadsheet"), range, body)
                            .setValueInputOption("USER_ENTERED")
                            .execute();

                    System.out.printf("%d rows appended.", result.getUpdates().getUpdatedRows());

                    values.clear();

                    //Identify if has sleep time until next API call.
                    if (cli.getParameterAsInteger("sleep") > 0) {
                        try {
                            Logger.getLogger(GoogleSheetsWriter.class.getName())
                                    .log(Level.INFO, "Sleeping {0} seconds until next API call", cli.getParameterAsInteger("sleep"));

                            Thread.sleep(Long.valueOf(cli.getParameterAsInteger("sleep") * 1000));
                        } catch (InterruptedException ex) {
                            Logger.getLogger(GoogleSheetsWriter.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }

            //System.out.printf("%d cells updated.", result.getUpdatedCells());
        } else {
            Logger.getLogger(GoogleSheetsWriter.class.getName())
                    .warning("Parameter spreadsheet is empty, please set it up.");
        }

        mitt.close();

        Logger.getLogger(GoogleSheetsWriter.class.getName())
                .info("Google Sheets Writer finalized.");
    }

}

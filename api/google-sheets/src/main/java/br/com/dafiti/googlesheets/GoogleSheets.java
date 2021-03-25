/*
 * Copyright (c) 2019 Dafiti Group
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
package br.com.dafiti.googlesheets;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Google sheets extractor.
 *
 * @author Helio Leal
 */
public class GoogleSheets {

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

        Logger.getLogger(GoogleSheets.class.getName())
                .info("Google Sheets API extration started.");

        //Define the mitt.
        Mitt mitt = new Mitt();

        //Define parameters. 
        mitt.getConfiguration()
                .addParameter("c", "credentials", "Credentials file", "", true, false)
                .addParameter("s", "spreadsheet", "Identifies the id of the spreadsheet", "", true, false)
                .addParameter("o", "output", "Identifies the output path and file name", "", true, false)
                .addParameter("k", "key", "Unique key, divided by + if has more than one field", "")
                .addParameter("p", "partition", "Define the partition field or fields, divided by +", "")
                .addParameter("f", "field", "(Optional)fields to be extracted", "")
                .addParameter("sh", "sheets", "(Optional)(Default consider all sheets) Identifies the sheets to extract, divided by +")
                .addParameter("d", "delimiter", "(Optional)(Default is ;) Identifies the delimiter character", ";")
                .addParameter("q", "quote", "(Optional)(Default is \") Identifies the quote character", "\"")
                .addParameter("sr", "skip_row", "(Optional)(Default is 0) Identifies where the sheet header begins", "0")
                .addParameter("t", "timeout", "(Optional)(Default is 5) Identifies the connection timeout", "5");

        //Read the command line interface. 
        CommandLineInterface cli = mitt.getCommandLineInterface(args);

        //Defines output file.
        mitt.setOutputFile(cli.getParameter("output"));

        //Defines fields.
        mitt.getConfiguration()
                .addCustomField("partition_field",
                        new Concat((List) cli.getParameterAsList("partition", "\\+")))
                .addCustomField("custom_primary_key",
                        new Concat((List) cli.getParameterAsList("key", "\\+")))
                .addCustomField("etl_load_date",
                        new Now());

        //Identifies the fields to extract data. 
        List<String> fields = cli.getParameterAsList("field", "\\+");

        //Identifies if it is to consider just some fields.
        if (!fields.isEmpty()) {
            mitt.getConfiguration().addField(fields);
            mitt.getConfiguration().addCustomField("sheet_title");
        }

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

        //Authorize.
        Credential credential = new AuthorizationCodeInstalledApp(googleAuthorizationCodeFlow, new LocalServerReceiver()).authorize("user");

        //Instance of sheets service.
        Sheets sheets = new Sheets.Builder(
                netHttpTransport,
                jsonFactory,
                setHttpTimeout(credential, cli.getParameterAsInteger("timeout"))
        )
                .setApplicationName("Google Sheets API extractor")
                .build();

        //Get the spreadsheet by its code.
        Spreadsheet spreadsheet = sheets
                .spreadsheets()
                .get(cli.getParameter("spreadsheet"))
                .execute();

        Logger.getLogger(GoogleSheets.class.getName())
                .log(Level.INFO,
                        "Retrieving data from spreadsheet {0}",
                        spreadsheet.getProperties().getTitle());

        //Only one sheet header is considered.
        boolean headerOnce = fields.isEmpty();

        //Identifies if user want a specific sheet(s).
        List<String> parameterSheets = cli.getParameterAsList("sheets", "\\+");

        //Loop through each sheet.
        for (Sheet sheet : spreadsheet.getSheets()) {
            Map<String, Integer> header = new HashMap();
            final String sheetName = sheet.getProperties().getTitle();

            //Identifies if it considers all sheets or just some.
            if (parameterSheets.isEmpty() || parameterSheets.contains(sheetName)) {

                Logger.getLogger(GoogleSheets.class.getName())
                        .log(Level.INFO,
                                "Retrieving data from sheet {0}",
                                sheetName);

                //Get the data of each sheet in spreadsheet
                List<List<Object>> values = sheets
                        .spreadsheets()
                        .values()
                        .get(cli.getParameter("spreadsheet"), sheetName)
                        .execute()
                        .getValues();

                //Identifies where the header begins.
                int skip = cli.getParameterAsInteger("skip_row");

                if (headerOnce) {
                    mitt.getConfiguration().addField(values.get(skip));
                    mitt.getConfiguration().addCustomField("sheet_title");
                    headerOnce = false;
                }

                boolean skipFirst = true;
                for (int index = 0; index < values.size(); index++) {
                    //Identifies if shold skip lines.
                    if (index >= skip) {
                        if (!skipFirst) {
                            //Identifies if the field list was provided. 
                            if (!fields.isEmpty()) {
                                ArrayList<String> record = new ArrayList();

                                for (String field : fields) {
                                    if (header.containsKey(field)) {
                                        record.add((String) values
                                                .get(index)
                                                .get(header.get(field)));
                                    } else {
                                        record.add("Unknown field " + field);
                                    }
                                }
                                record.add(sheetName);
                                mitt.write(record);
                            } else {
                                List<Object> sheetValues = values.get(index);
                                List<Object> record = new ArrayList();

                                for (int i = 0; i < header.size(); i++) {
                                    if (sheetValues.size() > i) {
                                        record.add(sheetValues.get(i));
                                    } else {
                                        record.add("");
                                    }
                                }

                                record.add(sheetName);
                                mitt.write(record);
                            }
                        } else {
                            for (int i = 0; i < values.get(index).size(); i++) {
                                header.put(String.valueOf(values.get(index).get(i)), i);
                            }
                        }
                        skipFirst = false;
                    }
                }
            }
        }

        mitt.close();

        Logger.getLogger(GoogleSheets.class.getName())
                .info("Google Sheets API extration finalized.");
    }

    /**
     *
     * @param requestInitializer
     * @return
     */
    private static HttpRequestInitializer setHttpTimeout(final HttpRequestInitializer requestInitializer, final int timeout) {
        return (HttpRequest httpRequest) -> {
            requestInitializer.initialize(httpRequest);
            httpRequest.setConnectTimeout(timeout * 60000);
            httpRequest.setReadTimeout(timeout * 60000);
        };
    }
}

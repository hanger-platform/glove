/*
 * Copyright (c) 2021 Dafiti Group
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
package br.com.dafiti.googlesheets.manager;

import br.com.dafiti.googlesheets.manager.api.GoogleSheetsApi;
import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.CopySheetToAnotherSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.SheetProperties;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 *
 * @author flavialima
 * @author Helio Leal
 */
public class GoogleSheetsManager {

    public static void main(String[] args) throws DuplicateEntityException, IOException, GeneralSecurityException {

        System.out.println("Google sheets manager started.");

        //Define the mitt.
        Mitt mitt = new Mitt();

        //Define parameters. 
        mitt.getConfiguration()
                .addParameter("c", "credentials", "Credentials file", "", true, true)
                .addParameter("s", "spreadsheet", "Spreadsheet ID", "", true, false)
                .addParameter("a", "action", "Action on Google Spreadsheet", "", true, false)
                .addParameter("d", "debug", "(Optional) Identify if it is debug mode; 0 is default", "0");

        //Command Line.
        CommandLineInterface cli = mitt.getCommandLineInterface(args);

        //Authenticate on Google Spreadsheet API.
        GoogleSheetsApi api
                = new GoogleSheetsApi(cli.getParameter("sheet"), cli.getParameterAsInteger("debug") == 1)
                        .authenticate(cli.getParameter("credentials"), cli.getParameter("spreadsheet"));
        
        System.out.println("Google sheets manager finalized.");
    }
}

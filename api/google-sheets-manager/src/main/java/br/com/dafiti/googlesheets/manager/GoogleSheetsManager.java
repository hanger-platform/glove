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

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;

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
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.FileInputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author flavialima
 * @author Helio Leal
 */
public class GoogleSheetsManager {

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

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

        //Define the transport.
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        //Load client secrets.
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(new FileInputStream((String) cli.getParameter("credentials"))));

        //Set up authorization code flow for all authorization scopes.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, DriveScopes.all())
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(System.getProperty("user.home"), ".store/google_drive")))
                .setAccessType("offline")
                .build();

        //Authorize.
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");

        //Build the service object.
        Drive service = new Drive.Builder(httpTransport, JSON_FACTORY, setHttpTimeout(credential))
                .setApplicationName("GLOVE - Google Drive API")
                .build();

        File copyMetadata = new File().setName("SOU UMA COPIA DE PLANILHA VIA JAVA E TENHO VARIAS ABAS");

        File presentationCopyFile
                = service.files().copy(cli.getParameter("spreadsheet"), copyMetadata).execute();

        String presentationCopyId = presentationCopyFile.getId();

        System.out.println("nova planilha criada: " + presentationCopyId);

    }

    /**
     * Set the Google Drive API timeout.
     *
     * @param requestInitializer
     * @return
     */
    private static HttpRequestInitializer setHttpTimeout(final HttpRequestInitializer requestInitializer) {
        return (HttpRequest httpRequest) -> {
            requestInitializer.initialize(httpRequest);
            httpRequest.setConnectTimeout(5 * 60000);
            httpRequest.setReadTimeout(5 * 60000);
        };
    }

}

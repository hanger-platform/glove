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

        // Objetivo
        // Pt 1: Copiar planilha para outra, mantendo os mesmos usurios
        System.out.println("Google sheets manager started.");

        //Mitt
        Mitt mitt = new Mitt();

        //Parâmetros.
        mitt.getConfiguration()
                .addParameter("c", "credentials", "Credentials file", "", true, true)
                .addParameter("s", "spreadsheet", "Spreadsheet ID", "", true, false)
                .addParameter("a", "action", "Action on Google Spreadsheet", "", true, false)
                .addParameter("d", "debug", "(Optional) Identify if it is debug mode; 0 is default", "0");

        //Command Line.
        CommandLineInterface cli = mitt.getCommandLineInterface(args);

        // Autenticar no Google API - Copiar classe de export do Google Sheets export.
        GoogleSheetsApi api
                = new GoogleSheetsApi(cli.getParameter("sheet"), cli.getParameterAsInteger("debug") == 1)
                        .authenticate(cli.getParameter("credentials"), cli.getParameter("spreadsheet"));

        String spreadsheetId = "1Xwaa65wWzK_3z7XuE9AwEwg5jLrWoTVBIIQcdJlSOHg";

        int sheetId = 0;

        String destinationSpreadsheetId = "1ygT8LoQDEVte55pX6MDrwxl0ecMuVSlacEbJx7eXi3w";

        CopySheetToAnotherSpreadsheetRequest requestBody = new CopySheetToAnotherSpreadsheetRequest();
        requestBody.setDestinationSpreadsheetId(destinationSpreadsheetId);

        Sheets sheetsService = createSheetsService();
        Sheets.Spreadsheets.SheetsOperations.CopyTo request
                = sheetsService.spreadsheets().sheets().copyTo(spreadsheetId, sheetId, requestBody);

        SheetProperties response = request.execute();

        // TODO: Change code below to process the `response` object:
        System.out.println(response);

        // Criar um novo metodo na classe de API chamado copy()
        // Parametro dos metodo copy(String spreadhseet id)
        // Mtodo exemplo:
        //api.createSpreadsheet("oi");
    }

    /**
     * Este metodo e um exemplo de estrutura para o metodo original
     */
    private static void copy() {

        // Criar uma nova planilha usando a API do Google 
        // Pesquisar no Google por "Java + Google Sheets API + Create a new spreadsheet"
        // Quando a planilha nova for criada, guardar o valor do iD dela numa variavel.
        String idNewSpreadsheet = "";

        // Copiar a planilha existente para a nova planilha 
        // Outros nomes: clone
        // Pesquisar no Google por "Java + Google Sheets API + Copy/clone a spreadsheet"
        // this.service.copy("id da planilha origem", "id da planilha destino");
        // Apos acoes acima, verificar se os metodos tem a possibilidade de compartilhar 
        // a planilha com os mesmos usuarios da antiga
        // Caso nao seja automatico, devemos buscar por um metodo que faca isso.
    }

    //  Copy
    public static Sheets createSheetsService() throws GeneralSecurityException, IOException {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        GoogleCredential credential = null; //aqui tem que ser o método authenticate

        return new Sheets.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("Google-SheetsSample/0.1")
                .build();
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.junit.Test;

/**
 *
 * @author flavialima
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

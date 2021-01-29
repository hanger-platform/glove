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
import java.io.IOException;

/**
 *
 * @author flavialima
 */
public class GoogleSheetsManager {

    public static void main(String[] args) throws DuplicateEntityException, IOException {

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
        
        // Criar um novo metodo na classe de API chamado copy()
        // Parametro dos metodo copy(String spreadhseet id)
        // Mtodo exemplo:
        //api.createSpreadsheet("teste2");

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
}

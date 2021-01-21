/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.dafiti.googlesheets.manager.model;

/**
 *
 * @author flavialima
 */
public class SpreadsheetDetails {
    
    private int id;
    private int columnCount;
    private int rowCount;
    private String name;

    public SpreadsheetDetails(int id, int columnCount, int rowCount, String name) {
        this.id = id;
        this.columnCount = columnCount;
        this.rowCount = rowCount;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public void setColumnCount(int columnCount) {
        this.columnCount = columnCount;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    
    
}

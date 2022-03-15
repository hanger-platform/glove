package br.com.dafiti.parquet.util;

/**
 *
 * @author Valdiney V GOMES
 */
public class Statistics {

    int rowNumber = 0;
    int inputRows = 0;
    int outputRows = 0;
    int outputUpdatedRows = 0;
    int duplicatedRows = 0;

    public int getRowNumber() {
        return rowNumber;
    }

    public void incrementRowNumber() {
        this.rowNumber += 1;
    }

    public int getInputRows() {
        return inputRows;
    }

    public void incrementInputRows() {
        this.inputRows += 1;
    }

    public int getOutputRows() {
        return outputRows;
    }

    public void incrementOutputRows() {
        this.outputRows += 1;
    }

    public int getOutputUpdatedRows() {
        return outputUpdatedRows;
    }

    public void incrementOutputUpdatedRows() {
        this.outputUpdatedRows += 1;
    }

    public int getDuplicatedRows() {
        return duplicatedRows;
    }

    public void incrementDuplicatedRows() {
        this.duplicatedRows += 1;
    }
}

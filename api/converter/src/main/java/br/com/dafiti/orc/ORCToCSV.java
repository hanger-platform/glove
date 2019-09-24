/*
 * Copyright (c) 2018 Dafiti Group
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
package br.com.dafiti.orc;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;
import org.apache.log4j.Logger;
import org.apache.orc.OrcFile;
import org.apache.orc.Reader;
import org.apache.orc.RecordReader;

/**
 * This class read a plain orc file and write the records into a csv file.
 *
 * @author Guilherme OLIVEIRA
 * @author Valdiney V GOMES
 */
public class ORCToCSV implements Runnable {

    private final File orcFile;
    private final File mergeFile;
    private final Character delimiter;
    private final Character quote;
    private final Character quoteEscape;
    private final boolean replace;
    private final int fieldKey;
    private final boolean duplicated;

    /**
     * Constructor.
     *
     * @param orcFile Orc File
     * @param mergeFile Delta file
     * @param delimiter File delimiter
     * @param quote File quote.
     * @param quoteEscape File escape.
     * @param replace Identify if should replace the orignal file.
     * @param fieldKey Unique key field.
     * @param duplicated Identify if duplicated is allowed.
     */
    public ORCToCSV(
            File orcFile,
            File mergeFile,
            Character delimiter,
            Character quote,
            Character quoteEscape,
            boolean replace,
            int fieldKey,
            boolean duplicated) {

        this.orcFile = orcFile;
        this.mergeFile = mergeFile;
        this.delimiter = delimiter;
        this.quote = quote;
        this.quoteEscape = quoteEscape;
        this.replace = replace;
        this.fieldKey = fieldKey;
        this.duplicated = duplicated;
    }

    /**
     * Convert a orc file to a csv file.
     */
    @Override
    public void run() {
        //Get the orc file path.
        String orcPath = orcFile.getAbsolutePath();

        //Unique key list.
        HashSet<String> key = new HashSet();

        //Statistics.
        int orcRecords = 0;
        int csvRecords = 0;
        int updatedRecords = 0;
        int duplicatedRecords = 0;

        //Log the process init.
        Logger.getLogger(this.getClass()).info("Converting Orc to CSV: " + orcPath);

        try {
            //Define the reader.
            Reader reader = OrcFile.createReader(new Path(orcFile.getAbsolutePath()),
                    OrcFile.readerOptions(new Configuration()));

            //Define the row.
            RecordReader rows = reader.rows();

            //Define the batch.
            VectorizedRowBatch rowBatchReader = reader.getSchema().createRowBatch();

            //Define a csvfile.
            File csvFile = new File(orcPath.replace(orcPath.substring(orcPath.indexOf("."), orcPath.length()), ".csv"));
            csvFile.delete();

            //Define a settings.
            CsvWriterSettings settings = new CsvWriterSettings();
            settings.setNullValue("");
            settings.setMaxCharsPerColumn(-1);

            //Define format settings
            settings.getFormat().setDelimiter(delimiter);
            settings.getFormat().setQuote(quote);
            settings.getFormat().setQuoteEscape(quoteEscape);

            //Define a writer.
            CsvWriter csvWriter = new CsvWriter(csvFile, settings);

            if (mergeFile != null) {
                //Value being processed. 
                String[] record;

                //Define a settings.
                CsvParserSettings parserSettings = new CsvParserSettings();
                parserSettings.setNullValue("");
                parserSettings.setMaxCharsPerColumn(-1);

                //Define format settings
                parserSettings.getFormat().setDelimiter(delimiter);
                parserSettings.getFormat().setQuote(quote);
                parserSettings.getFormat().setQuoteEscape(quoteEscape);

                //Define the input buffer.
                parserSettings.setInputBufferSize(5 * (1024 * 1024));

                //Define a csv parser.
                CsvParser csvParser = new CsvParser(parserSettings);

                //Init a parser.
                csvParser.beginParsing(mergeFile);

                while ((record = csvParser.parseNext()) != null) {
                    boolean add = false;

                    //Identify delta keys.
                    if (fieldKey >= 0 && !duplicated) {
                        add = !key.add(record[fieldKey]);

                        if (add) {
                            System.out.println("Duplicate key in delta file: " + record[fieldKey]);
                            duplicatedRecords++;
                        }
                    }

                    //Write to file. 
                    if (!add) {
                        csvWriter.writeRow(record);
                        csvRecords++;
                    }
                }
            }

            //Read a orc file.
            while (rows.nextBatch(rowBatchReader)) {
                for (int row = 0; row < rowBatchReader.size; ++row) {
                    boolean add = true;

                    orcRecords++;

                    //Define the buffer to stringify values. 
                    StringBuilder value;

                    //Init the record.
                    String[] csvRecord = new String[rowBatchReader.numCols];

                    //Get record values from orc file.
                    for (int col = 0; col < rowBatchReader.numCols; col++) {
                        value = new StringBuilder();
                        rowBatchReader.cols[col].stringifyValue(value, row);
                        csvRecord[col] = value.toString();
                    }

                    //Identify if can add a record.
                    if (fieldKey >= 0) {
                        add = !key.contains(csvRecord[fieldKey]);

                        if (!add) {
                            updatedRecords++;
                        }
                    }

                    //Write to file.
                    if (add) {
                        csvWriter.writeRow(csvRecord);
                    }
                }
            }

            //Print on console.
            System.out.println("Records: "
                    + orcRecords
                    + ", Delta: "
                    + csvRecords
                    + ", ( Updated: " + updatedRecords + ", Inserted: " + (csvRecords - updatedRecords) + ", Duplicated:" + duplicatedRecords + " )"
                    + " Final: "
                    + (orcRecords + (csvRecords - updatedRecords)));

            //Flush the output strean. 
            csvWriter.flush();

            //Close the output strean. 
            csvWriter.close();

            //Close the reader.
            rows.close();

            //Identify if should remove orc file. 
            if (replace) {
                orcFile.delete();
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass()).error("Error [" + ex + "] converting Orc to CSV");
            Logger.getLogger(this.getClass()).error(Arrays.toString(ex.getStackTrace()));

            System.exit(1);
        }
    }
}

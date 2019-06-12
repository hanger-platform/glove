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
package br.com.dafiti.parquet;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetReader;

/**
 * This class read a plain parquet file and write the records into a csv file.
 *
 * @author Valdiney V GOMES
 */
public class ParquetToCSV implements Runnable {

    private final File parquetFile;
    private final File mergeFile;
    private final Character delimiter;
    private final Character quote;
    private final Character escape;
    private final boolean replace;
    private final int fieldKey;
    private final boolean duplicated;

    /**
     * Constructor.
     *
     * @param parquetFile Parquet File
     * @param mergeFile Delta file
     * @param delimiter File delimiter
     * @param quote File quote.
     * @param escape File escape.
     * @param replace Identify if should replace the orignal file.
     * @param fieldKey Unique key field.
     * @param duplicated Identify if duplicated is allowed.
     */
    public ParquetToCSV(
            File parquetFile,
            File mergeFile,
            Character delimiter,
            Character quote,
            Character escape,
            boolean replace,
            int fieldKey,
            boolean duplicated) {

        this.parquetFile = parquetFile;
        this.mergeFile = mergeFile;
        this.delimiter = delimiter;
        this.quote = quote;
        this.escape = escape;
        this.replace = replace;
        this.fieldKey = fieldKey;
        this.duplicated = duplicated;
    }

    /**
     * Convert a parquet file to a csv file.
     */
    @Override
    public void run() {
        //Get the parquet file path.
        String parquetPath = parquetFile.getAbsolutePath();

        //Unique key list.
        HashSet<String> key = new HashSet();

        //Statistics.
        int parquetRecords = 0;
        int csvRecords = 0;
        int updatedRecords = 0;
        int duplicatedRecords = 0;

        //Log the process init.
        Logger.getLogger(this.getClass()).info("Converting Parquet to CSV: " + parquetPath);

        try {
            GenericRecord row;
            List<Field> fields = null;
            int fieldCount = 1;

            //Define the reader.
            ParquetReader<GenericRecord> parquetReader = AvroParquetReader.<GenericRecord>builder(new Path(parquetPath))
                    .disableCompatibility()
                    .build();

            //Define a csvfile.
            File csvFile = new File(parquetPath.replace(parquetPath.substring(parquetPath.indexOf("."), parquetPath.length()), ".csv"));
            csvFile.delete();

            //Define a settings.
            CsvWriterSettings settings = new CsvWriterSettings();
            settings.setNullValue("");
            settings.setMaxCharsPerColumn(-1);

            //Define format settings
            settings.getFormat().setDelimiter(delimiter);
            settings.getFormat().setQuote(quote);
            settings.getFormat().setQuoteEscape(escape);

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
                parserSettings.getFormat().setQuoteEscape(escape);

                //Define the input buffer.
                parserSettings.setInputBufferSize(3 * (1024 * 1024));

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

            //Read a parquet file.
            while ((row = parquetReader.read()) != null) {
                boolean add = true;
                parquetRecords++;

                //List the schema fields.
                if (fields == null) {
                    fields = row.getSchema().getFields();

                    //Get the number of column in schema.
                    fieldCount = fields.size();
                }

                //Init the record.
                String[] csvRecord = new String[fieldCount];

                //Get the record values from parquet file.
                for (Schema.Field field : fields) {
                    Object value = row.get(field.pos());
                    csvRecord[field.pos()] = (value == null) ? "" : value.toString();
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

            //Print on console.
            System.out.println("Records: "
                    + parquetRecords
                    + ", Delta: "
                    + csvRecords
                    + ", ( Updated: " + updatedRecords + ", Inserted: " + (csvRecords - updatedRecords) + ", Duplicated:" + duplicatedRecords + " )"
                    + " Final: "
                    + (parquetRecords + (csvRecords - updatedRecords)));

            //Flush the output strean. 
            csvWriter.flush();

            //Close the output strean. 
            csvWriter.close();

            //Close the output stream.
            parquetReader.close();

            //Identify if should remove parquet file. 
            if (replace) {
                parquetFile.delete();
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass()).error("Error [" + ex + "] converting Parquet to CSV");
            Logger.getLogger(this.getClass()).error(Arrays.toString(ex.getStackTrace()));

            System.exit(1);
        }
    }
}

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
package br.com.dafiti.csv;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 * This class read a plain csv and write into a plain csv file.
 *
 * @author Valdiney V GOMES
 */
public class CSVSplitter implements Runnable {

    private final File csvFile;
    private final int partitionColumn;
    private final Character delimiter;
    private final Character quote;
    private final Character quoteEscape;
    private final boolean header;
    private final boolean replace;

    /**
     * Constructor.
     *
     * @param csvFile Orc File
     * @param fieldPartition Partition field
     * @param delimiter File delimiter
     * @param quote File quote.
     * @param quoteEscape File escape.
     * @param header Identify if the file has header.
     * @param replace Identify if should replace the orignal file.
     */
    public CSVSplitter(
            File csvFile,
            int fieldPartition,
            Character delimiter,
            Character quote,
            Character quoteEscape,
            boolean header,
            boolean replace) {

        this.csvFile = csvFile;
        this.partitionColumn = fieldPartition;
        this.delimiter = delimiter;
        this.quote = quote;
        this.quoteEscape = quoteEscape;
        this.replace = replace;
        this.header = header;
    }

    /**
     * Convert a csv file to a csv file.
     */
    @Override
    public void run() {
        //Line being processed.
        int rowNumber = 0;

        //Get the csv file path.
        String csvPath = csvFile.getAbsolutePath();

        //Log the process init.
        Logger.getLogger(this.getClass()).info("Converting CSV to CSV: " + csvPath);

        try {
            String[] record;

            //Define the writer cache.
            HashMap<String, CsvWriter> partitionMap = new HashMap<>();

            //Define a writer settings.
            CsvWriterSettings writerSettings = new CsvWriterSettings();
            writerSettings.setNullValue("");
            writerSettings.setMaxCharsPerColumn(-1);

            //Define format settings
            writerSettings.getFormat().setDelimiter(delimiter);
            writerSettings.getFormat().setQuote(quote);
            writerSettings.getFormat().setQuoteEscape(quoteEscape);

            //Define a parser settings.
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
            csvParser.beginParsing(csvFile);

            //Process each csv line.
            while ((record = csvParser.parseNext()) != null) {
                //Ignore the header.
                if (!(rowNumber == 0 && header)) {
                    String partitionValue = record[partitionColumn];

                    //Identify if partition is in cache.
                    if (!partitionMap.containsKey(partitionValue)) {                        
                        //Create the partition file.
                        Path path = Paths.get(csvFile.getParent() + "/" + partitionValue.replaceAll("\\W", "") + ".csv");
                        Writer partitionFile = Files.newBufferedWriter(path, Charset.forName("UTF-8"), StandardOpenOption.APPEND);
                        
                        //Put the partition handler in cache.
                        partitionMap.put(partitionValue,
                                new CsvWriter(partitionFile, writerSettings));
                    }

                    //Write to file.
                    partitionMap.get(partitionValue).writeRow(record);
                }

                //Identify the record being processed.
                rowNumber++;
            }

            //Flush and close the output stream.
            partitionMap.forEach((k, v) -> {
                v.flush();
                v.close();
            }
            );

            //Identify if should remove csv file. 
            if (replace) {
                csvFile.delete();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Logger.getLogger(this.getClass()).error("Error [" + ex + "] converting CSV to CSV");

            System.exit(1);
        }
    }
}

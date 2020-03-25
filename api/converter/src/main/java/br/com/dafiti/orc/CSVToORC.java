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

import br.com.dafiti.datalake.S3;
import br.com.dafiti.util.Statistics;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.exec.vector.BytesColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.DecimalColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.DoubleColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.LongColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.TimestampColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;
import org.apache.hadoop.hive.serde2.io.DateWritable;
import org.apache.hadoop.hive.serde2.io.HiveDecimalWritable;
import org.apache.log4j.Logger;
import org.apache.orc.CompressionKind;
import org.apache.orc.OrcFile;
import org.apache.orc.OrcFile.EncodingStrategy;
import org.apache.orc.Reader;
import org.apache.orc.RecordReader;
import org.apache.orc.TypeDescription;
import org.apache.orc.TypeDescription.Category;
import org.apache.orc.Writer;
import org.apache.orc.mapred.OrcTimestamp;

/**
 * This class read a csv file and write the records into a orc file
 *
 * @author Guilherme OLIVEIRA
 * @author Valdiney V GOMES
 */
public class CSVToORC implements Runnable {

    private final File inputFile;
    private final TypeDescription schema;
    private final CompressionKind compression;
    private final Character delimiter;
    private final Character quote;
    private final Character quoteEscape;
    private final boolean replace;
    private final boolean header;
    private final int fieldKey;
    private final boolean duplicated;
    private final boolean merge;
    private final String bucketPath;
    private final String mode;
    private final boolean debug;

    /**
     * Constructor.
     *
     * @param inputFile CSV file.
     * @param compression Orc file compression.
     * @param delimiter File delimiter.
     * @param quote File quote.
     * @param quoteEscape File escape.
     * @param schema Avro schema file.
     * @param header Identify if the file has header.
     * @param replace Identify if should replace the orignal file.
     * @param fieldKey Unique key field.
     * @param duplicated Identify if duplicated is allowed
     * @param merge Identify if should merge existing files.
     * @param bucket Identify storage bucket.
     * @param mode Identify partition mode.
     * @param debug Identify if should show detailed log message.
     */
    public CSVToORC(
            File inputFile,
            String compression,
            Character delimiter,
            Character quote,
            Character quoteEscape,
            TypeDescription schema,
            boolean header,
            boolean replace,
            int fieldKey,
            boolean duplicated,
            boolean merge,
            String bucket,
            String mode,
            boolean debug) {

        this.inputFile = inputFile;
        this.schema = schema;
        this.delimiter = delimiter;
        this.quote = quote;
        this.quoteEscape = quoteEscape;
        this.replace = replace;
        this.header = header;
        this.fieldKey = fieldKey;
        this.duplicated = duplicated;
        this.merge = merge;
        this.bucketPath = bucket;
        this.mode = mode;
        this.debug = debug;

        switch (compression) {
            case "lz4":
                this.compression = CompressionKind.LZ4;
                break;
            case "lzo":
                this.compression = CompressionKind.LZO;
                break;
            case "snappy":
                this.compression = CompressionKind.SNAPPY;
                break;
            case "zlib":
                this.compression = CompressionKind.ZLIB;
                break;
            case "none":
                this.compression = CompressionKind.NONE;
                break;
            default:
                this.compression = CompressionKind.ZLIB;
                break;
        }
    }

    /**
     * Convert a csv file to a orc file based on determined struct.
     */
    @Override
    public void run() {
        //Log the process init.
        Logger.getLogger(this.getClass()).info("Converting CSV to Parquet: " + inputFile.getAbsolutePath());

        try {
            String path = FilenameUtils.getFullPath(inputFile.getAbsolutePath());
            String name = FilenameUtils.getBaseName(inputFile.getName());

            //Get the parquet and crc file.
            File orcFile = new File(path + name + "." + this.compression.toString().toLowerCase() + ".orc");
            File originalFile = new File(path + name + ".original.orc");

            //Unique key list.
            HashSet<String> key = new HashSet();

            //Defines a statistics object;
            Statistics statistics = new Statistics();

            //Remove old files. 
            Files.deleteIfExists(orcFile.toPath());

            if (merge) {
                Files.deleteIfExists(originalFile.toPath());
            }

            //Define the writer.
            Writer orcWriter = OrcFile.createWriter(new Path(orcFile.getAbsolutePath()), OrcFile
                    .writerOptions(new Configuration())
                    .setSchema(this.schema)
                    .compress(this.compression)
                    .encodingStrategy(EncodingStrategy.SPEED));

            //Define the vectorized row batch.
            VectorizedRowBatch rowBatchWriter = schema.createRowBatch();

            //Convert to parquet.
            if (inputFile.isDirectory()) {
                File[] files = inputFile.listFiles();

                for (File file : files) {
                    this.toOrc(file, orcWriter, rowBatchWriter, key, statistics);
                }
            } else {
                this.toOrc(inputFile, orcWriter, rowBatchWriter, key, statistics);
            }

            if (merge) {
                String object = orcFile.getName();

                //Download the original object. 
                new S3().downloadObject(bucketPath, object, mode, originalFile);

                //Identify if the original file was downloaded. 
                if (originalFile.exists()) {
                    //Value being processed.
                    String value;
                    int row = 0;

                    //Define the reader.
                    Reader reader = OrcFile.createReader(new Path(originalFile.getAbsolutePath()), OrcFile.readerOptions(new Configuration()));

                    //Define the row.
                    RecordReader rows = reader.rows();

                    //Define the batch.
                    VectorizedRowBatch rowBatchReader = reader.getSchema().createRowBatch();

                    //List the field name.
                    List<String> fieldNames = schema.getFieldNames();

                    //Read a orc file.
                    while (rows.nextBatch(rowBatchReader)) {
                        for (int originalFileRow = 0; originalFileRow < rowBatchReader.size; ++originalFileRow) {
                            boolean add = true;

                            //Identify merge file records.
                            statistics.incrementOutputRows();

                            //Define the buffer to stringify values. 
                            StringBuilder readerValue;

                            //Identify if can add a record.
                            if (fieldKey >= 0 && !duplicated) {
                                readerValue = new StringBuilder();
                                rowBatchReader.cols[fieldKey].stringifyValue(readerValue, originalFileRow);

                                //Get the field value.
                                value = readerValue.toString().replaceAll("^\"|\"$", "");

                                //Idenfify if should insert or update a record.
                                add = !key.contains(value);
                            }

                            if (add) {
                                //Identify the batch size. 
                                row = rowBatchWriter.size++;

                                for (int column = 0; column < fieldNames.size(); column++) {
                                    //stringify the value.
                                    readerValue = new StringBuilder();
                                    rowBatchReader.cols[column].stringifyValue(readerValue, originalFileRow);

                                    //Get the field value.
                                    value = readerValue.toString();

                                    //Get field category.
                                    Category category = schema.getChildren().get(column).getCategory();

                                    //Identify if the field is empty.
                                    if (value == null || value.isEmpty()) {
                                        value = null;
                                    }

                                    //Write data into a row batch. 
                                    this.rowBatchAppend(value, category, row, column, rowBatchWriter);
                                }

                                //Write data into orc file.
                                if (rowBatchWriter.size == rowBatchWriter.getMaxSize()) {
                                    orcWriter.addRowBatch(rowBatchWriter);
                                    rowBatchWriter.reset();
                                }
                            } else {
                                statistics.incrementOutputUpdatedRows();
                            }

                            //Identify the record being processed.
                            statistics.incrementRowNumber();
                        }
                    }
                }
            }

            //Write remaining data into orc file.
            if (rowBatchWriter.size != 0) {
                orcWriter.addRowBatch(rowBatchWriter);
                rowBatchWriter.reset();
            }

            //Close the orc file.
            orcWriter.close();

            //Identifies if the csv file is empty.
            if (statistics.getRowNumber() == 0) {
                throw new Exception("Empty csv file!");
            } else {
                //Print on console.
                System.out.println("[" + orcFile.getName() + "] records: "
                        + statistics.getOutputRows()
                        + ", Delta: "
                        + statistics.getInputRows() + statistics.getDuplicatedRows()
                        + ", ( Updated: " + statistics.getOutputUpdatedRows() + ", Inserted: " + (statistics.getInputRows() - statistics.getOutputUpdatedRows()) + ", Duplicated:" + statistics.getDuplicatedRows() + " )"
                        + " Final: "
                        + (statistics.getOutputRows() + (statistics.getInputRows() - statistics.getOutputUpdatedRows())));
            }

            //Remove the original file.
            if (merge) {
                Files.deleteIfExists(originalFile.toPath());
            }

            //Identify if should remove csv file. 
            if (replace) {
                if (inputFile.isDirectory()) {
                    FileUtils.deleteDirectory(inputFile);
                } else {

                    Files.deleteIfExists(inputFile.toPath());
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass()).error("Error [" + ex + "] converting CSV to ORC");
            Logger.getLogger(this.getClass()).error(Arrays.toString(ex.getStackTrace()));

            System.exit(1);
        }
    }

    /**
     * Append a value int a specific row and column.
     *
     * @param value Value
     * @param category Category
     * @param column Column
     * @param row Row
     * @param batchWriter BatchWriter
     * @throws ParseException
     */
    private void rowBatchAppend(
            String value,
            Category category,
            int row,
            int column,
            VectorizedRowBatch batchWriter) throws ParseException {

        //Identify if the value is null.
        if (value == null) {
            batchWriter.cols[column].noNulls = false;
            batchWriter.cols[column].isNull[row] = true;
        } else {
            //Convert values to fit the category.
            switch (category) {
                case BOOLEAN:
                    ((LongColumnVector) batchWriter.cols[column]).vector[row] = BooleanUtils.toBoolean(value) ? 1 : 0;
                    break;
                case DATE:
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                    ((LongColumnVector) batchWriter.cols[column]).vector[row] = new DateWritable((Date) format.parse(value)).getDays();
                    break;
                case TIMESTAMP:
                    ((TimestampColumnVector) batchWriter.cols[column]).set(row, new OrcTimestamp(value));
                    break;
                case INT:
                    ((LongColumnVector) batchWriter.cols[column]).vector[row] = NumberUtils.toInt(value);
                    break;
                case LONG:
                    ((LongColumnVector) batchWriter.cols[column]).vector[row] = NumberUtils.toLong(value);
                    break;
                case FLOAT:
                    ((DoubleColumnVector) batchWriter.cols[column]).vector[row] = NumberUtils.toFloat(value);
                    break;
                case DOUBLE:
                    ((DoubleColumnVector) batchWriter.cols[column]).vector[row] = NumberUtils.toDouble(value);
                    break;
                case DECIMAL:
                    ((DecimalColumnVector) batchWriter.cols[column]).set(row, new HiveDecimalWritable(value));
                    break;
                case STRING:
                    ((BytesColumnVector) batchWriter.cols[column]).setVal(row, value.replaceAll("^\"|\"$", "").getBytes());
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Convert a file to orc.
     *
     * @param file CSV File.
     * @param orcWriter Orc Writer.
     * @param rowBatchWriter Row Batch Writer.
     * @param key Unique keys mapper.
     * @param statistics Statistics collector.
     * @throws Exception
     */
    private void toOrc(
            File file,
            Writer orcWriter,
            VectorizedRowBatch rowBatchWriter,
            HashSet<String> key,
            Statistics statistics) throws IOException, Exception {

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
        csvParser.beginParsing(file);

        //Process each csv line.
        String[] record;

        while ((record = csvParser.parseNext()) != null) {
            boolean add = false;

            //Identifies if the csv the field count specified in the schema.                
            if (record.length < rowBatchWriter.numCols) {
                throw new Exception(file.getName() + " expected " + rowBatchWriter.numCols + " fields, but received only " + record.length + " : " + String.join("|", record));
            }

            //Identify insert rule. 
            if (fieldKey < 0 || duplicated) {
                add = true;
            } else if (fieldKey >= 0 && !duplicated) {
                add = key.add(record[fieldKey]);
            }

            //Identify duplicated key.
            if (!add) {
                if (debug) {
                    System.out.println("Duplicated key in file: [" + record[fieldKey] + "]");
                }
                statistics.incrementDuplicatedRows();
            } else {
                statistics.incrementInputRows();
            }

            //Ignore the header.
            if (!(statistics.getRowNumber() == 0 && header) && add) {
                //Identify the batch size. 
                int row = rowBatchWriter.size++;

                for (int column = 0; column < schema.getFieldNames().size(); column++) {
                    String value = "";

                    //Get field category.
                    Category category = schema.getChildren().get(column).getCategory();

                    //Get field value.
                    if ((record.length - 1) >= column) {
                        value = record[column];
                    }

                    //Identify if the field is empty.
                    if (value == null || value.isEmpty()) {
                        value = null;
                    }

                    //Write data into a row batch. 
                    this.rowBatchAppend(value, category, row, column, rowBatchWriter);
                }

                //Write data into orc file.
                if (rowBatchWriter.size == rowBatchWriter.getMaxSize()) {
                    orcWriter.addRowBatch(rowBatchWriter);
                    rowBatchWriter.reset();
                }
            }

            //Identify the record being processed.
            statistics.incrementRowNumber();
        }

        //Stop the parser.
        csvParser.stopParsing();
    }
}

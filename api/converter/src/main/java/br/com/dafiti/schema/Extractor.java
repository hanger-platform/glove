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
package br.com.dafiti.schema;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.log4j.Logger;
import org.json.JSONException;

/**
 * This class read a csv file, infer and write the parquet schema.
 *
 * @author Valdiney V GOMES
 */
public class Extractor implements Runnable {

    public static final int MAX_SAMPLE = 1000000;

    private final File file;
    private final File reserverWordsFile;
    private List<String> fieldList;
    private final ArrayList<String[]> fieldContent;
    private final ArrayList<String> fieldSchema;
    private final ArrayList<String> fieldDataType;
    private final ArrayList<String> fieldMetadata;
    private JSONArray jsonMetadata;
    private final Character delimiter;
    private final Character quote;
    private final Character escape;
    private String outputPath;
    private final String dialect;
    private final boolean hasHeader;
    private int sample;
    private String rowOnTheFly;

    /**
     * Constructor.
     *
     * @param csvFile CSV file.
     * @param delimiter File delimiter.
     * @param quote File quote.
     * @param escape File escape.
     * @param csvField Header fields.
     * @param metadata Table metadata.
     * @param outputFolder Metadata output folder.
     * @param dialect Identify the metadata dialect.
     * @param sample Sample de dados a ser analizado para definição de data
     * types.
     * @param reserverWordsFile Identify the reserved words file list.
     */
    public Extractor(
            File csvFile,
            Character delimiter,
            Character quote,
            Character escape,
            String csvField,
            String metadata,
            String outputFolder,
            String dialect,
            int sample,
            File reserverWordsFile) {

        this.fieldList = new ArrayList();
        this.fieldContent = new ArrayList();
        this.fieldSchema = new ArrayList();
        this.fieldDataType = new ArrayList();
        this.fieldMetadata = new ArrayList();
        this.jsonMetadata = new JSONArray();
        this.file = csvFile;
        this.delimiter = delimiter;
        this.quote = quote;
        this.escape = escape;
        this.outputPath = outputFolder;
        this.dialect = dialect;
        this.hasHeader = csvField.isEmpty();
        this.reserverWordsFile = reserverWordsFile;
        this.rowOnTheFly = "";

        //Limit the data sample.
        if (sample > MAX_SAMPLE) {
            this.sample = MAX_SAMPLE;
        }

        //Identify the output path.
        if (outputFolder.isEmpty()) {
            this.outputPath = csvFile.getParent().concat("/");
        }

        //Get the header fields passed by parameter. 
        if (!this.hasHeader) {
            this.fieldList = Arrays.asList(csvField.replace(" ", "").split(Pattern.quote(",")));
        }

        //Get the fields type passed by parameter. 
        if (!metadata.isEmpty()) {
            jsonMetadata = new JSONArray(metadata);
        }
    }

    /**
     * Extract the metadata from a csv file.
     */
    @Override
    public void run() {
        try {
            String[] row;

            //Define a settings.
            CsvParserSettings settings = new CsvParserSettings();
            settings.setNullValue("");
            settings.setMaxCharsPerColumn(-1);

            //Define format settings
            settings.getFormat().setDelimiter(delimiter);
            settings.getFormat().setQuote(quote);
            settings.getFormat().setQuoteEscape(escape);

            //Define a csv parser.
            CsvParser csvParser = new CsvParser(settings);

            //Init a parser.
            csvParser.beginParsing(file);

            //Process each csv line.
            while ((row = csvParser.parseNext()) != null) {
                //Identify if the file have a header
                if (hasHeader && fieldList.isEmpty()) {
                    fieldList.addAll(Arrays.asList(row));
                } else {
                    //Get a limited, but not too limited, data sample from the file. 
                    fieldContent.add(row);

                    if (fieldContent.size() == sample) {
                        break;
                    }
                }
            }

            //Stop the parser.
            csvParser.stopParsing();

            //Identify the reserved words. 
            List<String> reservedWords = new ArrayList();

            if (reserverWordsFile != null) {
                try (Scanner scanner = new Scanner(reserverWordsFile)) {
                    while (scanner.hasNext()) {
                        reservedWords.add(scanner.next());
                    }

                    System.out.println(reservedWords.size() + " reserved words loaded from " + reserverWordsFile.getAbsolutePath());
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }

            //Process each column. 
            for (int column = 0; column < fieldList.size(); column++) {
                String type = "";
                int length = 0;
                int stringField = 0;
                int integerField = 0;
                int doubleField = 0;
                int nullField = 0;
                boolean hasMetadata = false;

                //Get the field name.
                String field = fieldList
                        .get(column)
                        .replaceAll("\\s", "")
                        .replaceAll("\\W", "_")
                        .replaceAll("^_", "")
                        .toLowerCase();

                //Get field properties from metadata. 
                for (int i = 0; i < jsonMetadata.length(); i++) {
                    JSONObject metadata = jsonMetadata.getJSONObject(i);
                    hasMetadata = metadata.getString("field").equalsIgnoreCase(field);

                    if (hasMetadata) {
                        type = metadata.has("type") ? metadata.getString("type") : "";
                        length = metadata.has("length") ? metadata.getInt("length") : 0;

                        break;
                    }
                }

                //Concat postfix _rw to field name that is a reserved word. 
                if (reservedWords.contains(field.toLowerCase())) {
                    System.out.println("Reserved word found at " + field + " and replaced by " + field + "_rw");
                    field = field.concat("_rw");
                }

                //Identify if have a metadata. 
                if (!hasMetadata) {
                    //Process each record of each column.
                    for (int content = 0; content < fieldContent.size(); content++) {
                    	rowOnTheFly = String.join(" | ", fieldContent.get(content));
                    	
                        //Get the field value. 
                        String value = fieldContent.get(content)[column] == null ? "" : fieldContent.get(content)[column];

                        //Calculate the number of occurrences of each data type.
                        if (value.matches("^[-+]?[0-9]*") && !(value.isEmpty() || value.equals("-") || value.equals("+") || (!value.equals("0") && value.matches("^0[0-9]*"))) && value.length() <= 19) {
                            //Match integer.
                            integerField = integerField + 1;
                            length = value.length() > length ? value.length() : length;

                        } else if (value.matches("^[-+]?[0-9]*\\.[0-9]+([eE][-+]?[0-9]+)?") && !(value.isEmpty() || value.equals("-") || value.equals("+") || (!value.equals("0") && value.matches("^0[0-9]*")))) {
                            //Match double. 
                            doubleField = doubleField + 1;
                            length = value.length() > length ? value.length() : length;

                        } else if (value.isEmpty()) {
                            //Match null.
                            nullField = nullField + 1;

                        } else {
                            //Match string. 
                            stringField = stringField + 1;
                            length = value.getBytes().length > length ? value.getBytes().length : length;
                        }
                    }

                    //Identify the field type and size based on the number of ocurrences of each data type.
                    if ((nullField > 0 && stringField == 0 && integerField == 0 && doubleField == 0)) {
                        fieldMetadata.add("{\"field\":\"" + field + "\",\"type\":\"string\",\"length\":255}");

                        switch (dialect) {
                            case "spectrum":
                            case "athena":
                                fieldSchema.add("{\"name\":\"" + field + "\",\"type\":[\"null\",\"string\"],\"default\":null}");
                                fieldDataType.add(field + " " + "varchar(255)");
                                break;
                            case "redshift":
                                fieldDataType.add(field + " " + "varchar(255) ENCODE ZSTD");
                                break;
                            case "bigquery":
                                fieldSchema.add("{\"name\":\"" + field + "\",\"type\":\"STRING\"}");
                                break;
                        }

                    } else if (stringField > 0) {
                        int justifiedLength = (length * 2) > 65000 ? 65000 : (length * 2);

                        fieldMetadata.add("{\"field\":\"" + field + "\",\"type\":\"string\",\"length\":" + justifiedLength + "}");

                        switch (dialect) {
                            case "spectrum":
                            case "athena":
                                fieldSchema.add("{\"name\":\"" + field + "\",\"type\":[\"null\",\"string\"],\"default\":null}");
                                fieldDataType.add(field + " " + "varchar(" + justifiedLength + ")");
                                break;
                            case "redshift":
                                fieldDataType.add(field + " " + "varchar(" + justifiedLength + ") ENCODE ZSTD");
                                break;
                            case "bigquery":
                                fieldSchema.add("{\"name\":\"" + field + "\",\"type\":\"STRING\"}");
                                break;
                        }

                    } else if (integerField > 0 && stringField == 0 && doubleField == 0) {
                        fieldMetadata.add("{\"field\":\"" + field + "\",\"type\":\"integer\"}");

                        switch (dialect) {
                            case "spectrum":
                            case "athena":
                                fieldSchema.add("{\"name\":\"" + field + "\",\"type\":[\"null\",\"long\"],\"default\":null}");
                                fieldDataType.add(field + " " + "bigint");
                                break;
                            case "redshift":
                                fieldDataType.add(field + " " + "bigint ENCODE AZ64");
                                break;
                            case "bigquery":
                                fieldSchema.add("{\"name\":\"" + field + "\",\"type\":\"INTEGER\"}");
                                break;
                        }

                    } else if (doubleField > 0 && stringField == 0) {
                        fieldMetadata.add("{\"field\":\"" + field + "\",\"type\":\"number\"}");

                        switch (dialect) {
                            case "spectrum":
                                fieldSchema.add("{\"name\":\"" + field + "\",\"type\":[\"null\",\"double\"],\"default\":null}");
                                fieldDataType.add(field + " " + "double precision");
                                break;
                            case "athena":
                                fieldSchema.add("{\"name\":\"" + field + "\",\"type\":[\"null\",\"double\"],\"default\":null}");
                                fieldDataType.add(field + " " + "double");
                                break;
                            case "redshift":
                                fieldDataType.add(field + " " + "double precision ENCODE ZSTD");
                                break;
                            case "bigquery":
                                fieldSchema.add("{\"name\":\"" + field + "\",\"type\":\"FLOAT\"}");
                                break;
                        }
                    }
                } else {
                    switch (type) {
                        case "string":
                            int justifiedLength = length > 65000 ? 65000 : length;

                            fieldMetadata.add("{\"field\":\"" + field + "\",\"type\":\"string\",\"length\":" + justifiedLength + "}");

                            switch (dialect) {
                                case "spectrum":
                                case "athena":
                                    fieldSchema.add("{\"name\":\"" + field + "\",\"type\":[\"null\",\"string\"],\"default\":null}");
                                    fieldDataType.add(field + " " + "varchar(" + justifiedLength + ")");
                                    break;
                                case "redshift":
                                    fieldDataType.add(field + " " + "varchar(" + justifiedLength + ")  ENCODE ZSTD");
                                    break;
                                case "bigquery":
                                    fieldSchema.add("{\"name\":\"" + field + "\",\"type\":\"STRING\"}");
                                    break;
                            }

                            break;
                        case "integer":
                        case "long":
                            fieldMetadata.add("{\"field\":\"" + field + "\",\"type\":\"integer\"}");

                            switch (dialect) {
                                case "spectrum":
                                case "athena":
                                    fieldSchema.add("{\"name\":\"" + field + "\",\"type\":[\"null\",\"long\"],\"default\":null}");
                                    fieldDataType.add(field + " " + "bigint");
                                    break;
                                case "redshift":
                                    fieldDataType.add(field + " " + "bigint ENCODE AZ64");
                                    break;
                                case "bigquery":
                                    fieldSchema.add("{\"name\":\"" + field + "\",\"type\":\"INTEGER\"}");
                                    break;
                            }

                            break;
                        case "number":
                        case "bignumber":
                            fieldMetadata.add("{\"field\":\"" + field + "\",\"type\":\"number\"}");

                            switch (dialect) {
                                case "spectrum":
                                    fieldSchema.add("{\"name\":\"" + field + "\",\"type\":[\"null\",\"double\"],\"default\":null}");
                                    fieldDataType.add(field + " " + "double precision");
                                    break;
                                case "athena":
                                    fieldSchema.add("{\"name\":\"" + field + "\",\"type\":[\"null\",\"double\"],\"default\":null}");
                                    fieldDataType.add(field + " " + "double");
                                    break;
                                case "redshift":
                                    fieldDataType.add(field + " " + "double precision ENCODE ZSTD");
                                    break;
                                case "bigquery":
                                    fieldSchema.add("{\"name\":\"" + field + "\",\"type\":\"FLOAT\"}");
                                    break;
                            }

                            break;
                        case "timestamp":
                        case "date":
                            fieldMetadata.add("{\"field\":\"" + field + "\",\"type\":\"string\",\"length\":19}");

                            switch (dialect) {
                                case "spectrum":
                                case "athena":
                                    fieldSchema.add("{\"name\":\"" + field + "\",\"type\":[\"null\",\"string\"],\"default\":null}");
                                    fieldDataType.add(field + " " + "varchar(19)");
                                    break;
                                case "redshift":
                                    fieldDataType.add(field + " " + "varchar(19) ENCODE ZSTD");
                                    break;
                                case "bigquery":
                                    fieldSchema.add("{\"name\":\"" + field + "\",\"type\":\"STRING\"}");
                                    break;
                            }

                            break;
                        case "boolean":
                            fieldMetadata.add("{\"field\":\"" + field + "\",\"type\":\"boolean\"}");

                            switch (dialect) {
                                case "spectrum":
                                case "athena":
                                    fieldSchema.add("{\"name\":\"" + field + "\",\"type\":[\"null\",\"boolean\"],\"default\":null}");
                                    fieldDataType.add(field + " " + "boolean");
                                    break;
                                case "redshift":
                                    fieldDataType.add(field + " " + "boolean ENCODE ZSTD");
                                    break;
                                case "bigquery":
                                    fieldSchema.add("{\"name\":\"" + field + "\",\"type\":\"BOOLEAN\"}");
                                    break;
                            }

                            break;
                        default:
                            fieldMetadata.add("{\"field\":\"" + field + "\",\"type\":\"string\",\"length\":255}");

                            switch (dialect) {
                                case "spectrum":
                                case "athena":
                                    fieldSchema.add("{\"name\":\"" + field + "\",\"type\":[\"null\",\"string\"],\"default\":null}");
                                    fieldDataType.add(field + " " + "varchar(255)");
                                    break;
                                case "redshift":
                                    fieldDataType.add(field + " " + "varchar(255) ENCODE ZSTD");
                                    break;
                                case "bigquery":
                                    fieldSchema.add("{\"name\":\"" + field + "\",\"type\":\"STRING\"}");
                                    break;
                            }

                            break;
                    }
                }
            }

            if (fieldList.size() > 0) {
                //Write field list.  
                String tableColumn = String.join("\n", fieldList);
                FileWriter tableColumnFile = new FileWriter(this.outputPath.concat(file.getName().replace(".csv", "")).concat("_columns.csv"));

                try (BufferedWriter tableColumnsBuffer = new BufferedWriter(tableColumnFile)) {
                    tableColumnsBuffer.write(tableColumn);
                    tableColumnsBuffer.flush();
                    tableColumnFile.close();
                }

                //Write field list with datatype. 
                if (fieldDataType.size() > 0) {
                    String tableField = String.join(",", fieldDataType);
                    FileWriter tableFieldFile = new FileWriter(this.outputPath.concat(file.getName().replace(".csv", "")).concat("_fields.csv"));

                    try (BufferedWriter tableFieldBuffer = new BufferedWriter(tableFieldFile)) {
                        tableFieldBuffer.write(tableField);
                        tableFieldBuffer.flush();
                        tableFieldFile.close();
                    }
                }

                //Write table parquet schema.
                if (fieldSchema.size() > 0) {
                    String schemaField = "[".concat(String.join(",\n", fieldSchema)).concat("]");
                    FileWriter schemaFile = new FileWriter(this.outputPath.concat(file.getName().replace(".csv", "")).concat(".json"));

                    try (BufferedWriter schemaBuffer = new BufferedWriter(schemaFile)) {
                        schemaBuffer.write(schemaField);
                        schemaBuffer.flush();
                        schemaFile.close();
                    }
                }

                //Write table metadata.
                if (fieldMetadata.size() > 0) {
                    String tableMetadata = "[".concat(String.join(",\n", fieldMetadata)).concat("]");
                    FileWriter tableMetadataFile = new FileWriter(this.outputPath.concat(file.getName().replace(".csv", "")).concat("_metadata.csv"));

                    try (BufferedWriter metadataBuffer = new BufferedWriter(tableMetadataFile)) {
                        metadataBuffer.write(tableMetadata);
                        metadataBuffer.flush();
                        tableMetadataFile.close();
                    }
                }
            } else {
                Logger.getLogger(Extractor.class).info("CSV file is empty");
            }
        } catch (Exception  ex) {
        	System.out.println(ex + " on row: " + rowOnTheFly);
            System.exit(1);
        }
    }
}

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
package br.com.dafiti.metadata;

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
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * Read a csv file, infer and write the parquet schema.
 *
 * @author Valdiney V GOMES
 * @author Helio Leal
 */
public class Extractor implements Runnable {

    private static final Logger LOG = Logger.getLogger(Metadata.class.getName());
    private static final int MAX_SAMPLE = 1000000;

    private File file;
    private File reserverWordsFile;
    private ArrayList<String[]> fieldContent;
    private JSONArray jsonMetadata;
    private Character delimiter;
    private Character quote;
    private Character escape;
    private String outputPath;
    private String dialect;
    private String rowOnTheFly;
    private boolean hasHeader;
    private int sample;

    private Field field;

    /**
     * Constructor.
     *
     * @param csvFile CSV file.
     * @param reserverWordsFile Identify the reserved words file list.
     * @param delimiter File delimiter.
     * @param quote File quote.
     * @param escape File escape.
     * @param csvField Header fields.
     * @param metadata Table metadata.
     * @param outputFolder Metadata output folder.
     * @param dialect Identify the metadata dialect.
     * @param sample Sample de dados a ser analizado para definição de data
     * types.
     */
    public Extractor(
            File csvFile,
            File reserverWordsFile,
            Character delimiter,
            Character quote,
            Character escape,
            String csvField,
            String metadata,
            String outputFolder,
            String dialect,
            int sample) {

        this.fieldContent = new ArrayList();
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

        this.field = new Field();

        //Limit the data sample.
        if (sample > MAX_SAMPLE) {
            this.sample = MAX_SAMPLE;
        } else {
            this.sample = sample;
        }

        //Identify the output path.
        if (outputFolder.isEmpty()) {
            this.outputPath = csvFile.getParent().concat("/");
        }

        //Get the header fields passed by parameter. 
        if (!this.hasHeader) {
            this.field.setList(Arrays.asList(csvField.replace(" ", "").split(Pattern.quote(","))));
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
            this.fillDataSample();
            List<String> reservedWords = this.getReservedWords();
            Dialect clazz = (Dialect) Class.forName("br.com.dafiti.metadata." + dialect).newInstance();

            //Process each column. 
            for (int column = 0; column < this.field.getList().size(); column++) {
                String type = "";
                int length = 0;
                int stringField = 0;
                int integerField = 0;
                int doubleField = 0;
                int nullField = 0;
                boolean hasMetadata = false;

                //Get the field name.
                String name = this.field.getList()
                        .get(column)
                        .replaceAll("\\s", "")
                        .replaceAll("\\W", "_")
                        .replaceAll("^_", "")
                        .toLowerCase();

                //Get field properties from metadata. 
                for (int i = 0; i < jsonMetadata.length(); i++) {
                    JSONObject metadata = jsonMetadata.getJSONObject(i);
                    hasMetadata = metadata.getString("field").equalsIgnoreCase(name);

                    if (hasMetadata) {
                        type = metadata.has("type") ? metadata.getString("type") : "";
                        length = metadata.has("length") ? metadata.getInt("length") : 0;

                        break;
                    }
                }

                //Concat postfix _rw to field name that is a reserved word. 
                if (reservedWords.contains(name.toLowerCase())) {
                    System.out.println("Reserved word found at " + name + " and replaced by " + name + "_rw");
                    name = name.concat("_rw");
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
                        clazz.generateNull(this.field, name);

                    } else if (stringField > 0) {
                        clazz.generateString(this.field, name, (length * 2) > 65000 ? 65000 : (length * 2));

                    } else if (integerField > 0 && stringField == 0 && doubleField == 0) {
                        clazz.generateInteger(this.field, name);

                    } else if (doubleField > 0 && stringField == 0) {
                        clazz.generateNumber(this.field, name);

                    }
                } else {
                    switch (type) {
                        case "string":
                            clazz.generateString(this.field, name, length > 65000 ? 65000 : length);

                            break;
                        case "integer":
                        case "long":
                            clazz.generateInteger(this.field, name);

                            break;
                        case "number":
                        case "bignumber":
                            clazz.generateBigNumber(this.field, name);

                            break;
                        case "timestamp":
                            clazz.generateTimestamp(this.field, name);

                            break;
                        case "date":
                            clazz.generateDate(this.field, name);

                            break;
                        case "boolean":
                            clazz.generateBoolean(this.field, name);

                            break;
                        default:
                            clazz.generateNull(this.field, name);

                            break;
                    }
                }
            }

            if (this.field.getList().size() > 0) {
                //Write field list.                  
                writeFile("_columns.csv", String.join("\n", this.field.getList()));

                //Write field list with datatype. 
                if (this.field.getDataType().size() > 0) {
                    writeFile("_fields.csv", String.join(",", this.field.getDataType()));
                }

                //Write table parquet schema.
                if (this.field.getSchema().size() > 0) {
                    writeFile(".json", "[".concat(String.join(",\n", this.field.getSchema())).concat("]"));
                }

                //Write table metadata.
                if (this.field.getMetadata().size() > 0) {
                    writeFile("_metadata.csv", "[".concat(String.join(",\n", this.field.getMetadata())).concat("]"));
                }

            } else {
                LOG.info("CSV file is empty");
            }
        } catch (IOException
                | JSONException
                | ClassNotFoundException
                | InstantiationException
                | IllegalAccessException ex) {
            System.out.println(ex + " on row: " + rowOnTheFly);
            System.exit(1);
        }
    }

    /**
     *
     */
    private void fillDataSample() {
        CsvParser csvParser = new CsvParser(this.getCSVSettings());
        csvParser.beginParsing(file);

        String[] row;

        while ((row = csvParser.parseNext()) != null) {
            //Identify if file has a header
            if (hasHeader && this.field.getList().isEmpty()) {
                this.field.getList().addAll(Arrays.asList(row));
            } else {
                //Get a limited data sample from the file.
                fieldContent.add(row);

                if (fieldContent.size() == sample) {
                    break;
                }
            }
        }

        csvParser.stopParsing();
    }

    /**
     *
     * @return CsvParserSettings
     */
    private CsvParserSettings getCSVSettings() {
        CsvParserSettings settings = new CsvParserSettings();
        settings.setNullValue("");
        settings.setMaxCharsPerColumn(-1);
        settings.getFormat().setDelimiter(delimiter);
        settings.getFormat().setQuote(quote);
        settings.getFormat().setQuoteEscape(escape);

        return settings;
    }

    /**
     * Get Reserved words which cannot be used directly.
     *
     * @return List<String>
     */
    private List<String> getReservedWords() {
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
        return reservedWords;
    }

    /**
     * Write an output file.
     *
     * @param suffix String file name suffix
     * @param content String file content
     * @throws IOException
     */
    private void writeFile(String suffix, String content) throws IOException {
        FileWriter writer = new FileWriter(this.outputPath
                .concat(this.file.getName().replace(".csv", ""))
                .concat(suffix));

        try (BufferedWriter bf = new BufferedWriter(writer)) {
            bf.write(content);
            bf.flush();
            writer.close();
        }
    }

}

/**
 *
 * @author Helio Leal
 */
class Field {

    private List<String> list;    // antigo fieldList
    private ArrayList<String> schema; // fieldSchema;
    private ArrayList<String> dataType; // fieldDataType;
    private ArrayList<String> metadata; // fieldMetadata;

    public Field() {
        this.list = new ArrayList();
        this.schema = new ArrayList();
        this.dataType = new ArrayList();
        this.metadata = new ArrayList();
    }

    public void setList(List<String> list) {
        this.list = list;
    }

    public List<String> getList() {
        return list;
    }

    public ArrayList<String> getSchema() {
        return schema;
    }

    public void setSchema(ArrayList<String> schema) {
        this.schema = schema;
    }

    public ArrayList<String> getDataType() {
        return dataType;
    }

    public void setDataType(ArrayList<String> dataType) {
        this.dataType = dataType;
    }

    public ArrayList<String> getMetadata() {
        return metadata;
    }

    public void setMetadata(ArrayList<String> metadata) {
        this.metadata = metadata;
    }
}

/**
 *
 * @author helio.leal
 */
interface Dialect {

    public void generateNull(Field field, String name);

    public void generateString(Field field, String name, int length);

    public void generateInteger(Field field, String name);

    public void generateNumber(Field field, String name);

    public void generateBigNumber(Field field, String name);

    public void generateTimestamp(Field field, String name);

    public void generateDate(Field field, String name);

    public void generateBoolean(Field field, String name);

}

/**
 *
 * @author Helio Leal
 */
class spectrum implements Dialect {

    @Override
    public void generateNull(Field field, String name) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"string\",\"length\":255}");
        field.getSchema().add("{\"name\":\"" + name + "\",\"type\":[\"null\",\"string\"],\"default\":null}");
        field.getDataType().add(name + " " + "varchar(255)");
    }

    @Override
    public void generateString(Field field, String name, int length) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"string\",\"length\":" + length + "}");
        field.getSchema().add("{\"name\":\"" + name + "\",\"type\":[\"null\",\"string\"],\"default\":null}");
        field.getDataType().add(name + " " + "varchar(" + length + ")");
    }

    @Override
    public void generateInteger(Field field, String name) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"integer\"}");
        field.getSchema().add("{\"name\":\"" + name + "\",\"type\":[\"null\",\"long\"],\"default\":null}");
        field.getDataType().add(name + " " + "bigint");
    }

    @Override
    public void generateNumber(Field field, String name) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"number\"}");
        field.getSchema().add("{\"name\":\"" + name + "\",\"type\":[\"null\",\"double\"],\"default\":null}");
        field.getDataType().add(name + " " + "double precision");
    }

    @Override
    public void generateBigNumber(Field field, String name) {
        this.generateNumber(field, name);
    }

    @Override
    public void generateTimestamp(Field field, String name) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"string\",\"length\":19}");
        field.getSchema().add("{\"name\":\"" + name + "\",\"type\":[\"null\",\"string\"],\"default\":null}");
        field.getDataType().add(name + " " + "varchar(19)");
    }

    @Override
    public void generateDate(Field field, String name) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"string\",\"length\":19}");
        field.getSchema().add("{\"name\":\"" + name + "\",\"type\":[\"null\",\"string\"],\"default\":null}");
        field.getDataType().add(name + " " + "varchar(19)");
    }

    @Override
    public void generateBoolean(Field field, String name) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"boolean\"}");
        field.getSchema().add("{\"name\":\"" + name + "\",\"type\":[\"null\",\"boolean\"],\"default\":null}");
        field.getDataType().add(name + " " + "boolean");
    }
}

/**
 *
 * @author Helio Leal
 */
class athena implements Dialect {

    @Override
    public void generateNull(Field field, String name) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"string\",\"length\":255}");
        field.getSchema().add("{\"name\":\"" + name + "\",\"type\":[\"null\",\"string\"],\"default\":null}");
        field.getDataType().add(name + " " + "varchar(255)");
    }

    @Override
    public void generateString(Field field, String name, int length) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"string\",\"length\":" + length + "}");
        field.getSchema().add("{\"name\":\"" + name + "\",\"type\":[\"null\",\"string\"],\"default\":null}");
        field.getDataType().add(name + " " + "varchar(" + length + ")");
    }

    @Override
    public void generateInteger(Field field, String name) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"integer\"}");
        field.getSchema().add("{\"name\":\"" + name + "\",\"type\":[\"null\",\"long\"],\"default\":null}");
        field.getDataType().add(name + " " + "bigint");
    }

    @Override
    public void generateNumber(Field field, String name) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"number\"}");
        field.getSchema().add("{\"name\":\"" + name + "\",\"type\":[\"null\",\"double\"],\"default\":null}");
        field.getDataType().add(name + " " + "double precision");
    }

    @Override
    public void generateBigNumber(Field field, String name) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"number\"}");
        field.getSchema().add("{\"name\":\"" + name + "\",\"type\":[\"null\",\"double\"],\"default\":null}");
        field.getDataType().add(name + " " + "double");
    }

    @Override
    public void generateTimestamp(Field field, String name) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"string\",\"length\":19}");
        field.getSchema().add("{\"name\":\"" + name + "\",\"type\":[\"null\",\"string\"],\"default\":null}");
        field.getDataType().add(name + " " + "varchar(19)");
    }

    @Override
    public void generateDate(Field field, String name) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"string\",\"length\":19}");
        field.getSchema().add("{\"name\":\"" + name + "\",\"type\":[\"null\",\"string\"],\"default\":null}");
        field.getDataType().add(name + " " + "varchar(19)");
    }

    @Override
    public void generateBoolean(Field field, String name) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"boolean\"}");
        field.getSchema().add("{\"name\":\"" + name + "\",\"type\":[\"null\",\"boolean\"],\"default\":null}");
        field.getDataType().add(name + " " + "boolean");
    }
}

/**
 *
 * @author Helio Leal
 */
class redshift implements Dialect {

    @Override
    public void generateNull(Field field, String name) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"string\",\"length\":255}");
        field.getDataType().add(name + " " + "varchar(255) ENCODE ZSTD");
    }

    @Override
    public void generateString(Field field, String name, int length) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"string\",\"length\":" + length + "}");
        field.getDataType().add(name + " " + "varchar(" + length + ") ENCODE ZSTD");
    }

    @Override
    public void generateInteger(Field field, String name) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"integer\"}");
        field.getDataType().add(name + " " + "bigint ENCODE AZ64");
    }

    @Override
    public void generateNumber(Field field, String name) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"number\"}");
        field.getDataType().add(name + " " + "double precision ENCODE ZSTD");
    }

    @Override
    public void generateBigNumber(Field field, String name) {
        this.generateNumber(field, name);
    }

    @Override
    public void generateTimestamp(Field field, String name) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"string\",\"length\":19}");
        field.getDataType().add(name + " " + "varchar(19) ENCODE ZSTD");
    }

    @Override
    public void generateDate(Field field, String name) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"string\",\"length\":19}");
        field.getDataType().add(name + " " + "varchar(19) ENCODE ZSTD");
    }

    @Override
    public void generateBoolean(Field field, String name) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"boolean\"}");
        field.getDataType().add(name + " " + "boolean ENCODE ZSTD");
    }
}

/**
 *
 * @author helio.leal
 */
class bigquery implements Dialect {

    @Override
    public void generateNull(Field field, String name) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"string\",\"length\":255}");
        field.getSchema().add("{\"name\":\"" + name + "\",\"type\":\"STRING\"}");
    }

    @Override
    public void generateString(Field field, String name, int length) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"string\",\"length\":" + length + "}");
        field.getSchema().add("{\"name\":\"" + name + "\",\"type\":\"STRING\"}");
    }

    @Override
    public void generateInteger(Field field, String name) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"integer\"}");
        field.getSchema().add("{\"name\":\"" + name + "\",\"type\":\"INTEGER\"}");
    }

    @Override
    public void generateNumber(Field field, String name) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"number\"}");
        field.getSchema().add("{\"name\":\"" + name + "\",\"type\":\"FLOAT\"}");
    }

    @Override
    public void generateBigNumber(Field field, String name) {
        this.generateNumber(field, name);
    }

    @Override
    public void generateTimestamp(Field field, String name) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"string\",\"length\":19}");
        field.getSchema().add("{\"name\":\"" + name + "\",\"type\":\"TIMESTAMP\"}");
    }

    @Override
    public void generateDate(Field field, String name) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"string\",\"length\":19}");
        field.getSchema().add("{\"name\":\"" + name + "\",\"type\":\"DATE\"}");
    }

    @Override
    public void generateBoolean(Field field, String name) {
        field.getMetadata().add("{\"field\":\"" + name + "\",\"type\":\"boolean\"}");
        field.getSchema().add("{\"name\":\"" + name + "\",\"type\":\"BOOLEAN\"}");
    }
}

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

import br.com.dafiti.metadata.schema.FactoryMetadatable;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import br.com.dafiti.metadata.schema.Metadatable;

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
     *
     * @param file CSV file.
     * @param reservedWords Identify the reserved words file list.
     * @param delimiter File delimiter.
     * @param quote File quote.
     * @param escape File escape.
     * @param csvField Header fields.
     * @param metadata Table metadata.
     * @param outputFolder Metadata output folder.
     * @param dialect Identify the metadata dialect.
     * @param sample Sample de dados analizado para definição de datatypes.
     */
    public Extractor(
            File file,
            File reservedWords,
            Character delimiter,
            Character quote,
            Character escape,
            String csvField,
            String metadata,
            String outputFolder,
            String dialect,
            int sample) {

        this.file = file;
        this.delimiter = delimiter;
        this.quote = quote;
        this.escape = escape;
        this.outputPath = outputFolder;
        this.dialect = dialect;
        this.hasHeader = csvField.isEmpty();
        this.reserverWordsFile = reservedWords;
        this.rowOnTheFly = "";
        this.fieldContent = new ArrayList();
        this.jsonMetadata = new JSONArray();
        this.field = new Field();

        //Limit the data sample.
        if (sample > MAX_SAMPLE) {
            this.sample = MAX_SAMPLE;
        } else {
            this.sample = sample;
        }

        if (outputFolder.isEmpty()) {
            this.outputPath = file.getParent().concat("/");
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
            //Define which dialect to use to generate output metadata.
            Metadatable metadatable = FactoryMetadatable.getMetadatable(dialect);            

            this.fillDataSample();

            this.inferMetadata(metadatable);

            this.writeFiles();

        } catch (IOException | JSONException ex) {
            LOG.log(Level.SEVERE, "GLOVE - Metadata Inference fail [row: " + rowOnTheFly + "]", ex);
            System.exit(1);
        }
    }

    /**
     * Infer metadata based on data sample values.
     *
     * @param metadatable Metadatable gereric class based on dialect.
     * @throws JSONException
     */
    public void inferMetadata(Metadatable metadatable) throws JSONException {
        List<String> reservedWords = this.getReservedWords();

        //Process each column.
        for (int column = 0; column < this.field.getList().size(); column++) {
            String type = "";
            int length = 0;
            boolean hasMetadata = false;

            //Get the field name.
            String name = this.field.getList()
                    .get(column)
                    .replaceAll("\\s", "")
                    .replaceAll("\\W", "_")
                    .replaceAll("^_", "")
                    .toLowerCase();

            //Get field properties from metadata parameter.
            for (int i = 0; i < jsonMetadata.length(); i++) {
                JSONObject metadata = jsonMetadata.getJSONObject(i);
                hasMetadata = metadata.getString("field").equalsIgnoreCase(name);

                if (hasMetadata) {
                    type = metadata.has("type") ? metadata.getString("type").toLowerCase() : "";
                    length = metadata.has("length") ? metadata.getInt("length") : 0;

                    break;
                }
            }

            //Concat postfix _rw to field name that is a reserved word.
            if (reservedWords.contains(name.toLowerCase())) {
                System.out.println("Reserved word found at " + name + " and replaced by " + name + "_rw");
                name = name.concat("_rw");
            }

            if (!hasMetadata) {
                int stringCount = 0;
                int integerCount = 0;
                int doubleCount = 0;
                int nullCount = 0;

                //Process each record of each column.
                for (int content = 0; content < fieldContent.size(); content++) {
                    rowOnTheFly = String.join(" | ", fieldContent.get(content));
                    String value = fieldContent.get(content)[column] == null ? "" : fieldContent.get(content)[column];

                    //Calculate the number of occurrences of each data type.
                    if (value.matches("^[-+]?[0-9]*") && !(value.isEmpty() || value.equals("-") || value.equals("+") || (!value.equals("0") && value.matches("^0[0-9]*"))) && value.length() <= 19) {
                        integerCount = integerCount + 1;
                        length = value.length() > length ? value.length() : length;

                    } else if (value.matches("^[-+]?[0-9]*\\.[0-9]+([eE][-+]?[0-9]+)?") && !(value.isEmpty() || value.equals("-") || value.equals("+") || (!value.equals("0") && value.matches("^0[0-9]*")))) {
                        doubleCount = doubleCount + 1;
                        length = value.length() > length ? value.length() : length;

                    } else if (value.isEmpty()) {
                        nullCount = nullCount + 1;

                    } else {
                        stringCount = stringCount + 1;
                        length = value.getBytes().length > length ? value.getBytes().length : length;
                    }
                }

                //Identify the field type and size based on the number of ocurrences of each data type.
                if ((nullCount > 0 && stringCount == 0 && integerCount == 0 && doubleCount == 0)) {
                    metadatable.generateNull(this.field, name);

                } else if (stringCount > 0) {
                    metadatable.generateString(this.field, name, (length * 2) > 65000 ? 65000 : (length * 2));

                } else if (integerCount > 0 && stringCount == 0 && doubleCount == 0) {
                    metadatable.generateInteger(this.field, name);

                } else if (doubleCount > 0 && stringCount == 0) {
                    metadatable.generateNumber(this.field, name);

                }
            } else {
                switch (type) {
                    case "string":
                        metadatable.generateString(this.field, name, length > 65000 ? 65000 : length);

                        break;
                    case "integer":
                    case "long":
                        metadatable.generateInteger(this.field, name);

                        break;
                    case "number":
                    case "bignumber":
                        metadatable.generateBigNumber(this.field, name);

                        break;
                    case "timestamp":
                        metadatable.generateTimestamp(this.field, name);

                        break;
                    case "date":
                        metadatable.generateDate(this.field, name);

                        break;
                    case "boolean":
                        metadatable.generateBoolean(this.field, name);

                        break;
                    default:
                        metadatable.generateNull(this.field, name);

                        break;
                }
            }
        }
    }

    /**
     * Write output files.
     *
     * @throws IOException
     */
    public void writeFiles() throws IOException {
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
    }

    /**
     * Defines a limited data sample.
     */
    public void fillDataSample() {
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
     * Get Reserved words which cannot be used directly on database.
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

                LOG.log(Level.INFO, "{0} reserved words loaded from {1}", new Object[]{reservedWords.size(), reserverWordsFile.getAbsolutePath()});
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Error getting reserved words: ", ex);

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

    public ArrayList<String[]> getFieldContent() {
        return fieldContent;
    }

    public Field getField() {
        return field;
    }

}

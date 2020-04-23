/*
 * Copyright (c) 2020 Dafiti Group
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
package br.com.dafiti.mitt.decoder;

import br.com.dafiti.mitt.settings.WriterSettings;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author Valdiney V GOMES
 */
public class AvroDecoder implements Decoder {

    private final GenericData data;
    private static AvroDecoder instance;

    /**
     *
     */
    private AvroDecoder() {
        data = new GenericDatumReader<>().getData();
    }

    /**
     *
     * @return
     */
    public static AvroDecoder getInstance() {
        if (instance == null) {
            synchronized (AvroDecoder.class) {
                if (instance == null) {
                    instance = new AvroDecoder();
                }

            }
        }

        return instance;
    }

    /**
     *
     * @param file
     * @return
     */
    @Override
    public File decode(File file) {
        File decompress = new File(file.getParent() + "/" + FilenameUtils.removeExtension(file.getName()) + ".csv");

        try {
            //Defines a generic reader.   
            DataFileReader<GenericData.Record> reader = new DataFileReader<>(
                    file,
                    new GenericDatumReader<>(null, null, this.data));

            //Extracts schema.
            Schema schema = reader.getSchema();

            //Defines the output file write settings.
            WriterSettings writerSettings = new WriterSettings();

            CsvWriterSettings setting = new CsvWriterSettings();
            setting.getFormat().setDelimiter(writerSettings.getDelimiter());
            setting.getFormat().setQuote(writerSettings.getQuote());
            setting.getFormat().setQuoteEscape(writerSettings.getQuoteEscape());
            setting.setNullValue("");
            setting.setMaxCharsPerColumn(-1);
            setting.setHeaderWritingEnabled(true);

            //Extracts file header.
            setting.setHeaders(
                    schema
                            .getFields()
                            .stream()
                            .map(field -> field.name())
                            .toArray(String[]::new));

            //Defines the writer.
            CsvWriter csvWriter = new CsvWriter(decompress, setting);

            //Extracts file records.
            GenericData.Record record = new GenericData.Record(schema);

            while (reader.hasNext()) {
                reader.next(record);
                List<Object> row = new ArrayList();

                for (String header : setting.getHeaders()) {
                    row.add(record.get(header));
                }

                csvWriter.writeRow(row);
            }

            //Flush and close writer.
            csvWriter.flush();
            csvWriter.close();

            //Removes original file.
            Files.delete(file.toPath());
        } catch (IOException ex) {
            Logger.getLogger(AvroDecoder.class.getName()).log(Level.SEVERE, "Fail decoding AVRO file " + file.getName(), ex);
        }

        return decompress;
    }
}

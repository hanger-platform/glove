/*
 * Copyright (c) 2019 Dafiti Group
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
package br.com.dafiti.mitt.output;

import br.com.dafiti.mitt.transformation.Parser;
import br.com.dafiti.mitt.model.Configuration;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mozilla.universalchardet.UniversalDetector;

/**
 *
 * @author Valdiney V GOMES
 */
public class Output {

    private final CsvWriter writer;
    private final Parser parser;

    /**
     *
     * @param output
     * @param configuration
     * @param delimiter
     * @param quote
     * @param quoteEscape
     */
    public Output(
            File output,
            Configuration configuration,
            char delimiter,
            char quote,
            char quoteEscape) {

        parser = new Parser(configuration);

        CsvWriterSettings setting = new CsvWriterSettings();
        setting.getFormat().setDelimiter(delimiter);
        setting.getFormat().setQuote(quote);
        setting.getFormat().setQuoteEscape(quoteEscape);
        setting.setNullValue("");
        setting.setMaxCharsPerColumn(-1);
        
        setting.setHeaders(parser
                .listFieldName(true)
                .toArray(new String[0])
        );

        this.writer = new CsvWriter(output, setting);
        this.writer.writeHeaders();
    }

    /**
     *
     * @return
     */
    public Parser getParser() {
        return parser;
    }

    /**
     *
     * @param record
     */
    public void write(List<Object> record) {
        this.writer.writeRow(parser.evaluate(record));
    }

    /**
     *
     * @param record
     */
    public void write(String[] record) {
        List<Object> data = new ArrayList();

        for (String value : record) {
            data.add(value);
        }

        this.writer.writeRow(parser.evaluate(data));
    }

    /**
     *
     * @param files
     * @param delimiter
     * @param quote
     * @param escape
     * @param encode
     * @param header
     * @param remove
     * @param skipLines
     */
    public void write(
            File[] files,
            char delimiter,
            char quote,
            char escape,
            String encode,
            List<String> header,
            boolean remove,
            int skipLines) {

        for (File file : files) {
            this.write(
                    file,
                    delimiter,
                    quote,
                    escape,
                    encode,
                    header,
                    remove,
                    skipLines
            );
        }
    }

    /**
     *
     * @param file
     * @param delimiter
     * @param quote
     * @param escape
     * @param encode
     * @param header
     * @param remove
     * @param skipLines
     */
    public void write(
            File file,
            char delimiter,
            char quote,
            char escape,
            String encode,
            List<String> header,
            boolean remove,
            int skipLines) {

        String[] record;
        String encoding = null;

        parser.setFile(file);

        CsvParserSettings setting = new CsvParserSettings();
        setting.getFormat().setDelimiter(delimiter);
        setting.getFormat().setQuote(quote);
        setting.getFormat().setQuoteEscape(escape);
        setting.setNullValue("");
        setting.setMaxCharsPerColumn(-1);
        setting.setInputBufferSize(5 * (1024 * 1024));
        setting.setNumberOfRowsToSkip(skipLines);

        if (header.isEmpty()) {
            setting.setHeaderExtractionEnabled(true);
        } else {
            setting.setHeaders(header.toArray(new String[0]));
        }

        setting.selectFields(parser.listOriginalFieldName().toArray(new String[0]));

        if ("auto".equalsIgnoreCase(encode)) {
            try {
                encoding = UniversalDetector.detectCharset(file);
            } catch (IOException ex) {
                Logger.getLogger(Output.class.getName()).log(Level.SEVERE, "Encode do not detected!", ex);
            }
        } else {
            encoding = encode;
        }

        CsvParser csvParser = new CsvParser(setting);
        csvParser.beginParsing(file, encoding);

        while ((record = csvParser.parseNext()) != null) {
            this.write(record);
        }

        if (remove) {
            file.delete();
        }
    }

    /**
     *
     */
    public void close() {
        this.writer.flush();
        this.writer.close();
    }
}

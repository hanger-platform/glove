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
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.CsvWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.mozilla.universalchardet.UniversalDetector;

/**
 *
 * @author Valdiney V GOMES
 */
public class OutputProcessor implements Runnable {

    private final File input;
    private final Parser parser;
    private final CsvWriter writer;
    private final char delimiter;
    private final char quote;
    private final char quoteEscape;
    private final String encode;
    private final List<String> header;
    private final boolean remove;
    private final int skipLines;

    /**
     *
     * @param input
     * @param parser
     * @param writer
     * @param delimiter
     * @param quote
     * @param quoteEscape
     * @param encode
     * @param header
     * @param remove
     * @param skipLines
     */
    public OutputProcessor(
            File input,
            Parser parser,
            CsvWriter writer,
            char delimiter,
            char quote,
            char quoteEscape,
            String encode,
            List<String> header,
            boolean remove,
            int skipLines) {

        this.input = input;
        this.parser = parser;
        this.writer = writer;
        this.delimiter = delimiter;
        this.quote = quote;
        this.quoteEscape = quoteEscape;
        this.encode = encode;
        this.header = header;
        this.remove = remove;
        this.skipLines = skipLines;
    }

    /**
     *
     */
    @Override
    public void run() {
        this.write();

        if (this.writer != null) {
            this.writer.flush();
            this.writer.close();
        }
    }

    /**
     *
     */
    public void write() {
        String[] record;
        String encoding = null;

        parser.setFile(input);

        CsvParserSettings setting = new CsvParserSettings();
        setting.getFormat().setDelimiter(delimiter);
        setting.getFormat().setQuote(quote);
        setting.getFormat().setQuoteEscape(quoteEscape);
        setting.setNullValue("");
        setting.setMaxCharsPerColumn(-1);
        setting.setInputBufferSize(5 * (1024 * 1024));
        setting.setNumberOfRowsToSkip(skipLines);

        if (header.isEmpty()) {
            setting.setHeaderExtractionEnabled(true);
        } else {
            setting.setHeaders(header.toArray(new String[0]));
        }

        setting.selectFields(
                parser
                        .getConfiguration()
                        .getOriginalFieldsName()
                        .toArray(new String[0]));

        if ("auto".equalsIgnoreCase(encode)) {
            try {
                encoding = UniversalDetector.detectCharset(input);
            } catch (IOException ex) {
                Logger.getLogger(Output.class.getName()).log(Level.SEVERE, "Encode do not detected!", ex);
            } finally {
                if (encoding == null) {
                    encoding = "UTF-8";
                }
            }
        } else {
            encoding = encode;
        }

        CsvParser csvParser = new CsvParser(setting);
        csvParser.beginParsing(input, encoding);

        while ((record = csvParser.parseNext()) != null) {
            this.writer
                    .writeRow(
                            parser.evaluate(Arrays.asList(record))
                    );
        }

        if (remove) {
            input.delete();
        }
    }
}

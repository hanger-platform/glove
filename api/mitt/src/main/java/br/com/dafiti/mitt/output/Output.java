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
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author Valdiney V GOMES
 */
public class Output {

    private final File output;
    private final CsvWriter writer;
    private final Parser parser;
    private final char delimiter;
    private final char quote;
    private final char quoteEscape;

    private final int THREAD_POOL = 5;

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

        this.output = output;
        this.delimiter = delimiter;
        this.quote = quote;
        this.quoteEscape = quoteEscape;
        this.parser = new Parser(configuration);
        this.writer = this.getWriter(output);
    }

    /**
     *
     * @param output
     * @return
     */
    public CsvWriter getWriter(File output) {
        CsvWriter currentWriter = null;

        if (!FilenameUtils
                .getExtension(output.getName())
                .isEmpty()) {

            CsvWriterSettings setting = new CsvWriterSettings();
            setting.getFormat().setDelimiter(delimiter);
            setting.getFormat().setQuote(quote);
            setting.getFormat().setQuoteEscape(quoteEscape);
            setting.setNullValue("");
            setting.setMaxCharsPerColumn(-1);
            setting.setHeaderWritingEnabled(true);
            setting.setHeaders(
                    parser.getConfiguration().getFieldsName(true).toArray(new String[0])
            );

            currentWriter = new CsvWriter(output, setting);
        }

        return currentWriter;
    }

    /**
     *
     * @param record
     */
    public void write(List<Object> record) {
        this.writer
                .writeRow(
                        parser.evaluate(record)
                );
    }

    /**
     *
     * @param record
     */
    public void write(Object[] record) {
        this.write(
                Arrays.asList(record)
        );
    }

    /**
     *
     * @param writer
     * @param record
     */
    public void write(CsvWriter writer, Object[] record) {
        writer
                .writeRow(
                        parser.evaluate(
                                Arrays.asList(record)
                        )
                );
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

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL);

        for (File file : files) {
            if (file.length() != 0) {
                if (FilenameUtils
                        .getExtension(output.getName())
                        .isEmpty()) {

                    executor.execute(new OutputProcessor(
                            file,
                            parser,
                            this.getWriter(new File(output.getAbsolutePath() + "/" + file.getName())),
                            delimiter,
                            quote,
                            quoteEscape,
                            encode,
                            header,
                            remove,
                            skipLines));
                } else {
                    new OutputProcessor(
                            file,
                            parser,
                            writer,
                            delimiter,
                            quote,
                            quoteEscape,
                            encode,
                            header,
                            remove,
                            skipLines).write();
                }
            }
        }

        executor.shutdown();
    }

    /**
     *
     * @param file
     * @param delimiter
     * @param quote
     * @param quoteEscape
     * @param encode
     * @param header
     * @param remove
     * @param skipLines
     */
    public void write(
            File file,
            char delimiter,
            char quote,
            char quoteEscape,
            String encode,
            List<String> header,
            boolean remove,
            int skipLines) {

        new OutputProcessor(
                file,
                parser,
                writer,
                delimiter,
                quote,
                quoteEscape,
                encode,
                header,
                remove,
                skipLines).write();
    }

    /**
     *
     */
    public void close() {
        if (this.writer != null) {
            this.writer.flush();
            this.writer.close();
        }
    }
}

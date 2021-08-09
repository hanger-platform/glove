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

import br.com.dafiti.mitt.decoder.Decoder;
import br.com.dafiti.mitt.decoder.FactoryDecoder;
import br.com.dafiti.mitt.model.Configuration;
import br.com.dafiti.mitt.settings.ReaderSettings;
import br.com.dafiti.mitt.settings.WriterSettings;
import br.com.dafiti.mitt.transformation.Parser;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.mozilla.universalchardet.UniversalDetector;

/**
 *
 * @author Valdiney V GOMES
 */
public class OutputProcessor implements Runnable {

    private File input;
    private final Parser parser;
    private final CsvWriter writer;
    private final ReaderSettings readerSettings;
    private final WriterSettings writerSettings;

    /**
     *
     * @param configuration
     * @param readerSettings
     * @param writerSettings
     */
    public OutputProcessor(
            Configuration configuration,
            ReaderSettings readerSettings,
            WriterSettings writerSettings) {

        this.parser = new Parser(configuration);
        this.readerSettings = readerSettings;
        this.writerSettings = writerSettings;

        this.writer = new CsvWriter(writerSettings.getOutputFile(), this.getCSVSettings());
    }

    /**
     *
     * @param configuration
     * @param readerSettings
     * @param writerSettings
     * @param input
     */
    public OutputProcessor(
            Configuration configuration,
            ReaderSettings readerSettings,
            WriterSettings writerSettings,
            File input) {

        this.input = input;
        this.parser = new Parser(configuration);
        this.readerSettings = readerSettings;
        this.writerSettings = writerSettings;

        this.writer = new CsvWriter(
                new File(writerSettings.getOutputFile().getAbsolutePath() + "/" + FilenameUtils.removeExtension(input.getName()) + ".csv"),
                this.getCSVSettings());
    }

    /**
     *
     * @return
     */
    private CsvWriterSettings getCSVSettings() {
        CsvWriterSettings setting = new CsvWriterSettings();
        setting.getFormat().setDelimiter(writerSettings.getDelimiter());
        setting.getFormat().setQuote(writerSettings.getQuote());
        setting.getFormat().setQuoteEscape(writerSettings.getQuoteEscape());
        setting.setNullValue("");
        setting.setMaxCharsPerColumn(-1);
        setting.setHeaderWritingEnabled(writerSettings.isHeaderEnabled());
        setting.setHeaders(parser.getConfiguration().getFieldName(true).toArray(new String[0]));

        return setting;
    }

    /**
     *
     * @return
     */
    public File getInput() {
        return input;
    }

    /**
     *
     * @param input
     */
    public void setInput(File input) {
        this.input = input;
    }

    /**
     *
     * @return
     */
    public CsvWriter getWriter() {
        return writer;
    }

    /**
     *
     * @param record
     */
    public void write(List<Object> record) {
        this.getWriter()
                .writeRow(
                        parser.evaluate(record));
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
     */
    @Override
    public void run() {
        this.write();
        this.close();
    }

    /**
     *
     */
    public void write() {
        String[] record;
        String encode = readerSettings.getEncode();
        List<String> header = writerSettings.getHeader();

        //Identifies which file are being read. 
        parser.setFile(input);

        //Identifies if should decode the input file.
        Decoder decoder = FactoryDecoder.getDecoder(input);

        if (decoder != null) {
            this.setInput(decoder.decode(input, readerSettings.getProperties()));
        }

        //Identifies if the input file is empty after decode. 
        if (input.length() != 0) {
            //Sets the reader settings. 
            CsvParserSettings setting = new CsvParserSettings();
            setting.getFormat().setDelimiter(readerSettings.getDelimiter());
            setting.getFormat().setQuote(readerSettings.getQuote());
            setting.getFormat().setQuoteEscape(readerSettings.getQuoteEscape());
            setting.getFormat().setComment('\0');
            setting.setNullValue("");
            setting.setMaxCharsPerColumn(-1);
            setting.setInputBufferSize(5 * (1024 * 1024));
            setting.setNumberOfRowsToSkip(readerSettings.getSkipLines());

            //Identifies which encoder should be used. 
            if ("auto".equalsIgnoreCase(encode)) {
                try {
                    encode = UniversalDetector.detectCharset(input);
                } catch (IOException ex) {
                    Logger.getLogger(Output.class.getName()).log(Level.SEVERE, "Encode do not detected!", ex);
                } finally {
                    if (encode == null) {
                        encode = "UTF-8";
                    }
                }
            }

            //Identifies if header was provided or should be infered. 
            if (header.isEmpty()) {
                //Defines the pre parser settings.
                CsvParserSettings clone = setting.clone();
                clone.setHeaderExtractionEnabled(true);

                //Pre parser the output file.
                CsvParser pre = new CsvParser(clone);
                pre.beginParsing(input, encode);

                //Identifies if there are duplicated headers.
                Map<String, Integer> repeated = new HashMap();

                for (String field : pre.getRecordMetadata().headers()) {
                    if (!header.contains(field)) {
                        header.add(field);
                    } else {
                        int repetitions;

                        //Identifies how many times a header is repeated.  
                        if (repeated.containsKey(field)) {
                            repetitions = repeated.get(field) + 1;
                        } else {
                            repetitions = 1;
                        }

                        //Defines an index to identify repeated header. 
                        header.add(field + "_" + repetitions);
                        repeated.put(field, repetitions);
                    }
                }

                //Identifies if should log the file header. 
                if (parser
                        .getConfiguration()
                        .isDebug()) {
                    Logger.getLogger(Output.class.getName()).log(Level.INFO, "Header {0}", header.toString());
                }

                //Stop the pre parser.
                pre.stopParsing();

                //Marks the file first line to be ignored. 
                setting.setNumberOfRowsToSkip(
                        setting.getNumberOfRowsToSkip() + 1);
            }

            //Identifies the file header. 
            setting.setHeaders(header.toArray(new String[0]));

            //Identifies which columns should be read. 
            setting.selectFields(
                    parser.getConfiguration()
                            .getOriginalFieldName().toArray(new String[0]));

            //Read the input file and write to the output.
            CsvParser csvParser = new CsvParser(setting);
            csvParser.beginParsing(input, encode);

            while ((record = csvParser.parseNext()) != null) {
                this.writer.writeRow(
                        parser.evaluate(Arrays.asList((Object[]) record)));
            }

            //Stop the parser.
            csvParser.stopParsing();
        } else {
            Logger.getLogger(Output.class.getName()).log(Level.INFO, "File {0} is empty", input.getName());
        }

        //Idenfies if the input file should be removed. 
        if (readerSettings.isRemove()) {
            input.delete();
        }
    }

    /**
     *
     */
    public void close() {
        if (this.writer != null) {
            if (this.writer.getRecordCount() == 0) {
                this.writer.writeHeaders();
            } else {
                this.writer.flush();
            }

            this.writer.close();
        }
    }
}

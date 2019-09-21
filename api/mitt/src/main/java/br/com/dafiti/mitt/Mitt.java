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
package br.com.dafiti.mitt;

import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.model.Configuration;
import br.com.dafiti.mitt.output.Output;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.filefilter.WildcardFileFilter;

/**
 *
 * @author Valdiney V GOMES
 */
public class Mitt {

    public static final Character DELIMITER = ';';
    public static final Character QUOTE = '"';
    public static final Character QUOTE_ESCAPE = '"';
    public static final String ENCODE = "UTF-8";

    private char delimiter;
    private char quote;
    private char quoteEscape;
    private File file;
    private Configuration configuration;
    private Output output;
    private CommandLineInterface commandLineInterface;

    /**
     *
     */
    public Mitt() {
        this.delimiter = DELIMITER;
        this.quote = QUOTE;
        this.quoteEscape = QUOTE_ESCAPE;
    }

    /**
     *
     * @param file
     */
    public Mitt(File file) {
        this.file = file;
        this.delimiter = DELIMITER;
        this.quote = QUOTE;
        this.quoteEscape = QUOTE_ESCAPE;
    }

    /**
     *
     * @param file
     */
    public Mitt(String file) {
        this.file = new File(file);
        this.delimiter = DELIMITER;
        this.quote = QUOTE;
        this.quoteEscape = QUOTE_ESCAPE;
    }

    /**
     *
     * @param delimiter
     * @param quote
     * @param escape
     * @param file
     */
    public Mitt(
            File file,
            char delimiter,
            char quote,
            char escape) {

        this.file = file;
        this.delimiter = delimiter;
        this.quote = quote;
        this.quoteEscape = escape;
    }

    /**
     *
     * @param delimiter
     * @param quote
     * @param escape
     * @param file
     */
    public Mitt(
            String file,
            char delimiter,
            char quote,
            char escape) {

        this.file = new File(file);
        this.delimiter = delimiter;
        this.quote = quote;
        this.quoteEscape = escape;
    }

    /**
     *
     * @param delimiter
     */
    public void setDelimiter(char delimiter) {
        this.delimiter = delimiter;
    }

    /**
     *
     * @param quote
     */
    public void setQuote(char quote) {
        this.quote = quote;
    }

    /**
     *
     * @param quoteEscape
     */
    public void setQuoteEscape(char quoteEscape) {
        this.quoteEscape = quoteEscape;
    }

    /**
     *
     * @param file
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     *
     * @param output
     */
    public void setOutput(String output) {
        this.file = new File(output);
    }

    /**
     *
     * @return
     */
    public File getFile() {
        return file;
    }

    /**
     *
     * @return
     */
    public char getDelimiter() {
        return delimiter;
    }

    /**
     *
     * @return
     */
    public char getQuote() {
        return quote;
    }

    /**
     *
     * @return
     */
    public char getQuoteEscape() {
        return quoteEscape;
    }

    /**
     *
     * @return
     */
    public Configuration getConfiguration() {
        if (configuration == null) {
            configuration = new Configuration();
        }

        return configuration;
    }

    /**
     *
     * @return
     */
    private Output getWriter() {
        if (output == null) {
            output = new Output(
                    file,
                    this.getConfiguration(),
                    this.getDelimiter(),
                    this.getQuote(),
                    this.getQuoteEscape()
            );
        }

        return output;
    }

    /**
     *
     * @param arguments
     * @return
     */
    public CommandLineInterface getCommandLineInterface(String[] arguments) {
        if (commandLineInterface == null) {
            commandLineInterface = new CommandLineInterface(
                    this.getConfiguration(),
                    arguments
            );
        }

        return commandLineInterface;
    }

    /**
     *
     * @param record
     */
    public void write(List record) {
        this.getWriter().write(record);
    }

    /**
     *
     * @param record
     */
    public void write(String[] record) {
        this.getWriter().write(record);
    }

    /**
     *
     * @param file
     */
    public void write(File file) {
        this.getWriter().write(file,
                DELIMITER,
                QUOTE,
                QUOTE_ESCAPE,
                ENCODE,
                new ArrayList(),
                true,
                0
        );
    }

    /**
     *
     * @param file
     * @param delimiter
     * @param skipLines
     */
    public void write(
            File file,
            char delimiter,
            int skipLines) {

        this.getWriter().write(file,
                delimiter,
                QUOTE,
                QUOTE_ESCAPE,
                ENCODE,
                new ArrayList(),
                true,
                skipLines
        );
    }

    /**
     *
     * @param file
     * @param delimiter
     * @param quote
     * @param quoteEscape
     * @param encode
     */
    public void write(
            File file,
            char delimiter,
            char quote,
            char quoteEscape,
            String encode) {

        this.getWriter().write(
                file,
                delimiter,
                quote,
                quoteEscape,
                encode,
                new ArrayList(),
                true,
                0
        );
    }

    /**
     *
     * @param file
     * @param header
     */
    public void write(
            File file,
            List<String> header) {

        this.getWriter().write(file,
                DELIMITER,
                QUOTE,
                QUOTE_ESCAPE,
                ENCODE,
                header,
                true,
                0
        );
    }

    /**
     *
     * @param file
     * @param delimiter
     * @param quote
     * @param quoteEscape
     * @param encode
     * @param header
     */
    public void write(
            File file,
            char delimiter,
            char quote,
            char quoteEscape,
            String encode,
            List<String> header) {

        this.getWriter().write(
                file,
                delimiter,
                quote,
                quoteEscape,
                encode,
                header,
                true,
                0
        );
    }

    /**
     *
     * @param file
     * @param wildcard
     */
    public void write(
            File file,
            String wildcard) {

        this.write(file,
                wildcard,
                DELIMITER,
                QUOTE,
                QUOTE_ESCAPE,
                ENCODE,
                new ArrayList(),
                true,
                0
        );
    }

    /**
     *
     * @param file
     * @param wildcard
     * @param delimiter
     */
    public void write(
            File file,
            String wildcard,
            char delimiter) {

        this.write(file,
                wildcard,
                delimiter,
                QUOTE,
                QUOTE_ESCAPE,
                ENCODE,
                new ArrayList(),
                true,
                0
        );
    }

    /**
     *
     * @param file
     * @param wildcard
     * @param delimiter
     * @param header
     */
    public void write(
            File file,
            String wildcard,
            char delimiter,
            List<String> header) {

        this.write(file,
                wildcard,
                delimiter,
                QUOTE,
                QUOTE_ESCAPE,
                ENCODE,
                header,
                true,
                0
        );
    }

    /**
     *
     * @param file
     * @param wildcard
     * @param header
     */
    public void write(
            File file,
            String wildcard,
            List<String> header) {

        this.write(file,
                wildcard,
                DELIMITER,
                QUOTE,
                QUOTE_ESCAPE,
                ENCODE,
                header,
                true,
                0
        );
    }

    /**
     *
     * @param file
     * @param wildcard
     * @param delimiter
     * @param quote
     * @param quoteEscape
     * @param encode
     * @param header
     */
    public void write(
            File file,
            String wildcard,
            char delimiter,
            char quote,
            char quoteEscape,
            String encode,
            List<String> header) {

        this.write(
                file,
                wildcard,
                delimiter,
                quote,
                quoteEscape,
                encode,
                header,
                true,
                0
        );
    }

    /**
     *
     * @param file
     * @param wildcard
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
            String wildcard,
            char delimiter,
            char quote,
            char quoteEscape,
            String encode,
            List<String> header,
            boolean remove,
            int skipLines) {

        FileFilter fileFilter;
        File[] files = null;

        if (file.isDirectory()) {
            fileFilter = new WildcardFileFilter(wildcard);
            files = file.listFiles(fileFilter);
        }

        if (files != null) {
            this.getWriter().write(
                    files,
                    delimiter,
                    quote,
                    quoteEscape,
                    encode,
                    header,
                    remove,
                    skipLines
            );
        }
    }

    /**
     *
     */
    public void close() {
        if (file != null) {
            this.getWriter().close();
        }
    }
}

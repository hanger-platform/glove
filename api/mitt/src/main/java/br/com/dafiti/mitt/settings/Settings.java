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
package br.com.dafiti.mitt.settings;

/**
 *
 * @author Valdiney V GOMES
 */
abstract class Settings<T> {

    private char delimiter;
    private char quote;
    private char quoteEscape;
    private String encode;

    public static final Character DELIMITER = ';';
    public static final Character QUOTE = '"';
    public static final Character QUOTE_ESCAPE = '"';
    public static final String ENCODE = "UTF-8";

    public Settings() {
        this.delimiter = DELIMITER;
        this.quote = QUOTE;
        this.quoteEscape = QUOTE_ESCAPE;
        this.encode = ENCODE;
    }

    public char getDelimiter() {
        return delimiter;
    }

    public T setDelimiter(char delimiter) {
        this.delimiter = delimiter;
        return (T) this;
    }

    public char getQuote() {
        return quote;
    }

    public T setQuote(char quote) {
        this.quote = quote;
        return (T) this;
    }

    public char getQuoteEscape() {
        return quoteEscape;
    }

    public T setQuoteEscape(char quoteEscape) {
        this.quoteEscape = quoteEscape;
        return (T) this;
    }

    public String getEncode() {
        return encode;
    }

    public T setEncode(String encode) {
        this.encode = encode;
        return (T) this;
    }
}

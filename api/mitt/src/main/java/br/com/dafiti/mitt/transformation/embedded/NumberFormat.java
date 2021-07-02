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
package br.com.dafiti.mitt.transformation.embedded;
import br.com.dafiti.mitt.transformation.Parser;
import br.com.dafiti.mitt.transformation.Transformable;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Flavia LIMA
 * @author Valdiney V GOMES
 */
public class NumberFormat implements Transformable {
    private final String field;
    private final String language;
    private final String country;
    private java.text.NumberFormat numberFormat;
    public NumberFormat(String field, String language, String country) {
        this.field = field;
        this.language = language;
        this.country = country;
    }
    @Override
    public void init() {
        numberFormat = java.text.NumberFormat.getInstance(new Locale(language, country));
    }
    @Override
    public String getValue(
            Parser parser,
            List<Object> record) {
        String value = "";
        try {
            value = String.valueOf(
                    numberFormat.parse(
                            ((String) parser.evaluate(record, field))
                    )
            );
        } catch (ParseException ex) {
            Logger.getLogger(NumberFormat.class.getName()).log(Level.SEVERE, "Error formatting a number", ex);
        }
        return value;
    }
}
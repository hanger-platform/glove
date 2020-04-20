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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Converts UNIX timestamp date to an output format
 *
 * @author Helio Leal
 */
public class UnixTimestamp implements Transformable {

    private final Object field;
    private final String outputFormat;

    public UnixTimestamp(String field) {
        this.field = field;
        this.outputFormat = "yyyy-MM-dd HH:mm:ss";
    }

    public UnixTimestamp(String field, String outputFormat) {
        this.field = field;
        this.outputFormat = outputFormat;
    }

    @Override
    public void init() {
    }

    @Override
    public String getValue(
            Parser parser,
            List<Object> record) {

        String value = new String();
        Object unixTimestampDate = parser.evaluate(record, field);

        if (unixTimestampDate != null) {
            //Convert unixtimestamp seconds to milliseconds.
            Date date = new java.util.Date(Long.valueOf((String) unixTimestampDate) * 1000L);
            //The format of your output value.
            value = new SimpleDateFormat(outputFormat).format(date);
        }

        return value;
    }
}

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
import com.google.common.base.Splitter;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * Returns only a part of a field value based on a given delimiter
 *
 * @author Fernando Saga
 * @author Helio Leal
 */
public class SplitPart implements Transformable {

    private final String field;
    private final String delimiter;
    private final String position;

    public SplitPart(String field, String delimiter, String position) {
        this.field = field;
        this.delimiter = delimiter;
        this.position = position;
    }

    @Override
    public void init() {
    }

    @Override
    public String getValue(
            Parser parser,
            List<Object> record) {
        String value = "";

        if (StringUtils.isNumeric(this.position)) {
            List<String> list = Splitter
                    .on(this.delimiter)
                    .splitToList((String) parser.evaluate(record, field));

            //Identifies if array out of bounds won't happen.
            if (Integer.valueOf(this.position) < list.size()) {
                value = list.get(Integer.valueOf(this.position));
            }
        }

        return value;
    }
}

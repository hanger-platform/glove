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
package br.com.dafiti.mitt.transformation.embedded;

import br.com.dafiti.mitt.transformation.Parser;
import br.com.dafiti.mitt.transformation.Transformable;
import java.util.List;

/**
 *
 * @author Valdiney V GOMES
 */
public class Concat implements Transformable {

    private final List<Object> fields;
    private final String separator;

    public Concat(List<Object> fields) {
        this.fields = fields;
        this.separator = "";
    }

    public Concat(List<Object> fields, String separator) {
        this.fields = fields;
        this.separator = separator;
    }

    @Override
    public void init() {
    }

    @Override
    public String getValue(
            Parser parser,
            List<Object> record) {
        
        String value = new String();
        int total = 0;

        for (Object field : fields) {
            total++;
            value += parser.evaluate(record, field) + (fields.size() > total ? this.separator : "");
        }

        return value;
    }
}

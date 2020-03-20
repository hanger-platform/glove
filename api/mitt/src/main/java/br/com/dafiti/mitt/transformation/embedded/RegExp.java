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
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Helio Leal
 */
public class RegExp implements Transformable {

    private final Object field;
    private final String regexp;
    private final String defaultValue;
    private final HashMap<String, Pattern> patterns = new HashMap();

    public RegExp(String field, String regexp) {
        this.field = field;
        this.regexp = regexp;
        this.defaultValue = "";
    }

    public RegExp(String field, String regexp, String defaultValue) {
        this.field = field;
        this.regexp = regexp;
        this.defaultValue = defaultValue;
    }

    @Override
    public void init() {
    }

    @Override
    public String getValue(
            Parser parser,
            List<Object> record) {

        String value = defaultValue;
        Object content = parser.evaluate(record, field);
        Pattern pattern;
        Matcher matcher;

        if (content != null) {
            try {
                if (patterns.containsKey(regexp)) {
                    pattern = patterns.get(regexp);
                } else {
                    pattern = Pattern.compile(regexp);

                    if (pattern != null) {
                        patterns.put(regexp, Pattern.compile(regexp));
                    }
                }

                matcher = pattern.matcher(String.valueOf(content));

                if (matcher.find()) {
                    value = matcher.group();
                }
            } catch (Exception ex) {
                Logger.getLogger(DateFormat.class.getName()).log(Level.SEVERE, "Error parsing RegExp " + regexp, ex);
            }
        }

        return value;
    }
}

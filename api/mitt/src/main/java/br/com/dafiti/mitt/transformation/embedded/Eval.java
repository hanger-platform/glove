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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.apache.commons.lang3.math.NumberUtils;

/**
 *
 * @author Valdiney V GOMES
 */
public class Eval implements Transformable {

    private Parser parser;
    private List<Object> record;
    private List<String> fields;
    private final String expression;
    private final ScriptEngine engine;

    public Eval(String expression) {
        this.expression = expression;
        this.engine = new ScriptEngineManager().getEngineByName("JavaScript");
    }

    @Override
    public void init(
            Parser parser,
            List<Object> record) {

        this.parser = parser;
        this.record = record;
        this.fields = parser.getConfiguration().getFieldsName();
    }

    @Override
    public String getValue() {
        String value = new String();
        String parseable = this.expression;

        for (String chunck : expression.split("\\W")) {
            if (this.fields.contains(chunck)) {
                String evaluated = this.parser.evaluate(record, chunck).toString();
                parseable = parseable.replaceFirst(
                        chunck,
                        NumberUtils.isCreatable(evaluated) ? evaluated : '"' + evaluated + '"'
                );
            }
        }

        try {
            value = engine.eval(parseable).toString();
        } catch (ScriptException ex) {
            Logger.getLogger(Eval.class.getName()).log(Level.SEVERE, "Fail parsing expression " + this.expression, ex);
        }

        return value;
    }
}

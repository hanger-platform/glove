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
package br.com.dafiti.mitt.transformation;

import br.com.dafiti.mitt.model.Configuration;
import br.com.dafiti.mitt.model.Field;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Valdiney V GOMES
 */
public class Parser {

    private final Map<String, Field> fields = new ConcurrentHashMap<>();

    private File file;
    private final Scanner scanner;
    private final Configuration configuration;

    /**
     *
     * @param file
     */
    public void setFile(File file) {
        this.file = file;
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
     * @param configuration
     */
    public Parser(Configuration configuration) {
        this.scanner = Scanner.getInstance();
        this.configuration = configuration;
    }

    /**
     *
     * @param configuration
     * @param debug
     */
    public Parser(Configuration configuration, boolean debug) {
        this.scanner = Scanner.getInstance();
        this.configuration = configuration;
    }

    /**
     *
     * @return
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     *
     * @param record
     * @return
     */
    public List<Object> evaluate(List<Object> record) {
        List<Object> values = new ArrayList();

        configuration
                .getFields()
                .forEach((field) -> {
                    values.add(
                            this.evaluate(record, field)
                    );
                });

        //Logs input and parsed output record. 
        if (configuration.isDebug()) {
            Logger.getLogger(Parser.class.getName()).log(Level.INFO, "{0} -> {1}", new Object[]{record, values});
        }

        return values;
    }

    /**
     *
     * @param record
     * @param fieldName
     * @return
     */
    public Object evaluate(
            List<Object> record,
            String fieldName) {

        return this.evaluate(
                record,
                new Field(fieldName));
    }

    /**
     *
     * @param record
     * @param object
     * @return
     */
    public Object evaluate(
            List<Object> record,
            Object object) {

        Field field;

        //Identifies if the object is a field. 
        if (object instanceof Field) {
            field = (Field) object;
        } else {
            //Get the object identifier. 
            String identifier = String.valueOf(object);

            //Identifies if it is in cache. 
            if (fields.containsKey(identifier)) {
                //Gets field from cache. 
                field = fields.get(identifier);
            } else {
                //Puts the scanned field in cache. 
                field = scanner.scan(identifier);
                fields.put(identifier, field);
            }
        }

        return this.evaluate(record, field);
    }

    /**
     *
     * @param record
     * @param field
     * @return
     */
    public Object evaluate(
            List<Object> record,
            Field field) {

        Object value = null;

        //Identifies if the field has transformation. 
        if (field.getTransformation() != null) {
            value = field
                    .getTransformation()
                    .getValue(this, record);
        } else {
            //Identifies if it is an original field.          
            Integer index = configuration.getOriginalFieldIndex(field);

            if (index != null) {
                if (index < record.size()) {
                    //Picks up value from the record. 
                    value = record.get(index);
                }
            } else {
                //Identifies if it is a custom field.   
                index = configuration.getFieldIndex(field);

                if (index != null) {
                    Field clone = configuration
                            .getFields()
                            .get(index);

                    //Identifies if a field has transformation.
                    if (clone.getTransformation() != null) {
                        value = clone
                                .getTransformation()
                                .getValue(this, record);
                    } else {
                        Logger.getLogger(Parser.class.getName()).log(Level.SEVERE, "Field {0} does not have value or transformation. Impaired record {1}!", new Object[]{field.getName(), record});
                    }
                } else {
                    Logger.getLogger(Parser.class.getName()).log(Level.SEVERE, "Field {0} using expression {1} does not exists!", new Object[]{field.getName(), field.getExpression()});
                }
            }
        }

        return value;
    }
}

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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Valdiney V GOMES
 */
public class Parser {

    private final Configuration configuration;
    private File file;

    /**
     *
     * @param configuration
     */
    public Parser(Configuration configuration) {
        this.configuration = configuration;
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
     * @return
     */
    public File getFile() {
        return file;
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
     * @param fieldName
     * @return
     */
    public Object evaluate(
            List<Object> record,
            String fieldName) {

        return this.evaluate(
                record,
                new Field(fieldName)
        );
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

        if (object instanceof Field) {
            field = (Field) object;
        } else {
            field = Scanner.getInstance().scan((String) object);
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

        Object evaluated = null;

        //Identifies if field has transformation. 
        if (field.getTransformation() != null) {
            evaluated = field.getTransformation(this, record).getValue();
        } else {
            //Get only original fields. 
            List<Field> fields = configuration.getOriginalFields();

            //Identifies if field exists.          
            int index = fields.indexOf(field);

            if (index != -1) {
                if (index < record.size()) {
                    evaluated = record.get(fields.indexOf(field));
                }
            } else {
                //Get all fields. 
                fields = configuration.getFields();

                //Identifies if field exists.
                index = fields.indexOf(field);

                if (index != -1) {
                    Field other = fields.get(index);

                    //Identifies if the field has transformation.
                    if (other.getTransformation() != null) {
                        evaluated = other.getTransformation(this, record).getValue();
                    } else {
                        Logger.getLogger(Parser.class.getName()).log(Level.SEVERE, "Field {0} does not have related value or transformation!", field.getName());
                    }
                } else {
                    Logger.getLogger(Parser.class.getName()).log(Level.SEVERE, "Field {0} does not exists!", field.getName());
                }
            }
        }

        return evaluated;
    }

    /**
     *
     * @param record
     * @return
     */
    public List<Object> evaluate(List<Object> record) {
        List<Object> fields = new ArrayList();

        configuration.getFields().forEach((field) -> {
            fields.add(this.evaluate(record, field));
        });

        return fields;
    }
}

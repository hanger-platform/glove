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
import java.util.stream.Collectors;

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
    public List<Field> listField() {
        return configuration.getFields();
    }

    /**
     *
     * @return
     */
    public List<String> listFieldName() {
        return this.listFieldName(false);
    }

    /**
     *
     * @param table
     * @return
     */
    public List<String> listFieldName(boolean table) {
        List<String> nameList = new ArrayList();

        this.listField().forEach((field) -> {
            String name = field.getAlias() == null || field.getAlias().isEmpty()
                    ? field.getName()
                    : field.getAlias();

            if (table) {
                name = name.replaceAll("\\W", "_").toLowerCase();
            }

            nameList.add(name);
        });

        return nameList;
    }

    /**
     *
     * @return
     */
    public List<Transformable> listFieldTransformation() {
        List<Transformable> transformation = new ArrayList();

        this.listField().forEach((value) -> {
            transformation.add(value.getTransformation());
        });

        return transformation;
    }

    /**
     *
     * @return
     */
    public List<Field> listOriginalField() {
        List<Field> original = this
                .listField()
                .stream()
                .filter(originalField -> originalField.isOriginal())
                .collect(Collectors.toList());

        return original;
    }

    /**
     *
     * @return
     */
    public List<String> listOriginalFieldName() {
        List<String> fieldName = new ArrayList();

        this.listField()
                .stream()
                .filter(originalField -> originalField.isOriginal())
                .collect(Collectors.toList()).forEach((value) -> {
            fieldName.add(value.getName());
        });

        return fieldName;
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

        if (field.getTransformation() == null) {
            int index = this.listOriginalField().indexOf(field);

            if (index == -1) {
                Logger.getLogger(Parser.class.getName()).log(Level.SEVERE, "Field {0} does not exists!", field.getName());
            } else {
                if (index < record.size()) {
                    evaluated = record.get(this.listOriginalField().indexOf(field));
                }
            }
        } else {
            evaluated = field.getTransformation(this, record).getValue();
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

        this.listField().forEach((field) -> {
            fields.add(this.evaluate(record, field));
        });

        return fields;
    }
}

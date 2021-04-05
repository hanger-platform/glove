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
package br.com.dafiti.mitt.model;

import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.transformation.Scanner;
import br.com.dafiti.mitt.transformation.Transformable;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Valdiney V GOMES
 */
public class Configuration {

    private final List<Field> originalField;
    private final List<String> fieldName;
    private final List<String> originalFieldName;
    private final List<String> normalizedFieldName;
    private final List<Transformable> tranformation;
    private final Map<Field, Integer> fieldIndex;
    private final Map<Field, Integer> originalFieldIndex;

    private final boolean debug;
    private final Scanner scanner;
    private final List<Field> fields = new ArrayList();
    private final List<Parameter> parameters = new ArrayList();

    public Configuration() {
        this.scanner = Scanner.getInstance();

        this.originalField = new ArrayList();
        this.fieldName = new ArrayList();
        this.originalFieldName = new ArrayList();
        this.normalizedFieldName = new ArrayList();
        this.tranformation = new ArrayList();
        this.fieldIndex = new HashMap();
        this.originalFieldIndex = new HashMap();

        this.debug = false;
    }

    public Configuration(boolean debug) {
        this.scanner = Scanner.getInstance();

        this.originalField = new ArrayList();
        this.fieldName = new ArrayList();
        this.originalFieldName = new ArrayList();
        this.normalizedFieldName = new ArrayList();
        this.tranformation = new ArrayList();
        this.fieldIndex = new HashMap();
        this.originalFieldIndex = new HashMap();

        this.debug = debug;
    }

    /**
     *
     * @return
     */
    public List<Field> getFields() {
        return fields;
    }

    /**
     *
     * @return
     */
    public List<String> getFieldName() {
        return this.getFieldsName(false);
    }

    /**
     *
     * @param cleanup
     * @return
     */
    public List<String> getFieldsName(boolean cleanup) {
        return fieldName;
    }

    /**
     *
     * @return
     */
    public List<Transformable> getFieldsTransformation() {
        return tranformation;
    }

    /**
     *
     * @return
     */
    public List<Field> getOriginalField() {
        return originalField;
    }

    /**
     *
     * @return
     */
    public List<String> getOriginalFieldName() {
        return originalFieldName;
    }

    /**
     *
     * @param field
     * @return
     */
    public Integer getFieldIndex(Field field) {
        return fieldIndex.get(field);
    }

    /**
     *
     * @param field
     * @return
     */
    public Integer getOriginalFieldIndex(Field field) {
        return originalFieldIndex.get(field);
    }

    /**
     *
     * @param field
     * @throws DuplicateEntityException
     */
    private void addField(Field field) throws DuplicateEntityException {
        if (!fields.contains(field)) {
            String name
                    = field.getAlias() == null || field.getAlias().isEmpty()
                    ? field.getName()
                    : field.getAlias();

            fields.add(field);
            fieldName.add(name);
            fieldIndex.put(field, (fieldIndex.size() - 1) + 1);
            tranformation.add(field.getTransformation());

            if (field.isOriginal()) {
                originalField.add(field);
                originalFieldName.add(field.getName());
                originalFieldIndex.put(field, (originalFieldIndex.size() - 1) + 1);
            }

            normalizedFieldName.add(Normalizer
                    .normalize(name, Normalizer.Form.NFD)
                    .replaceAll("[^\\p{ASCII}]", "")
                    .replaceAll("\\W", "_")
                    .replaceAll("^_", "")
                    .replaceAll("_$", "")
                    .toLowerCase());
        } else {
            throw new DuplicateEntityException("Duplicated field: " + field.getName());
        }
    }

    /**
     *
     * @param name
     * @return
     * @throws br.com.dafiti.mitt.exception.DuplicateEntityException
     */
    public Configuration addField(String name) throws DuplicateEntityException {
        this.addField(scanner.scan(name));
        return this;
    }

    /**
     *
     * @param name
     * @param alias
     * @return
     * @throws br.com.dafiti.mitt.exception.DuplicateEntityException
     */
    public Configuration addField(
            String name,
            String alias) throws DuplicateEntityException {

        this.addField(
                new Field(name, alias));

        return this;
    }

    /**
     *
     * @param fields
     * @return
     * @throws DuplicateEntityException
     */
    public Configuration addField(List fields) throws DuplicateEntityException {
        for (Object field : fields) {

            if (field instanceof Field) {
                this.addField((Field) field);
            } else {
                this.addField(scanner.scan((String) field));
            }
        }

        return this;
    }

    /**
     *
     * @param name
     * @param transformation
     * @return
     * @throws br.com.dafiti.mitt.exception.DuplicateEntityException
     */
    public Configuration addField(
            String name,
            Transformable transformation) throws DuplicateEntityException {

        this.addField(
                new Field(name, transformation));

        return this;
    }

    /**
     *
     * @param name
     * @param alias
     * @param transformation
     * @return
     * @throws br.com.dafiti.mitt.exception.DuplicateEntityException
     */
    public Configuration addField(
            String name,
            String alias,
            Transformable transformation) throws DuplicateEntityException {

        this.addField(
                new Field(name, alias, transformation));

        return this;
    }

    /**
     *
     * @param name
     * @return
     * @throws br.com.dafiti.mitt.exception.DuplicateEntityException
     */
    public Configuration addCustomField(String name) throws DuplicateEntityException {
        this.addField(scanner.scan(name, false));
        return this;
    }

    /**
     *
     * @param name
     * @param transformation
     * @return
     * @throws br.com.dafiti.mitt.exception.DuplicateEntityException
     */
    public Configuration addCustomField(
            String name,
            Transformable transformation) throws DuplicateEntityException {

        this.addField(
                new Field(
                        name,
                        transformation,
                        false));

        return this;
    }

    /**
     *
     * @return
     */
    public List<Parameter> getParameters() {
        return parameters;
    }

    /**
     *
     * @param field
     * @throws DuplicateEntityException
     */
    private void addParameter(Parameter parameter) throws DuplicateEntityException {
        if (!parameters.contains(parameter)) {
            parameters.add(parameter);
        } else {
            throw new DuplicateEntityException("Duplicated parameter: " + parameter.getName());
        }
    }

    /**
     *
     * @param abreviation
     * @param name
     * @param description
     * @return
     * @throws DuplicateEntityException
     */
    public Configuration addParameter(
            String abreviation,
            String name,
            String description) throws DuplicateEntityException {

        this.addParameter(
                new Parameter(
                        abreviation,
                        name,
                        description,
                        "",
                        true,
                        true));

        return this;
    }

    /**
     *
     * @param abreviation
     * @param name
     * @param description
     * @param defaultValue
     * @return
     * @throws DuplicateEntityException
     */
    public Configuration addParameter(
            String abreviation,
            String name,
            String description,
            String defaultValue) throws DuplicateEntityException {

        this.addParameter(
                new Parameter(
                        abreviation,
                        name,
                        description,
                        defaultValue,
                        true,
                        true));

        return this;
    }

    /**
     *
     * @param abreviation
     * @param name
     * @param description
     * @param argument
     * @return
     * @throws DuplicateEntityException
     */
    public Configuration addParameter(
            String abreviation,
            String name,
            String description,
            boolean argument) throws DuplicateEntityException {

        this.addParameter(
                new Parameter(
                        abreviation,
                        name,
                        description,
                        "",
                        argument,
                        true));

        return this;
    }

    /**
     *
     * @param abreviation
     * @param name
     * @param description
     * @param defaultValue
     * @param argument
     * @return
     * @throws DuplicateEntityException
     */
    public Configuration addParameter(
            String abreviation,
            String name,
            String description,
            String defaultValue,
            boolean argument) throws DuplicateEntityException {

        this.addParameter(
                new Parameter(
                        abreviation,
                        name,
                        description,
                        defaultValue,
                        argument,
                        true));

        return this;
    }

    /**
     *
     * @param abreviation
     * @param name
     * @param description
     * @param defaultValue
     * @param argument
     * @param optional
     * @return
     * @throws DuplicateEntityException
     */
    public Configuration addParameter(
            String abreviation,
            String name,
            String description,
            String defaultValue,
            boolean argument,
            boolean optional) throws DuplicateEntityException {

        this.addParameter(
                new Parameter(
                        abreviation,
                        name,
                        description,
                        defaultValue,
                        argument,
                        optional));

        return this;
    }

    /**
     *
     * @return
     */
    public boolean isDebug() {
        return debug;
    }
}

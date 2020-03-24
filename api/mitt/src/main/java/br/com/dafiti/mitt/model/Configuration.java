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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author Valdiney V GOMES
 */
public class Configuration {

    private List<Field> originalFields;
    private List<Transformable> fieldTransformations;
    private Map<Field, Integer> fieldIndex;
    private Map<Field, Integer> originalFieldIndex;

    private final boolean debug;
    private final Scanner scanner;
    private final List<Field> fields = new ArrayList();
    private final List<Parameter> parameters = new ArrayList();

    public Configuration() {
        this.debug = false;
        this.scanner = new Scanner();
    }

    public Configuration(boolean debug) {
        this.debug = debug;
        this.scanner = new Scanner();
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
    public List<String> getFieldsName() {
        return this.getFieldsName(false);
    }

    /**
     *
     * @param removeSpecialCharacteres
     * @return
     */
    public List<String> getFieldsName(boolean removeSpecialCharacteres) {
        List<String> nameList = new ArrayList();

        this.fields.forEach((field) -> {
            String name = field.getAlias() == null || field.getAlias().isEmpty()
                    ? field.getName()
                    : field.getAlias();

            if (removeSpecialCharacteres) {
                name = name.replaceAll("\\W", "_").toLowerCase();
                name = name.replaceAll("^_", "");
                name = name.replaceAll("_$", "");
            }

            nameList.add(name);
        });

        return nameList;
    }

    /**
     *
     * @return
     */
    public List<Transformable> getFieldsTransformation() {
        if (fieldTransformations == null) {
            fieldTransformations = new ArrayList();

            this.fields.forEach((field) -> {
                fieldTransformations.add(field.getTransformation());
            });
        }

        return fieldTransformations;
    }

    /**
     *
     * @return
     */
    public List<Field> getOriginalFields() {
        if (originalFields == null) {
            originalFields = new ArrayList();
            
            this.fields.forEach((field) -> {
                if (field.isOriginal()) {
                    originalFields.add(field);
                }
            });
        }

        return originalFields;
    }

    /**
     *
     * @return
     */
    public List<String> getOriginalFieldsName() {
        List<String> fieldName = new ArrayList();

        this
                .getOriginalFields()
                .stream()
                .collect(Collectors.toList()).forEach((value) -> {
            fieldName.add(value.getName());
        });

        return fieldName;
    }

    /**
     *
     * @param field
     * @return
     */
    public Integer getFieldIndex(Field field) {
        if (fieldIndex == null) {
            fieldIndex = new HashMap();

            List<Field> all = this.getFields();

            for (int i = 0; i < all.size(); i++) {
                fieldIndex.put(all.get(i), i);
            }
        }

        return fieldIndex.get(field);
    }

    /**
     *
     * @param field
     * @return
     */
    public Integer getOriginalFieldIndex(Field field) {
        if (originalFieldIndex == null) {
            originalFieldIndex = new HashMap();

            List<Field> originals = this.getOriginalFields();

            for (int i = 0; i < originals.size(); i++) {
                originalFieldIndex.put(originals.get(i), i);
            }
        }

        return originalFieldIndex.get(field);
    }

    /**
     *
     * @param field
     * @throws DuplicateEntityException
     */
    private void addField(Field field) throws DuplicateEntityException {
        if (!fields.contains(field)) {
            fields.add(field);
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

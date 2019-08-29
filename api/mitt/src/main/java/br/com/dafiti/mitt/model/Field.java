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

import br.com.dafiti.mitt.transformation.Parser;
import br.com.dafiti.mitt.transformation.Transformable;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author Valdiney V GOMES
 */
public class Field {

    private final String name;
    private final Transformable transformation;
    private final String alias;
    private final boolean original;

    /**
     *
     * @param name
     */
    public Field(String name) {
        this.name = name;
        this.transformation = null;
        this.alias = null;
        this.original = true;
    }

    /**
     *
     * @param name
     * @param alias
     */
    public Field(
            String name, 
            String alias) {
        
        this.name = name;
        this.transformation = null;
        this.alias = alias;
        this.original = true;
    }

    /**
     *
     * @param name
     * @param transformation
     */
    public Field(
            String name, 
            Transformable transformation) {
        
        this.name = name;
        this.transformation = transformation;
        this.alias = null;
        this.original = true;
    }

    /**
     *
     * @param name
     * @param alias
     * @param transformation
     */
    public Field(
            String name, 
            String alias, 
            Transformable transformation) {
        
        this.name = name;
        this.transformation = transformation;
        this.alias = alias;
        this.original = true;
    }

    /**
     *
     * @param name
     * @param transformation
     * @param original
     */
    public Field(
            String name,
            Transformable transformation,
            boolean original) {

        this.name = name;
        this.transformation = transformation;
        this.alias = null;
        this.original = original;
    }

    /**
     *
     * @param name
     * @param alias
     * @param transformation
     * @param original
     */
    public Field(
            String name,
            String alias,
            Transformable transformation,
            boolean original) {

        this.name = name;
        this.transformation = transformation;
        this.alias = alias;
        this.original = original;
    }

    /**
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @return
     */
    public Transformable getTransformation() {
        return transformation;
    }

    /**
     *
     * @param parser
     * @param record
     * @return
     */
    public Transformable getTransformation(
            Parser parser, 
            List<Object> record) {
        
        transformation.init(parser, record);
        return transformation;
    }

    /**
     *
     * @return
     */
    public String getAlias() {
        return alias;
    }

    /**
     *
     * @return
     */
    public boolean isOriginal() {
        return original;
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + Objects.hashCode(this.name);
        return hash;
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        final Field other = (Field) obj;
        return Objects.equals(this.name, other.name);
    }
}

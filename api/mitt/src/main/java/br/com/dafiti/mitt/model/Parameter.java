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

import java.util.Objects;

/**
 *
 * @author Valdiney V GOMES
 */
public class Parameter {

    private final String abreviation;
    private final String name;
    private final String description;
    private final String defaultValue;
    private final boolean argument;
    private final boolean optional;

    /**
     *
     * @param abreviation
     * @param name
     * @param argument
     * @param defaultValue
     * @param description
     * @param optional
     */
    public Parameter(
            String abreviation,
            String name,
            String description,
            String defaultValue,
            boolean argument,
            boolean optional) {

        this.abreviation = abreviation;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
        this.argument = argument;
        this.optional = optional;
    }

    public String getAbreviation() {
        return abreviation;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public boolean hasArgument() {
        return argument;
    }

    public boolean isOptional() {
        return optional;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.name);
        return hash;
    }

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
        final Parameter other = (Parameter) obj;
        return Objects.equals(this.name, other.name);
    }
}

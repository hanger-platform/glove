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

import br.com.dafiti.mitt.model.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;

/**
 *
 * @author Valdiney V GOMES
 */
public class Scanner {

    private static Scanner self;
    private final Map<String, Class<? extends Transformable>> transformations = new HashMap();

    /**
     *
     * @return
     */
    public static Scanner getInstance() {
        if (self == null) {
            self = new Scanner();
        }

        return self;
    }

    /**
     *
     */
    private Scanner() {
        Set<Class<? extends Transformable>> classes = new Reflections().getSubTypesOf(Transformable.class);
        Iterator<Class<? extends Transformable>> iterator = classes.iterator();

        while (iterator.hasNext()) {
            Class<? extends Transformable> tranformation = iterator.next();
            String transformationName = tranformation.getSimpleName().toLowerCase();
            this.transformations.put(transformationName, tranformation);
        }
    }

    /**
     *
     * @param content
     * @return
     */
    public Field scan(String content) {
        return this.scan(content, true);
    }

    /**
     *
     * @param content
     * @param original
     * @return
     */
    public Field scan(String content, boolean original) {
        String name;
        Field field;
        Transformable instance = null;

        if (content.contains("::")) {
            String chunck = "";
            boolean partial = false;
            List<String> parameters = new ArrayList();
            List<String> functionListParameterItem = new ArrayList();

            name = StringUtils.substringBefore(content, "::");

            if (name.isEmpty()) {
                name = "anonymous_" + UUID.randomUUID();
            }

            String function = StringUtils.substringAfter(content, "::");
            String functionName = StringUtils.substringBefore(function, "(").toLowerCase();
            String functionParameter = StringUtils.substringBeforeLast(StringUtils.substringAfter(function, "("), ")");
            String functionListParameter = StringUtils.substringBefore(StringUtils.substringAfter(functionParameter, "["), "]");
            String[] functionListParameterChunck = StringUtils.split(functionListParameter, ',');

            for (String string : functionListParameterChunck) {
                if (string.contains("(") && string.contains(")")) {
                    functionListParameterItem.add(string);
                } else if (string.contains("(")) {
                    partial = true;
                    chunck = string + ",";
                } else if (partial && !(string.contains("(") || string.contains(")"))) {
                    chunck += string + ",";
                } else if (string.contains(")") && partial) {
                    partial = false;
                    functionListParameterItem.add(chunck + string);
                    chunck = "";
                } else {
                    functionListParameterItem.add(string);
                }
            }

            if (functionParameter.startsWith("{") && functionParameter.endsWith("}")) {
                parameters.add(
                        functionParameter
                                .replace("{", "")
                                .replace("}", "")
                );
            } else {
                functionParameter = functionParameter.replace(
                        functionListParameter,
                        Base64.encodeBase64String(
                                String.join("+", functionListParameterItem).getBytes()
                        )
                );

                parameters = Arrays.asList(
                        StringUtils
                                .split(functionParameter, ',')
                );
            }

            if (this.transformations.containsKey(functionName)) {
                try {
                    Class<? extends Transformable> clazz = this.transformations.get(functionName);
                    Constructor[] constructors = clazz.getDeclaredConstructors();

                    if (constructors.length == 0) {
                        instance = clazz.newInstance();
                    } else {
                        for (Constructor constructor : constructors) {
                            int functionParameterCount = parameters.size();

                            if (functionParameterCount == 0) {
                                instance = clazz.newInstance();
                                break;
                            } else {
                                if (constructor.getParameterCount() == functionParameterCount) {
                                    List<Object> constructorParameters = new ArrayList();

                                    for (int i = 0; i < functionParameterCount; i++) {
                                        switch (constructor.getParameterTypes()[i].getSimpleName()) {
                                            case "List":
                                                List<Field> fieldList = new ArrayList();

                                                for (String parameter : parameters) {
                                                    if (parameter.startsWith("[") && parameter.endsWith("]")) {
                                                        String[] fields = new String(
                                                                Base64.decodeBase64(
                                                                        parameter.replace("[", "").replace("]", ""))
                                                        ).split("\\+");

                                                        for (String parameterField : fields) {
                                                            fieldList.add(this.scan(parameterField));
                                                        }
                                                    }
                                                }

                                                constructorParameters.add(fieldList);
                                                break;
                                            case "Field":
                                                constructorParameters.add(this.scan(parameters.get(i)));
                                            case "String":
                                                constructorParameters.add(parameters.get(i));
                                                break;
                                            default:
                                                if (constructor.getParameterTypes()[i].isPrimitive()) {
                                                    Method method = constructor
                                                            .getParameterTypes()[i]
                                                            .getDeclaredMethod("valueOf", String.class);
                                                    constructorParameters.add(method.invoke(parameters.get(i)));
                                                }

                                                break;
                                        }
                                    }

                                    instance = (Transformable) constructor.newInstance(constructorParameters.toArray());
                                }
                            }
                        }
                    }
                } catch (InstantiationException
                        | IllegalAccessException
                        | IllegalArgumentException
                        | InvocationTargetException
                        | NoSuchMethodException
                        | SecurityException ex) {

                    Logger.getLogger(Scanner.class.getName()).log(Level.SEVERE, "Fail evaluating transformation " + functionName + " with parameter  " + functionParameter, ex);
                }
            }
        } else {
            name = content;
        }

        if (instance == null) {
            field = new Field(name);
        } else {
            field = new Field(name, instance, original);
        }

        return field;
    }
}

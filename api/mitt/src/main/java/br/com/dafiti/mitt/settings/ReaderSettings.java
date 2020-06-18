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
package br.com.dafiti.mitt.settings;

import java.io.FileReader;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Valdiney V GOMES
 */
public class ReaderSettings extends Settings<ReaderSettings> {

    private int skipLines;
    private boolean remove;
    private Properties properties;

    public ReaderSettings() {
        this.skipLines = 0;
        this.remove = true;
    }

    public int getSkipLines() {
        return skipLines;
    }

    public ReaderSettings setSkipLines(int skipLines) {
        this.skipLines = skipLines;
        return this;
    }

    public boolean isRemove() {
        return remove;
    }

    public ReaderSettings setRemove(boolean remove) {
        this.remove = remove;
        return this;
    }

    public Properties getProperties() {
        if (properties == null) {
            properties = new Properties();
        }

        return properties;
    }

    public ReaderSettings setProperties(String payload) {
        try {
            JSONParser parser = new JSONParser();
            JSONObject propertyObject = (JSONObject) parser.parse(payload);

            if (!propertyObject.isEmpty()) {
                propertyObject
                        .keySet()
                        .forEach(e -> this.addProperties((String) e, (String) propertyObject.get(e)));
            }
        } catch (ParseException ex) {
            Logger.getLogger(ReaderSettings.class.getName()).log(Level.SEVERE, "Fail parsing reader settings payload", ex);
        }

        return this;
    }

    public ReaderSettings addProperties(String key, String value) {
        this.getProperties().setProperty(key, value);
        return this;
    }
}

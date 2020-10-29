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
package br.com.dafiti.braze;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Fernando Saga
 */
public class Detail {

    private final String endpointDetail;
    private final String output;
    private final String service;
    private final List key;
    private final List partition;
    private final List fields;
    private final int sleep;
    private final JSONObject credentials;

    public Detail(
            String endpointDetail,
            String output,
            String service,
            List key,
            List partition,
            List fields,
            int sleep,
            JSONObject credentials) {

        this.endpointDetail = endpointDetail;
        this.output = output;
        this.service = service;
        this.key = key;
        this.partition = partition;
        this.fields = fields;
        this.sleep = sleep;
        this.credentials = credentials;
    }

    void extract() throws IOException, ParseException, DuplicateEntityException {
        //Defines a MITT instance. 
        Mitt mitt = new Mitt();

        //Defines output file.
        mitt.setOutputFile(this.output);

        //Defines fields.
        mitt.getConfiguration()
                .addCustomField("partition_field", new Concat((List) this.partition))
                .addCustomField("custom_primary_key", new Concat((List) this.key))
                .addCustomField("etl_load_date", new Now())
                .addField(this.fields);

        //Identifies original fields.
        List<String> listFields = mitt.getConfiguration().getOriginalFieldsName();

        //Connect to API.
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(this.endpointDetail).openConnection();
        httpURLConnection.setRequestProperty("Authorization", (String) credentials.get("authorization"));
        httpURLConnection.setRequestProperty("Accept", "application/json");
        httpURLConnection.setRequestMethod("GET");

        //Get API Call response.
        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(httpURLConnection.getInputStream()))) {
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                JSONArray jsonArray
                        = (JSONArray) ((JSONObject) new JSONParser()
                                .parse(line))
                                .get(this.service);

                Logger.getLogger(Braze.class.getName()).log(Level.INFO, "{0} {1} found ", new Object[]{jsonArray.size(), this.service});

                //Fetchs notifications list.
                for (Object object : jsonArray) {
                    List record = new ArrayList();

                    listFields.forEach((field) -> {
                        //Identifies if the field exists.
                        if (((JSONObject) object).containsKey(field)) {
                            record.add(((JSONObject) object).get(field));
                        } else {
                            record.add(null);
                        }
                    });

                    mitt.write(record);
                }
            }
        }

        httpURLConnection.disconnect();

        //Identify if has sleep time until next API call.
        if (this.sleep > 0) {
            try {
                Logger.getLogger(Braze.class.getName())
                        .log(Level.INFO, "Sleeping {0} seconds until next API call", this.sleep);

                Thread.sleep(Long.valueOf(this.sleep * 1000));
            } catch (InterruptedException ex) {
                Logger.getLogger(Braze.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        mitt.close();
    }
}

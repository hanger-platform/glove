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
public class ListDetail {

    private final String endpointList;
    private final String endpointDetail;
    private final String output;
    private final String service;
    private final Character delimiter;
    private final List key;
    private final List partition;
    private final List fields;
    private final int sleep;    
    private final JSONObject credentials;

    public ListDetail(
            String endpointList,
            String endpointDetail,
            String output,
            String service,
            Character delimiter,
            List key,
            List partition,
            List fields,
            int sleep,
            JSONObject credentials) {

        this.endpointList = endpointList;
        this.endpointDetail = endpointDetail;
        this.output = output;
        this.service = service;
        this.delimiter = delimiter;
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

        boolean nextPage = true;
        int page = 0;

        while (nextPage) {
            String list = this.endpointList.replace("<<page>>", String.valueOf(page));

            Logger.getLogger(Braze.class.getName()).log(Level.INFO, "Retrieving data from URL: {0}", new Object[]{list});

            //Connect to API.
            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(list).openConnection();
            httpURLConnection.setRequestProperty("Authorization", (String) credentials.get("authorization"));
            httpURLConnection.setRequestProperty("Accept", "application/json");
            httpURLConnection.setRequestMethod("GET");

            //Get API Call response.
            try (BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(httpURLConnection.getInputStream()))) {
                String listJSON;

                //Get service list from API.
                while ((listJSON = bufferedReader.readLine()) != null) {
                    JSONArray jsonArray
                            = (JSONArray) ((JSONObject) new JSONParser()
                                    .parse(listJSON))
                                    .get(this.service);

                    Logger.getLogger(Braze.class.getName()).log(Level.INFO, "{0} {1} found ", new Object[]{jsonArray.size(), this.service});

                    //Identify if at least 1 service was found on the page.
                    if (jsonArray.size() > 0) {
                        page++;

                        //Fetchs service list.
                        for (Object object : jsonArray) {
                            String detail = this.endpointDetail
                                    .replace(
                                            "<<id>>",
                                            String.valueOf(((JSONObject) object).get("id"))
                                    );

                            //Connect to API.
                            HttpURLConnection connectionDetails = (HttpURLConnection) new URL(detail).openConnection();
                            connectionDetails.setRequestProperty("Authorization", (String) credentials.get("authorization"));
                            connectionDetails.setRequestProperty("Accept", "application/json");
                            connectionDetails.setRequestMethod("GET");

                            //Get API Call response.
                            try (BufferedReader bfDetail = new BufferedReader(
                                    new InputStreamReader(connectionDetails.getInputStream()))) {
                                String line;

                                //Get a details from API.
                                while ((line = bfDetail.readLine()) != null) {
                                    java.util.List record = new ArrayList();
                                    JSONObject details = (JSONObject) new JSONParser().parse(line);

                                    listFields.forEach((field) -> {
                                        //Identifies if the field exists.
                                        if (details.containsKey(field)) {
                                            record.add(details.get(field));
                                        } else if ("id".equals(field)) {
                                            record.add(String.valueOf(((JSONObject) object).get("id")));
                                        } else {
                                            record.add(null);
                                        }
                                    });

                                    mitt.write(record);
                                }
                            }
                            connectionDetails.disconnect();
                        }
                    } else {
                        nextPage = false;
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
        }
    }
}

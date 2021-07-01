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
package br.com.dafiti.one.signal;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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
 * Extract the details of multiple notifications
 *
 * @author Helio Leal
 */
public class Notifications {

    private final String NOTIFICATIONS_URL = "https://onesignal.com/api/v1/notifications?app_id=<<app_id>>&offset=<<offset>>";
    private final String APP_URL = "https://onesignal.com/api/v1/apps/";
    private final String output;
    private final List<String> apps;
    private final List key;
    private final List partition;
    private final List fields;
    private final JSONObject credentials;
    private final int sleep;
    private final int limit;
    private final int retries;

    public Notifications(
            String output,
            List apps,
            List key,
            List partition,
            List fields,
            int sleep,
            JSONObject credentials,
            int limit,
            int retries) {
        this.output = output;
        this.apps = apps;
        this.key = key;
        this.partition = partition;
        this.fields = fields;
        this.credentials = credentials;
        this.sleep = sleep;
        this.limit = limit;
        this.retries = retries;
    }

    void extract() throws DuplicateEntityException, IOException, ParseException {
        //Defines a MITT instance. 
        Mitt mitt = new Mitt();

        //Defines output file.
        mitt.setOutputFile(this.output);

        //Defines fields.
        mitt.getConfiguration()
                .addCustomField("partition_field", new Concat(this.partition))
                .addCustomField("custom_primary_key", new Concat(this.key))
                .addCustomField("etl_load_date", new Now())
                .addField(this.fields);

        //Identifies original fields.
        List<String> fields = mitt.getConfiguration().getOriginalFieldName();

        //Fetchs all apps.
        for (String app : this.apps) {
            String authKey = this.getAuthKey(app);
            boolean nextOffset = true;
            int offset = 0;
            int limit = 50;

            // Check if it is to consider limit parameter.
            if (this.limit > 0) {
                limit = this.limit;
            }

            //Will loop throug all offsets.
            while (nextOffset) {
                String list = NOTIFICATIONS_URL
                        .replace("<<app_id>>", app)
                        .replace("<<offset>>", String.valueOf(offset));

                // Check if it is to consider limit parameter.
                if (this.limit > 0) {
                    list = list + "&limit=" + String.valueOf(this.limit);
                }

                //Retries if some error ocurrs, otherwise, breaks this for.
                for (int retry = 0; retry < retries; retry++) {
                    try {
                        nextOffset = true;

                        Logger.getLogger(OneSignal.class.getName()).log(Level.INFO, "Retrieving data from URL: {0}", new Object[]{list});

                        //Connect to API.
                        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(list).openConnection();
                        httpURLConnection.setRequestProperty("Authorization", "Basic " + authKey);
                        httpURLConnection.setRequestProperty("Accept", "application/json");
                        httpURLConnection.setRequestMethod("GET");

                        //Get API Call response.
                        try (BufferedReader bufferedReader = new BufferedReader(
                                new InputStreamReader(httpURLConnection.getInputStream()))) {
                            String response;

                            //Get service list from API.
                            while ((response = bufferedReader.readLine()) != null) {
                                JSONArray jsonArray
                                        = (JSONArray) ((JSONObject) new JSONParser()
                                                .parse(response))
                                                .get("notifications");

                                Logger.getLogger(OneSignal.class.getName()).log(Level.INFO, "{0} notifications found ", new Object[]{jsonArray.size()});

                                //Identify if at least 1 service was found on the page.
                                if (jsonArray.size() > 0) {
                                    offset += limit;

                                    //Fetchs notifications list.
                                    for (Object object : jsonArray) {
                                        List record = new ArrayList();

                                        fields.forEach((field) -> {
                                            //Identifies if the field exists.
                                            if (((JSONObject) object).containsKey(field)) {
                                                record.add(((JSONObject) object).get(field));
                                            } else {
                                                record.add(null);
                                            }
                                        });

                                        mitt.write(record);
                                    }
                                } else {
                                    nextOffset = false;
                                }
                            }
                        }
                        httpURLConnection.disconnect();

                        //If no errors ocurred, breaks the retry for.
                        break;
                    } catch (IOException ex) {
                        Logger.getLogger(OneSignal.class.getName()).log(Level.SEVERE, "GLOVE - One Signal fail: ", ex);
                        nextOffset = false;
                    }

                    //Identify if has sleep time until next API call.
                    if (this.sleep > 0) {
                        try {
                            Logger.getLogger(OneSignal.class.getName())
                                    .log(Level.INFO, "Sleeping {0} seconds until next API call", this.sleep);

                            Thread.sleep(Long.valueOf(this.sleep * 1000));
                        } catch (InterruptedException ex) {
                            Logger.getLogger(OneSignal.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        }
        mitt.close();
    }

    /**
     * Retrives key for an app.
     *
     * @param app_id String App id
     * @return Basic Auth Key
     */
    String getAuthKey(String app_id)
            throws MalformedURLException, IOException, ParseException {
        String basicAuthKey = "";

        Logger.getLogger(OneSignal.class.getName()).log(Level.INFO, "Retrieving auth key from: {0}", new Object[]{APP_URL + app_id});

        //Connect to API.
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(APP_URL + app_id).openConnection();
        httpURLConnection.setRequestProperty("Authorization", (String) credentials.get("authorization"));
        httpURLConnection.setRequestProperty("Accept", "application/json");
        httpURLConnection.setRequestMethod("GET");

        //Get API Call response.
        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(httpURLConnection.getInputStream()))) {
            String response;

            //Get auth key response from API.
            while ((response = bufferedReader.readLine()) != null) {
                basicAuthKey = (String) ((JSONObject) new JSONParser()
                        .parse(response))
                        .get("basic_auth_key");
            }
        }

        return basicAuthKey;
    }
}

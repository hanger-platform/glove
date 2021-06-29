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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Extract all of your current user data
 *
 * @author Helio Leal
 */
public class Users {

    private final String CSV_EXPORT_URL = "https://onesignal.com/api/v1/players/csv_export?app_id=";
    private final String APP_URL = "https://onesignal.com/api/v1/apps/";
    private final String output;
    private final String encode;
    private final List<String> apps;
    private final List key;
    private final List partition;
    private final List fields;
    private final JSONObject credentials;
    private final Character delimiter;
    private final int sleep;

    public Users(
            String output,
            String encode,
            List apps,
            List key,
            List partition,
            List fields,
            Character delimiter,
            int sleep,
            JSONObject credentials) {
        this.output = output;
        this.apps = apps;
        this.encode = encode;
        this.key = key;
        this.partition = partition;
        this.fields = fields;
        this.delimiter = delimiter;
        this.sleep = sleep;
        this.credentials = credentials;
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
                .addCustomField("etl_load_date", new Now());

        //Identifies if fields parameter was filled.
        if (this.fields.isEmpty()) {
            mitt.getConfiguration()
                    .addField("id")
                    .addField("session_count")
                    .addField("language")
                    .addField("timezone")
                    .addField("device_os")
                    .addField("device_type")
                    .addField("tags")
                    .addField("last_active")
                    .addField("playtime")                    
                    .addField("created_at")
                    .addField("invalid_identifier");
        } else {
            mitt.getConfiguration().addField(this.fields);
        }

        //Defines the output path for temp files.
        Path outputPath = Files.createTempDirectory("one_signal_");

        //Fetchs all apps.
        for (String app : this.apps) {
            String authKey = this.getAuthKey(app);

            Logger.getLogger(OneSignal.class.getName()).log(Level.INFO, "POST request for endpoint: {0}", new Object[]{CSV_EXPORT_URL + app});

            //Connect to API.
            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(CSV_EXPORT_URL + app).openConnection();
            httpURLConnection.setRequestProperty("Authorization", "Basic " + authKey);
            httpURLConnection.setRequestProperty("Accept", "application/json");
            httpURLConnection.setRequestMethod("POST");

            //Get API Call response.
            try (BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(httpURLConnection.getInputStream()))) {
                String response;

                //Get endpoint response from API.
                while ((response = bufferedReader.readLine()) != null) {
                    String url = (String) ((JSONObject) new JSONParser()
                            .parse(response))
                            .get("csv_file_url");

                    //Identify if file is ready
                    boolean ready = false;

                    while (!ready) {
                        HttpURLConnection httpConnection = (HttpURLConnection) new URL(url).openConnection();
                        int responseCode = httpConnection.getResponseCode();

                        //Identifies if file is ready to download.
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            Logger.getLogger(OneSignal.class.getName()).log(Level.INFO, "Downloading file from URL: {0}", new Object[]{url});

                            httpConnection.disconnect();
                            ready = true;

                            //Download file from URL
                            FileUtils.copyURLToFile(
                                    new URL(url),
                                    new File(outputPath + "/" + app + ".csv.gz")
                            );

                        } else {
                            //Response 403 means that file is not ready.
                            if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                                Logger.getLogger(OneSignal.class.getName()).log(Level.INFO, "File {0} is not ready to download, error: {1}", new Object[]{url, responseCode});

                                //Identify if has sleep time until next API call.
                                if (this.sleep > 0) {
                                    try {
                                        Logger.getLogger(OneSignal.class.getName())
                                                .log(Level.INFO, "Sleeping {0} seconds until next try", this.sleep);

                                        Thread.sleep(Long.valueOf(this.sleep * 1000));
                                    } catch (InterruptedException ex) {
                                        Logger.getLogger(OneSignal.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }

                            } else {
                                ready = true;
                                throw new IOException("GLOVE - One Signal fail: " + responseCode);
                            }
                        }
                    }
                }
            }
            httpURLConnection.disconnect();
        }

        Logger.getLogger(OneSignal.class.getName()).log(Level.INFO, "Writing output file to: {0}", this.output);

        //Write to the output.
        mitt.getReaderSettings().setDelimiter(this.delimiter);
        mitt.getReaderSettings().setEncode(this.encode);
        mitt.write(outputPath.toFile(), "*");
        
        Files.delete(outputPath);
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

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.DateFormat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import java.util.ArrayList;

/**
 *
 * @author Helio Leal
 */
public class Campaigns {

    private final String url;
    private final String token;
    private final String output;
    private final List key;
    private final List partition;
    private final List field;
    private final int sleep;

    public static final String CAMPAIGNS_LIST = "campaigns/list?page=";
    public static final String CAMPAIGNS_DETAIL = "campaigns/details?campaign_id=";

    public Campaigns(
            String url,
            String token,
            String output,
            List key,
            List partition,
            List field,
            int sleep) {

        this.url = url;
        this.token = token;
        this.output = output;
        this.key = key;
        this.partition = partition;
        this.field = field;
        this.sleep = sleep;
    }

    /**
     *
     */
    void extract() throws
            MalformedURLException,
            IOException,
            ParseException,
            DuplicateEntityException {
        boolean nextPage = true;
        int page = 0;

        //Defines a MITT instance. 
        Mitt mitt = new Mitt();

        //Defines output file.
        mitt.setOutputFile(this.output);

        //Defines custom fields.
        mitt.getConfiguration()
                .addCustomField("partition_field", new Concat(this.partition))
                .addCustomField("custom_primary_key", new Concat(this.key))
                .addCustomField("etl_load_date", new Now());

        //Identifies if fields parameter was filled.
        if (this.field.isEmpty()) {
            mitt.getConfiguration()
                    .addField("created_at")
                    .addField("updated_at")
                    .addField("name")
                    .addField("archived")
                    .addField("draft")
                    .addField("schedule_type")
                    .addField("channels")
                    .addField("first_sent")
                    .addField("last_sent")
                    .addField("tags")
                    .addField("messages")
                    .addField("conversion_behaviors");
        } else {
            mitt.getConfiguration().addField(this.field);
        }

        //Identifies original fields.
        List<String> fields = mitt.getConfiguration().getOriginalFieldsName();

        do {
            Logger.getLogger(Campaigns.class.getName()).log(Level.INFO, "Retrieving data from URL: {0}", new Object[]{this.url + CAMPAIGNS_LIST + page});
            HttpURLConnection httpURLConnection = this.getAPIResponse(this.url + CAMPAIGNS_LIST + page);

            //Get API Call response.
            try (BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(httpURLConnection.getInputStream()))) {
                String output;

                //Get campaign list from API.
                while ((output = bufferedReader.readLine()) != null) {
                    JSONObject jsonObject = (JSONObject) new JSONParser().parse(output);
                    JSONArray campaigns = (JSONArray) jsonObject.get("campaigns");

                    Logger.getLogger(Campaigns.class.getName()).log(Level.SEVERE, "{0} campaigns found ", new Object[]{campaigns.size()});

                    //Identify if at least 1 campaign was found on the page.
                    if (campaigns.size() > 0) {
                        page++;

                        //Fetchs campaigns list.
                        for (Object campaign : campaigns) {
                            JSONObject jsonCampaign = (JSONObject) campaign;

                            HttpURLConnection connectionCampaignDetails
                                    = this.getAPIResponse(this.url
                                            + CAMPAIGNS_DETAIL
                                            + jsonCampaign.get("id"));

                            //Get API Call response.
                            try (BufferedReader brCampaignDetail = new BufferedReader(
                                    new InputStreamReader(connectionCampaignDetails.getInputStream()))) {
                                String line;

                                //Get a campaign details from API.
                                while ((line = brCampaignDetail.readLine()) != null) {
                                    List record = new ArrayList();
                                    JSONObject details = (JSONObject) new JSONParser().parse(line);

                                    for (String field : fields) {
                                        //Identifies if the field exists.
                                        if (details.containsKey(field)) {
                                            record.add(details.get(field));
                                        } else {
                                            record.add(null);
                                        }
                                    }

                                    mitt.write(record);
                                }
                            }
                            connectionCampaignDetails.disconnect();
                        }
                    } else {
                        nextPage = false;
                    }
                }
            }
            httpURLConnection.disconnect();

        } while (nextPage);

        mitt.close();
    }

    /**
     * Get API response.
     *
     * @param url String
     * @return BufferedReader
     * @throws MalformedURLException
     * @throws IOException
     */
    HttpURLConnection
            getAPIResponse(String url) throws MalformedURLException, IOException {
        //Connect to API.
        HttpURLConnection httpURLConnection
                = (HttpURLConnection) new URL(url).openConnection();
        httpURLConnection.setRequestProperty("Authorization", this.token);
        httpURLConnection.setRequestProperty("Accept", "application/json");
        httpURLConnection.setRequestMethod("GET");

        return httpURLConnection;
    }
}

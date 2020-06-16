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
    private final List fields;

    public static final String CAMPAIGNS_LIST = "campaigns/list";
    public static final String CAMPAIGNS_DETAIL = "campaigns/details";

    public Campaigns(
            String url,
            String token,
            String output,
            List key,
            List partition,
            List fields) {

        this.url = url;
        this.token = token;
        this.output = output;
        this.key = key;
        this.partition = partition;
        this.fields = fields;
    }

    /**
     *
     */
    void extract() throws MalformedURLException, IOException, ParseException {
        //Connect to API.
        URL url = new URL(this.url + CAMPAIGNS_LIST);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setRequestProperty("Authorization", this.token);
        httpURLConnection.setRequestProperty("Accept", "application/json");
        httpURLConnection.setRequestMethod("GET");

        //Get API Call response.
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(httpURLConnection.getInputStream()));

        String output;
        while ((output = bufferedReader.readLine()) != null) {
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(output);
            JSONArray campaigns = (JSONArray) jsonObject.get("campaigns");

            Logger.getLogger(Campaigns.class.getName()).log(Level.SEVERE, "{0} campaigns found ", new Object[]{campaigns.size()});

            //Fetchs campaigns list.
            for (Object campaign : campaigns) {
                JSONObject jsonCampaign = (JSONObject) campaign;
                
                Logger.getLogger(Campaigns.class.getName()).log(Level.INFO, "Retrieving data from campaign: {0} of id {1}", new Object[]{jsonCampaign.get("name"), jsonCampaign.get("id")});

            }
        }

        httpURLConnection.disconnect();
    }
}

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
package br.com.dafiti.facebook;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.DateFormat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import com.facebook.ads.sdk.APIContext;
import com.facebook.ads.sdk.APIException;
import com.facebook.ads.sdk.APINodeList;
import com.facebook.ads.sdk.Ad;
import com.facebook.ads.sdk.AdAccount;
import com.facebook.ads.sdk.AdAccount.APIRequestGetCampaigns;
import com.facebook.ads.sdk.AdSet;
import com.facebook.ads.sdk.AdSet.APIRequestGetAds;
import com.facebook.ads.sdk.Campaign;
import com.facebook.ads.sdk.Lead;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Helio Leal
 */
public class AdsLeads {

    private final APIContext apiContext;
    private final String output;
    private final List<String> adAccount;
    private final String startDate;
    private final String endDate;
    private final List key;
    private final List partition;
    private final List fields;

    public AdsLeads(
            APIContext apiContext,
            String output,
            List<String> adAccount,
            String startDate,
            String endDate,
            List key,
            List partition,
            List fields) {

        this.apiContext = apiContext;
        this.adAccount = adAccount;
        this.output = output;
        this.startDate = startDate;
        this.endDate = endDate;
        this.key = key;
        this.partition = partition;
        this.fields = fields;
    }

    /**
     *
     */
    void extract() throws DuplicateEntityException, APIException {
        //Defines a MITT instance. 
        Mitt mitt = new Mitt(this.output);

        //Defines fields.
        mitt.getConfiguration()
                .addCustomField("partition_field", new Concat(this.partition))
                .addCustomField("custom_primary_key", new Concat(this.key))
                .addCustomField("etl_load_date", new Now());

        //Identifies if fields parameter was filled.
        if (this.fields.isEmpty()) {
            mitt.getConfiguration()
                    .addField("id")
                    .addField("created_time", new DateFormat("created_time", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss"))
                    .addField("ad_id")
                    .addField("form_id")
                    .addField("field_data");
        } else {
            mitt.getConfiguration().addField(this.fields);
        }

        //Identifies original fields.
        List<String> fields = mitt.getConfiguration().getOriginalFieldsName();

        //Iterates for each account.
        for (String account : this.adAccount) {
            Logger.getLogger(AdCampaign.class.getName()).log(Level.INFO, "Retrieving campaing from account {0}", account);

            AdAccount adAccount = new AdAccount(account, this.apiContext);
            APIRequestGetCampaigns request = adAccount.getCampaigns();

            //Request campaign fields.
            APINodeList<Campaign> campaigns = request
                    .requestField("name")
                    .requestField("adset").execute();

            //Enables auto pagination.
            campaigns = campaigns.withAutoPaginationIterator(true);

            //Reads each campaign.
            for (Campaign campaign : campaigns) {
                Logger.getLogger(AdCampaign.class.getName()).log(Level.INFO, "Retrieving adSets from campaign {0}", campaign.getFieldName());

                APINodeList<AdSet> adSets = campaign.getAdSets()
                        .requestField("name")
                        .requestField("ad").execute();

                //Enables auto pagination.
                adSets = adSets.withAutoPaginationIterator(true);

                //Reads each adSets.
                for (AdSet adSet : adSets) {
                    Logger.getLogger(AdCampaign.class.getName()).log(Level.INFO, "Retrieving ads from adSets {0}", adSet.getFieldName());

                    //Defines the campaign ads edge request.
                    APIRequestGetAds adsRequest = adSet.getAds();

                    //Defines a time range filter
                    adsRequest.setTimeRange("{\"since\":\"" + this.startDate + "\",\"until\":\"" + this.endDate + "\"}");

                    //Request campaign fields.
                    APINodeList<Ad> ads = adsRequest
                            .requestField("id")
                            .requestField("name").execute();

                    //Enables auto pagination.
                    ads = ads.withAutoPaginationIterator(true);

                    //Reads each ads.
                    for (Ad ad : ads) {
                        Logger.getLogger(AdCampaign.class.getName()).log(Level.INFO, "Retrieving adsLeads from ad {0}", ad.getFieldName());

                        Ad.APIRequestGetLeads leadsRequest = ad.getLeads();

                        //Define fields to be requested.
                        fields.forEach((field) -> {
                            leadsRequest.requestField(field);
                        });

                        APINodeList<Lead> leads = leadsRequest.execute();

                        //Enables auto pagination.
                        leads = leads.withAutoPaginationIterator(true);

                        //Reads each leads ad.
                        for (Lead lead : leads) {
                            List record = new ArrayList();

                            JsonObject jsonObject = lead.getRawResponseAsJsonObject();

                            fields.forEach((field) -> {

                                //Identifies if the field exists. 
                                if (jsonObject.has(field)) {
                                    JsonElement jsonElement = jsonObject.get(field);

                                    //Identifies if the fiels is a primitive.
                                    if (jsonElement.isJsonPrimitive()) {
                                        record.add(jsonElement.getAsString());
                                    } else {
                                        record.add(jsonElement);
                                    }
                                } else {
                                    record.add(null);
                                }
                            });

                            mitt.write(record);
                        }
                    }
                }
            }
        }

        mitt.close();
    }
}

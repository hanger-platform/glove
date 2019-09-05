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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Valdiney V GOMES
 */
public class Ads {

    private final APIContext apiContext;
    private final String output;
    private final List<String> adAccount;
    private final String startDate;
    private final String endDate;
    private final List key;
    private final List partition;

    public Ads(
            APIContext apiContext,
            String output,
            List<String> adAccount,
            String startDate,
            String endDate,
            List key,
            List partition) {

        this.apiContext = apiContext;
        this.adAccount = adAccount;
        this.output = output;
        this.startDate = startDate;
        this.endDate = endDate;
        this.key = key;
        this.partition = partition;
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
                .addCustomField("etl_load_date", new Now())
                .addField("id")
                .addField("adset_id")
                .addField("campaign_id")
                .addField("account_id")
                .addField("bid_amount")
                .addField("bid_type")
                .addField("configured_status")
                .addField("created_time", new DateFormat("created_time", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss"))
                .addField("demolink_hash")
                .addField("display_sequence")
                .addField("effective_status")
                .addField("engagement_audience")
                .addField("is_autobid")
                .addField("last_updated_by_app_id")
                .addField("name")
                .addField("preview_shareable_link")
                .addField("priority")
                .addField("source_ad_id")
                .addField("status")
                .addField("updated_time", new DateFormat("updated_time", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss"));

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

                    //Define fields to be requested.
                    fields.forEach((field) -> {
                        adsRequest.requestField(field);
                    });

                    //Request campaign fields.
                    APINodeList<Ad> ads = adsRequest.execute();

                    //Enables auto pagination.
                    ads = ads.withAutoPaginationIterator(true);

                    for (Ad ad : ads) {
                        List record = new ArrayList();

                        fields.forEach((field) -> {
                            JsonObject jsonObject = ad.getRawResponseAsJsonObject();

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

        mitt.close();
    }
}

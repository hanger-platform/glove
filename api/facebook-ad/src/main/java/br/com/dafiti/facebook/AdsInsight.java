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
import br.com.dafiti.mitt.transformation.embedded.Now;
import com.facebook.ads.sdk.APIContext;
import com.facebook.ads.sdk.APIException;
import com.facebook.ads.sdk.APINodeList;
import com.facebook.ads.sdk.AdAccount;
import com.facebook.ads.sdk.AdAccount.APIRequestGetCampaigns;
import com.facebook.ads.sdk.AdsInsights;
import com.facebook.ads.sdk.Campaign;
import com.facebook.ads.sdk.Campaign.APIRequestGetInsights;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Valdiney V GOMES
 */
public class AdsInsight {

    private final APIContext apiContext;
    private final String output;
    private final List<String> adAccount;
    private final String startDate;
    private final String endDate;
    private final List key;
    private final List partition;
    private final List fields;

    public AdsInsight(
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
                    .addField("account_id")
                    .addField("campaign_id")
                    .addField("adset_id")
                    .addField("ad_id")
                    .addField("ad_bid_type")
                    .addField("ad_bid_value")
                    .addField("ad_delivery")
                    .addField("ad_name")
                    .addField("adset_bid_type")
                    .addField("adset_bid_value")
                    .addField("adset_budget_type")
                    .addField("adset_budget_value")
                    .addField("adset_delivery")
                    .addField("adset_end")
                    .addField("adset_name")
                    .addField("adset_start")
                    .addField("auction_bid")
                    .addField("auction_competitiveness")
                    .addField("auction_max_competitor_bid")
                    .addField("buying_type")
                    .addField("campaign_name")
                    .addField("canvas_avg_view_percent")
                    .addField("canvas_avg_view_time")
                    .addField("clicks")
                    .addField("cost_per_estimated_ad_recallers")
                    .addField("cost_per_inline_post_engagement")
                    .addField("cost_per_unique_click")
                    .addField("cost_per_unique_inline_link_click")
                    .addField("cpc")
                    .addField("cpm")
                    .addField("cpp")
                    .addField("created_time")
                    .addField("ctr")
                    .addField("date_start")
                    .addField("date_stop")
                    .addField("estimated_ad_recall_rate")
                    .addField("estimated_ad_recall_rate_lower_bound")
                    .addField("estimated_ad_recall_rate_upper_bound")
                    .addField("estimated_ad_recallers")
                    .addField("estimated_ad_recallers_lower_bound")
                    .addField("estimated_ad_recallers_upper_bound")
                    .addField("frequency")
                    .addField("gender_targeting")
                    .addField("impressions")
                    .addField("inline_link_click_ctr")
                    .addField("inline_link_clicks")
                    .addField("inline_post_engagement")
                    .addField("instant_experience_clicks_to_open")
                    .addField("instant_experience_clicks_to_start")
                    .addField("instant_experience_outbound_clicks")
                    .addField("labels")
                    .addField("location")
                    .addField("objective")
                    .addField("place_page_name")
                    .addField("quality_score_ectr")
                    .addField("quality_score_ecvr")
                    .addField("quality_score_enfbr")
                    .addField("quality_score_organic")
                    .addField("reach")
                    .addField("social_spend")
                    .addField("spend")
                    .addField("unique_clicks")
                    .addField("unique_ctr")
                    .addField("unique_inline_link_click_ctr")
                    .addField("unique_inline_link_clicks")
                    .addField("unique_link_clicks_ctr")
                    .addField("updated_time")
                    .addField("actions")
                    .addField("action_values")
                    .addField("video_avg_time_watched_actions");
        } else {
            mitt.getConfiguration().addField(this.fields);
        }

        //Identifies original fields.
        List<String> fields = mitt.getConfiguration().getOriginalFieldsName();

        //Iterates for each account.
        for (String account : this.adAccount) {
            Logger.getLogger(AdsInsight.class.getName()).log(Level.INFO, "Retrieving Campaing from account {0}", account);

            AdAccount adAccount = new AdAccount(account, this.apiContext);
            APIRequestGetCampaigns campaignRequest = adAccount.getCampaigns();

            //Request campaign fields.
            APINodeList<Campaign> campaigns = campaignRequest
                    .requestField("name")
                    .requestField("insights").execute();

            //Enables auto pagination.
            campaigns = campaigns.withAutoPaginationIterator(true);

            for (Campaign campaign : campaigns) {
                Logger.getLogger(AdsInsight.class.getName()).log(Level.INFO, "Retrieving AdsInsights from campaign {0}", campaign.getFieldName());

                APIRequestGetInsights adInsightsRequest = campaign.getInsights();

                //Defines some filters.
                adInsightsRequest.setLevel(AdsInsights.EnumLevel.VALUE_AD);
                adInsightsRequest.setTimeIncrement("1");
                adInsightsRequest.setTimeRange("{\"since\":\"" + this.startDate + "\",\"until\":\"" + this.endDate + "\"}");
                adInsightsRequest.setActionAttributionWindows(
                        Arrays.asList(
                                AdsInsights.EnumActionAttributionWindows.VALUE_DEFAULT
                        )
                );

                //Define fields to be requested.
                fields.forEach((field) -> {
                    adInsightsRequest.requestField(field);
                });

                //Request campaign fields.
                APINodeList<AdsInsights> adsInsights = adInsightsRequest.execute();

                //Enables auto pagination.
                adsInsights = adsInsights.withAutoPaginationIterator(true);

                for (AdsInsights adsInsight : adsInsights) {
                    List record = new ArrayList();

                    fields.forEach((field) -> {
                        JsonObject jsonObject = adsInsight.getRawResponseAsJsonObject();

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

        mitt.close();
    }
}

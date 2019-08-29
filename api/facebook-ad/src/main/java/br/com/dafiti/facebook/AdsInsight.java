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

    public AdsInsight(
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

                APIRequestGetInsights insightsRequest = campaign.getInsights();

                //Defines some filters.
                insightsRequest.setLevel(AdsInsights.EnumLevel.VALUE_AD);
                insightsRequest.setTimeIncrement("1");
                insightsRequest.setTimeRange("{\"since\":\"" + this.startDate + "\",\"until\":\"" + this.endDate + "\"}");
                insightsRequest.setActionAttributionWindows(
                        Arrays.asList(
                                AdsInsights.EnumActionAttributionWindows.VALUE_DEFAULT
                        )
                );

                APINodeList<AdsInsights> adsInsights = insightsRequest
                        .requestField("account_id")
                        .requestField("campaign_id")
                        .requestField("adset_id")
                        .requestField("ad_id")
                        .requestField("ad_bid_type")
                        .requestField("ad_bid_value")
                        .requestField("ad_delivery")
                        .requestField("ad_name")
                        .requestField("adset_bid_type")
                        .requestField("adset_bid_value")
                        .requestField("adset_budget_type")
                        .requestField("adset_budget_value")
                        .requestField("adset_delivery")
                        .requestField("adset_end")
                        .requestField("adset_name")
                        .requestField("adset_start")
                        .requestField("auction_bid")
                        .requestField("auction_competitiveness")
                        .requestField("auction_max_competitor_bid")
                        .requestField("buying_type")
                        .requestField("campaign_name")
                        .requestField("canvas_avg_view_percent")
                        .requestField("canvas_avg_view_time")
                        .requestField("clicks")
                        .requestField("cost_per_estimated_ad_recallers")
                        .requestField("cost_per_inline_post_engagement")
                        .requestField("cost_per_unique_click")
                        .requestField("cost_per_unique_inline_link_click")
                        .requestField("cpc")
                        .requestField("cpm")
                        .requestField("cpp")
                        .requestField("created_time")
                        .requestField("ctr")
                        .requestField("date_start")
                        .requestField("date_stop")
                        .requestField("estimated_ad_recall_rate")
                        .requestField("estimated_ad_recall_rate_lower_bound")
                        .requestField("estimated_ad_recall_rate_upper_bound")
                        .requestField("estimated_ad_recallers")
                        .requestField("estimated_ad_recallers_lower_bound")
                        .requestField("estimated_ad_recallers_upper_bound")
                        .requestField("frequency")
                        .requestField("gender_targeting")
                        .requestField("impressions")
                        .requestField("inline_link_click_ctr")
                        .requestField("inline_link_clicks")
                        .requestField("inline_post_engagement")
                        .requestField("instant_experience_clicks_to_open")
                        .requestField("instant_experience_clicks_to_start")
                        .requestField("instant_experience_outbound_clicks")
                        .requestField("labels")
                        .requestField("location")
                        .requestField("objective")
                        .requestField("place_page_name")
                        .requestField("quality_score_ectr")
                        .requestField("quality_score_ecvr")
                        .requestField("quality_score_enfbr")
                        .requestField("quality_score_organic")
                        .requestField("reach")
                        .requestField("social_spend")
                        .requestField("spend")
                        .requestField("unique_clicks")
                        .requestField("unique_ctr")
                        .requestField("unique_inline_link_click_ctr")
                        .requestField("unique_inline_link_clicks")
                        .requestField("unique_link_clicks_ctr")
                        .requestField("updated_time")
                        .requestField("actions")
                        .requestField("action_values")
                        .requestField("video_avg_time_watched_actions").execute();

                //Enables auto pagination.
                adsInsights = adsInsights.withAutoPaginationIterator(true);

                List record;

                for (AdsInsights adsInsight : adsInsights) {
                    record = new ArrayList();

                    record.add(adsInsight.getFieldAccountId());
                    record.add(adsInsight.getFieldCampaignId());
                    record.add(adsInsight.getFieldAdsetId());
                    record.add(adsInsight.getFieldAdId());
                    record.add(adsInsight.getFieldAdBidType());
                    record.add(adsInsight.getFieldAdBidValue());
                    record.add(adsInsight.getFieldAdDelivery());
                    record.add(adsInsight.getFieldAdName());
                    record.add(adsInsight.getFieldAdsetBidType());
                    record.add(adsInsight.getFieldAdsetBidValue());
                    record.add(adsInsight.getFieldAdsetBudgetType());
                    record.add(adsInsight.getFieldAdsetBudgetValue());
                    record.add(adsInsight.getFieldAdsetDelivery());
                    record.add(adsInsight.getFieldAdsetEnd());
                    record.add(adsInsight.getFieldAdsetName());
                    record.add(adsInsight.getFieldAdsetStart());
                    record.add(adsInsight.getFieldAuctionBid());
                    record.add(adsInsight.getFieldAuctionCompetitiveness());
                    record.add(adsInsight.getFieldAuctionMaxCompetitorBid());
                    record.add(adsInsight.getFieldBuyingType());
                    record.add(adsInsight.getFieldCampaignName());
                    record.add(adsInsight.getFieldCanvasAvgViewPercent());
                    record.add(adsInsight.getFieldCanvasAvgViewTime());
                    record.add(adsInsight.getFieldClicks());
                    record.add(adsInsight.getFieldCostPerEstimatedAdRecallers());
                    record.add(adsInsight.getFieldCostPerInlinePostEngagement());
                    record.add(adsInsight.getFieldCostPerUniqueClick());
                    record.add(adsInsight.getFieldCostPerUniqueInlineLinkClick());
                    record.add(adsInsight.getFieldCpc());
                    record.add(adsInsight.getFieldCpm());
                    record.add(adsInsight.getFieldCpp());
                    record.add(adsInsight.getFieldCreatedTime());
                    record.add(adsInsight.getFieldCtr());
                    record.add(adsInsight.getFieldDateStart());
                    record.add(adsInsight.getFieldDateStop());
                    record.add(adsInsight.getFieldEstimatedAdRecallRate());
                    record.add(adsInsight.getFieldEstimatedAdRecallRateLowerBound());
                    record.add(adsInsight.getFieldEstimatedAdRecallRateUpperBound());
                    record.add(adsInsight.getFieldEstimatedAdRecallers());
                    record.add(adsInsight.getFieldEstimatedAdRecallersLowerBound());
                    record.add(adsInsight.getFieldEstimatedAdRecallersUpperBound());
                    record.add(adsInsight.getFieldFrequency());
                    record.add(adsInsight.getFieldGenderTargeting());
                    record.add(adsInsight.getFieldImpressions());                    
                    record.add(adsInsight.getFieldInlineLinkClickCtr());
                    record.add(adsInsight.getFieldInlineLinkClicks());
                    record.add(adsInsight.getFieldInlinePostEngagement());
                    record.add(adsInsight.getFieldInstantExperienceClicksToOpen());
                    record.add(adsInsight.getFieldInstantExperienceClicksToStart());
                    record.add(adsInsight.getFieldInstantExperienceOutboundClicks());
                    record.add(adsInsight.getFieldLabels());
                    record.add(adsInsight.getFieldLocation());
                    record.add(adsInsight.getFieldObjective());
                    record.add(adsInsight.getFieldPlacePageName());
                    record.add(adsInsight.getFieldQualityScoreEctr());
                    record.add(adsInsight.getFieldQualityScoreEcvr());
                    record.add(adsInsight.getFieldQualityScoreEnfbr());
                    record.add(adsInsight.getFieldQualityScoreOrganic());
                    record.add(adsInsight.getFieldReach());
                    record.add(adsInsight.getFieldSocialSpend());
                    record.add(adsInsight.getFieldSpend());
                    record.add(adsInsight.getFieldUniqueClicks());
                    record.add(adsInsight.getFieldUniqueCtr());
                    record.add(adsInsight.getFieldUniqueInlineLinkClickCtr());
                    record.add(adsInsight.getFieldUniqueInlineLinkClicks());
                    record.add(adsInsight.getFieldUniqueLinkClicksCtr());
                    record.add(adsInsight.getFieldUpdatedTime());
                    record.add(adsInsight.getFieldActions());
                    record.add(adsInsight.getFieldActionValues());
                    record.add(adsInsight.getFieldVideoTimeWatchedActions());

                    mitt.write(record);
                }
            }
        }

        mitt.close();
    }
}

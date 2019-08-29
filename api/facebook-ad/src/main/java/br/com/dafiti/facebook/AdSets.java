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
import com.facebook.ads.sdk.AdAccount;
import com.facebook.ads.sdk.AdAccount.APIRequestGetCampaigns;
import com.facebook.ads.sdk.AdSet;
import com.facebook.ads.sdk.Campaign;
import com.facebook.ads.sdk.Campaign.APIRequestGetAdSets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Valdiney V GOMES
 */
public class AdSets {

    private final APIContext apiContext;
    private final String output;
    private final List<String> adAccount;
    private final String startDate;
    private final String endDate;
    private final List key;
    private final List partition;

    public AdSets(
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
                .addField("campaign_id")
                .addField("account_id")
                .addField("name")
                .addField("bid_amount")
                .addField("bid_strategy")
                .addField("billing_event")
                .addField("configured_status")
                .addField("created_time", new DateFormat("created_time", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss"))
                .addField("daily_budget")
                .addField("daily_min_spend_target")
                .addField("daily_spend_cap")
                .addField("destination_type")
                .addField("effective_status")
                .addField("end_time")
                .addField("instagram_actor_id")
                .addField("is_dynamic_creative")
                .addField("lifetime_budget")
                .addField("lifetime_imps")
                .addField("lifetime_min_spend_target")
                .addField("lifetime_spend_cap")
                .addField("optimization_goal")
                .addField("optimization_sub_event")
                .addField("recurring_budget_semantics")
                .addField("review_feedback")
                .addField("rf_prediction_id")
                .addField("source_adset_id")
                .addField("start_time", new DateFormat("start_time", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss"))
                .addField("status")
                .addField("updated_time", new DateFormat("updated_time", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss"))
                .addField("use_new_app_click");

        //Iterates for each account.
        for (String account : this.adAccount) {
            Logger.getLogger(AdCampaign.class.getName()).log(Level.INFO, "Retrieving campaing from account {0}", account);

            AdAccount adAccount = new AdAccount(account, this.apiContext);
            APIRequestGetCampaigns campaignRequest = adAccount.getCampaigns();

            //Request ampaign fields.
            APINodeList<Campaign> campaigns = campaignRequest
                    .requestField("name")
                    .requestField("adset")
                    .execute();

            //Enables auto pagination.
            campaigns = campaigns.withAutoPaginationIterator(true);

            //Reads each campaign.
            for (Campaign campaign : campaigns) {
                Logger.getLogger(AdCampaign.class.getName()).log(Level.INFO, "Retrieving adSets from campaign {0}", campaign.getFieldName());

                //Defines the campaign adset edge request.
                APIRequestGetAdSets adSetsRequest = campaign.getAdSets();

                //Defines a time range filter.
                adSetsRequest.setTimeRange("{\"since\":\"" + this.startDate + "\",\"until\":\"" + this.endDate + "\"}");

                APINodeList<AdSet> adSets = adSetsRequest
                        .requestField("id")
                        .requestField("campaign_id")
                        .requestField("account_id")
                        .requestField("name")
                        .requestField("bid_amount")
                        .requestField("bid_strategy")
                        .requestField("billing_event")
                        .requestField("configured_status")
                        .requestField("created_time")
                        .requestField("daily_budget")
                        .requestField("daily_min_spend_target")
                        .requestField("daily_spend_cap")
                        .requestField("destination_type")
                        .requestField("effective_status")
                        .requestField("end_time")
                        .requestField("instagram_actor_id")
                        .requestField("is_dynamic_creative")
                        .requestField("lifetime_budget")
                        .requestField("lifetime_imps")
                        .requestField("lifetime_min_spend_target")
                        .requestField("lifetime_spend_cap")
                        .requestField("optimization_goal")
                        .requestField("optimization_sub_event")
                        .requestField("recurring_budget_semantics")
                        .requestField("review_feedback")
                        .requestField("rf_prediction_id")
                        .requestField("source_adset_id")
                        .requestField("start_time")
                        .requestField("status")
                        .requestField("updated_time")
                        .requestField("use_new_app_click").execute();

                //Enables auto pagination.
                adSets = adSets.withAutoPaginationIterator(true);

                List record;

                for (AdSet adSet : adSets) {
                    record = new ArrayList();

                    record.add(adSet.getFieldId());
                    record.add(adSet.getFieldCampaignId());
                    record.add(adSet.getFieldAccountId());
                    record.add(adSet.getFieldName());
                    record.add(adSet.getFieldBidAmount());
                    record.add(adSet.getFieldBidStrategy());
                    record.add(adSet.getFieldBillingEvent());
                    record.add(adSet.getFieldConfiguredStatus());
                    record.add(adSet.getFieldCreatedTime());
                    record.add(adSet.getFieldDailyBudget());
                    record.add(adSet.getFieldDailyMinSpendTarget());
                    record.add(adSet.getFieldDailySpendCap());
                    record.add(adSet.getFieldDestinationType());
                    record.add(adSet.getFieldEffectiveStatus());
                    record.add(adSet.getFieldEndTime());
                    record.add(adSet.getFieldInstagramActorId());
                    record.add(adSet.getFieldIsDynamicCreative());
                    record.add(adSet.getFieldLifetimeBudget());
                    record.add(adSet.getFieldLifetimeImps());
                    record.add(adSet.getFieldLifetimeMinSpendTarget());
                    record.add(adSet.getFieldLifetimeSpendCap());
                    record.add(adSet.getFieldOptimizationGoal());
                    record.add(adSet.getFieldOptimizationSubEvent());
                    record.add(adSet.getFieldRecurringBudgetSemantics());
                    record.add(adSet.getFieldReviewFeedback());
                    record.add(adSet.getFieldRfPredictionId());
                    record.add(adSet.getFieldSourceAdsetId());
                    record.add(adSet.getFieldStartTime());
                    record.add(adSet.getFieldStatus());
                    record.add(adSet.getFieldUpdatedTime());
                    record.add(adSet.getFieldUseNewAppClick());

                    mitt.write(record);
                }
            }
        }

        mitt.close();
    }
}

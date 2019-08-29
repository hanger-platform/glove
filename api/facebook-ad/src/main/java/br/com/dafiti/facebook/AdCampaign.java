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
import com.facebook.ads.sdk.Campaign;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Valdiney V GOMES
 */
public class AdCampaign {

    private final APIContext apiContext;
    private final String output;
    private final List<String> adAccount;
    private final String startDate;
    private final String endDate;
    private final List key;
    private final List partition;

    public AdCampaign(
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
                .addField("account_id")
                .addField("bid_strategy")
                .addField("boosted_object_id")
                .addField("budget_rebalance_flag")
                .addField("budget_remaining")
                .addField("buying_type")
                .addField("can_create_brand_lift_study")
                .addField("can_use_spend_cap")
                .addField("configured_status")
                .addField("created_time", new DateFormat("created_time", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss"))
                .addField("daily_budget")
                .addField("effective_status")
                .addField("last_budget_toggling_time", new DateFormat("last_budget_toggling_time", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss"))
                .addField("lifetime_budget")
                .addField("name")
                .addField("objective")
                .addField("source_campaign_id")
                .addField("spend_cap")
                .addField("start_time", new DateFormat("start_time", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss"))
                .addField("status")
                .addField("stop_time", new DateFormat("stop_time", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss"))
                .addField("topline_id")
                .addField("updated_time", new DateFormat("updated_time", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss"));

        //Iterates for each account.
        for (String account : this.adAccount) {
            Logger.getLogger(AdCampaign.class.getName()).log(Level.INFO, "Retrieving campaing from account {0}", account);

            AdAccount adAcount = new AdAccount(account, this.apiContext);
            APIRequestGetCampaigns campaignRequest = adAcount.getCampaigns();

            //Define a time range filter.
            campaignRequest.setTimeRange("{\"since\":\"" + this.startDate + "\",\"until\":\"" + this.endDate + "\"}");

            //Request campaign fields.
            APINodeList<Campaign> campaigns = campaignRequest
                    .requestField("id")
                    .requestField("account_id")
                    .requestField("bid_strategy")
                    .requestField("boosted_object_id")
                    .requestField("budget_rebalance_flag")
                    .requestField("budget_remaining")
                    .requestField("buying_type")
                    .requestField("can_create_brand_lift_study")
                    .requestField("can_use_spend_cap")
                    .requestField("configured_status")
                    .requestField("created_time")
                    .requestField("daily_budget")
                    .requestField("effective_status")
                    .requestField("last_budget_toggling_time")
                    .requestField("lifetime_budget")
                    .requestField("name")
                    .requestField("objective")
                    .requestField("source_campaign_id")
                    .requestField("spend_cap")
                    .requestField("start_time")
                    .requestField("status")
                    .requestField("stop_time")
                    .requestField("topline_id")
                    .requestField("updated_time").execute();

            //Enables auto pagination.
            campaigns = campaigns.withAutoPaginationIterator(true);

            List record;

            for (Campaign campaign : campaigns) {
                record = new ArrayList();

                record.add(campaign.getFieldId());
                record.add(campaign.getFieldAccountId());
                record.add(campaign.getFieldBidStrategy());
                record.add(campaign.getFieldBoostedObjectId());
                record.add(campaign.getFieldBudgetRebalanceFlag());
                record.add(campaign.getFieldBudgetRemaining());
                record.add(campaign.getFieldBuyingType());
                record.add(campaign.getFieldCanCreateBrandLiftStudy());
                record.add(campaign.getFieldCanUseSpendCap());
                record.add(campaign.getFieldConfiguredStatus());
                record.add(campaign.getFieldCreatedTime());
                record.add(campaign.getFieldDailyBudget());
                record.add(campaign.getFieldEffectiveStatus());
                record.add(campaign.getFieldLastBudgetTogglingTime());
                record.add(campaign.getFieldLifetimeBudget());
                record.add(campaign.getFieldName());
                record.add(campaign.getFieldObjective());
                record.add(campaign.getFieldSourceCampaignId());
                record.add(campaign.getFieldSpendCap());
                record.add(campaign.getFieldStartTime());
                record.add(campaign.getFieldStatus());
                record.add(campaign.getFieldStopTime());
                record.add(campaign.getFieldToplineId());
                record.add(campaign.getFieldUpdatedTime());

                mitt.write(record);
            }
        }

        mitt.close();
    }
}

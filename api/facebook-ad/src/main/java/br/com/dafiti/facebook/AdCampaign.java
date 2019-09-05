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

        //Identifies original fields.
        List<String> fields = mitt.getConfiguration().getOriginalFieldsName();

        //Iterates for each account.
        for (String account : this.adAccount) {
            Logger.getLogger(AdCampaign.class.getName()).log(Level.INFO, "Retrieving campaing from account {0}", account);

            AdAccount adAccount = new AdAccount(account, this.apiContext);
            APIRequestGetCampaigns campaignRequest = adAccount.getCampaigns();

            //Define a time range filter.
            campaignRequest.setTimeRange("{\"since\":\"" + this.startDate + "\",\"until\":\"" + this.endDate + "\"}");

            //Define fields to be requested.
            fields.forEach((field) -> {
                campaignRequest.requestField(field, true);
            });

            //Request campaign fields.
            APINodeList<Campaign> campaigns = campaignRequest.execute();

            //Enables auto pagination.
            campaigns = campaigns.withAutoPaginationIterator(true);

            for (Campaign campaign : campaigns) {
                List record = new ArrayList();

                fields.forEach((field) -> {
                    JsonObject jsonObject = campaign.getRawResponseAsJsonObject();

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

        mitt.close();
    }
}

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

                    APINodeList<Ad> ads = adsRequest
                            .requestField("id")
                            .requestField("adset_id")
                            .requestField("campaign_id")
                            .requestField("account_id")
                            .requestField("bid_amount")
                            .requestField("bid_type")
                            .requestField("configured_status")
                            .requestField("created_time")
                            .requestField("demolink_hash")
                            .requestField("display_sequence")
                            .requestField("effective_status")
                            .requestField("engagement_audience")
                            .requestField("is_autobid")
                            .requestField("last_updated_by_app_id")
                            .requestField("name")
                            .requestField("preview_shareable_link")
                            .requestField("priority")
                            .requestField("source_ad_id")
                            .requestField("status")
                            .requestField("updated_time").execute();

                    //Enables auto pagination.
                    ads = ads.withAutoPaginationIterator(true);

                    List record;

                    for (Ad ad : ads) {
                        record = new ArrayList();

                        record.add(ad.getFieldId());
                        record.add(ad.getFieldAdsetId());
                        record.add(ad.getFieldCampaignId());
                        record.add(ad.getFieldAccountId());
                        record.add(ad.getFieldBidAmount());
                        record.add(ad.getFieldBidType());
                        record.add(ad.getFieldConfiguredStatus());
                        record.add(ad.getFieldCreatedTime());
                        record.add(ad.getFieldDemolinkHash());
                        record.add(ad.getFieldDisplaySequence());
                        record.add(ad.getFieldEffectiveStatus());
                        record.add(ad.getFieldEngagementAudience());
                        record.add(ad.getFieldIsAutobid());
                        record.add(ad.getFieldLastUpdatedByAppId());
                        record.add(ad.getFieldName());
                        record.add(ad.getFieldPreviewShareableLink());
                        record.add(ad.getFieldPriority());
                        record.add(ad.getFieldSourceAdId());
                        record.add(ad.getFieldStatus());
                        record.add(ad.getFieldUpdatedTime());

                        mitt.write(record);
                    }
                }
            }
        }

        mitt.close();
    }
}

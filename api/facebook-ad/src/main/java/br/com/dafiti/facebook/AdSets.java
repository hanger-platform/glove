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
import com.facebook.ads.sdk.AdSet;
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
public class AdSets {

    private final APIContext apiContext;
    private final String output;
    private final List<String> adAccount;
    private final String startDate;
    private final String endDate;
    private final List key;
    private final List partition;
    private final List<String> fields;
    private final List<String> attributes;
    private final String filtering;

    private static final Logger LOG = Logger.getLogger(AdSets.class.getName());

    public AdSets(
            APIContext apiContext,
            String output,
            List<String> adAccount,
            String startDate,
            String endDate,
            List key,
            List partition,
            List fields,
            List attibutes,
            String filtering) {

        this.apiContext = apiContext;
        this.adAccount = adAccount;
        this.output = output;
        this.startDate = startDate;
        this.endDate = endDate;
        this.key = key;
        this.partition = partition;
        this.fields = fields;
        this.attributes = attibutes;
        this.filtering = filtering;
    }

    /**
     *
     */
    void extract() throws DuplicateEntityException, APIException {
        //Defines a MITT instance. 
        Mitt mitt = new Mitt();
        mitt.setOutputFile(this.output);

        //Defines fields.
        mitt.getConfiguration()
                .addCustomField("partition_field", new Concat(this.partition))
                .addCustomField("custom_primary_key", new Concat(this.key))
                .addCustomField("etl_load_date", new Now());

        //Defines the default report attributes
        if (this.attributes.isEmpty()) {
            this.attributes.add("account_id");
            this.attributes.add("campaign_id");
            this.attributes.add("id");
            this.attributes.add("name");
            this.attributes.add("status");
        }

        //Defines the output fields.
        mitt.getConfiguration().addField(this.attributes);

        //Identifies if fields parameter was filled.
        if (!this.fields.isEmpty()) {
            for (String field : this.fields) {
                mitt.getConfiguration().addCustomField(field);
            }
        }

        //Identifies original fields.
        List<String> originalFields = mitt.getConfiguration().getOriginalFieldsName();

        //Iterates for each account.
        this.adAccount.forEach(account -> {
            try {
                LOG.log(Level.INFO, "Retrieving campaing from account {0}", account.trim());

                AdAccount adAccount = new AdAccount(account.trim(), this.apiContext);
                AdAccount.APIRequestGetAdSets adSetsRequest = adAccount.getAdSets();

                //Defines a time range filter.
                adSetsRequest.setTimeRange("{\"since\":\"" + this.startDate + "\",\"until\":\"" + this.endDate + "\"}");

                //Define the filters.
                if (this.filtering != null) {
                    LOG.log(Level.INFO, "Filter: {0}", this.filtering);
                    adSetsRequest.setParam("filtering", this.filtering);
                }

                //Define fields to be requested.
                this.attributes.forEach((attribute) -> {
                    adSetsRequest.requestField(attribute);
                });

                //Request campaign fields.
                APINodeList<AdSet> adSets = adSetsRequest.execute();

                //Enables auto pagination.
                adSets = adSets.withAutoPaginationIterator(true);

                for (AdSet adSet : adSets) {
                    List record = new ArrayList();

                    originalFields.forEach((field) -> {
                        JsonObject jsonObject = adSet.getRawResponseAsJsonObject();

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
            } catch (APIException ex) {
                LOG.log(Level.SEVERE, "Fail retrieving campaigns from account {0}, perhaps this account doesn't exist.", account.trim());
                ex.printStackTrace();
            }
        });

        mitt.close();
    }
}

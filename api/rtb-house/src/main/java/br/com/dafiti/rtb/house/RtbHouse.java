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
package br.com.dafiti.rtb.house;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import com.google.common.net.MediaType;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Helio Leal
 */
public class RtbHouse {

    private static final Logger LOG = Logger.getLogger(RtbHouse.class.getName());
    private static final String URI_ADVERTISERS = "/advertisers";
    private static final String URI_SUMMARY_STATS = "/advertisers/<<advertiser>>/summary-stats?dayFrom=<<start_date>>&dayTo=<<end_date>>&groupBy=day-advertiser-subcampaign&metrics=campaignCost";

    /**
     * RTB House API data transfer
     *
     * @param args cli parameteres provided by command line.
     */
    public static void main(String[] args) {
        LOG.info("Glove - RTB House Extractor started");

        //Define the mitt.
        Mitt mitt = new Mitt();

        try {
            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("c", "credentials", "Credentials file", "", true, false)
                    .addParameter("o", "output", "Output file", "", true, false)
                    .addParameter("a", "advertiser", "Advertisers to be extracted, divided by + if has more than one", "", true, false)
                    .addParameter("f", "field", "Fields to be extracted", "", true, false)
                    .addParameter("s", "start_date", "Start date", "", true, false)
                    .addParameter("e", "end_date", "End date", "", true, false)
                    .addParameter("p", "partition", "(Optional)  Partition, divided by + if has more than one field")
                    .addParameter("k", "key", "(Optional) Unique key, divided by + if has more than one field", "");

            //Reads the command line interface. 
            CommandLineInterface cli = mitt.getCommandLineInterface(args);

            //Defines output file.
            mitt.setOutputFile(cli.getParameter("output"));

            //Defines fields.
            mitt.getConfiguration()
                    .addCustomField("partition_field", new Concat((List) cli.getParameterAsList("partition", "\\+")))
                    .addCustomField("custom_primary_key", new Concat((List) cli.getParameterAsList("key", "\\+")))
                    .addCustomField("etl_load_date", new Now())
                    .addField(cli.getParameterAsList("field", "\\+"));

            //Get original fields.
            List<String> fields = mitt.getConfiguration().getOriginalFieldsName();

            //Reads the credentials file. 
            JSONParser parser = new JSONParser();
            JSONObject credentials = (JSONObject) parser.parse(new FileReader(cli.getParameter("credentials")));

            //Get all advertisers.
            JSONArray advertisers = executeRequest(credentials.get("url").toString() + URI_ADVERTISERS, credentials);

            if (advertisers.size() > 0) {
                for (Object advertiser : advertisers) {

                    //Identify if advertiser is to be considered.
                    if (cli.getParameterAsList("advertiser", "\\+")
                            .stream()
                            .anyMatch((a) -> (a.toUpperCase()
                            .equals(((JSONObject) advertiser)
                                    .get("name").toString().toUpperCase())))) {

                        String url = credentials.get("url").toString() + URI_SUMMARY_STATS
                                .replace("<<advertiser>>", ((JSONObject) advertiser).get("hash").toString())
                                .replace("<<start_date>>", cli.getParameter("start_date"))
                                .replace("<<end_date>>", cli.getParameter("end_date"));

                        //Get all advertisers.
                        JSONArray data = executeRequest(url, credentials);

                        //Identify if at least 1 element was found.
                        if (data.size() > 0) {
                            for (Object element : data) {
                                List record = new ArrayList();

                                fields.forEach((field) -> {

                                    //Identifies if the field exists.
                                    if (((JSONObject) element).containsKey(field)) {
                                        record.add(((JSONObject) element).get(field));

                                        //Identify if it is to consider currency.
                                    } else if (fields.contains("currency")) {
                                        record.add(((JSONObject) advertiser).get("currency"));

                                    } else {
                                        record.add(null);
                                    }
                                });

                                mitt.write(record);
                            }
                        } else {
                            LOG.info("Glove - Any element found for " + ((JSONObject) advertiser).get("name").toString());
                        }
                    }
                }
            } else {
                LOG.info("Glove - Any advertiser was found.");
            }

        } catch (DuplicateEntityException
                | IOException
                | ParseException ex) {

            LOG.log(Level.SEVERE, "RTB House Extractor failure: ", ex);
            System.exit(1);
        } finally {
            mitt.close();

            LOG.info("Glove - RTB House Extractor finalized.");
            System.exit(0);
        }
    }

    /**
     * Execute an API request.
     *
     * @param url
     * @param credentials
     * @return
     * @throws org.json.simple.parser.ParseException
     */
    public static JSONArray executeRequest(String url, JSONObject credentials)
            throws ParseException {
        LOG.log(Level.INFO, "GET request for endpoint: {0}", new Object[]{url});

        //Requests report data.
        HttpResponse<String> response = Unirest
                .get(url)
                .header("Content-Type", "application/json")
                .basicAuth(credentials.get("login").toString(), credentials.get("password").toString())
                .asString();

        JSONArray jsonArray = (JSONArray) ((JSONObject) new JSONParser()
                .parse(response.getBody()))
                .get("data");

        LOG.log(Level.INFO, "{0} elements found ", new Object[]{jsonArray.size()});

        return jsonArray;
    }
}

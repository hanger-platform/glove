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
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import com.facebook.ads.sdk.APIContext;
import com.facebook.ads.sdk.APIException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Valdiney V GOMES
 */
public class FacebookAd {

    public static void main(String[] args) {
        Logger.getLogger(FacebookAd.class.getName()).info("GLOVE - Facebook Ad Extractor started");

        //Define the mitt.
        Mitt mitt = new Mitt();

        try {
            //Define parameters.
            mitt.getConfiguration()
                    .addParameter("c", "credentials", "Credentials file", "", true, false)
                    .addParameter("o", "output", "Identifies the local path that saves downloaded files from Facebook", "", true, false)
                    .addParameter("b", "account", "Facebook account, divided by + if has more than one", "", true, false)
                    .addParameter("r", "report", "Identifies the report to extract", "", true, false)
                    .addParameter("s", "start_date", "Start date", new SimpleDateFormat("yyyy-MM-dd").format(new Date()))
                    .addParameter("e", "end_date", "End date", new SimpleDateFormat("yyyy-MM-dd").format(new Date()))
                    .addParameter("a", "attributes", "Facebook Report fields, divided by + if has more than one field", "", true, false)
                    .addParameter("p", "partition", "Define the partition field or fields, divided by +", "")
                    .addParameter("k", "key", "(Optional) Unique key, divided by + if has more than one field", "")
                    .addParameter("f", "fields", "(Optional) Output fields, divided by + if has more than one field", "")
                    .addParameter("z", "breakdowns", "(Optional) Breakdowns of report, divided by + if has more than one field", "");

            //Reads the command line interface. 
            CommandLineInterface cli = mitt.getCommandLineInterface(args);

            //Reads the credentials file. 
            JSONParser parser = new JSONParser();
            JSONObject credentials = (JSONObject) parser.parse(new FileReader(cli.getParameter("credentials")));

            //Defines the FacebookAd api context.
            APIContext apiContext = new APIContext(
                    (String) credentials.get("token"),
                    (String) credentials.get("client-secret")
            );

            //Identifies the approprieted report to extract.
            switch (cli.getParameter("report").toLowerCase()) {
                case "adcampaign":
                    new AdCampaign(
                            apiContext,
                            cli.getParameter("output"),
                            cli.getParameterAsList("account", "\\+"),
                            cli.getParameter("start_date"),
                            cli.getParameter("end_date"),
                            cli.getParameterAsList("key", "\\+"),
                            cli.getParameterAsList("partition", "\\+"),
                            cli.getParameterAsList("fields", "\\+")).extract();
                    break;
                case "adsets":
                    new AdSets(
                            apiContext,
                            cli.getParameter("output"),
                            cli.getParameterAsList("account", "\\+"),
                            cli.getParameter("start_date"),
                            cli.getParameter("end_date"),
                            cli.getParameterAsList("key", "\\+"),
                            cli.getParameterAsList("partition", "\\+"),
                            cli.getParameterAsList("fields", "\\+")).extract();
                    break;
                case "ads":
                    new Ads(
                            apiContext,
                            cli.getParameter("output"),
                            cli.getParameterAsList("account", "\\+"),
                            cli.getParameter("start_date"),
                            cli.getParameter("end_date"),
                            cli.getParameterAsList("key", "\\+"),
                            cli.getParameterAsList("partition", "\\+"),
                            cli.getParameterAsList("fields", "\\+")).extract();
                    break;
                case "adsinsights":
                    new AdsInsight(
                            apiContext,
                            cli.getParameter("output"),
                            cli.getParameterAsList("account", "\\+"),
                            cli.getParameter("start_date"),
                            cli.getParameter("end_date"),
                            cli.getParameterAsList("key", "\\+"),
                            cli.getParameterAsList("partition", "\\+"),
                            cli.getParameterAsList("fields", "\\+"),
                            cli.getParameterAsList("breakdowns", "\\+"),
                            cli.getParameterAsList("attributes", "\\+")).extract();
                    break;
                default:
                    Logger.getLogger(FacebookAd.class.getName()).log(Level.SEVERE, "Extractor {0} not yet implemented", cli.getParameter("report"));
            }
        } catch (DuplicateEntityException
                | IOException
                | ParseException
                | APIException ex) {

            Logger.getLogger(FacebookAd.class.getName()).log(Level.SEVERE, "GLOVE - Facebook Ad Export fail: ", ex);
            System.exit(1);
        } finally {
            mitt.close();
        }

        Logger.getLogger(FacebookAd.class.getName()).info("GLOVE - Facebook Ad Extractor finalized");
    }
}

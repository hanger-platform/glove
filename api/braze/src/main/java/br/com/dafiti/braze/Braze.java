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
package br.com.dafiti.braze;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Helio Leal
 */
public class Braze {

    /**
     * Braze File transfer
     *
     * @param args cli parameteres provided by command line.
     */
    public static void main(String[] args) {
        Logger.getLogger(Braze.class.getName()).info("GLOVE - Braze Extractor started");

        //Define the mitt.
        Mitt mitt = new Mitt();

        try {
            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("c", "credentials", "Credentials file", "", true, false)
                    .addParameter("o", "output", "Output file", "", true, false)
                    .addParameter("el", "endpoint", "Identifies the endpoint name", "", true, false)
                    .addParameter("el", "endpoint_list", "Identifies the endpoint that contains a list to extract data from", "", true, false)
                    .addParameter("ed", "endpoint_detail", "Identifies the endpoint that contains the details of each list item", "", true, false)
                    .addParameter("f", "field", "Fields to be extracted from the file", "", true, false)
                    .addParameter("d", "delimiter", "(Optional) File delimiter; ';' as default", ";")
                    .addParameter("s", "sleep", "(Optional) Sleep time in seconds at one request and another; 0 is default", "0")
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

            //Reads the credentials file. 
            JSONParser parser = new JSONParser();
            JSONObject credentials = (JSONObject) parser.parse(new FileReader(cli.getParameter("credentials")));

            boolean nextPage = true;
            int page = 0;

            //Identifies original fields.
            List<String> fields = mitt.getConfiguration().getOriginalFieldsName();

            while (nextPage) {
                String list = cli.getParameter("endpoint_list").replace("<<page>>", String.valueOf(page));

                Logger.getLogger(Braze.class.getName()).log(Level.INFO, "Retrieving data from URL: {0}", new Object[]{list});

                //Connect to API.
                HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(list).openConnection();
                httpURLConnection.setRequestProperty("Authorization", (String) credentials.get("authorization"));
                httpURLConnection.setRequestProperty("Accept", "application/json");
                httpURLConnection.setRequestMethod("GET");

                //Get API Call response.
                try (BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(httpURLConnection.getInputStream()))) {
                    String output;

                    //Get campaign list from API.
                    while ((output = bufferedReader.readLine()) != null) {
                        JSONArray jsonArray
                                = (JSONArray) ((JSONObject) new JSONParser()
                                        .parse(output))
                                        .get(cli.getParameter("endpoint"));

                        Logger.getLogger(Braze.class.getName()).log(Level.INFO, "{0} {1} found ", new Object[]{jsonArray.size(), cli.getParameter("endpoint")});

                        //Identify if at least 1 campaign was found on the page.
                        if (jsonArray.size() > 0) {
                            page++;

                            //Fetchs campaigns list.
                            for (Object object : jsonArray) {
                                String detail = cli
                                        .getParameter("endpoint_detail")
                                        .replace(
                                                "<<id>>",
                                                String.valueOf(((JSONObject) object).get("id"))
                                        );

                                HttpURLConnection connectionDetails = (HttpURLConnection) new URL(detail).openConnection();
                                connectionDetails.setRequestProperty("Authorization", (String) credentials.get("authorization"));
                                connectionDetails.setRequestProperty("Accept", "application/json");
                                connectionDetails.setRequestMethod("GET");

                                //Get API Call response.
                                try (BufferedReader bfDetail = new BufferedReader(
                                        new InputStreamReader(connectionDetails.getInputStream()))) {
                                    String line;

                                    //Get a campaign details from API.
                                    while ((line = bfDetail.readLine()) != null) {
                                        List record = new ArrayList();
                                        JSONObject details = (JSONObject) new JSONParser().parse(line);

                                        fields.forEach((field) -> {
                                            //Identifies if the field exists.
                                            if (details.containsKey(field)) {
                                                record.add(details.get(field));
                                            } else {
                                                record.add(null);
                                            }
                                        });

                                        mitt.write(record);
                                    }
                                }
                                connectionDetails.disconnect();
                            }
                        } else {
                            nextPage = false;
                        }
                    }
                }
                httpURLConnection.disconnect();

                //Identify if has sleep time until next API call.
                if (cli.getParameterAsInteger("sleep") > 0) {
                    try {
                        Logger.getLogger(Braze.class.getName())
                                .log(Level.INFO, "Sleeping {0} seconds until next API call", cli.getParameterAsInteger("sleep"));

                        Thread.sleep(Long.valueOf(cli.getParameterAsInteger("sleep") * 1000));
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Braze.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        } catch (DuplicateEntityException
                | FileNotFoundException
                | ParseException ex) {

            Logger.getLogger(Braze.class.getName()).log(Level.SEVERE, "GLOVE - Braze fail: ", ex);
            System.exit(1);
        } catch (IOException ex) {

            Logger.getLogger(Braze.class.getName()).log(Level.SEVERE, "GLOVE - Braze fail: ", ex);
            System.exit(1);
        } finally {
            mitt.close();
        }

        Logger.getLogger(Braze.class.getName()).info("GLOVE - Braze Extractor finalized");
    }
}

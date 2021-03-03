/*
 * Copyright (c) 2021 Dafiti Group
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
package br.com.dafiti.hi;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.settings.ReaderSettings;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Valdiney V GOMES
 */
public class Hi {

    private static final Logger LOG = Logger.getLogger(Hi.class.getName());
    private static final String HI_PLAFTORM_ENDPOINT = "http://plataforma1.seekr.com.br";

    /**
     * Kestraa File transfer
     *
     * @param args cli parameteres provided by command line.
     */
    public static void main(String[] args) {
        LOG.info("GLOVE - Hi Platform API extractor started");

        int page = 0;
        boolean process = true;
        boolean paginate = false;

        //Define the mitt.
        Mitt mitt = new Mitt();

        try {
            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("c", "credentials", "Credentials file", "", true, false)
                    .addParameter("o", "output", "Output file", "", true, false)
                    .addParameter("f", "field", "Fields to be extracted from input file", "", true, false)
                    .addParameter("e", "endpoint", "API Endpoint name", "", true, false)
                    .addParameter("p", "parameters", "Endpoint parameters", "", true, false)
                    .addParameter("g", "paginate", "Identifies if the endpoint has pagination", false)
                    .addParameter("a", "partition", "(Optional)  Partition, divided by + if has more than one field")
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

            //Retrives API credentials. 
            String key = credentials.get("key").toString();
            String secretKey = credentials.get("secretKey").toString();

            //Identifies if the endpoint has pagination. 
            paginate = cli.getParameterAsBoolean("paginate");

            //Identifies endpoint parameters. 
            JSONObject parameters = null;
            String endpointParameter = cli.getParameter("parameters");

            if (!endpointParameter.isEmpty()) {
                try {
                    parameters = (JSONObject) parser.parse(endpointParameter);
                } catch (ParseException ex) {
                    Logger.getLogger(ReaderSettings.class.getName()).log(Level.SEVERE, "Fail parsing endpoint parameters : " + endpointParameter, ex);
                }
            }

            do {
                //Identifies the page number. 
                page++;

                //Defines the credential parameter values. 
                String ts = String.valueOf(System.currentTimeMillis() / 1000);
                String hash = DigestUtils.sha1Hex(secretKey.concat(ts));

                //Connect to the API. 
                try (CloseableHttpClient client = HttpClients.createDefault()) {
                    HttpGet httpGet = new HttpGet(HI_PLAFTORM_ENDPOINT + "/api/v3/" + cli.getParameter("endpoint") + ".json");

                    //Sets default URI parameters. 
                    URIBuilder uriBuilder = new URIBuilder(httpGet.getURI())
                            .addParameter("key", key)
                            .addParameter("ts", ts)
                            .addParameter("hash", hash)
                            .addParameter("page", String.valueOf(page))
                            .addParameter("per_page", String.valueOf(20));

                    //Sets endpoint URI parameters. 
                    if (parameters != null && !parameters.isEmpty()) {
                        for (Object k : parameters.keySet()) {
                            uriBuilder.addParameter((String) k, (String) parameters.get(k));
                        }
                    }

                    //Sets URI parameters. 
                    httpGet.setURI(uriBuilder.build());

                    //Execute a request. 
                    CloseableHttpResponse response = client.execute(httpGet);

                    //Identifies the response status code. 
                    int statusCode = response.getStatusLine().getStatusCode();

                    if (statusCode == 200) {
                        JSONObject payload = (JSONObject) new JSONParser()
                                .parse(
                                        EntityUtils.toString(response.getEntity(), "UTF-8")
                                );

                        //Identifies if there are payload to process. 
                        if (!payload.isEmpty()) {
                            JSONArray content = (JSONArray) payload.get(cli.getParameter("endpoint"));

                            content.forEach(object -> {
                                List record = new ArrayList();

                                mitt.getConfiguration()
                                        .getOriginalFieldsName()
                                        .forEach(field -> {
                                            try {
                                                record.add(JsonPath.read(object, "$." + field));
                                            } catch (PathNotFoundException ex) {
                                                record.add("");
                                            }
                                        });

                                mitt.write(record);
                            });
                        }
                    } else {
                        throw new Exception("HTTP Exception " + statusCode);
                    }
                }
            } while (paginate && process);
        } catch (Exception ex) {

            LOG.log(Level.SEVERE, "GLOVE - Hi Platform API extractor fail: ", ex);
            System.exit(1);
        } finally {
            mitt.close();
        }

        LOG.info("GLOVE - Hi Platform API extractor finalized");
    }
}

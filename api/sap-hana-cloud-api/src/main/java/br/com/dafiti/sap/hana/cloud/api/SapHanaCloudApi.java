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
package br.com.dafiti.sap.hana.cloud.api;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.model.Configuration;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import java.io.FileReader;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Helio Leal
 */
public class SapHanaCloudApi {

    private static final Logger LOG = Logger.getLogger(SapHanaCloudApi.class.getName());
    private static final String SAP_HANA_CLOUD_API_TOKEN_URL = "https://hdbprd.authentication.us10.hana.ondemand.com/oauth/token";
    private static final String SAP_HANA_CLOUD_API_ENDPOINT = "https://gfg-monitor-prd.cfapps.us10.hana.ondemand.com/monitor/";

    /**
     *
     * @param args cli parameteres provided by command line.
     */
    public static void main(String[] args) {
        LOG.info("GLOVE - SAPBTP API extractor started");

        JSONObject parameters = null;

        //Define the mitt.
        Mitt mitt = new Mitt();

        try {
            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("c", "credentials", "Credentials file", "", true, false)
                    .addParameter("o", "output", "Output file", "", true, false)
                    .addParameter("f", "field", "Fields to be retrieved from an endpoint", "", true, false)
                    .addParameter("e", "endpoint", "Endpoint name", "", true, false)
                    .addParameter("p", "parameters", "(Optional) Endpoint parameters", "", true, true)
                    .addParameter("b", "object", "(Optional) Json object", "", true, true)
                    //.addParameter("g", "paginate", "(Optional) Identifies if the endpoint has pagination", false)
                    .addParameter("a", "partition", "(Optional)  Partition, divided by + if has more than one field", "")
                    .addParameter("k", "key", "(Optional) Unique key, divided by + if has more than one field", "");

            //Reads the command line interface. 
            CommandLineInterface cli = mitt.getCommandLineInterface(args);

            //Defines output file.
            mitt.setOutputFile(cli.getParameter("output"));

            //Defines fields.
            Configuration configuration = mitt.getConfiguration();

            if (cli.hasParameter("partition")) {
                configuration
                        .addCustomField("partition_field", new Concat((List) cli.getParameterAsList("partition", "\\+")));
            }

            if (cli.hasParameter("key")) {
                configuration
                        .addCustomField("custom_primary_key", new Concat((List) cli.getParameterAsList("key", "\\+")));
            }

            configuration
                    .addCustomField("etl_load_date", new Now())
                    .addField(cli.getParameterAsList("field", "\\+"));

            //Reads the credentials file. 
            JSONParser parser = new JSONParser();
            JSONObject credentials = (JSONObject) parser.parse(new FileReader(cli.getParameter("credentials")));

            //Retrieves API credentials. 
            String username = credentials.get("username").toString();
            String password = credentials.get("password").toString();

            //Connect to the API. 
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                //Retrieves API Token.
                HttpGet httpGet = new HttpGet(SAP_HANA_CLOUD_API_TOKEN_URL);

                //Sets default URI parameters. 
                URIBuilder tokenUriBuilder = new URIBuilder(httpGet.getURI())
                        .addParameter("grant_type", "client_credentials")
                        .addParameter("response_type", "token");

                //Sets URI parameters. 
                httpGet.setURI(tokenUriBuilder.build());

                //Encode username and password to make api call.
                String encoding = Base64.getEncoder().encodeToString((username + ":" + password).getBytes("UTF-8"));

                //Sets Headers
                httpGet.addHeader("Authorization", "Basic " + encoding);

                //Executes a request. 
                CloseableHttpResponse closeableHttpResponse = client.execute(httpGet);

                //Gets a reponse entity. 
                String tokenEntity = EntityUtils.toString(closeableHttpResponse.getEntity(), "UTF-8");

                //Identifies if token was received.
                if (!tokenEntity.isEmpty()) {
                    JSONObject tokenJson = (JSONObject) new JSONParser().parse(tokenEntity);

                    if (!tokenJson.isEmpty()) {
                        LOG.log(Level.INFO, "Token was received successfully");

                        //Identifies endpoint parameters. 
                        String endpointParameter = cli.getParameter("parameters");

                        if (endpointParameter != null && !endpointParameter.isEmpty()) {
                            try {
                                parameters = (JSONObject) parser.parse("{\"$filter\":\"DT_HORA_INICIO_EXECUCAO ge 2021-12-13T16:17:00Z\"}");
                            } catch (ParseException ex) {
                                LOG.log(Level.INFO, "Fail parsing endpoint parameters: {0}", endpointParameter);
                            }
                        }

                        //Retrieves endpoint Token.
                        HttpGet endPoint = new HttpGet(SAP_HANA_CLOUD_API_ENDPOINT + URLEncoder.encode(cli.getParameter("endpoint")));

                        //Sets default URI parameters. 
                        URIBuilder uriBuilder = new URIBuilder(endPoint.getURI());

                        //Sets endpoint URI parameters. 
                        if (parameters != null && !parameters.isEmpty()) {
                            for (Object k : parameters.keySet()) {
                                uriBuilder.addParameter((String) k, (String) parameters.get(k));
                            }
                        }

                        //Sets URI parameters. 
                        httpGet.setURI(uriBuilder.build());

                        //Set Headers.
                        endPoint.addHeader("Authorization", "Bearer " + tokenJson.get("access_token").toString());

                        //Executes a request. 
                        CloseableHttpResponse responseHttp = client.execute(endPoint);

                        //Gets a reponse entity. 
                        String entity = EntityUtils.toString(responseHttp.getEntity(), "UTF-8");

                        //Identifies if there are payload to process. 
                        if (!entity.isEmpty()) {
                            JSONObject json = (JSONObject) new JSONParser().parse(entity);

                            //Identifies the response status code.
                            switch ((int) responseHttp.getStatusLine().getStatusCode()) {
                                case 200 /*OK*/:
                                    Object object;

                                    //Identifies which object should be picked up from the payload.
                                    if (cli.getParameter("object") == null || cli.getParameter("object").isEmpty()) {
                                        object = json.get("value");
                                    } else {
                                        if ("*".equals(cli.getParameter("object"))) {
                                            object = json;
                                        } else {
                                            object = json.get(cli.getParameter("object"));
                                        }
                                    }

                                    break;
                                case 400 /*BAD REQUEST*/:
                                    LOG.log(Level.INFO, "Bad Request");
                                    break;
                                case 403 /*FORBIDDEN*/:
                                    LOG.log(Level.INFO, "Forbidden");
                                    break;
                                default:
                                    throw new Exception("HTTP Exception " + responseHttp.getStatusLine().getStatusCode());
                            }

                        } else {
                            throw new Exception("Empty response for data request " + httpGet.getURI());
                        }
                    }

                } else {
                    throw new Exception("Empty response for token request " + httpGet.getURI());
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "GLOVE - SAPBTP API extractor fail: ", ex);
            System.exit(1);
        } finally {
            mitt.close();
        }

        LOG.info("GLOVE - SAPBTP API extractor finalized");

    }

}

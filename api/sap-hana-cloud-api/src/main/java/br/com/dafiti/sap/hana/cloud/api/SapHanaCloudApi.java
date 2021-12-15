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
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

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
        LOG.info("GLOVE - SAP HANA CLOUD API extractor started");

        //Defines the mitt.
        Mitt mitt = new Mitt();

        try {
            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("c", "credentials", "Credentials file", "", true, false)
                    .addParameter("o", "output", "Output file", "", true, false)
                    .addParameter("f", "field", "Fields to be retrieved from an endpoint", "", true, false)
                    .addParameter("e", "uri", "URI", "", true, false)
                    .addParameter("b", "object", "(Optional) Json object", "", true, true)
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
                LOG.log(Level.INFO, "Retrieving token from: {0}", SAP_HANA_CLOUD_API_TOKEN_URL);

                //Retrieves API Token.
                HttpGet httpGet = new HttpGet(SAP_HANA_CLOUD_API_TOKEN_URL);

                //Sets default URI parameters. 
                URIBuilder tokenUriBuilder = new URIBuilder(httpGet.getURI())
                        .addParameter("grant_type", "client_credentials")
                        .addParameter("response_type", "token");

                //Sets URI parameters. 
                httpGet.setURI(tokenUriBuilder.build());

                //Encode user and password for api call.
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
                        //Identifies if the endpoint has pagination. 
                        boolean paginate = true;

                        //Identifies the uri.
                        String uri = cli.getParameter("uri").replaceAll(" ", "%20");

                        do {
                            //Idenfities if the endpoint has pagination. 
                            if (paginate) {
                                LOG.log(Level.INFO, "Retrieving data from URL: {0}", SAP_HANA_CLOUD_API_ENDPOINT + uri);
                            }

                            paginate = false;

                            //API call for endpoint.
                            HttpGet url = new HttpGet(SAP_HANA_CLOUD_API_ENDPOINT + uri);

                            //Set Headers.
                            url.addHeader("Authorization", "Bearer " + tokenJson.get("access_token").toString());

                            //Executes a request. 
                            CloseableHttpResponse responseHttp = client.execute(url);

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

                                        //Identifies if the payload is an array or an object.
                                        if (object instanceof JSONArray) {

                                            ((JSONArray) object).forEach(item -> {
                                                List record = new ArrayList();

                                                mitt.getConfiguration()
                                                        .getOriginalFieldName()
                                                        .forEach(field -> {
                                                            try {
                                                                record.add(JsonPath.read(item, "$." + field));
                                                            } catch (PathNotFoundException ex) {
                                                                record.add("");
                                                            }
                                                        });

                                                mitt.write(record);
                                            });

                                        }

                                        //Identifies if there is next page.
                                        if (json.containsKey("@odata.nextLink")) {
                                            uri = json.get("@odata.nextLink").toString();
                                            paginate = true;
                                        }

                                        break;
                                    case 400 /*BAD REQUEST*/:
                                        LOG.log(Level.INFO, "[Bad Request]: {0}", json.toJSONString());
                                        break;
                                    case 403 /*FORBIDDEN*/:
                                        LOG.log(Level.INFO, "[Forbidden]: {0}", json.toJSONString());
                                        break;
                                    default:
                                        throw new Exception("HTTP Exception " + responseHttp.getStatusLine().getStatusCode());
                                }
                            } else {
                                throw new Exception("Empty response for data request " + httpGet.getURI());
                            }
                        } while (paginate);
                    }
                } else {
                    throw new Exception("Empty response for token request: " + httpGet.getURI());
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "GLOVE - SAP HANA CLOUD API extractor fail: ", ex);
            System.exit(1);
        } finally {
            mitt.close();
        }

        LOG.info("GLOVE - SAP HANA CLOUD API extractor finalized");

    }

}

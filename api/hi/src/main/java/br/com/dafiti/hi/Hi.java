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
import br.com.dafiti.mitt.model.Configuration;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import java.io.FileReader;
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
    private static final String HI_PLAFTORM_ENDPOINT = "http://plataforma1.seekr.com.br/api/v3/";
    private static final int MAX_RETRY = 3;

    /**
     * Kestraa File transfer
     *
     * @param args cli parameteres provided by command line.
     */
    public static void main(String[] args) {
        LOG.info("GLOVE - Hi Platform API extractor started");

        int page = 0;
        int retries = 0;
        boolean process = true;
        boolean paginate = false;
        boolean retry = false;
        JSONObject parameters = null;

        //Define the mitt.
        Mitt mitt = new Mitt();

        try {
            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("c", "credentials", "Credentials file", "", true, false)
                    .addParameter("o", "output", "Output file", "", true, false)
                    .addParameter("f", "field", "Fields to be retrieved from an endpoint in a JsonPath fashion", "", true, false)
                    .addParameter("e", "endpoint", "Endpoint name", "", true, false)
                    .addParameter("p", "parameters", "(Optional) Endpoint parameters", "", true, true)
                    .addParameter("b", "object", "(Optional) Json object", "", true, true)
                    .addParameter("g", "paginate", "(Optional) Identifies if the endpoint has pagination", false)
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

            //Retrives API credentials. 
            String key = credentials.get("key").toString();
            String secretKey = credentials.get("secretKey").toString();

            //Identifies if the endpoint has pagination. 
            paginate = cli.hasParameter("paginate");

            //Identifies endpoint parameters. 
            String endpointParameter = cli.getParameter("parameters");

            if (endpointParameter != null && !endpointParameter.isEmpty()) {
                try {
                    parameters = (JSONObject) parser.parse(endpointParameter);
                } catch (ParseException ex) {
                    LOG.log(Level.INFO, "Fail parsing endpoint parameters: {0}", endpointParameter);
                }
            }

            do {
                //Identifies if is a retry. 
                if (!retry) {
                    page++;
                }

                //Idenfities if the endpoint has pagination. 
                if (paginate) {
                    LOG.log(Level.INFO, "Page: {0} (50 per page)", page);
                }

                //Connect to the API. 
                try (CloseableHttpClient client = HttpClients.createDefault()) {
                    HttpGet httpGet = new HttpGet(HI_PLAFTORM_ENDPOINT + cli.getParameter("endpoint") + ".json");

                    //Defines the credential parameter values. 
                    String ts = String.valueOf(System.currentTimeMillis() / 1000);
                    String hash = DigestUtils.sha1Hex(secretKey.concat(ts));

                    //Sets default URI parameters. 
                    URIBuilder uriBuilder = new URIBuilder(httpGet.getURI())
                            .addParameter("key", key)
                            .addParameter("ts", ts)
                            .addParameter("hash", hash)
                            .addParameter("page", String.valueOf(page))
                            .addParameter("per_page", String.valueOf(50));

                    //Sets endpoint URI parameters. 
                    if (parameters != null && !parameters.isEmpty()) {
                        for (Object k : parameters.keySet()) {
                            uriBuilder.addParameter((String) k, (String) parameters.get(k));
                        }
                    }

                    //Sets URI parameters. 
                    httpGet.setURI(uriBuilder.build());

                    //Executes a request. 
                    CloseableHttpResponse response = client.execute(httpGet);

                    //Gets a reponse entity. 
                    String entity = EntityUtils.toString(response.getEntity(), "UTF-8");

                    if (!entity.isEmpty()) {
                        JSONObject json = (JSONObject) new JSONParser().parse(entity);

                        //Identifies if there are payload to process. 
                        if (!json.isEmpty()) {
                            //Identifies if there are a response node.
                            if (json.containsKey("response")) {
                                long statusCode = JsonPath.read(json, "$.response.code");

                                //Identifies the response status code.
                                switch ((int) statusCode) {
                                    case 200 /*OK*/:
                                        Object object;

                                        //Identifies which object should be picked up from the payload.
                                        if (cli.getParameter("object") == null || cli.getParameter("object").isEmpty()) {
                                            object = json.get(cli.getParameter("endpoint"));
                                        } else {
                                            if ("*".equals(cli.getParameter("object"))) {
                                                object = json;
                                            } else {
                                                object = json.get(cli.getParameter("object"));
                                            }
                                        }

                                        //Identifies if the payload is an array or an object.
                                        if (object instanceof JSONArray) {
                                            if (((JSONArray) object).isEmpty()) {
                                                process = false;
                                            } else {
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
                                        } else if (object instanceof JSONObject) {
                                            if (((JSONObject) object).isEmpty()) {
                                                process = false;
                                            } else {
                                                List record = new ArrayList();

                                                mitt.getConfiguration()
                                                        .getOriginalFieldName()
                                                        .forEach(field -> {
                                                            try {
                                                                record.add(JsonPath.read(object, "$." + field));
                                                            } catch (PathNotFoundException ex) {
                                                                record.add("");
                                                            }
                                                        });

                                                mitt.write(record);
                                            }
                                        }

                                        //Identifies that retry is not needed.
                                        retry = false;

                                        break;
                                    case 403 /*NO_PERMISSION*/:
                                        retries++;

                                        //Identifies that is a retry.
                                        retry = true;

                                        if (retries > MAX_RETRY) {
                                            throw new Exception("HTTP Exception " + statusCode);
                                        } else {
                                            Thread.sleep(retries * 10000);
                                            LOG.log(Level.INFO, "Authentication error, retry {0}", retries);
                                        }

                                        break;
                                    default:
                                        throw new Exception("HTTP Exception " + statusCode);
                                }
                            } else {
                                LOG.log(Level.WARNING, "No response found: {0}", json.toString());
                            }
                        }
                    } else {
                        throw new Exception("Empty response entity for request " + httpGet.getURI());
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

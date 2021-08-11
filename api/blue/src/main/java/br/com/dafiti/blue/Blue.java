/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.dafiti.blue;

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
 * @author flavialima
 */
public class Blue {

    private static final Logger LOG = Logger.getLogger(Blue.class.getName());
    private static final String HTTP_BLUE_ENDPOINT = "http://getblue.io/rest/report/api/campaigns";
    private static final int MAX_RETRY = 3;

    public static void main(String[] args) {
        LOG.info("GLOVE - Blue API extractor started");

        int page = 0;
        int retries = 0;
        //boolean process = true;
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
                    .addParameter("f", "field", "Fields to be retrieved from an endpoint in JsonPath fashion", "", true, false)
                    .addParameter("a", "app", "App name", "", true, false)
                    .addParameter("p", "parameters", "(Optional) Endpoint parameters", "")
                    .addParameter("g", "paginate", "(Optional) Identifies if the endpoint has pagination", false)
                    .addParameter("r", "partition", "(Optional)  Partition, divided by + if has more than one field", "");

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

            configuration
                    .addCustomField("etl_load_date", new Now())
                    .addField(cli.getParameterAsList("field", "\\+"));

            //Reads the credentials file. 
            JSONParser parser = new JSONParser();
            JSONObject credentials = (JSONObject) parser.parse(new FileReader(cli.getParameter("credentials")));

            //Retrives API credentials. 
            String token = credentials.get("tokenUuid").toString();
            String app = cli.getParameter("app");

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
                try ( CloseableHttpClient client = HttpClients.createDefault()) {
                    HttpGet httpGet = new HttpGet(HTTP_BLUE_ENDPOINT);

                    httpGet.setHeader("app", app);
                    httpGet.setHeader("tokenUuid", token);

                    //Sets default URI parameters. 
                    URIBuilder uriBuilder = new URIBuilder(httpGet.getURI());

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

                        JSONArray obj = (JSONArray) new JSONParser().parse(entity);

                        if (!obj.isEmpty()) {

                            switch ((int) response.getStatusLine().getStatusCode()) {

                                case 200:
                                    for (Object json : obj) {
                                        List record = new ArrayList();

                                        mitt.getConfiguration()
                                                .getOriginalFieldName()
                                                .forEach(field -> {
                                                    try {
                                                        record.add(JsonPath.read(json, "$." + field));
                                                    } catch (PathNotFoundException ex) {
                                                        record.add("");
                                                    }
                                                });
                                        mitt.write(record);
                                    }
                                    
                                    //Identifies that retry is not needed.
                                    retry = false;
                                    
                                    break;
                                            
                                case 403:
                                    retries++;

                                    //Identifies that is a retry.
                                    retry = true;
                                    
                                    if (retries > MAX_RETRY) {
                                        throw new Exception("HTTP Exception " + response.getStatusLine().getStatusCode());
                                    } else {
                                        Thread.sleep(retries * 10000);
                                        LOG.log(Level.INFO, "Authentication error, retry {0}", retries);
                                    }
                                    break;

                                default:
                                    throw new Exception("HTTP Exception " + response.getStatusLine().getStatusCode());
                            }

                        }
                    } else {
                        throw new Exception("Empty response entity for request " + httpGet.getURI());
                    }
                }
            } while (paginate);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "GLOVE - Blue API extractor fail: ", ex);
            System.exit(1);
        } finally {
            mitt.close();
        }

        LOG.info("GLOVE - Blue API extractor finalized");

    }

}

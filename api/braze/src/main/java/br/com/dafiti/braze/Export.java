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
import br.com.dafiti.mitt.decoder.ZipDecoder;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Helio Leal
 */
public class Export {

    private final String endpointDetail;
    private final String output;
    private final String service;
    private final List key;
    private final List partition;
    private final List fields;
    private final int sleep;
    private final JSONObject credentials;
    private final String requestBody;

    public Export(
            String endpointDetail,
            String output,
            String service,
            List key,
            List partition,
            List fields,
            int sleep,
            JSONObject credentials,
            String requestBody) {

        this.endpointDetail = endpointDetail;
        this.output = output;
        this.service = service;
        this.key = key;
        this.partition = partition;
        this.fields = fields;
        this.sleep = sleep;
        this.credentials = credentials;
        this.requestBody = requestBody;
    }

    void extract() throws IOException, ParseException, DuplicateEntityException {
        //Defines a MITT instance. 
        Mitt mitt = new Mitt();

        //Defines output file.
        mitt.setOutputFile(this.output);

        //Defines fields.
        mitt.getConfiguration()
                .addCustomField("partition_field", new Concat((List) this.partition))
                .addCustomField("custom_primary_key", new Concat((List) this.key))
                .addCustomField("etl_load_date", new Now());

        //Defines the standart field who has JSON content.
        mitt.getConfiguration().addField("content");        
        
        //Defines fields.
        mitt.getConfiguration().addField(this.fields);
        
        //Defines the input file don't has header. 
        mitt.getWriterSettings().setHeader(mitt
                .getConfiguration()
                .getOriginalFieldsName());

        //Defines the output path for temp files.
        Path outputPath = Files.createTempDirectory("braze_export_users_");

        //Temporary file name.
        String filename = UUID.randomUUID() + ".zip";

        Logger.getLogger(Export.class.getName()).log(Level.INFO, "POST request for endpoint: {0}", new Object[]{this.endpointDetail});

        //Executes API call
        HttpResponse<String> response = Unirest.post(this.endpointDetail)
                .header("Content-Type", "application/json")
                .header("Authorization", (String) credentials.get("authorization"))
                .header("cache-control", "no-cache")
                .body("{\n  \"segment_id\": \"c458898b-4593-4fe8-afa6-7fcb8421330f\",\n  \"callback_endpoint\" : \"https://rest.iad-03.braze.com/users/export/segment/callback/\",\n  \"fields_to_export\" : [\"country\",\"external_id\",\"braze_id\",\"random_bucket\"]\n}")
                //.body(this.requestBody)
                .asString();

        Logger.getLogger(Export.class.getName()).log(Level.INFO, "POST response: Status {0} - {1}", new Object[]{response.getStatus(), response.getBody()});

        //Identifies if request was successfully done.
        if (response.getStatus() == HTTP_CREATED
                || response.getStatus() == HTTP_OK) {

            //Get url with file to download.
            String url = (String) ((JSONObject) new JSONParser()
                    .parse(response.getBody()))
                    .get("url");

            //Identify if file is ready
            boolean ready = false;

            while (!ready) {
                //Executes download URL.
                HttpResponse<String> responseReady = Unirest.get(url).asString();

                //Identifies if file is ready to download.
                if (responseReady.getStatus() == HTTP_OK) {
                    Logger.getLogger(Braze.class.getName()).log(Level.INFO, "Downloading file from URL: {0} to: {1}", new Object[]{url, outputPath});
                    ready = true;

                    //Download file from URL
                    FileUtils.copyURLToFile(
                            new URL(url),
                            new File(outputPath + "/" + filename)
                    );

                } else {
                    //Response 403 means that file is not ready.
                    if (responseReady.getStatus() == HttpURLConnection.HTTP_FORBIDDEN) {
                        Logger.getLogger(Braze.class.getName()).log(Level.INFO, "File {0} is not ready to download, error: {1}", new Object[]{url, responseReady.getStatus()});

                        //Identify if has sleep time until next API call.
                        if (this.sleep > 0) {
                            try {
                                Logger.getLogger(Braze.class.getName())
                                        .log(Level.INFO, "Sleeping {0} seconds until next try", this.sleep);

                                Thread.sleep(Long.valueOf(this.sleep * 1000));
                            } catch (InterruptedException ex) {
                                Logger.getLogger(Braze.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }

                    } else {
                        ready = true;
                        throw new IOException("GLOVE - Braze export fail: " + responseReady.getStatus() + " - " + responseReady.getBody());
                    }
                }
            }

            Logger.getLogger(Braze.class.getName()).log(Level.INFO, "Unzipping file: {0}/{1}", new Object[]{outputPath, filename});

            //Decompress zip file.
            ZipDecoder zipDecoder = new ZipDecoder();
            zipDecoder.decode(new File(outputPath + "/" + filename), null);

            Logger.getLogger(Braze.class.getName()).log(Level.INFO, "Writing output file to: {0}", this.output);

            //Write to the output.
            mitt.write(outputPath.toFile(), "*.txt");

        } else {
            Logger.getLogger(Braze.class.getName()).log(
                    Level.SEVERE,
                    "Braze - request error: Status {0} - {1}.",
                    new Object[]{response.getStatus(), response.getStatusText()}
            );
        }

        //Remove temporary path. 
        //Files.delete(outputPath);
        mitt.close();
    }
}

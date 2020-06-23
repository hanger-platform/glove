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
package br.com.dafiti.microsoft.blob.storage;

/**
 * Microsoft Blob Storage Extractor.
 *
 * @author Fernando Saga
 */
import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.ListBlobsOptions;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class MicrosoftBlobStorage {

    public static void main(String[] args) throws IOException, ParseException {

        Logger.getLogger(MicrosoftBlobStorage.class.getName()).log(Level.INFO, "GLOVE - Microsoft Blob Storage Extractor started");

        //Defines a MITT instance. 
        Mitt mitt = new Mitt();

        try {
            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("j", "credentials", "Credentials file", "", true, false)
                    .addParameter("C", "container", "Blob container", "", true, false)
                    .addParameter("O", "output", "Output file", "", true, false)
                    .addParameter("sd", "start_date", "Start date", "", true, false)
                    .addParameter("ed", "end_date", "End date", "", true, false)
                    .addParameter("f", "field", "Fields to be extracted from the file", "", true, false)
                    .addParameter("d", "delimiter", "(Optional) File delimiter; ';' as default", ";")
                    .addParameter("P", "prefix", "(Optional) Return blobs whose names begin with the specified prefix", null)
                    .addParameter("pa", "partition", "(Optional)  Partition, divided by + if has more than one field")
                    .addParameter("k", "key", "(Optional) Unique key, divided by + if has more than one field", "")
                    .addParameter("t", "timeout", "(Optional)(Default is 60) API timeout in minutes.", "60")
                    .addParameter("e", "encode", "(Optional) Encode file.", "auto")
                    .addParameter("pr", "properties", "(Optional) Reader properties.", "");

            //Read the command line interface. 
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

            String accountName = credentials.get("accountName").toString();
            String sasToken = credentials.get("sasToken").toString();

            //Get your Storage account blob service URL endpoint.
            String endpoint = String.format(
                    Locale.ROOT,
                    "https://%s.blob.core.windows.net",
                    accountName
            );

            //Create an instance of the client.
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .endpoint(endpoint)
                    .sasToken(sasToken)
                    .buildClient();

            //Create an instance of the specified container.
            BlobContainerClient blobContainerClient
                    = blobServiceClient.getBlobContainerClient(cli.getParameter("container"));

            //Get list blobs
            PagedIterable<BlobItem> listBlobs = blobContainerClient.listBlobs(
                    new ListBlobsOptions().setPrefix(cli.getParameter("prefix")),
                    Duration.ofMinutes(Long.valueOf(cli.getParameter("timeout")))
            );

            Path outputPath = Files.createTempDirectory("microsoft_blob_storage_");

            Logger.getLogger(MicrosoftBlobStorage.class.getName()).log(Level.INFO, "Downloading files from: {0}", cli.getParameter("container"));

            for (BlobItem blobItem : listBlobs) {
                String fileName = blobItem.getName();

                //Create an instance of the specified blob.
                BlobClient blobClient = blobContainerClient.getBlobClient(fileName);

                if (blobClient.exists()) {
                    LocalDate updatedDate = blobClient.getProperties().getLastModified().toLocalDate();

                    //Identifies if the file modification date is between start_date and end_date.
                    if (updatedDate.compareTo(LocalDate.parse(cli.getParameter("start_date"))) >= 0
                            && updatedDate.compareTo(LocalDate.parse(cli.getParameter("end_date"))) <= 0) {

                        Logger.getLogger(MicrosoftBlobStorage.class.getName()).log(Level.INFO, "Transfering: {0}", fileName);

                        blobClient.downloadToFile(outputPath.toString().concat("/").concat(fileName));
                    }
                }
            }

            Logger.getLogger(MicrosoftBlobStorage.class.getName()).log(Level.INFO, "Writing output file to: {0}", cli.getParameter("output"));

            // Write to the output.
            mitt.getReaderSettings().setDelimiter(cli.getParameter("delimiter").charAt(0));
            mitt.getReaderSettings().setEncode(cli.getParameter("encode"));
            if (cli.getParameter("properties") != null) {
                mitt.getReaderSettings().setProperties(cli.getParameter("properties"));
            }
            mitt.write(outputPath.toFile());

            //Remove temporary path. 
            Files.delete(outputPath);

        } catch (DuplicateEntityException
                | IOException ex) {
            Logger.getLogger(MicrosoftBlobStorage.class.getName()).log(Level.SEVERE, "Microsoft Blob Storage - Failure: ", ex);
            System.exit(1);
        } finally {
            mitt.close();
        }

        Logger.getLogger(MicrosoftBlobStorage.class.getName()).info("GLOVE - Microsoft Blob Storage Extractor finalized.");
    }
}

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
package br.com.dafiti.googleplaystore;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import com.google.api.client.util.IOUtils;
import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import org.joda.time.LocalDate;

/**
 * Google Play Store extractor.
 *
 * @author Helio Leal
 * @author Fernando Saga
 */
public class GooglePlayStore {

    public static void main(String[] args) {
        Logger.getLogger(GooglePlayStore.class.getName()).info("GLOVE - Google Play Store Extractor started");

        //Define the mitt.
        Mitt mitt = new Mitt();

        try {
            //Define parameters.
            mitt.getConfiguration()
                    .addParameter("j", "json_key_path", "Path and name of the json generate in Google Cloud Platform.", "", true, false)
                    .addParameter("o", "output", "Identifies the local path that saves downloaded files from Google Cloud Platform Storage", "", true, false)
                    .addParameter("b", "bucket", "Bucket name", "", true, false)
                    .addParameter("sd", "start_date", "Start date", "", true, false)
                    .addParameter("ed", "end_date", "End date", "", true, false)
                    .addParameter("ph", "path", "Path of the chosen location on Google Cloud Storage to be extracted", "", true, false)
                    .addParameter("f", "field", "fields to be extracted", "", true, false)
                    .addParameter("p", "partition", "Define the partition field or fields, divided by +", "")
                    .addParameter("d", "dimension", "(Optional) Define the dimension")
                    .addParameter("s", "package_name", "(Optional) Extraction package name", "*")
                    .addParameter("k", "key", "(Optional) Unique key, divided by + if has more than one field", "");

            //Read the command line interface. 
            CommandLineInterface cli = mitt.getCommandLineInterface(args);

            //Define output file.
            mitt.setOutput(cli.getParameter("output"));

            //Define fields.
            mitt.getConfiguration()
                    .addCustomField("partition_field", new Concat((List) cli.getParameterAsList("partition", "\\+")))
                    .addCustomField("custom_primary_key", new Concat((List) cli.getParameterAsList("key", "\\+")))
                    .addCustomField("etl_load_date", new Now())
                    .addField(cli.getParameterAsList("field", "\\+"));

            //Define credentials to consume the API.
            Storage storage = StorageOptions
                    .newBuilder()
                    .setCredentials(ServiceAccountCredentials.fromStream(new FileInputStream(cli.getParameter("json_key_path"))))
                    .build()
                    .getService();

            //Get list of blobs from google storage.
            Page<Blob> blobs = storage.list(cli.getParameter("bucket"), Storage.BlobListOption.currentDirectory(), Storage.BlobListOption.prefix(cli.getParameter("path")));

            //Temp path.
            String tmpPath = System.getProperty("java.io.tmpdir") + "/" + cli.getParameter("package_name").replace(".", "_") + "/" + cli.getParameter("path");

            //Only create folder to receive files from storage if blobs get at least one result.
            if (blobs.iterateAll().iterator().hasNext()) {
                //Path of downloaded files.
                File filePath = new File(tmpPath);

                if (!filePath.exists()) {
                    filePath.mkdirs();
                }
            }

            //Fetch blobs and save them into a directory.
            for (Blob blob : blobs.iterateAll()) {
                //Identify with the filter came via parameter, if it is to consider this blob.
                if (blob.getName().contains("_" + cli.getParameter("package_name") + "_") || cli.getParameter("package_name").trim().equals("*")) {

                    //Identify if dimension is filled or if it is null.
                    if (cli.getParameter("dimension") == null || cli.getParameter("dimension").isEmpty() || blob.getName().contains("_" + cli.getParameter("dimension"))) {

                        LocalDate updatedDate = LocalDate.fromDateFields(new Date(blob.getUpdateTime()));

                        //Identify if blob update is between start and end date came via parameter.
                        if (updatedDate.compareTo(LocalDate.parse(cli.getParameter("start_date"))) >= 0 && updatedDate.compareTo(LocalDate.parse(cli.getParameter("end_date"))) <= 0) {

                            InputStream inputStream = Channels.newInputStream(blob.reader());
                            OutputStream output = new FileOutputStream(System.getProperty("java.io.tmpdir") + "/" + cli.getParameter("package_name").replace(".", "_") + "/" + blob.getName());

                            //Identify the encoding type.
                            if (blob.getContentEncoding().equals("gzip")) {
                                java.util.zip.GZIPInputStream gis = new GZIPInputStream(inputStream);
                                //Copy content of inputstrem to output.
                                IOUtils.copy(gis, output, true);
                            } else {
                                //Copy content of inputstrem to output.
                                IOUtils.copy(inputStream, output, true);
                            }
                            output.close();
                        }
                    }
                }
            }

            //Writes all source files to a single target file.
            mitt.write(new File(tmpPath), "*.csv", ',', '"', '"', "UTF-16LE", null);
        } catch (DuplicateEntityException
                | IOException ex) {

            Logger.getLogger(GooglePlayStore.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(1);
        } finally {
            mitt.close();
        }

        Logger.getLogger(GooglePlayStore.class.getName()).info("GLOVE - Google Play Store Extractor finalized");
    }
}

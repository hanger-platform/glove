/*
 * Copyright (c) 2022 Dafiti Group
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
package br.com.dafiti.quicksight.ingestion;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import com.amazonaws.services.quicksight.AmazonQuickSight;
import com.amazonaws.services.quicksight.AmazonQuickSightClientBuilder;
import com.amazonaws.services.quicksight.model.CreateIngestionRequest;
import com.amazonaws.services.quicksight.model.DescribeDataSetRequest;
import com.amazonaws.services.quicksight.model.DescribeDataSetResult;
import com.amazonaws.services.quicksight.model.DescribeIngestionRequest;
import com.amazonaws.services.quicksight.model.DescribeIngestionResult;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Helio Leal
 */
public class QuicksightIngestion {

    private static final Logger LOG = Logger.getLogger(QuicksightIngestion.class.getName());

    public static void main(String[] args) {
        LOG.info("Quicksight Ingestion started");

        //Defines the mitt.
        Mitt mitt = new Mitt();

        try {

            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("a", "account", "The Amazon Web Services account ID", "", true, false)
                    .addParameter("d", "dataset", "The ID of the dataset used in the ingestion", "", true, false)
                    .addParameter("t", "type", "(Optional) The type of ingestion that you want to create, available: INCREMENTAL_REFRESH and FULL_REFRESH (default)", "FULL_REFRESH")
                    .addParameter("s", "sleep", "(Optional) Sleep time in seconds at one request and another; 10 is default", "10");

            //Reads the command line interface. 
            CommandLineInterface cli = mitt.getCommandLineInterface(args);

            AmazonQuickSight client = AmazonQuickSightClientBuilder
                    .standard()
                    .build();

            //Defines a unique id based on unix epoch.
            String ingestionId = String.valueOf(System.currentTimeMillis() / 1000);

            //Starts a new ingestion.
            client.createIngestion(
                    new CreateIngestionRequest()
                            .withDataSetId(cli.getParameter("dataset"))
                            .withAwsAccountId(cli.getParameter("account"))
                            .withIngestionType(cli.getParameter("type"))
                            .withIngestionId(ingestionId)
            );

            //Describes dataset properties.
            DescribeDataSetResult describeDataSetResult = client.describeDataSet(
                    new DescribeDataSetRequest()
                            .withAwsAccountId(cli.getParameter("account"))
                            .withDataSetId(cli.getParameter("dataset")));
            LOG.log(Level.INFO, "Refresh started for dataset: {0}", new Object[]{describeDataSetResult.getDataSet().getName()});

            //Control ingestion status.
            boolean running = true;

            do {
                //Describes a SPICE ingestion.
                DescribeIngestionResult describeIngestionResult
                        = client.describeIngestion(
                                new DescribeIngestionRequest()
                                        .withDataSetId(cli.getParameter("dataset"))
                                        .withAwsAccountId(cli.getParameter("account"))
                                        .withIngestionId(ingestionId));
                String status = describeIngestionResult.getIngestion().getIngestionStatus().toUpperCase();

                switch (status) {
                    case "INITIALIZED":
                    case "QUEUED":
                    case "RUNNING":
                        try {
                            LOG.log(Level.INFO, "Ingestion in progress, status: {1}", new Object[]{cli.getParameter("dataset"), status});
                            Thread.sleep(Long.valueOf(cli.getParameterAsInteger("sleep") * 1000));
                        } catch (InterruptedException ex) {
                            LOG.log(Level.SEVERE, "Error: ", ex);
                        }

                        break;
                    case "COMPLETED":
                        running = false;
                        LOG.log(
                                Level.INFO,
                                "Refresh completed. RowsIngested {0}, RowsDropped {1}, IngestionTimeInSeconds {2}, IngestionSizeInBytes {3}",
                                new Object[]{
                                    describeIngestionResult.getIngestion().getRowInfo().getRowsIngested(),
                                    describeIngestionResult.getIngestion().getRowInfo().getRowsDropped(),
                                    describeIngestionResult.getIngestion().getIngestionTimeInSeconds(),
                                    describeIngestionResult.getIngestion().getIngestionSizeInBytes()
                                });
                        break;
                    default:
                        LOG.log(Level.SEVERE, "Quicksight Ingestion fail, status: ", status);
                        System.exit(1);
                }

            } while (running);

        } catch (DuplicateEntityException ex) {
            LOG.log(Level.SEVERE, "Quicksight Ingestion fail: ", ex);
            System.exit(1);
        } finally {
            mitt.close();
        }

        LOG.info("Quicksight Ingestion finalized");
    }
}

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
package br.com.dafiti.quicksight.resources;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.model.Configuration;
import br.com.dafiti.quicksight.config.QuicksightClient;
import com.amazonaws.services.quicksight.model.DescribeDataSetPermissionsRequest;
import com.amazonaws.services.quicksight.model.DescribeDataSetPermissionsResult;
import com.amazonaws.services.quicksight.model.ListDataSetsRequest;
import com.amazonaws.services.quicksight.model.ListDataSetsResult;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Helio Leal
 */
public class DataSetPermissions extends QuicksightClient implements Describable {

    public DataSetPermissions(String region, String awsAccountId, String namespace) {
        super(region, awsAccountId, namespace);
    }

    @Override
    public void extract(Mitt mitt) {
        String nextToken = null;

        do {
            ListDataSetsResult listDatasetsResult = client
                    .listDataSets(new ListDataSetsRequest()
                            .withAwsAccountId(this.awsAccountId)
                            .withNextToken(nextToken));

            nextToken = listDatasetsResult.getNextToken();
            listDatasetsResult.getDataSetSummaries().forEach(dataset -> {

                DescribeDataSetPermissionsResult describeDataSetPermissionsResult
                        = client.describeDataSetPermissions(
                                new DescribeDataSetPermissionsRequest()
                                        .withAwsAccountId(this.awsAccountId)
                                        .withDataSetId(dataset.getDataSetId()));

                describeDataSetPermissionsResult.getPermissions().forEach(resourcePermission -> {
                    resourcePermission.getActions().forEach(action -> {
                        List<Object> record = new ArrayList<>();

                        record.add(dataset.getArn());
                        record.add(dataset.getDataSetId());
                        record.add(resourcePermission.getPrincipal());
                        record.add(action);

                        mitt.write(record);
                    });
                });

            });
        } while (nextToken != null);
    }

    @Override
    public void setFields(Configuration configuration) throws DuplicateEntityException {
        configuration.addField("arn");
        configuration.addField("dataset_id");
        configuration.addField("principal");
        configuration.addField("action");
    }
}

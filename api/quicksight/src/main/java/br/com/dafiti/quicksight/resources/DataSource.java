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
import com.amazonaws.services.quicksight.model.ListDataSourcesRequest;
import com.amazonaws.services.quicksight.model.ListDataSourcesResult;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Helio Leal
 */
public class DataSource extends QuicksightClient implements Describable {

    public DataSource(String region, String awsAccountId, String namespace) {
        super(region, awsAccountId, namespace);
    }

    @Override
    public void extract(Mitt mitt) {
       String nextToken = null;

        do {
            ListDataSourcesResult listDatasourcesResult = client
                    .listDataSources(new ListDataSourcesRequest()
                            .withAwsAccountId(this.awsAccountId)                            
                            .withNextToken(nextToken));

            nextToken = listDatasourcesResult.getNextToken();
            listDatasourcesResult.getDataSources().forEach(datasource -> {

                List<Object> record = new ArrayList<>();

                record.add(datasource.getArn());
                record.add(datasource.getCreatedTime());
                record.add(datasource.getDataSourceId());
                record.add(datasource.getDataSourceParameters());
                record.add(datasource.getErrorInfo());
                record.add(datasource.getLastUpdatedTime());
                record.add(datasource.getName());
                record.add(datasource.getSslProperties());
                record.add(datasource.getStatus());
                record.add(datasource.getType());
                record.add(datasource.getVpcConnectionProperties());

                mitt.write(record);
            });
        } while (nextToken != null);
    }

    @Override
    public void setFields(Configuration configuration) throws DuplicateEntityException {
        configuration.addField("arn");
        configuration.addField("created_time");
        configuration.addField("datasource_id");
        configuration.addField("datasource_parameters");
        configuration.addField("error_info");
        configuration.addField("last_updated_time");
        configuration.addField("name");
        configuration.addField("ssl_properties");
        configuration.addField("status");
        configuration.addField("type");
        configuration.addField("vpc_connection_properties");
    }
}

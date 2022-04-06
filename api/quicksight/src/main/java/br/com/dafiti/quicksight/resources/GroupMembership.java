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
import com.amazonaws.services.quicksight.model.ListGroupMembershipsRequest;
import com.amazonaws.services.quicksight.model.ListGroupMembershipsResult;
import com.amazonaws.services.quicksight.model.ListGroupsRequest;
import com.amazonaws.services.quicksight.model.ListGroupsResult;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Helio Leal
 */
public class GroupMembership extends QuicksightClient implements Describable {

    public GroupMembership(String region, String awsAccountId, String namespace) {
        super(region, awsAccountId, namespace);
    }

    @Override
    public void extract(Mitt mitt) {
        String nextToken = null;

        do {
            ListGroupsResult listGroupsResult = client.listGroups(new ListGroupsRequest()
                    .withAwsAccountId(this.awsAccountId)
                    .withNamespace(this.namespace)
                    .withNextToken(nextToken));

            nextToken = listGroupsResult.getNextToken();
            listGroupsResult.getGroupList().forEach(group -> {

                ListGroupMembershipsResult listGroupMembershipsResult = client.listGroupMemberships(new ListGroupMembershipsRequest()
                        .withGroupName(group.getGroupName())
                        .withNamespace(this.namespace)
                        .withAwsAccountId(this.awsAccountId));

                listGroupMembershipsResult.getGroupMemberList().forEach(groupMember -> {
                    List<Object> record = new ArrayList<>();

                    record.add(group.getGroupName());
                    record.add(groupMember.getArn());
                    record.add(groupMember.getMemberName());

                    mitt.write(record);
                });
            });
        } while (nextToken != null);
    }

    @Override
    public void setFields(Configuration configuration) throws DuplicateEntityException {
        configuration.addField("group_name");
        configuration.addField("arn");
        configuration.addField("member_name");
    }
}

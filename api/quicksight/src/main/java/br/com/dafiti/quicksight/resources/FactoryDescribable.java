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

/**
 *
 * @author Helio Leal
 */
public class FactoryDescribable {

    public static Describable getDescribable(
            String resource,
            String region,
            String awsAccountId,
            String namespace) {

        Describable describable = null;

        switch (resource) {
            case "dashboard":
                describable = new Dashboard(region, awsAccountId, namespace);
                break;
            case "dashboard_permissions":
                describable = new DashboardPermissions(region, awsAccountId, namespace);
                break;
            case "dataset":
                describable = new DataSet(region, awsAccountId, namespace);
                break;
            case "dataset_permissions":
                describable = new DataSetPermissions(region, awsAccountId, namespace);
                break;
            case "datasource":
                describable = new DataSource(region, awsAccountId, namespace);
                break;
            case "group":
                describable = new Group(region, awsAccountId, namespace);
                break;
            case "group_membership":
                describable = new GroupMembership(region, awsAccountId, namespace);
                break;
            case "user":
                describable = new User(region, awsAccountId, namespace);
                break;
            default:
                throw new UnsupportedOperationException("Type " + resource + " not supported yet.");
        }

        return describable;
    }
}

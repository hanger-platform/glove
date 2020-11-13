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
package br.com.dafiti.sisense.metadata;

import br.com.dafiti.status.CubeStatus;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Valdiney V GOMES
 */
public class ElastiCubeStatus {

    private final String token;
    private final String server;
    private final String cube;

    /**
     *
     * @param token
     * @param server
     * @param cube
     */
    public ElastiCubeStatus(
            String token,
            String server,
            String cube) {

        this.token = token;
        this.server = server;
        this.cube = cube;
    }

    /**
     *
     * @return
     */
    public int getStatus() {
        int status = 0;
        HttpClient httpClient = new HttpClient();
        GetMethod method = new GetMethod("https://" + this.server + "/api/elasticubes/servers/localhost/status");
        method.setRequestHeader("Authorization", "Bearer " + this.token);

        try {
            if (httpClient.executeMethod(method) == 200) {
                JSONParser parser = new JSONParser();
                JSONArray elastCubes = (JSONArray) parser.parse(method.getResponseBodyAsString());

                for (Object cubeInformation : elastCubes) {
                    JSONObject cubeStatus = (JSONObject) cubeInformation;

                    if (((String) cubeStatus.get("title")).equalsIgnoreCase(this.cube)) {
                        status = Integer.valueOf(cubeStatus.get("status").toString());
                        break;
                    }
                }
            }
        } catch (IOException
                | ParseException ex) {

            Logger.getLogger(ElastiCubeStatus.class.getName()).log(Level.SEVERE, "Fail getting cube status", ex);
        }

        return status;
    }

    /**
     *
     * @param statusNumber
     * @return
     */
    public String getStatusMessage(int statusNumber) {
        CubeStatus status = null;
        String statusMessage = "";

        try {
            status = CubeStatus.valueOf("_" + statusNumber);
        } catch (Exception ex) {
            Logger.getLogger(ElastiCubeStatus.class.getName()).log(Level.SEVERE, "Fail getting status description of ", statusNumber);
        }

        if (status != null) {
            statusMessage = status.status;
        } else {
            statusMessage = "Undefined";
        }

        return statusMessage;
    }
}

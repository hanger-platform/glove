package br.com.dafiti.sisense.action;

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
import br.com.dafiti.sisense.metadata.ElastiCubeStatus;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;

/**
 *
 * @author Valdiney V GOMES
 */
public class ElastiCubeStartBuild {

    private final String token;
    private final String server;
    private final String cube;

    /**
     *
     * @param token
     * @param server
     * @param cube
     */
    public ElastiCubeStartBuild(
            String token,
            String server,
            String cube) {

        this.token = token;
        this.server = server;
        this.cube = cube;
    }

    /**
     *
     */
    public void startBuild() {
        int status;
        boolean monitor = false;
        ElastiCubeStatus elastiCubesStatus = new ElastiCubeStatus(this.token, this.server, this.cube);

        //Identifies cube status.
        status = elastiCubesStatus.getStatus();

        //Identifies if cube is running (2) or falted (4).
        if (status != 2 && status != 4) {
            monitor = true;
        } else {
            try {
                HttpClient httpClient = new HttpClient();
                PostMethod method = new PostMethod("https://" + this.server + "/api/elasticubes/localhost/" + this.cube + "/startBuild?type=Full");
                method.setRequestHeader("Authorization", "Bearer " + this.token);
                int httpStatus = httpClient.executeMethod(method);

                //Identifies if cube was built sucessfully.
                if (httpStatus == 200) {
                    monitor = true;
                } else {
                    Logger.getLogger(ElastiCubeStartBuild.class.getName()).log(Level.INFO, "{0}: {1}", new Object[]{this.cube, httpStatus});
                }
            } catch (IOException ex) {
                Logger.getLogger(ElastiCubeStartBuild.class.getName()).log(Level.INFO, "Fail building cube " + this.cube, ex);
            }
        }

        //Identifies if should wait for build finish.
        if (monitor) {
            while (true) {
                try {
                    TimeUnit.SECONDS.sleep(30);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ElastiCubeStartBuild.class.getName()).log(Level.SEVERE, "Fail waiting for cube status", ex);
                }

                status = elastiCubesStatus.getStatus();
                Logger.getLogger(ElastiCubeStartBuild.class.getName()).log(Level.INFO, "{0}: {1}", new Object[]{this.cube, elastiCubesStatus.getStatusMessage(status)});

                //Identifies cube is building (514).
                if (status != 514) {
                    //Identifies if any status different than "running (2)" is got.
                    if (status != 2) {
                        System.exit(1);
                    }else{
                        Logger.getLogger(ElastiCubeStartBuild.class.getName()).log(Level.INFO, "{0} built successfully", this.cube);
                    }

                    break;
                }
            }
        }
    }
}

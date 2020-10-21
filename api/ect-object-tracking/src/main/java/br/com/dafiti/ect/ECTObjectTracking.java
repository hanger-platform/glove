/*
 * Copyright (c) 2018 Dafiti Group
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
package br.com.dafiti.ect;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Valdiney V GOMES
 */
public class ECTObjectTracking {

    /**
     *
     * @param args (See help)
     */
    public static void main(String[] args) {
        Logger.getLogger(ECTObjectTracking.class.getName()).info("GLOVE - ECT Object Tracking Extractor");

        //Defines a MITT instance. 
        Mitt mitt = new Mitt();

        try {
            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("c", "credentials", "Credentials file", "", true, false)
                    .addParameter("ob", "object", "Object codes divided by + or in a file", "", true, false)
                    .addParameter("o", "output", "Output path", "", true, false)
                    .addParameter("t", "type", "(Optional) L: list of objects. O server will make the query individual of each informed identifier or F: range of objects; L as default", "L")
                    .addParameter("r", "result", "(Optional) L: All will be returned the events of the object or U: will be returned only object's last event; L as default", "L")
                    .addParameter("l", "language", "(Optional) 101: Will be returned all events in the Portuguese language 102: Will be returned all events in the English language; 101 as default", "101")
                    .addParameter("p", "partition", "(Optional)  Partition field; event_date as default", "::dateformat(event_date,dd/MM/yyyy HH:mm,yyyyMM)")
                    .addParameter("k", "key", "(Optional) Unique key; number+event_type+event_code+event_status+event_date as default", "::concat([[number,event_type,event_code,event_status,::dateformat(event_date,dd/MM/yyyy HH:mm,yyyyMMddHHmm)]],|)")
                    .addParameter("t", "thread", "(Optional) Threads; 5 as default", "5")
                    .addParameter("ch", "chunk", "(Optional) Objects to be retrieved in each thread; 1000 as default", "1000");

            //Reads the command line interface. 
            CommandLineInterface cli = mitt.getCommandLineInterface(args);

            //Reads the credentials file. 
            JSONParser parser = new JSONParser();
            JSONObject credentials = (JSONObject) parser.parse(new FileReader(cli.getParameter("credentials")));

            //Retrieves objects code from parameter or file.
            List<String> objects = new ArrayList();

            //Identify object for tracking information. 
            if (cli.getParameter("object").startsWith("file:")) {
                try (FileReader file = new FileReader(cli.getParameter("object").replace("file:", ""))) {
                    String record;
                    BufferedReader buffer = new BufferedReader(file);

                    while ((record = buffer.readLine()) != null) {
                        //Code accepts only numbers and letters.
                        String number = record.replaceAll("\\P{Alnum}", "");

                        if (!number.isEmpty()) {
                            objects.add(number);
                        }
                    }
                }
            } else {
                objects = cli.getParameterAsList("object", "\\+");
            }

            //Cleans the output directory.
            File directory = new File(cli.getParameter("output").endsWith("/") ? cli.getParameter("output") : (cli.getParameter("output") + "/"));

            if (directory.exists()) {
                FileFilter fileFilter = new WildcardFileFilter("tracking_*");
                File[] files = directory.listFiles(fileFilter);

                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
            }

            //Defines the executor size. 
            ExecutorService executor = Executors.newFixedThreadPool(cli.getParameterAsInteger("thread"));

            //Defines a lote holder.
            List<String> lote = new ArrayList();

            for (String object : objects) {
                lote.add(object);
                if (lote.size() == (cli.getParameterAsInteger("chunk") < 5000 ? cli.getParameterAsInteger("chunk") : 5000)) {
                    executor.execute(
                            new ECTObjectTrackingRunner(
                                    (String) credentials.get("user"),
                                    (String) credentials.get("password"),
                                    cli.getParameter("type"),
                                    cli.getParameter("result"),
                                    cli.getParameter("language"),
                                    cli.getParameter("output"),
                                    cli.getParameterAsList("key", "\\+"),
                                    cli.getParameterAsList("partition", "\\+"),
                                    lote
                            )
                    );

                    lote = new ArrayList();
                }
            }

            if (!lote.isEmpty()) {
                executor.execute(
                        new ECTObjectTrackingRunner(
                                (String) credentials.get("user"),
                                (String) credentials.get("password"),
                                cli.getParameter("type"),
                                cli.getParameter("result"),
                                cli.getParameter("language"),
                                cli.getParameter("output"),
                                cli.getParameterAsList("key", "\\+"),
                                cli.getParameterAsList("partition", "\\+"),
                                lote
                        )
                );
            }

            //Finaliza the executor. 
            executor.shutdown();
        } catch (IOException
                | ParseException
                | DuplicateEntityException ex) {

            Logger.getLogger(ECTObjectTracking.class.getName()).log(Level.SEVERE, "Object tracking: ", ex);
            System.exit(1);
        } finally {
            mitt.close();
        }

        Logger.getLogger(ECTObjectTracking.class.getName()).info("GLOVE - ECT Object Tracking Extractor");
    }
}

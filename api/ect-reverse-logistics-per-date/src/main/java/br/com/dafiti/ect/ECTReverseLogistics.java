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
package br.com.dafiti.ect;

import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.cli.CommandLineInterface;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
public class ECTReverseLogistics {

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        Logger.getLogger(ECTReverseLogistics.class.getName()).info("GLOVE - ECT Reverse Logistics Per Day Extractor started");

        //Defines a MITT instance. 
        Mitt mitt = new Mitt();

        try {
            //Defines parameters.
            mitt.getConfiguration()
                    .addParameter("c", "credentials", "Credentials file", "", true, false)
                    .addParameter("s", "start_date", "Start date", "", true, false)
                    .addParameter("e", "end_date", "End date", "", true, false)
                    .addParameter("o", "output", "Output path", "", true, false)
                    .addParameter("ot", "orderType", "(Optional) Order Type: L, A or C; A as default", "A")
                    .addParameter("p", "partition", "(Optional)  Partition field; history_update_date as default", "::dateformat(history_update_date,dd-MM-yyyy,yyyyMM)")
                    .addParameter("k", "key", "(Optional) Unique key; administrative_code,order_type,order_number,history_status as default", "::concat([[administrative_code,order_type,order_number,history_status]],|)")
                    .addParameter("t", "thread", "(Optional) Threads; 5 as default", "5");

            //Reads the command line interface. 
            CommandLineInterface cli = mitt.getCommandLineInterface(args);

            //Reads the credentials file. 
            JSONParser parser = new JSONParser();
            JSONObject credentials = (JSONObject) parser.parse(new FileReader(cli.getParameter("credentials")));

            //Retrieves authorization numbers from parameter or file.
            List<String> dates = cli.getParameterAsList("date", "\\+");

            //Defines the date range.
            LocalDate startDate = LocalDate.parse(cli.getParameter("start_date"), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDate endDate = LocalDate.parse(cli.getParameter("end_date"), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDate currentDate = startDate;

            while (currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) {
                dates.add(currentDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                currentDate = currentDate.plusDays(1);
            }

            //Cleans the output directory.
            File directory = new File(cli.getParameter("output").endsWith("/") ? cli.getParameter("output") : (cli.getParameter("output") + "/"));

            if (directory.exists()) {
                FileFilter fileFilter = new WildcardFileFilter("reverse_logistics_*");
                File[] files = directory.listFiles(fileFilter);

                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
            }

            //Defines the executor size. 
            ExecutorService executor = Executors.newFixedThreadPool(cli.getParameterAsInteger("thread"));

            dates.forEach((date) -> {
                executor.execute(
                        new ECTReverseLogisticsRunner(
                                (String) credentials.get("user"),
                                (String) credentials.get("password"),
                                (String) credentials.get("administrativeCode"),
                                cli.getParameter("orderType"),
                                cli.getParameter("output"),
                                cli.getParameterAsList("key", "\\+"),
                                cli.getParameterAsList("partition", "\\+"),
                                date
                        )
                );
            });

            executor.shutdown();

            if (executor.isTerminated()) {
                Logger.getLogger(ECTReverseLogistics.class.getName()).info("GLOVE - ECT Reverse Logistics Per Day Extractor finalized");
            }
        } catch (IOException
                | ParseException
                | DuplicateEntityException ex) {

            Logger.getLogger(ECTReverseLogistics.class.getName()).log(Level.SEVERE, "Reverse logistics: ", ex);
            System.exit(1);
        } finally {
            mitt.close();
        }
    }
}

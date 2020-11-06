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

import br.com.correios.webservice.rastro.Eventos;
import br.com.correios.webservice.rastro.Objeto;
import br.com.correios.webservice.rastro.Rastro;
import br.com.correios.webservice.rastro.Service;
import br.com.correios.webservice.rastro.Sroxml;
import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.DateFormat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Valdiney V GOMES
 */
public class ECTObjectTrackingRunner implements Runnable {

    private final String user;
    private final String password;
    private final String type;
    private final String result;
    private final String language;
    private final String output;
    private final List key;
    private final List partition;
    private final List<String> objects;

    private final int API_DEFAULT_RETRY = 3;

    public ECTObjectTrackingRunner(
            String user,
            String password,
            String type,
            String result,
            String language,
            String output,
            List key,
            List partition,
            List<String> objects) {

        this.user = user;
        this.password = password;
        this.type = type;
        this.result = result;
        this.language = language;
        this.output = output;
        this.key = key;
        this.partition = partition;
        this.objects = objects;
    }

    @Override
    public void run() {
        //Defines a MITT instance. 
        Mitt mitt = new Mitt();

        //Defines output file.
        mitt.setOutputFile((output.endsWith("/") ? output : (output + "/")) + "tracking_" + UUID.randomUUID() + ".csv");

        try {
            //Defines fields.
            if (!partition.isEmpty()) {
                mitt.getConfiguration()
                        .addCustomField("partition_field", new Concat(partition));
            }

            mitt.getConfiguration()
                    .addCustomField("custom_primary_key", new Concat(key))
                    .addCustomField("etl_load_date", new Now())
                    .addField("number")
                    .addField("event_type")
                    .addField("event_code")
                    .addField("event_status")
                    .addField("event_description")
                    .addField("event_date", new DateFormat("event_date", "dd/MM/yyyy HH:mm", "yyyy-MM-dd HH:mm:ss"))
                    .addField("initials")
                    .addField("name")
                    .addField("category")
                    .addField("event_city")
                    .addField("event_place")
                    .addField("event_state");

            this.loteProcessor(mitt);
        } catch (DuplicateEntityException ex) {
            Logger.getLogger(ECTObjectTrackingRunner.class.getName()).log(Level.SEVERE, "Object tracking runner: ", ex);
            System.exit(1);
        } finally {
            mitt.close();
        }
    }

    /**
     *
     * @param mitt
     */
    public void loteProcessor(Mitt mitt) {
        this.loteProcessor(mitt, 1);
    }

    /**
     *
     * @param mitt
     */
    private void loteProcessor(Mitt mitt, int retry) {
        Sroxml sroxml = new Sroxml();
        List record;

        try {
            //Defines the service port.
            Service service = new Rastro().getServicePort();

            //Search for objects. 
            sroxml = service.buscaEventosLista(
                    user,
                    password,
                    type,
                    result,
                    language,
                    objects
            );
        } catch (Exception ex) {
            try {
                if (retry < API_DEFAULT_RETRY) {
                    Logger.getLogger(ECTObjectTrackingRunner.class.getName()).log(Level.INFO, "Tracking retry {0} for objects: {1}, caused by {2}", new Object[]{retry, String.join(",", objects), ex});
                    TimeUnit.MINUTES.sleep(1);
                    retry = retry + 1;
                    this.loteProcessor(mitt, retry);
                } else {
                    Logger.getLogger(ECTObjectTrackingRunner.class.getName()).log(Level.SEVERE, "Tracking retrying limit exceeded!");
                    System.exit(1);
                }
            } catch (InterruptedException e) {
                Logger.getLogger(ECTObjectTrackingRunner.class.getName()).log(Level.SEVERE, "Tracking runner retry error: ", e);
                System.exit(1);
            }
        }

        //Retrives all objects.
        for (Objeto object : sroxml.getObjeto()) {
            if (object.getErro() == null) {
                for (Eventos event : object.getEvento()) {
                    record = new ArrayList();

                    record.add(object.getNumero());
                    record.add(event.getTipo());
                    record.add(event.getCodigo());
                    record.add(event.getStatus());
                    record.add(event.getDescricao());
                    record.add(event.getData() + " " + event.getHora());
                    record.add(object.getSigla());
                    record.add(object.getNome());
                    record.add(object.getCategoria());
                    record.add(event.getCidade());
                    record.add(event.getLocal());
                    record.add(event.getUf());

                    mitt.write(record);
                }
            } else {
                record = new ArrayList();

                record.add(object.getNumero());
                record.add("ERR");
                record.add("");
                record.add("");
                record.add(object.getErro());
                record.add("01/01/1900 00:00:00");

                mitt.write(record);
            }
        }
    }
}

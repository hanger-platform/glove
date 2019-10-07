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

import br.com.correios.logisticareversa.service.ColetasSolicitadas;
import br.com.correios.logisticareversa.service.HistoricoColeta;
import br.com.correios.logisticareversa.service.LogisticaReversaService;
import br.com.correios.logisticareversa.service.LogisticaReversaWS;
import br.com.correios.logisticareversa.service.ObjetoPostal;
import br.com.correios.logisticareversa.service.RetornoAcompanhamento;
import br.com.dafiti.mitt.Mitt;
import br.com.dafiti.mitt.exception.DuplicateEntityException;
import br.com.dafiti.mitt.transformation.embedded.Concat;
import br.com.dafiti.mitt.transformation.embedded.DateFormat;
import br.com.dafiti.mitt.transformation.embedded.Now;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.ws.BindingProvider;

/**
 *
 * @author Valdiney V GOMES
 */
public class ECTReverseLogisticsRunner implements Runnable {

    private final String user;
    private final String password;
    private final String administrativeCode;
    private final String orderType;
    private final String output;
    private final List key;
    private final List partition;
    private final String date;

    private final int API_DEFAULT_RETRY = 5;

    /**
     *
     * @param user
     * @param password
     * @param administrativeCode
     * @param orderType
     * @param key
     * @param partition
     * @param date
     * @param output
     */
    public ECTReverseLogisticsRunner(
            String user,
            String password,
            String administrativeCode,
            String orderType,
            String output,
            List key,
            List partition,
            String date) {

        this.user = user;
        this.password = password;
        this.administrativeCode = administrativeCode;
        this.orderType = orderType;
        this.output = output;
        this.key = key;
        this.partition = partition;
        this.date = date;
    }

    /**
     *
     */
    @Override
    public void run() {
        //Defines a MITT instance. 
        Mitt mitt = new Mitt();

        //Defines output file.
        mitt.setOutput((output.endsWith("/") ? output : (output + "/")) + "reverse_logistics_" + date.replaceAll("\\W", "_") + ".csv");

        try {
            //Defines fields.
            mitt.getConfiguration()
                    .addCustomField("partition_field", new Concat(partition))
                    .addCustomField("custom_primary_key", new Concat(key))
                    .addCustomField("etl_load_date", new Now())
                    .addField("administrative_code")
                    .addField("order_type")
                    .addField("order_number")
                    .addField("history_status")
                    .addField("history_status_description")
                    .addField("history_update_date", new DateFormat("history_update_date", "dd-MM-yyyy HH:mm:ss", "yyyy-MM-dd HH:mm:ss"))
                    .addField("history_note")
                    .addField("object_tag_number")
                    .addField("object_customer_control")
                    .addField("object_status")
                    .addField("object_status_description")
                    .addField("object_update_date", new DateFormat("object_update_date", "dd-MM-yyyy HH:mm:ss", "yyyy-MM-dd HH:mm:ss"))
                    .addField("object_cubic_weight")
                    .addField("object_real_weight")
                    .addField("object_postage_rate");

            this.dateProcessor(mitt, date);
        } catch (DuplicateEntityException ex) {
            Logger.getLogger(ECTReverseLogisticsRunner.class.getName()).log(Level.SEVERE, "Reverse logistics runner error: ", ex);
        } finally {
            mitt.close();
        }
    }

    /**
     *
     * @param mitt
     * @param date
     */
    public void dateProcessor(
            Mitt mitt,
            String date) {

        this.dateProcessor(mitt, date, 1);
    }

    /**
     *
     * @param mitt
     * @param date
     * @param retry
     */
    public void dateProcessor(
            Mitt mitt,
            String date,
            int retry) {

        try {
            //Defines the service.
            LogisticaReversaService service = new LogisticaReversaService();

            //Defines the service port.
            LogisticaReversaWS port = service.getLogisticaReversaWSPort();

            //Defines the basic authentication.
            BindingProvider prov = (BindingProvider) port;
            prov.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, user);
            prov.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, password);

            //Search for reverse logistics orders.
            RetornoAcompanhamento acompanhamento = port.acompanharPedidoPorData(
                    administrativeCode,
                    orderType,
                    date);

            //Retrives all reverse logistics orders.
            List<ColetasSolicitadas> coletasSolicitadas = acompanhamento.getColeta();

            if (!coletasSolicitadas.isEmpty()) {
                coletasSolicitadas.forEach((coleta) -> {
                    List<HistoricoColeta> historicoColetas = coleta.getHistorico();

                    historicoColetas.forEach((historico) -> {
                        List record = new ArrayList();

                        record.add(acompanhamento.getCodigoAdministrativo());
                        record.add(acompanhamento.getTipoSolicitacao());
                        record.add(coleta.getNumeroPedido());
                        record.add(historico.getStatus());
                        record.add(historico.getDescricaoStatus());
                        record.add(historico.getDataAtualizacao() + " " + historico.getHoraAtualizacao());
                        record.add(historico.getObservacao());

                        //Identifies if it have a postal object related and if the object is with status 6, collected.
                        if (historico.getStatus() == 6) {
                            if (!coleta.getObjeto().isEmpty()) {
                                ObjetoPostal objeto = null;

                                boolean coletado = false;

                                //Identies the last object state: 6 is collected, 7 is delivered and 55 is waiting. 
                                for (ObjetoPostal objetoPostal : coleta.getObjeto()) {
                                    if (objetoPostal.getUltimoStatus().equals("7")) {
                                        objeto = objetoPostal;
                                        break;
                                    } else if (objetoPostal.getUltimoStatus().equals("6")) {
                                        coletado = true;
                                        objeto = objetoPostal;
                                    } else {
                                        if (!coletado) {
                                            objeto = objetoPostal;
                                        }
                                    }
                                }

                                record.add(objeto.getNumeroEtiqueta());
                                record.add(objeto.getControleObjetoCliente());
                                record.add(objeto.getUltimoStatus());
                                record.add(objeto.getDescricaoStatus());
                                record.add(objeto.getDataUltimaAtualizacao() + " " + objeto.getHoraUltimaAtualizacao());
                                record.add(objeto.getPesoCubico());
                                record.add(objeto.getPesoReal());
                                record.add(objeto.getValorPostagem());
                            }
                        }

                        mitt.write(record);
                    });
                });
            }
        } catch (Exception ex) {
            try {
                if (retry < API_DEFAULT_RETRY) {
                    Logger.getLogger(ECTReverseLogisticsRunner.class.getName()).log(Level.INFO, "Reverse logistics retry {0} for date: {1}, caused by {2}", new Object[]{retry, String.join(",", date), ex});
                    TimeUnit.MINUTES.sleep(1);
                    retry = retry + 1;
                    this.dateProcessor(mitt, date, retry);
                } else {
                    Logger.getLogger(ECTReverseLogisticsRunner.class.getName()).log(Level.SEVERE, "Reverse logistics retrying limit exceeded!");
                    System.exit(1);
                }
            } catch (InterruptedException e) {
                Logger.getLogger(ECTReverseLogisticsRunner.class.getName()).log(Level.SEVERE, "Reverse logistics runner retry error: ", e);
                System.exit(1);
            }
        }
    }
}

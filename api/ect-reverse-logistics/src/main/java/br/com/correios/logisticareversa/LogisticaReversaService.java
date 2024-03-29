
package br.com.correios.logisticareversa;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.3.2
 * Generated source version: 2.2
 * 
 */
@WebServiceClient(name = "logisticaReversaService", targetNamespace = "http://service.logisticareversa.correios.com.br/", wsdlLocation = "https://cws.correios.com.br/logisticaReversaWS/logisticaReversaService/logisticaReversaWS?wsdl")
public class LogisticaReversaService
    extends Service
{

    private final static URL LOGISTICAREVERSASERVICE_WSDL_LOCATION;
    private final static WebServiceException LOGISTICAREVERSASERVICE_EXCEPTION;
    private final static QName LOGISTICAREVERSASERVICE_QNAME = new QName("http://service.logisticareversa.correios.com.br/", "logisticaReversaService");

    static {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL("https://cws.correios.com.br/logisticaReversaWS/logisticaReversaService/logisticaReversaWS?wsdl");
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        LOGISTICAREVERSASERVICE_WSDL_LOCATION = url;
        LOGISTICAREVERSASERVICE_EXCEPTION = e;
    }

    public LogisticaReversaService() {
        super(__getWsdlLocation(), LOGISTICAREVERSASERVICE_QNAME);
    }

    public LogisticaReversaService(WebServiceFeature... features) {
        super(__getWsdlLocation(), LOGISTICAREVERSASERVICE_QNAME, features);
    }

    public LogisticaReversaService(URL wsdlLocation) {
        super(wsdlLocation, LOGISTICAREVERSASERVICE_QNAME);
    }

    public LogisticaReversaService(URL wsdlLocation, WebServiceFeature... features) {
        super(wsdlLocation, LOGISTICAREVERSASERVICE_QNAME, features);
    }

    public LogisticaReversaService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public LogisticaReversaService(URL wsdlLocation, QName serviceName, WebServiceFeature... features) {
        super(wsdlLocation, serviceName, features);
    }

    /**
     * 
     * @return
     *     returns LogisticaReversaWS
     */
    @WebEndpoint(name = "logisticaReversaWSPort")
    public LogisticaReversaWS getLogisticaReversaWSPort() {
        return super.getPort(new QName("http://service.logisticareversa.correios.com.br/", "logisticaReversaWSPort"), LogisticaReversaWS.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns LogisticaReversaWS
     */
    @WebEndpoint(name = "logisticaReversaWSPort")
    public LogisticaReversaWS getLogisticaReversaWSPort(WebServiceFeature... features) {
        return super.getPort(new QName("http://service.logisticareversa.correios.com.br/", "logisticaReversaWSPort"), LogisticaReversaWS.class, features);
    }

    private static URL __getWsdlLocation() {
        if (LOGISTICAREVERSASERVICE_EXCEPTION!= null) {
            throw LOGISTICAREVERSASERVICE_EXCEPTION;
        }
        return LOGISTICAREVERSASERVICE_WSDL_LOCATION;
    }

}

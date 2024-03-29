
package br.com.correios.logisticareversa;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java de acompanharPedidoPorDataResponse complex type.
 * 
 * <p>O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * 
 * <pre>
 * &lt;complexType name="acompanharPedidoPorDataResponse"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="acompanharPedidoPorData" type="{http://service.logisticareversa.correios.com.br/}retornoAcompanhamento" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "acompanharPedidoPorDataResponse", propOrder = {
    "acompanharPedidoPorData"
})
public class AcompanharPedidoPorDataResponse {

    protected RetornoAcompanhamento acompanharPedidoPorData;

    /**
     * Obtém o valor da propriedade acompanharPedidoPorData.
     * 
     * @return
     *     possible object is
     *     {@link RetornoAcompanhamento }
     *     
     */
    public RetornoAcompanhamento getAcompanharPedidoPorData() {
        return acompanharPedidoPorData;
    }

    /**
     * Define o valor da propriedade acompanharPedidoPorData.
     * 
     * @param value
     *     allowed object is
     *     {@link RetornoAcompanhamento }
     *     
     */
    public void setAcompanharPedidoPorData(RetornoAcompanhamento value) {
        this.acompanharPedidoPorData = value;
    }

}

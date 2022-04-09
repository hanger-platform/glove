
package br.com.correios.logisticareversa;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java de cancelarPedidoResponse complex type.
 * 
 * <p>O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * 
 * <pre>
 * &lt;complexType name="cancelarPedidoResponse"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="cancelarPedido" type="{http://service.logisticareversa.correios.com.br/}retornoCancelamento" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "cancelarPedidoResponse", propOrder = {
    "cancelarPedido"
})
public class CancelarPedidoResponse {

    protected RetornoCancelamento cancelarPedido;

    /**
     * Obtém o valor da propriedade cancelarPedido.
     * 
     * @return
     *     possible object is
     *     {@link RetornoCancelamento }
     *     
     */
    public RetornoCancelamento getCancelarPedido() {
        return cancelarPedido;
    }

    /**
     * Define o valor da propriedade cancelarPedido.
     * 
     * @param value
     *     allowed object is
     *     {@link RetornoCancelamento }
     *     
     */
    public void setCancelarPedido(RetornoCancelamento value) {
        this.cancelarPedido = value;
    }

}

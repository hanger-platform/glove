
package br.com.correios.logisticareversa;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java de dnecTO complex type.
 * 
 * <p>O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * 
 * <pre>
 * &lt;complexType name="dnecTO"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="dnec_mensagem" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="dnec_resultset" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="dnec_retorno" type="{http://www.w3.org/2001/XMLSchema}long"/&gt;
 *         &lt;element name="dnec_total" type="{http://www.w3.org/2001/XMLSchema}double"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "dnecTO", propOrder = {
    "dnecMensagem",
    "dnecResultset",
    "dnecRetorno",
    "dnecTotal"
})
public class DnecTO {

    @XmlElement(name = "dnec_mensagem")
    protected String dnecMensagem;
    @XmlElement(name = "dnec_resultset")
    protected String dnecResultset;
    @XmlElement(name = "dnec_retorno")
    protected long dnecRetorno;
    @XmlElement(name = "dnec_total")
    protected double dnecTotal;

    /**
     * Obtém o valor da propriedade dnecMensagem.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDnecMensagem() {
        return dnecMensagem;
    }

    /**
     * Define o valor da propriedade dnecMensagem.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDnecMensagem(String value) {
        this.dnecMensagem = value;
    }

    /**
     * Obtém o valor da propriedade dnecResultset.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDnecResultset() {
        return dnecResultset;
    }

    /**
     * Define o valor da propriedade dnecResultset.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDnecResultset(String value) {
        this.dnecResultset = value;
    }

    /**
     * Obtém o valor da propriedade dnecRetorno.
     * 
     */
    public long getDnecRetorno() {
        return dnecRetorno;
    }

    /**
     * Define o valor da propriedade dnecRetorno.
     * 
     */
    public void setDnecRetorno(long value) {
        this.dnecRetorno = value;
    }

    /**
     * Obtém o valor da propriedade dnecTotal.
     * 
     */
    public double getDnecTotal() {
        return dnecTotal;
    }

    /**
     * Define o valor da propriedade dnecTotal.
     * 
     */
    public void setDnecTotal(double value) {
        this.dnecTotal = value;
    }

}

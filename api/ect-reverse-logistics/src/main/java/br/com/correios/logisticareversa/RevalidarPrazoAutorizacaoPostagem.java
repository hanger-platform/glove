
package br.com.correios.logisticareversa;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java de revalidarPrazoAutorizacaoPostagem complex type.
 * 
 * <p>O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * 
 * <pre>
 * &lt;complexType name="revalidarPrazoAutorizacaoPostagem"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="codAdministrativo" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="numeroPedido" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="qtdeDias" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "revalidarPrazoAutorizacaoPostagem", propOrder = {
    "codAdministrativo",
    "numeroPedido",
    "qtdeDias"
})
public class RevalidarPrazoAutorizacaoPostagem {

    protected String codAdministrativo;
    protected String numeroPedido;
    protected String qtdeDias;

    /**
     * Obtém o valor da propriedade codAdministrativo.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCodAdministrativo() {
        return codAdministrativo;
    }

    /**
     * Define o valor da propriedade codAdministrativo.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCodAdministrativo(String value) {
        this.codAdministrativo = value;
    }

    /**
     * Obtém o valor da propriedade numeroPedido.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNumeroPedido() {
        return numeroPedido;
    }

    /**
     * Define o valor da propriedade numeroPedido.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNumeroPedido(String value) {
        this.numeroPedido = value;
    }

    /**
     * Obtém o valor da propriedade qtdeDias.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getQtdeDias() {
        return qtdeDias;
    }

    /**
     * Define o valor da propriedade qtdeDias.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setQtdeDias(String value) {
        this.qtdeDias = value;
    }

}


package br.com.correios.logisticareversa;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java de solicitarPostagemReversa complex type.
 * 
 * <p>O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * 
 * <pre>
 * &lt;complexType name="solicitarPostagemReversa"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="codAdministrativo" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="codigo_servico" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="cartao" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="destinatario" type="{http://service.logisticareversa.correios.com.br/}destinatario" minOccurs="0"/&gt;
 *         &lt;element name="coletas_solicitadas" type="{http://service.logisticareversa.correios.com.br/}coletaReversa" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "solicitarPostagemReversa", propOrder = {
    "codAdministrativo",
    "codigoServico",
    "cartao",
    "destinatario",
    "coletasSolicitadas"
})
public class SolicitarPostagemReversa {

    protected String codAdministrativo;
    @XmlElement(name = "codigo_servico")
    protected String codigoServico;
    protected String cartao;
    protected Destinatario destinatario;
    @XmlElement(name = "coletas_solicitadas")
    protected List<ColetaReversa> coletasSolicitadas;

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
     * Obtém o valor da propriedade codigoServico.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCodigoServico() {
        return codigoServico;
    }

    /**
     * Define o valor da propriedade codigoServico.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCodigoServico(String value) {
        this.codigoServico = value;
    }

    /**
     * Obtém o valor da propriedade cartao.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCartao() {
        return cartao;
    }

    /**
     * Define o valor da propriedade cartao.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCartao(String value) {
        this.cartao = value;
    }

    /**
     * Obtém o valor da propriedade destinatario.
     * 
     * @return
     *     possible object is
     *     {@link Destinatario }
     *     
     */
    public Destinatario getDestinatario() {
        return destinatario;
    }

    /**
     * Define o valor da propriedade destinatario.
     * 
     * @param value
     *     allowed object is
     *     {@link Destinatario }
     *     
     */
    public void setDestinatario(Destinatario value) {
        this.destinatario = value;
    }

    /**
     * Gets the value of the coletasSolicitadas property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the coletasSolicitadas property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getColetasSolicitadas().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ColetaReversa }
     * 
     * 
     */
    public List<ColetaReversa> getColetasSolicitadas() {
        if (coletasSolicitadas == null) {
            coletasSolicitadas = new ArrayList<ColetaReversa>();
        }
        return this.coletasSolicitadas;
    }

}

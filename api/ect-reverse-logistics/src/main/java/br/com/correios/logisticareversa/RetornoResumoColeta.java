
package br.com.correios.logisticareversa;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java de retornoResumoColeta complex type.
 * 
 * <p>O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * 
 * <pre>
 * &lt;complexType name="retornoResumoColeta"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="codigoObjeto" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="enderecoResumoRemetente" type="{http://service.logisticareversa.correios.com.br/}enderecoResumoTO" minOccurs="0"/&gt;
 *         &lt;element name="enderecoResumoDestinatario" type="{http://service.logisticareversa.correios.com.br/}enderecoResumoTO" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "retornoResumoColeta", propOrder = {
    "codigoObjeto",
    "enderecoResumoRemetente",
    "enderecoResumoDestinatario"
})
public class RetornoResumoColeta {

    protected String codigoObjeto;
    protected EnderecoResumoTO enderecoResumoRemetente;
    protected EnderecoResumoTO enderecoResumoDestinatario;

    /**
     * Obtém o valor da propriedade codigoObjeto.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCodigoObjeto() {
        return codigoObjeto;
    }

    /**
     * Define o valor da propriedade codigoObjeto.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCodigoObjeto(String value) {
        this.codigoObjeto = value;
    }

    /**
     * Obtém o valor da propriedade enderecoResumoRemetente.
     * 
     * @return
     *     possible object is
     *     {@link EnderecoResumoTO }
     *     
     */
    public EnderecoResumoTO getEnderecoResumoRemetente() {
        return enderecoResumoRemetente;
    }

    /**
     * Define o valor da propriedade enderecoResumoRemetente.
     * 
     * @param value
     *     allowed object is
     *     {@link EnderecoResumoTO }
     *     
     */
    public void setEnderecoResumoRemetente(EnderecoResumoTO value) {
        this.enderecoResumoRemetente = value;
    }

    /**
     * Obtém o valor da propriedade enderecoResumoDestinatario.
     * 
     * @return
     *     possible object is
     *     {@link EnderecoResumoTO }
     *     
     */
    public EnderecoResumoTO getEnderecoResumoDestinatario() {
        return enderecoResumoDestinatario;
    }

    /**
     * Define o valor da propriedade enderecoResumoDestinatario.
     * 
     * @param value
     *     allowed object is
     *     {@link EnderecoResumoTO }
     *     
     */
    public void setEnderecoResumoDestinatario(EnderecoResumoTO value) {
        this.enderecoResumoDestinatario = value;
    }

}

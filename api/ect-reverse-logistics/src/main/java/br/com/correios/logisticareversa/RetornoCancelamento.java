
package br.com.correios.logisticareversa;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java de retornoCancelamento complex type.
 * 
 * <p>O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * 
 * <pre>
 * &lt;complexType name="retornoCancelamento"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="codigo_administrativo" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="objeto_postal" type="{http://service.logisticareversa.correios.com.br/}objetoSimplificado" minOccurs="0"/&gt;
 *         &lt;element name="data" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="hora" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="cod_erro" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="msg_erro" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "retornoCancelamento", propOrder = {
    "codigoAdministrativo",
    "objetoPostal",
    "data",
    "hora",
    "codErro",
    "msgErro"
})
public class RetornoCancelamento {

    @XmlElement(name = "codigo_administrativo")
    protected String codigoAdministrativo;
    @XmlElement(name = "objeto_postal")
    protected ObjetoSimplificado objetoPostal;
    protected String data;
    protected String hora;
    @XmlElement(name = "cod_erro")
    protected String codErro;
    @XmlElement(name = "msg_erro")
    protected String msgErro;

    /**
     * Obtém o valor da propriedade codigoAdministrativo.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCodigoAdministrativo() {
        return codigoAdministrativo;
    }

    /**
     * Define o valor da propriedade codigoAdministrativo.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCodigoAdministrativo(String value) {
        this.codigoAdministrativo = value;
    }

    /**
     * Obtém o valor da propriedade objetoPostal.
     * 
     * @return
     *     possible object is
     *     {@link ObjetoSimplificado }
     *     
     */
    public ObjetoSimplificado getObjetoPostal() {
        return objetoPostal;
    }

    /**
     * Define o valor da propriedade objetoPostal.
     * 
     * @param value
     *     allowed object is
     *     {@link ObjetoSimplificado }
     *     
     */
    public void setObjetoPostal(ObjetoSimplificado value) {
        this.objetoPostal = value;
    }

    /**
     * Obtém o valor da propriedade data.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getData() {
        return data;
    }

    /**
     * Define o valor da propriedade data.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setData(String value) {
        this.data = value;
    }

    /**
     * Obtém o valor da propriedade hora.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHora() {
        return hora;
    }

    /**
     * Define o valor da propriedade hora.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHora(String value) {
        this.hora = value;
    }

    /**
     * Obtém o valor da propriedade codErro.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCodErro() {
        return codErro;
    }

    /**
     * Define o valor da propriedade codErro.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCodErro(String value) {
        this.codErro = value;
    }

    /**
     * Obtém o valor da propriedade msgErro.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMsgErro() {
        return msgErro;
    }

    /**
     * Define o valor da propriedade msgErro.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMsgErro(String value) {
        this.msgErro = value;
    }

}

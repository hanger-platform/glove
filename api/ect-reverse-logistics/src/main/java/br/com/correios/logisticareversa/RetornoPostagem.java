
package br.com.correios.logisticareversa;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java de retornoPostagem complex type.
 * 
 * <p>O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * 
 * <pre>
 * &lt;complexType name="retornoPostagem"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="status_processamento" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="data_processamento" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="hora_processamento" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="cod_erro" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="msg_erro" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="resultado_solicitacao" type="{http://service.logisticareversa.correios.com.br/}resultadoSolicitacao" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "retornoPostagem", propOrder = {
    "statusProcessamento",
    "dataProcessamento",
    "horaProcessamento",
    "codErro",
    "msgErro",
    "resultadoSolicitacao"
})
public class RetornoPostagem {

    @XmlElement(name = "status_processamento")
    protected String statusProcessamento;
    @XmlElement(name = "data_processamento")
    protected String dataProcessamento;
    @XmlElement(name = "hora_processamento")
    protected String horaProcessamento;
    @XmlElement(name = "cod_erro")
    protected String codErro;
    @XmlElement(name = "msg_erro")
    protected String msgErro;
    @XmlElement(name = "resultado_solicitacao", nillable = true)
    protected List<ResultadoSolicitacao> resultadoSolicitacao;

    /**
     * Obtém o valor da propriedade statusProcessamento.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStatusProcessamento() {
        return statusProcessamento;
    }

    /**
     * Define o valor da propriedade statusProcessamento.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStatusProcessamento(String value) {
        this.statusProcessamento = value;
    }

    /**
     * Obtém o valor da propriedade dataProcessamento.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDataProcessamento() {
        return dataProcessamento;
    }

    /**
     * Define o valor da propriedade dataProcessamento.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDataProcessamento(String value) {
        this.dataProcessamento = value;
    }

    /**
     * Obtém o valor da propriedade horaProcessamento.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHoraProcessamento() {
        return horaProcessamento;
    }

    /**
     * Define o valor da propriedade horaProcessamento.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHoraProcessamento(String value) {
        this.horaProcessamento = value;
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

    /**
     * Gets the value of the resultadoSolicitacao property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the resultadoSolicitacao property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getResultadoSolicitacao().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ResultadoSolicitacao }
     * 
     * 
     */
    public List<ResultadoSolicitacao> getResultadoSolicitacao() {
        if (resultadoSolicitacao == null) {
            resultadoSolicitacao = new ArrayList<ResultadoSolicitacao>();
        }
        return this.resultadoSolicitacao;
    }

}

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.09.20 at 09:34:23 PM PDT 
//


package org.isotc211._2005.gco;

import org.isotc211._2005.gmi.*;
import org.isotc211._2005.gmx.AbstractCTCatalogueType;
import org.isotc211._2005.gmx.AbstractMXFileType;
import org.isotc211._2005.srv.*;
import org.isotc211.iso19139.d_2007_04_17.gmd.*;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for AbstractObject_Type complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AbstractObject_Type">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *       &lt;/sequence>
 *       &lt;attGroup ref="{http://www.isotc211.org/2005/gco}ObjectIdentification"/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AbstractObject_Type")
@XmlSeeAlso({
    MIGCPType.class,
    MIAcquisitionInformationType.class,
    LEAlgorithmType.class,
    MIRangeElementDescriptionType.class,
    MIRequirementType.class,
    LEProcessStepReportType.class,
    MIEventType.class,
    MIOperationType.class,
    MISensorTypeCodeType.class,
    LEProcessingType.class,
    AbstractMIGeolocationInformationType.class,
    MIInstrumentType.class,
    LIProcessStepType.class,
    LISourceType.class,
    MIPlatformPassType.class,
    MIPlatformType.class,
    MDRangeDimensionType.class,
    MIObjectiveType.class,
    MDMetadataType.class,
    MIRequestedDateType.class,
    MIPlanType.class,
    TypeNameType.class,
    MultiplicityRangeType.class,
    MultiplicityType.class,
    MemberNameType.class,
    MDDimensionType.class,
    LILineageType.class,
    DQDataQualityType.class,
    CIContactType.class,
    MDApplicationSchemaInformationType.class,
    MDPortrayalCatalogueReferenceType.class,
    MDBrowseGraphicType.class,
    MDFormatType.class,
    CIDateType.class,
    DSAssociationType.class,
    AbstractRSReferenceSystemType.class,
    AbstractMDContentInformationType.class,
    MDUsageType.class,
    MDMaintenanceInformationType.class,
    EXTemporalExtentType.class,
    MDReferenceSystemType.class,
    MDIdentifierType.class,
    MDDistributionType.class,
    AbstractMDSpatialRepresentationType.class,
    CITelephoneType.class,
    CISeriesType.class,
    MDDistributorType.class,
    CIResponsiblePartyType.class,
    DQScopeType.class,
    AbstractEXGeographicExtentType.class,
    EXVerticalExtentType.class,
    EXExtentType.class,
    CIOnlineResourceType.class,
    AbstractDQResultType.class,
    MDDigitalTransferOptionsType.class,
    PTLocaleType.class,
    PTFreeTextType.class,
    MDGeometricObjectsType.class,
    MDMediumType.class,
    MDKeywordsType.class,
    CICitationType.class,
    MDExtendedElementInformationType.class,
    MDMetadataExtensionInformationType.class,
    MDConstraintsType.class,
    CIAddressType.class,
    MDAggregateInformationType.class,
    MDStandardOrderProcessType.class,
    AbstractDQElementType.class,
    MDRepresentativeFractionType.class,
    AbstractMXFileType.class,
    AbstractDSAggregateType.class,
    DSDataSetType.class,
    AbstractCTCatalogueType.class,
    SVServiceType.class,
    AbstractMDIdentificationType.class,
    SVInterfaceType.class,
    SVOperationChainMetadataType.class,
    SVPortSpecificationType.class,
    SVCoupledResourceType.class,
    SVOperationType.class,
    SVParameterType.class,
    SVOperationMetadataType.class,
    AbstractSVAbstractServiceSpecificationType.class,
    SVPortType.class,
    SVOperationChainType.class
})
public abstract class AbstractObjectType {

    @XmlAttribute(name = "id")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;
    @XmlAttribute(name = "uuid")
    protected String uuid;

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the uuid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * Sets the value of the uuid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUuid(String value) {
        this.uuid = value;
    }

}
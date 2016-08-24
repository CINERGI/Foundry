
package org.cinergi.sdsc.metadata;

import org.isotc211._2005.gco.CharacterStringPropertyType;
import org.isotc211._2005.gmi.MIMetadataType;
import org.isotc211.iso19139.d_2007_04_17.gmd.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class ISO19139Metadata {

    public static List<EXGeographicBoundingBoxType> getBoundingBox(File file) throws Exception {

        JAXBContext jc = JAXBContext.newInstance(MIMetadataType.class);
        Unmarshaller u = jc.createUnmarshaller();
        JAXBElement object = (JAXBElement) u.unmarshal(file);
        MIMetadataType metadata = (MIMetadataType) object.getValue();
        return getBoundingBox(metadata);
    }

    public static List<EXGeographicBoundingBoxType> getBoundingBox(MDMetadataType metadata) throws Exception {

        List<EXGeographicBoundingBoxType> boxes = new ArrayList<EXGeographicBoundingBoxType>();
        List<MDIdentificationPropertyType> mdipts = metadata.getIdentificationInfo();
        for (MDIdentificationPropertyType mdipt : mdipts) {
            JAXBElement amditObject = mdipt.getAbstractMDIdentification();
            AbstractMDIdentificationType amdit = (AbstractMDIdentificationType) amditObject.getValue();
            if (amdit instanceof MDDataIdentificationType) {
                MDDataIdentificationType mdit = (MDDataIdentificationType) amdit;
                List<EXExtentPropertyType> exepts = mdit.getExtent();
                for (EXExtentPropertyType exept : exepts) {
                    EXExtentType exet = exept.getEXExtent();
                    List<EXGeographicExtentPropertyType> exgepts = exet.getGeographicElement();
                    for (EXGeographicExtentPropertyType exgept : exgepts) {
                        JAXBElement aexgetObject = exgept.getAbstractEXGeographicExtent();
                        AbstractEXGeographicExtentType aexget = (AbstractEXGeographicExtentType) aexgetObject.getValue();
                        if (aexget instanceof EXGeographicBoundingBoxType) {
                            EXGeographicBoundingBoxType bound = (EXGeographicBoundingBoxType) aexget;
                            boxes.add(bound);
                        } else if (aexget instanceof EXGeographicDescriptionType) {
                            EXGeographicDescriptionType exgdt = (EXGeographicDescriptionType) aexget;
                            MDIdentifierPropertyType mdipt1 = exgdt.getGeographicIdentifier();
                            JAXBElement mditObject = mdipt1.getMDIdentifier();
                            MDIdentifierType mdit1 = (MDIdentifierType) mditObject.getValue();
                            if (mdit1 != null && mdit1.getAuthority() != null && mdit1.getAuthority().getCICitation() != null
                                    && mdit1.getAuthority().getCICitation().getTitle() != null &&
                                    mdit1.getAuthority().getCICitation().getTitle().getCharacterString() != null) {
                                String title = (String) mdit1.getAuthority().getCICitation().getTitle().getCharacterString().getValue();
                                String code = (String) mdit1.getCode().getCharacterString().getValue();
                                System.out.println("title=" + title + ", code=" + code);
                            }
                        } else {
                            System.out.println("----> 300 " + aexget.getClass());
                            //EXGeographicBoundingBoxType boundingBox = aexget.getEXGeographicBoundingBox()
                        }
                    }
                }
            }
        }

        return boxes;
    }

    public static String getTextDescription(File file) throws Exception {

        JAXBContext jc = JAXBContext.newInstance(MIMetadataType.class);
        Unmarshaller u = jc.createUnmarshaller();
        JAXBElement object = (JAXBElement) u.unmarshal(file);
        MIMetadataType metadata = (MIMetadataType) object.getValue();
        return getTextDescription(metadata);
    }


    public static String getTextDescription(MDMetadataType metadata) throws Exception {
        String result = "";
        List<MDIdentificationPropertyType> mdipts = metadata.getIdentificationInfo();
        for (MDIdentificationPropertyType mdipt : mdipts) {
            JAXBElement amditObject = mdipt.getAbstractMDIdentification();
            AbstractMDIdentificationType amdit = (AbstractMDIdentificationType) amditObject.getValue();

            result += amdit.getCitation().getCICitation().getTitle().getCharacterString().getValue().toString().trim() + " \n";
            if (amdit instanceof MDDataIdentificationType) {
                MDDataIdentificationType mdit = (MDDataIdentificationType) amdit;
                if (mdit.getAbstract() != null && mdit.getAbstract().getCharacterString() != null
                        && mdit.getAbstract().getCharacterString().getValue() != null) {
                    result += mdit.getAbstract().getCharacterString().getValue().toString().trim() + " \n";
                }
                if (mdit.getPurpose() != null && mdit.getPurpose().getCharacterString() != null
                        && mdit.getPurpose().getCharacterString().getValue() != null) {
                    result += mdit.getPurpose().getCharacterString().getValue().toString().trim() + " \n";
                }
            }
        }
        return result;
    }


    public static List<String> getPlaces(MDMetadataType metadata) throws Exception {

        List<String> places = new ArrayList<String>();
        List<MDIdentificationPropertyType> mdipts = metadata.getIdentificationInfo();
        for (MDIdentificationPropertyType mdipt : mdipts) {
            JAXBElement amditObject = mdipt.getAbstractMDIdentification();
            AbstractMDIdentificationType amdit = (AbstractMDIdentificationType) amditObject.getValue();
            List<MDKeywordsPropertyType> keywords = amdit.getDescriptiveKeywords();
            for (MDKeywordsPropertyType keyword : keywords) {
                MDKeywordsType key = keyword.getMDKeywords();
                if (key.getType() != null && key.getType().getMDKeywordTypeCode() != null &&
                        key.getType().getMDKeywordTypeCode().getValue() != null &&
                        key.getType().getMDKeywordTypeCode().getCodeListValue() != null) {
                    if (key.getType().getMDKeywordTypeCode().getValue().equals("place") &&
                            key.getType().getMDKeywordTypeCode().getCodeListValue().equals("place")) {
                        for (CharacterStringPropertyType cspt : key.getKeyword()) {
                            places.add(cspt.getCharacterString().getValue().toString());
                        }
                    }
                }
            }
        }
        return places;
    }

}
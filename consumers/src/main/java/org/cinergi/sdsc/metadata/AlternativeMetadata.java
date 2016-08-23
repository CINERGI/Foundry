
package org.cinergi.sdsc.metadata;

import org.cinergi.alternative.metadata.Metadata;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AlternativeMetadata {

    public static Metadata.Idinfo.Spdom.Bounding getBoundingBox(File file) throws Exception {

	JAXBContext jc = JAXBContext.newInstance(Metadata.class);
	Unmarshaller u = jc.createUnmarshaller();
	Metadata metadata = (Metadata)u.unmarshal(file);
	return metadata.getIdinfo().getSpdom().getBounding();
    }


    public static Metadata.Idinfo.Spdom.Bounding getBoundingBox(Metadata metadata) throws Exception {

	return metadata.getIdinfo().getSpdom().getBounding();
    }


    public static String getTextDescription(File file) throws Exception {

	JAXBContext jc = JAXBContext.newInstance(Metadata.class);
	Unmarshaller u = jc.createUnmarshaller();
	Metadata metadata = (Metadata)u.unmarshal(file);
        return getTextDescription(metadata);
    }


    public static String getTextDescription(Metadata metadata) throws Exception {

	String result = "";
	Metadata.Idinfo.Descript desc = metadata.getIdinfo().getDescript();

	try {
	    result += metadata.getIdinfo().getCitation().getCiteinfo().getTitle().trim()+" \n";
        } catch (Exception ex) {}


	if (desc.getAbstract() != null) {
	    result += desc.getAbstract().trim()+" \n";
	}

	if (desc.getPurpose() != null) {
	    result += " "+desc.getPurpose().trim()+" \n";
	}

	if (desc.getSupplinf() != null) {
	    result += " "+desc.getSupplinf().trim()+" \n";
	}

	return result;
    }


    public static List<String> getPlaces(Metadata metadata) throws Exception {

	List<String> places = new ArrayList<String>();
	Metadata.Idinfo.Keywords.Place place = metadata.getIdinfo().getKeywords().getPlace();
        places.add(place.getPlacekey());
	return places;

    }


}
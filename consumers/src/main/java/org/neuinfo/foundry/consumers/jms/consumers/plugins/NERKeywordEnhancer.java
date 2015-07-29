package org.neuinfo.foundry.consumers.jms.consumers.plugins;

import bnlpkit.nlp.common.index.DocumentInfo;
import bnlpkit.nlp.common.index.FileInfo;
import bnlpkit.nlp.common.index.NEInfo;
import bnlpkit.nlp.common.index.SentenceInfo;
import bnlpkit.util.cinergi.ner.FastNamedEntityRecognizer;
import bnlpkit.util.cinergi.ner.NERConfig;
import org.neuinfo.foundry.common.model.EntityInfo;
import org.neuinfo.foundry.common.model.Keyword;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bozyurt on 2/3/15.
 */
public class NERKeywordEnhancer {
    FastNamedEntityRecognizer ner;
    static NERKeywordEnhancer instance = null;
    static Map<String,String> neType2FacetLabelMapping = new HashMap<String, String>();

    static {
        neType2FacetLabelMapping.put("rock material","Rock");
        neType2FacetLabelMapping.put("mineral","Mineral");
        neType2FacetLabelMapping.put("geographic location","Geographic Location");
        neType2FacetLabelMapping.put("material","Material (Other)");
        neType2FacetLabelMapping.put("marine feature","Marine Feature");
        neType2FacetLabelMapping.put("hydrologic feature", "Hydrologic Feature");
        neType2FacetLabelMapping.put("property","Named Property");
        neType2FacetLabelMapping.put("project","Project");
        neType2FacetLabelMapping.put("process","Process (Other)");
        neType2FacetLabelMapping.put("feature","Feature (Other)");
        neType2FacetLabelMapping.put("sediment","Sediment");
        neType2FacetLabelMapping.put("activity","Activity (Other)");
        neType2FacetLabelMapping.put("chemical substance","Chemical Entity");
        neType2FacetLabelMapping.put("instrument","Instrument");
        neType2FacetLabelMapping.put("geologic feature","Geologic Feature");
        neType2FacetLabelMapping.put("physiographic feature","Physiographic Feature");
        neType2FacetLabelMapping.put("experiment","Experiment");
    }

    private NERKeywordEnhancer() throws Exception {
        NERConfig config = new NERConfig();
        config.setKeywordCRFModelFile("ner/cinergi/training.crf");
        ner = new FastNamedEntityRecognizer(config);
        ner.setVerbose(true);
        ner.initialize();
    }


    public synchronized static NERKeywordEnhancer getInstance() throws Exception {
        if (instance == null) {
            instance = new NERKeywordEnhancer();
        }
        return instance;
    }


    public synchronized Map<String, Keyword> findKeywords(List<String> text2AnnotateList) {
        Map<String, Keyword> keywordMap = new HashMap<String, Keyword>();

        for (String text : text2AnnotateList) {
            try {
                FileInfo fi = ner.handleWithSBDInMem(text);
                for (DocumentInfo di : fi.getDiList()) {
                    for (SentenceInfo si : di.getSiList()) {
                        if (si.hasNamedEntities()) {
                            String sentence = si.getText().getText();
                            for (NEInfo nei : si.getNeList()) {
                                String term = nei.extractNE(sentence);
                                Keyword keyword = keywordMap.get(term);
                                if (keyword == null) {
                                    keyword = new Keyword(term);
                                    keywordMap.put(term, keyword);
                                }
                                String neiType = nei.getType().replaceAll("_"," ");
                                String category = neType2FacetLabelMapping.get(neiType);
                                if (category == null) {
                                    category = neiType;
                                }
                                EntityInfo ei = new EntityInfo("", "",
                                        nei.getStartIdx(), nei.getEndIdx(), category);
                                keyword.addEntityInfo(ei);
                            }
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return keywordMap;
    }


}

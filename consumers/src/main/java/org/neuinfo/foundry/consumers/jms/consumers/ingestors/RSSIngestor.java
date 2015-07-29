package org.neuinfo.foundry.consumers.jms.consumers.ingestors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.neuinfo.foundry.common.util.JSONUtils;
import org.neuinfo.foundry.common.util.RSSFeedParser;
import org.neuinfo.foundry.consumers.plugin.Ingestor;
import org.neuinfo.foundry.consumers.plugin.Result;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by bozyurt on 10/29/14.
 */
public class RSSIngestor implements Ingestor {
    private Map<String, String> optionMap;
    private String ingestURL;
    private boolean allowDuplicates = false;
    private Iterator<RSSFeedParser.RSSItem> rssItemIterator;
    private int numRecs = -1;

    @Override
    public void initialize(Map<String, String> options) throws Exception {
        this.optionMap = options;
        this.allowDuplicates = options.containsKey("allowDuplicates") ?
                Boolean.parseBoolean(options.get("allowDuplicates")) : false;
        this.ingestURL = options.get("ingestURL");
    }

    @Override
    public void startup() throws Exception {
        RSSFeedParser parser = new RSSFeedParser(this.ingestURL);

        RSSFeedParser.RSSFeed feed = parser.getFeed();
        this.rssItemIterator = feed.getItems().iterator();
        this.numRecs = feed.getItems().size();
    }

    @Override
    public Result prepPayload() {
        try {
            RSSFeedParser.RSSItem rssItem = rssItemIterator.next();
            JSONObject json = new JSONObject();
            json.put("guid", rssItem.getGuid());
            json.put("title", rssItem.getTitle());
            json.put("link", rssItem.getLink());
            json.put("description", rssItem.getDescription());
            JSONUtils.add2JSON(json, "author", rssItem.getAuthor());
            JSONUtils.add2JSON(json, "comments", rssItem.getComments());
            SimpleDateFormat sdf = new SimpleDateFormat(" yyyy-MM-dd'T'HH:mm:ss.SSSZZ");

            JSONUtils.add2JSON(json, "pubDate", sdf.format(rssItem.getPubDate()));
            JSONArray jsArr = new JSONArray();
            if (rssItem.getCategories() != null && !rssItem.getCategories().isEmpty()) {
                for(RSSFeedParser.RSSCategory cat : rssItem.getCategories()) {
                    jsArr.put( cat.getCategory());
                }
            }
            json.put("categories", jsArr);

            return new Result(json, Result.Status.OK_WITH_CHANGE);
        } catch (Throwable t) {
            t.printStackTrace();
            return new Result(null, Result.Status.ERROR, t.getMessage());
        }
    }

    @Override
    public String getName() {
        return "RSSIngestor";
    }

    @Override
    public int getNumRecords() {
        return this.numRecs;
    }

    @Override
    public String getOption(String optionName) {
        return optionMap.get(optionName);
    }

    @Override
    public void shutdown() {
        // no op
    }

    @Override
    public boolean hasNext() {
        return rssItemIterator.hasNext();
    }
}

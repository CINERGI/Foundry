package org.neuinfo.foundry.consumers.common;

/**
 * Created by bozyurt on 5/1/14.
 * dwv 2019-03-26. Environment Variables can be used in configuration
 * ${envVar:defaultValue}
 * not all properties support this, at present. No int values (aka ports).
 * only params in workflows
 * */
public class Constants {
    public final static String MONGODB_ID_FIELD = "_id";
    public final static String TYPE = "mongodb";
}

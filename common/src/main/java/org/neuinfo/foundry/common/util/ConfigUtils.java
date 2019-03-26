package org.neuinfo.foundry.common.util;

import org.apache.commons.lang3.StringUtils;

public class ConfigUtils {

    public static String envVarParser (String value) {
        if (StringUtils.isBlank(value)) return null;

        String defValue  = null;
        String envVar = "";

        value = value.trim();
        if (value.startsWith("${") && value.endsWith("}")) {
            int colon = value.indexOf(":");
            if (colon > 1 ) {
                envVar = value.substring(2, colon).trim();
                defValue = value.substring(colon+1, value.length() -1 ).trim();;
            }
            else {
                envVar = value.substring(2,value.length() -1);
            }

            if (!StringUtils.isBlank(envVar)){
                value = System.getenv(envVar);
                if (StringUtils.isBlank(value)) {
                    return defValue;
                }
            } else {
                return defValue;
            }

        }
        return value;
    }

}

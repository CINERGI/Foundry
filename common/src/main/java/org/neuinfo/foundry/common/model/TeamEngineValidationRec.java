package org.neuinfo.foundry.common.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by bozyurt on 3/4/15.
 */
public class TeamEngineValidationRec {
    private boolean passed = false;
    private String rulesetName;
    private Date rulesetDate;
    private String resultXmlStr;

    public TeamEngineValidationRec(String rulesetName, Date rulesetDate, String resultXmlStr, boolean passed) {
        this.rulesetName = rulesetName;
        this.rulesetDate = rulesetDate;
        this.resultXmlStr = resultXmlStr;
        this.passed = passed;
    }

    public String getRulesetName() {
        return rulesetName;
    }

    public Date getRulesetDate() {
        return rulesetDate;
    }

    public String getResultXmlStr() {
        return resultXmlStr;
    }

    public boolean isPassed() {
        return passed;
    }

    public static Date toDate(String dateStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return sdf.parse(dateStr);
    }

    public static String fromDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return sdf.format(date);
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TeamEngineValidationRec{");
        sb.append("rulesetName='").append(rulesetName).append('\'');
        sb.append(", rulesetDate=").append(rulesetDate);
        sb.append(",passed=").append(passed);
        sb.append("\n");
        sb.append(", resultXmlStr='").append(resultXmlStr).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

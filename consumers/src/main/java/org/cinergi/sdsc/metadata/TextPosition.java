package org.cinergi.sdsc.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TextPosition {

    @JsonProperty("text")
    private String text ;
    @JsonProperty("begin")
    private int begin ;
    @JsonProperty("end")
    private int end ;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getBegin() {
        return begin;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }
}

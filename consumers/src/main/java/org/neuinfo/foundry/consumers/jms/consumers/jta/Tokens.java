package org.neuinfo.foundry.consumers.jms.consumers.jta;

public class Tokens {
    private String token;
    private String start;
    private String end;

    public Tokens(String str) {
        token = str;
        start = "";
        end = "";
    }

    public Tokens(Tokens other) {
        token = other.getToken();
        start = other.getStart();
        end = other.getEnd();
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }


}

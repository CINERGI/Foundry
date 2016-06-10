package org.neuinfo.foundry.consumers.jms.consumers.jta;

public class POS {
	String token;
	String pos;
	String start;
	String end;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("POS{");
        sb.append("token='").append(token).append('\'');
        sb.append(", pos='").append(pos).append('\'');
        sb.append(", start='").append(start).append('\'');
        sb.append(", end='").append(end).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

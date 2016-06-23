package org.neuinfo.foundry.common.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * based on https://github.com/srkirkland/Inflector/blob/master/Inflector/Inflector.cs
 * <p/>
 * Created by bozyurt on 8/4/15.
 */
public class Inflector {
    List<Rule> singulars = new ArrayList<Rule>();
    List<Rule> plurals = new ArrayList<Rule>();
    Set<String> uncountables = new HashSet<String>();
    Set<String> irregularSingularSet = new HashSet<String>();


    public Inflector() {
        uncountables.addAll(Arrays.asList("equipment", "information", "rice", "money",
                "species", "series", "fish", "sheep", "deer", "aircraft"));

        addSingular("s$", "");
        addSingular("(n)ews$", "$1ews");
        addSingular("([ti])a$", "$1um");
        addSingular("((a)naly|(b)a|(d)iagno|(p)arenthe|(p)rogno|(s)ynop|(t)he)ses$", "$1$2sis");
        addSingular("(^analy)ses$", "$1sis");
        addSingular("([^f])ves$", "$1fe");
        addSingular("(hive)s$", "$1");
        addSingular("(tive)s$", "$1");
        addSingular("([lr])ves$", "$1f");
        addSingular("([^aeiouy]|qu)ies$", "$1y");
        addSingular("(s)eries$", "$1eries");
        addSingular("(m)ovies$", "$1ovie");
        addSingular("(x|ch|ss|sh)es$", "$1");
        addSingular("([m|l])ice$", "$1ouse");
        addSingular("(bus)es$", "$1");
        addSingular("(o)es$", "$1");
        addSingular("(shoe)s$", "$1");
        addSingular("(cris|ax|test)es$", "$1is");
        addSingular("(octop|vir|alumn|fung)i$", "$1us");
        addSingular("(alias|status)es$", "$1");
        addSingular("^(ox)en", "$1");
        addSingular("(vert|ind)ices$", "$1ex");
        addSingular("(matr)ices$", "$1ix");
        addSingular("(quiz)zes$", "$1");

        addIrregular("person", "people");
        addIrregular("man", "men");
        addIrregular("child", "children");
        addIrregular("sex", "sexes");
        addIrregular("move", "moves");
        addIrregular("goose", "geese");
        addIrregular("alumna", "alumnae");
        addIrregular("process", "processes");
        Collections.reverse(singulars);
    }

    public String toSingular(String phrase) {

        if (phrase.length() < 4 || Utils.isAllCapital(phrase)) {
            return phrase;
        }
        String[] toks = phrase.split("\\s+");

        String lastTerm = applyRules(singulars, toks[toks.length - 1]);
        toks[toks.length - 1] = lastTerm;
        StringBuilder sb = new StringBuilder(phrase.length());
        for (String tok : toks) {
            sb.append(tok).append(' ');
        }
        return sb.toString().trim();
    }


    public static String toCamelCase(String phrase) {
        String[] toks = phrase.split("\\s+");
        StringBuilder sb = new StringBuilder(phrase.length());
        for (String tok : toks) {
            sb.append(Character.toUpperCase(tok.charAt(0)));
            if (tok.length() > 1) {
                sb.append(tok.substring(1));
            }
            sb.append(' ');
        }

        return sb.toString().trim();
    }

    private String applyRules(List<Rule> rules, String term) {
        if (uncountables.contains(term) || irregularSingularSet.contains(term)) {
            return term;
        }
        String result = term;
        for (Rule rule : rules) {
            if ((result = rule.apply(term)) != null) {
                break;
            }
        }
        if (result == null) {
            result = term;
        }
        return result;
    }

    void addSingular(String regex, String replacement) {
        singulars.add(new Rule(regex, replacement));
    }


    void addIrregular(String singular, String plural) {
        addSingular("(" + plural.charAt(0) + ")" + plural.substring(1) + "$", "$1" + singular.substring(1));
        irregularSingularSet.add(singular);
    }

    public static class Rule {
        Pattern pattern;
        String replacement;

        public Rule(String regEx, String replacement) {
            this.pattern = Pattern.compile(regEx, Pattern.CASE_INSENSITIVE);
            this.replacement = replacement;
        }

        public String apply(String term) {
            Matcher matcher = pattern.matcher(term);
            if (matcher.find()) {
                return matcher.replaceFirst(replacement);
            }
            return null;
        }
    }

    public static void main(String[] args) {
        Inflector inflector = new Inflector();

        System.out.println(inflector.toSingular("Thermal Maturities"));
        System.out.println(Inflector.toCamelCase("thermal maturities"));

        System.out.println(inflector.toSingular("Hydrologic process"));
    }
}

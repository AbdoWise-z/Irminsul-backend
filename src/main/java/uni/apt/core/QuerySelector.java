package uni.apt.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuerySelector {
    private final List<String> words;

    public QuerySelector() {
        words = new ArrayList<>();
    }

    public QuerySelector addWord(String word) {
        words.add(word);
        return this;
    }

    public List<String> getWords() {
        return words;
    }

    public static List<QuerySelector> parseString(String input) {
        List<QuerySelector> querySelectors = new ArrayList<>();

        Pattern pattern = Pattern.compile("([^\"]\\S*|\".+?\")\\s*");
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String match = matcher.group(1);
            QuerySelector querySelector = new QuerySelector();

            if (match.startsWith("\"") && match.endsWith("\"")) {
                String[] words = match.substring(1, match.length() - 1).split("\\s+");
                for (String word : words) {
                    word = Trim(word);

                    if (!isWordAllowed(word)) continue;

                    querySelector.addWord(word);
                }
            } else {
                match = Trim(match);
                if (isWordAllowed(match))
                    querySelector.addWord(match);
            }

            if (querySelector.words.size() > 0)
                querySelectors.add(querySelector);
        }

        return querySelectors;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QuerySelector selector = (QuerySelector) o;

        return Objects.equals(words, selector.words);
    }

    @Override
    public int hashCode() {
        return words.hashCode();
    }

    private static final String[] TrimCharsStart = {"," , "." , "/" , "\\" , "|" , ">" , "<" , "?" , "'" , "\"" , ":"};
    private static final String[] TrimCharsEnd = {"," , "." , "/" , "\\" , "|" , ">" , "<" , "?" , "'" , "\"" , ":" , "'r" , "'s" , "'re" , "'ll" , "n't"};
    public static String Trim(String s){
        s = s.trim();

        boolean done;
        do{
            done = true;
            for (String ch : TrimCharsStart){
                if (s.startsWith(ch)){
                    s = s.substring(ch.length());
                    done = false;
                }
            }
        } while (!done);

        do{
            done = true;
            for (String ch : TrimCharsEnd){
                if (s.endsWith(ch)){
                    s = s.substring(0 , s.length() - ch.length());
                    done = false;
                }
            }
        } while (!done);

        /**
         * @author MOA
         * */
        Pattern pt = Pattern.compile("^[a-zA-Z0-9\\-]*$");
        if (!pt.matcher(s).matches()){
            return null;
        }


        return s;
    }

    private static final String[] RemoveWords = {"and" , "the"};
    public static boolean isWordAllowed(String word){
        if (word == null) return false;
        if (word.length() < 3) return false; //a , an and garbage letters
        for (String str : RemoveWords){
            if (str.equals(word)) {
                return false;
            }
        }
        return true;
    }

}

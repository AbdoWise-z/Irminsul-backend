import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuerySelector {
    private List<String> words;

    public QuerySelector() {
        words = new ArrayList<>();
    }

    public void addWord(String word) {
        words.add(word);
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
            if (match.startsWith("\"") && match.endsWith("\"")) {
                QuerySelector querySelector = new QuerySelector();
                String[] words = match.substring(1, match.length() - 1).split("\\s+");
                for (String word : words) {
                    querySelector.addWord(word);
                }
                querySelectors.add(querySelector);
            } else {
                QuerySelector querySelector = new QuerySelector();
                querySelector.addWord(match);
                querySelectors.add(querySelector);
            }
        }

        return querySelectors;
    }


}

package uni.apt.engine;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoIterable;
import org.bson.Document;
import uni.apt.Defaults;
import uni.apt.RankerMain;
import uni.apt.core.Log;
import uni.apt.core.OnlineDB;
import uni.apt.core.QuerySelector;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class Ranker {
    private static Log log = Log.getLog(RankerMain.class);

    private MongoCollection<Document> SuggestionsDB;
    private MongoCollection<Document> IndexerDB;
    private MongoCollection<Document> PopularityDB;

    public class SearchResult{

        //stuff for the ranker
        public String link;
        public String word;
        public String originalWord;
        public float TF;
        public float IDF;
        public float popularity;

        //stuff for the display
        public long paragraphIndex;
        public long titleIndex;

        public SearchResult(String link, String word, String originalWord, float TF, float IDF, float popularity, long paragraphIndex, long titleIndex) {
            this.link = link;
            this.word = word;
            this.originalWord = originalWord;
            this.TF = TF;
            this.IDF = IDF;
            this.popularity = popularity;
            this.paragraphIndex = paragraphIndex;
            this.titleIndex = titleIndex;
        }

        public SearchResult() {}
    }

    // value = TF * IDF * factor_1 + Popularity * (TF * IDF > 0 ? 1 : 0) * factor_2

    public List<SearchResult> search(String query , Class<? extends Thread> threadClass){

        if (!RankerSearchThread.class.isAssignableFrom(threadClass)){
            throw new IllegalArgumentException("threadClass must implement RankerSearchThread");
        }

        if (IndexerDB == null)
            throw new IllegalStateException("must call init before");

        List<QuerySelector> selectors = QuerySelector.parseString(query);

        List<RankerSearchThread> searchResults = new LinkedList<>();
        for (QuerySelector s : selectors){
            searchResults.add(_internalSearch(s , false));
        }

        for (RankerSearchThread th : searchResults)
            th.WaitForResult(); //join

        List<SearchResult> results = new LinkedList<>();
        for (RankerSearchThread th : searchResults)
            results.addAll(th.getResult());

        results = _filterResults(results , false);

        return results;
    }

    private RankerSearchThread _internalSearch(QuerySelector word , boolean ordered){
        //TODO: implement me
        return null;
    }

    private List<SearchResult> _filterResults(List<SearchResult> results , boolean phase){
        //TODO: implement me
        return null;
    }
}

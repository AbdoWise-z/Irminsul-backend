package uni.apt.server;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.http.HttpStatusCode;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;
import uni.apt.ServerMain;
import uni.apt.core.Log;
import uni.apt.core.OnlineDB;
import uni.apt.engine.MongoSearchThread;
import uni.apt.engine.Ranker;
import uni.apt.engine.RankerScoreCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class SearchHandler {

    private static final Log log = Log.getLog(SearchHandler.class);

    @GetMapping(value = {"/search/", "/search"})
    public List<Ranker.FinalSearchResult> search(@RequestParam Map<String,String> params, ModelMap model){
        String query = params.get("q");
        if (query == null){
            throw new HttpServerErrorException(HttpStatusCode.valueOf(502));
        }

        log.i("Searching for : " + query);

        if (query.isEmpty()) {
            return new ArrayList<>(); //return empty result for empty queries
        }

        OnlineDB.SuggestionInsert(query);
        return ServerMain.ranker.search(query, MongoSearchThread.class, new RankerScoreCalculator() {
            @Override
            public float getScore(float tfIDF, float wordsMatch, float orderScore, float popularity) {
//                    if (tf > 0.6) { // a spam page
//                        return -0.5f;
//                    }
                return tfIDF * wordsMatch * 10 + orderScore * wordsMatch * 100.0f + popularity * 10;
            }

            @Override
            public float getWordScore(String original, String matched, float TF, int type) {
                return (float) (1.0 - (LevenshteinDistance.getDefaultInstance().apply(original, matched) / (float) Math.max(original.length(), matched.length())));
            }
        });
    }
}

package uni.apt;

import org.apache.commons.text.similarity.CosineDistance;
import org.apache.commons.text.similarity.LevenshteinDistance;
import uni.apt.core.Log;
import uni.apt.core.OnlineDB;
import uni.apt.engine.MongoSearchThread;
import uni.apt.engine.Ranker;
import uni.apt.engine.RankerScoreCalculator;
import uni.apt.engine.WordRecord;

import java.util.List;

public class RankerMain {
    private static void printf(String str , Object... args){
        System.out.printf(str , args);
    }
    private static final Log log = Log.getLog(RankerMain.class);

    public static void main(String[] args){
        OnlineDB.init();

        Ranker ranker = new Ranker();
        List< Ranker.SearchResult> result = ranker.search("genshin impact", MongoSearchThread.class, new RankerScoreCalculator() {
            @Override
            public float getScore(float tf, float IDF, float wordsMatch, float orderScore, float popularity) {
                if (tf > 0.6) { // a spam page
                    return -0.5f;
                }
                return tf * IDF * wordsMatch * 0.25f + orderScore * 10.0f;
            }

            @Override
            public float getWordScore(String original, String matched, float TF, int type) {
                return (type == WordRecord.HEADER ? 2 : 1) * (float) (1.0 - (LevenshteinDistance.getDefaultInstance().apply(original , matched) / (float) Math.max(original.length() , matched.length())));
            }
        });
    }

}

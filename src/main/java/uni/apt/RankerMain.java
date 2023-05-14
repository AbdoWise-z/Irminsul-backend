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
import java.util.Scanner;

public class RankerMain {
    private static void printf(String str , Object... args){
        System.out.printf(str , args);
    }
    private static final Log log = Log.getLog(RankerMain.class);

    public static void main(String[] args){
        OnlineDB.init();

        Ranker ranker = new Ranker();
        Scanner sr = new Scanner(System.in);
        while (true) {
            String query = sr.nextLine();
            List<Ranker.FinalSearchResult> result = ranker.search(query, MongoSearchThread.class, new RankerScoreCalculator() {
                @Override
                public float getScore(float tfIDF, float wordsMatch, float orderScore, float popularity) {
//                    if (tf > 0.6) { // a spam page
//                        return -0.5f;
//                    }
                    return tfIDF * wordsMatch * 0.25f + orderScore * 10.0f;
                }

                @Override
                public float getWordScore(String original, String matched, float TF, int type) {
                    return (type == WordRecord.HEADER ? 2 : 1) * (float) (1.0 - (LevenshteinDistance.getDefaultInstance().apply(original, matched) / (float) Math.max(original.length(), matched.length())));
                }
            });

            for (int i = 0; i < result.size(); i++) {
                String para = OnlineDB.getParagraph(result.get(i).paragraphID);
                printf("=================== %d ===================\n", i);
                printf("Link:       %s\n", result.get(i).link);
                printf("Title:      %s\n", OnlineDB.getTitle(result.get(i).titleID));
                printf("Paragraph:  %s\n", para.substring(0, Math.min(para.length(), 60)));
                printf("Score:  %f\n", result.get(i).score);
            }
            printf("=================== %s ===================\n", "END");

        }
    }

}

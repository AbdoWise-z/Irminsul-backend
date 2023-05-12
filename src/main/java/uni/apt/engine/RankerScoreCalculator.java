package uni.apt.engine;

public interface RankerScoreCalculator {
    float getScore(float tf , float IDF , float wordsMatch , float orderScore , float popularity);

    float getWordScore(String original , String matched , float TF , int type);
}

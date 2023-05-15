package uni.apt.engine;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.bson.Document;
import uni.apt.RankerMain;
import uni.apt.core.InversionCounter;
import uni.apt.core.Log;
import uni.apt.core.OnlineDB;
import uni.apt.core.QuerySelector;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class Ranker {
    private static Log log = Log.getLog(RankerMain.class);

    public static class SearchResult{

        //stuff for the ranker
        public String link;
        public String word;
        public String originalWord;
        public float TF;
        public float IDF;
        public float popularity;

        //public ArrayList<Integer> tag;
        public ArrayList<Integer> wordIndex;

        //stuff for the display
        public ArrayList<Long> paragraphIndex;
        public ArrayList<Integer> type;

        public long titleIndex;


        public SearchResult() {}
    }

    public static class FinalSearchResult{
        public String link;
        public long paragraphID;
        public long titleID;
        public float score;
    }

    public List<FinalSearchResult> search(String query , Class<? extends Thread> threadClass , RankerScoreCalculator calculator){

        if (!RankerSearchThread.class.isAssignableFrom(threadClass)){
            throw new IllegalArgumentException("threadClass must implement RankerSearchThread");
        }

        List<QuerySelector> selectors = QuerySelector.parseString(query);

        if (selectors.size() == 0){
            return new ArrayList<>(); //so the user didn't search for anything
                                      //IDK what to do , but I'll just return empty array for now
        }

        List<RankerSearchThread> searchResults = new LinkedList<>();
        for (QuerySelector s : selectors){
            searchResults.add(_internalSearch(s , threadClass));
        }

        for (RankerSearchThread th : searchResults)
            th.WaitForResult(); //join

        List<SearchResult>[] results = new List[selectors.size()];
        int i = 0;
        for (RankerSearchThread th : searchResults)
            results[i++] = th.getResult();

        return _rankResults(results , calculator);
    }

    private RankerSearchThread _internalSearch(QuerySelector word, Class<? extends Thread> threadClass){
        if (word.getWords().size() == 1){ //just a one word search :)
            try {
                Constructor<? extends Thread> c = threadClass.getConstructor(String.class);
                RankerSearchThread r = (RankerSearchThread) c.newInstance(word.getWords().get(0));
                ((Thread) r).start();
                return r;
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException ignored) {
            }
        }else{ //phrase search !!
            PhraseSearchRankerThread ph = new PhraseSearchRankerThread(word.getWords() , threadClass);
            ph.start();
            return ph;
        }

        return null;
    }

    private static class ParagraphMatching{
        float[] wordsMatching;
        List<List<Integer>> wordsMapping;
        //used to calculate the order score
    }

    private static class IntermediateSearchResult{
        float tfIDF;
        float popularity;

        long highestScoreParagraph = -1;
        float _score = -1;

        Map<Long , ParagraphMatching> paragraphMatching; //<id , value>
        long titleId;
        String link; //we stay null until we start sorting
    }

    private List<FinalSearchResult> _rankResults(List<SearchResult>[] results , RankerScoreCalculator calc){
        HashMap<String , IntermediateSearchResult> temp = new HashMap<>();
        for (int i = 0;i < results.length;i++){
            for (SearchResult result : results[i]){
                IntermediateSearchResult inter = temp.get(result.link);

                if (inter == null){ //init it
                    inter = new IntermediateSearchResult();
                    inter.popularity = result.popularity;
                    inter.titleId = result.titleIndex;
                    inter.tfIDF = 0;
                    inter.link = null;
                    inter.paragraphMatching = new HashMap<>();
                }

                float match_factor = (float) (1.0 - (LevenshteinDistance.getDefaultInstance().apply(result.originalWord , result.word) / (float) Math.max(result.word.length() , result.originalWord.length())));
                if (match_factor < 0.7f)
                    match_factor = (float) Math.pow(match_factor , 2); //if a website contains too many similar words to the search word , then it will skyrocket its score
                                                     //to avoid that , I clip the match_factor for less similar words

                inter.tfIDF += result.IDF * result.TF * match_factor;

                for (int j = 0; j < result.wordIndex.size(); j++) {

                    ParagraphMatching para = inter.paragraphMatching.get(result.paragraphIndex.get(j));
                    if (para == null){
                        para = new ParagraphMatching();
                        para.wordsMatching = new float[results.length];
                        para.wordsMapping = new ArrayList<>(results.length);
                    }

                    para.wordsMatching[i] = Math.max(para.wordsMatching[i] , calc.getWordScore(result.originalWord , result.word , result.TF , result.type.get(j)) / results.length);

                    while (para.wordsMapping.size() <= i){
                        para.wordsMapping.add(new ArrayList<>());
                    }

                    List<Integer> positions = para.wordsMapping.get(i);
                    positions.add(result.wordIndex.get(j));

                    inter.paragraphMatching.put(result.paragraphIndex.get(j) , para);
                }

                temp.put(result.link , inter);
            }
        }

        //now we have all the data ready , time to rank them up

        ArrayList<FinalSearchResult> toBeSorted = new ArrayList<>(temp.size());
        for (Map.Entry<String , IntermediateSearchResult> res : temp.entrySet()){
            res.getValue().link = res.getKey();
            //res.getValue().IDF = (float) indexerWebsites / temp.size();

            for (Map.Entry<Long, ParagraphMatching> para : res.getValue().paragraphMatching.entrySet()) {
                float wM = 0;
                for (float f : para.getValue().wordsMatching)
                    wM += f;

                float s = calc.getScore(res.getValue().tfIDF, wM, CalculateOrderValue(para.getValue()), res.getValue().popularity);

                if (res.getValue()._score < s) {
                        res.getValue()._score = s;
                        res.getValue().highestScoreParagraph = para.getKey();
                }
            }

            res.getValue().paragraphMatching.clear();
            res.getValue().paragraphMatching = null; //release mem

            FinalSearchResult add = new FinalSearchResult();
            add.score = res.getValue()._score;
            add.link = res.getValue().link;
            add.paragraphID = res.getValue().highestScoreParagraph;
            add.titleID = res.getValue().titleId;

            toBeSorted.add(add);
        }

        if (toBeSorted.size() > 1)
            toBeSorted.sort((o1, o2) -> -Float.compare(o1.score, o2.score));


        return toBeSorted;
    }

    private static float CalculateOrderValue(ParagraphMatching para){
        if (para.wordsMapping.size() <= 1) return 0; //only one item , then the order is fine
        return (float) getOrder(para.wordsMapping , 0 , -1) / para.wordsMapping.size();
    }


    private static class PhraseSearchRankerThread extends Thread implements RankerSearchThread {
        private final List<String> words;
        private final List<SearchResult> results;
        private final Class<? extends Thread> clazz;

        public PhraseSearchRankerThread(List<String> words , Class<? extends Thread> c){
            this.words = words;
            results = new LinkedList<>();
            clazz = c;
        }
        @Override
        public void setStrict(boolean b) {
            //not used
        }

        @Override
        public List<SearchResult> getResult() {
            return results;
        }

        @Override
        public void WaitForResult() {
            try {
                this.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            super.run();
            List<RankerSearchThread> searchThreads = new ArrayList<>(results.size());
            StringBuilder phrase = null;
            for (String str : words) {
                try {
                    Constructor<? extends Thread> c = clazz.getConstructor(String.class);
                    RankerSearchThread r = (RankerSearchThread) c.newInstance(str);
                    r.setStrict(true);
                    ((Thread) r).start();
                    searchThreads.add(r);

                    if (phrase == null)
                        phrase = new StringBuilder(str);
                    else
                        phrase.append(" ").append(str);

                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException ignored) {
                }
            }

            if (phrase == null)
                throw new RuntimeException("StringBuild is null");


            for (RankerSearchThread t : searchThreads)
                t.WaitForResult();

            List<SearchResult>[] results = new List[words.size()];
            int i = 0;
            for (RankerSearchThread th : searchThreads)
                results[i++] = th.getResult();

            HashMap<String, IntermediatePhraseSearchResult> temp = new HashMap<>();
            for (i = 0; i < results.length; i++) {
                for (SearchResult result : results[i]) {
                    IntermediatePhraseSearchResult inter = temp.get(result.link);
                    if (inter == null) { //init it
                        inter = new IntermediatePhraseSearchResult();
                        inter.IDF = 0;
                        inter.popularity = result.popularity;
                        inter.titleID = result.titleIndex;

                        inter.TF = 1.0f;
                        inter.wordsMapping = new HashMap<>();
                    }

                    inter.IDF = Math.max(result.IDF , inter.IDF);
                    inter.TF  = Math.min(result.TF  , inter.TF );

                    for (int j = 0; j < result.wordIndex.size(); j++) {
                        int finalJ = j;
                        List<List<Integer>> para = inter.wordsMapping.computeIfAbsent(result.paragraphIndex.get(j), k -> new ParagraphInfo(result.type.get(finalJ))).words;
                        while (para.size() <= i){
                            para.add(new ArrayList<>());
                        }
                        List<Integer> positions = para.get(i);
                        positions.add(result.wordIndex.get(j));
                    }

                    temp.put(result.link, inter);
                }
            }

            int indexerWebsites = OnlineDB.MetaDB.find(new Document("obj-id" , "indexer-meta")).first().getInteger("websites");

            for (Map.Entry<String , IntermediatePhraseSearchResult> ent : temp.entrySet()){
                SearchResult result = new SearchResult();
                IntermediatePhraseSearchResult res = ent.getValue();

                result.link = ent.getKey();
                result.word = phrase.toString();
                result.originalWord = result.word;
                result.TF = res.TF;
                result.IDF = (float) indexerWebsites / temp.size();
                result.titleIndex = res.titleID;
                result.popularity = res.popularity;

                result.paragraphIndex = new ArrayList<>();
                result.wordIndex      = new ArrayList<>();
                //result.tag            = new ArrayList<>();
                result.type           = new ArrayList<>();

                for (Map.Entry<Long , ParagraphInfo> mapItem : res.wordsMapping.entrySet()){
                    List<List<Integer>> indexes = mapItem.getValue().words;
                    if (indexes.size() == words.size()){
                        int index = getOrder(indexes , 0 , -1);
                        if (index == words.size()){
                            result.paragraphIndex.add(mapItem.getKey());
                            result.wordIndex.add(index);
                            result.type.add(mapItem.getValue().type);
                            //result.tag.add(0);
                        }
                    }
                }

                if (result.wordIndex.size() != 0){
                    this.results.add(result);
                }
            }
        }
    }

    private static int getOrder(List<List<Integer>> indexes , int posX , int prev){
        if (posX == indexes.size())
            return 0;

        List<Integer> selections = indexes.get(posX);

        if (selections.size() == 0){
            return getOrder(indexes , posX + 1 , -1);
        }

        int m = 0;
        for (Integer i : selections){
            int k = getOrder(indexes , posX + 1 , i);
            if (i - prev == 1 || prev == -1){
                m = Math.max(m , k + 1);
            }else{
                m = Math.max(m , k);
            }
        }

        return m;
    }

    static class ParagraphInfo{
        List<List<Integer>> words = new ArrayList<>();
        int type;

        public ParagraphInfo(int type){
            this.type = type;
        }
    }

    static class IntermediatePhraseSearchResult {
        float TF;
        float IDF;
        float popularity;

        long titleID;

        Map<Long , ParagraphInfo> wordsMapping;
    }
}

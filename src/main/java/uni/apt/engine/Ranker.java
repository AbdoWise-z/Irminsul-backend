package uni.apt.engine;

import org.apache.commons.text.similarity.LevenshteinDistance;
import uni.apt.RankerMain;
import uni.apt.core.InversionCounter;
import uni.apt.core.Log;
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

        public ArrayList<Integer> tag;
        public ArrayList<Integer> wordIndex;

        //stuff for the display
        public ArrayList<Long> paragraphIndex;
        public ArrayList<Integer> type;

        public long titleIndex;


        public SearchResult() {}
    }

    public static class FinalSearchResult{
        public String link;
        public int paragraphID;
        public int titleID;
        public float score;
    }

    public List<SearchResult> search(String query , Class<? extends Thread> threadClass , RankerScoreCalculator calculator){

        if (!RankerSearchThread.class.isAssignableFrom(threadClass)){
            throw new IllegalArgumentException("threadClass must implement RankerSearchThread");
        }

        List<QuerySelector> selectors = QuerySelector.parseString(query);

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
        float wordsMatching;
        List<Integer> wordsMapping;
        //used to calculate the order score
    }

    private static class IntermediateSearchResult{
        float TF;
        float IDF;
        float popularity;

        long highestScoreParagraph = -1;
        float _score = -1;

        Map<Long , ParagraphMatching> paragraphMatching; //<id , value>

        long titleId;
        String link; //we stay null until we start sorting
    }

    private List<SearchResult> _rankResults(List<SearchResult>[] results , RankerScoreCalculator calc){
        HashMap<String , IntermediateSearchResult> temp = new HashMap<>();
        for (int i = 0;i < results.length;i++){
            for (SearchResult result : results[i]){
                IntermediateSearchResult inter = temp.get(result.link);
                if (inter == null){ //init it
                    inter = new IntermediateSearchResult();
                    inter.popularity = result.popularity;
                    inter.titleId = result.titleIndex;
                    inter.IDF = 0;
                    inter.TF = 0;
                    inter.link = null;
                    inter.paragraphMatching = new HashMap<>();
                }

                float match_factor = (float) (1.0 - (LevenshteinDistance.getDefaultInstance().apply(result.originalWord , result.word) / (float) Math.max(result.word.length() , result.originalWord.length())));
                if (match_factor < 0.85f)
                    match_factor = 0; //if a website contains too many similar words to the search word , then it will skyrocket its score
                                      //to avoid that , I clip the match_factor for less similar words

                inter.IDF += result.IDF * match_factor;
                inter.TF  += result.TF * match_factor;

                for (int j = 0; j < result.wordIndex.size(); j++) {
                    ParagraphMatching para = inter.paragraphMatching.get(result.paragraphIndex.get(j));
                    if (para == null){
                        para = new ParagraphMatching();
                        para.wordsMatching = 0;
                        para.wordsMapping = new ArrayList<>(results.length);
                    }

                    para.wordsMatching += calc.getWordScore(result.originalWord , result.word , result.TF , result.type.get(j)) / results.length;
                    Integer pos = para.wordsMapping.size() > i ? para.wordsMapping.get(i) : null;
                    if (pos == null){
                        para.wordsMapping.add(result.wordIndex.get(j));
                    }else{
                        if (i != 0){
                            Integer prev = para.wordsMapping.get(i - 1);
                            if (prev > pos){
                                if (result.wordIndex.get(j) > prev){
                                    para.wordsMapping.set(i , result.wordIndex.get(j));
                                }
                            }
                        }
                    }
                    //para.wordsMapping.set(i , pos);
                    inter.paragraphMatching.put(result.paragraphIndex.get(j) , para);
                }

                temp.put(result.link , inter);
            }
        }

        //now we have all the data ready , time to rank them up

        ArrayList<IntermediateSearchResult> toBeSorted = new ArrayList<>(temp.size());
        for (Map.Entry<String , IntermediateSearchResult> res : temp.entrySet()){
            res.getValue().link = res.getKey();

            for (Map.Entry<Long, ParagraphMatching> para : res.getValue().paragraphMatching.entrySet()) {
                float s = calc.getScore(res.getValue().TF, res.getValue().IDF, para.getValue().wordsMatching, CalculateOrderValue(para.getValue()), res.getValue().popularity);
                if (res.getValue()._score < s) {
                    res.getValue()._score = s;
                    res.getValue().highestScoreParagraph = para.getKey();
                }
            }
            res.getValue().paragraphMatching.clear();
            res.getValue().paragraphMatching = null; //release mem

            toBeSorted.add(res.getValue());
        }

        toBeSorted.sort((o1, o2) -> -Float.compare(o1._score, o2._score));

        return null;
    }

    private static float CalculateOrderValue(ParagraphMatching para){
        if (para.wordsMapping.size() == 1) return 0; //only one item , then the order is fine

        double max_inversions = para.wordsMapping.size() * (para.wordsMapping.size() - 1) / 2.0;
        int[] arr = new int[para.wordsMapping.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = para.wordsMapping.get(i);
        }
        return (float) ((max_inversions - InversionCounter.countInversions(arr)) / max_inversions);
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

                        inter.TF = 0.59f;
                        inter.wordsMapping = new HashMap<>();
                    }

                    inter.IDF = Math.max(result.IDF , inter.IDF);
                    inter.TF  = Math.min(result.TF  , inter.TF );


                    for (int j = 0; j < result.wordIndex.size(); j++) {
                        List<List<Integer>> para = inter.wordsMapping.computeIfAbsent(result.paragraphIndex.get(j), k -> new ArrayList<>());
                        List<Integer> positions = para.size() > i ? para.get(i) : null;
                        if (positions == null){
                            positions = new LinkedList<>();
                            para.add(positions);
                        }
                        positions.add(result.wordIndex.get(j));
                    }

                    temp.put(result.link, inter);
                }
            }

            for (Map.Entry<String , IntermediatePhraseSearchResult> ent : temp.entrySet()){
                SearchResult result = new SearchResult();
                IntermediatePhraseSearchResult res = ent.getValue();

                result.link = ent.getKey();
                result.word = phrase.toString();
                result.originalWord = result.word;
                result.TF = res.TF;
                result.IDF = res.IDF;
                result.titleIndex = res.titleID;
                result.popularity = res.popularity;

                result.paragraphIndex = new ArrayList<>();
                result.wordIndex      = new ArrayList<>();
                result.tag            = new ArrayList<>();
                result.type           = new ArrayList<>();

                for (Map.Entry<Long , List<List<Integer>>> mapItem : res.wordsMapping.entrySet()){
                    List<List<Integer>> indexes = mapItem.getValue();
                    if (indexes.size() == words.size()){

                    }
                }

                if (result.wordIndex.size() != 0){
                    this.results.add(result);
                }
            }
        }
    }

    private static int getOrder(List<List<Integer>> indexes , int posX){

    }

    static class IntermediatePhraseSearchResult {
        float TF;
        float IDF;
        float popularity;

        long titleID;

        Map<Long , List< List<Integer>> > wordsMapping;
    }
}

package uni.apt;

import uni.apt.core.Log;
import uni.apt.engine.Indexer;
import uni.apt.engine.IndexerIO;

public class EngineMain {

    private static final Log log = Log.getLog(EngineMain.class);
    private static void printf(String str , Object... args){
        System.out.printf(str , args);
    }

    public static void main(String[] args){
        Indexer db = new Indexer(1); //dummy indexer to load the old data on it


        long _start = System.currentTimeMillis();
        if (IndexerIO.getFromFile(Defaults.INDEXER_LOCAL_FILE , db)){
            log.i("Loaded (" + (System.currentTimeMillis() - _start) + " ms)");
        }else{
            log.e("Failed to load");
        }

        log.w("Finished");

    }
}

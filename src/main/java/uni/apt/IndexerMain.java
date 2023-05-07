package uni.apt;

import uni.apt.core.Log;
import uni.apt.core.OnlineDB;
import uni.apt.engine.Indexer;
import uni.apt.engine.IndexerIO;

import java.io.File;
import java.util.Scanner;

public class IndexerMain {
    private static void printf(String str , Object... args){
        System.out.printf(str , args);
    }
    private static final Log log = Log.getLog(IndexerMain.class);

    public static void main(String[] args) throws InterruptedException {
        OnlineDB.init();
        Thread.sleep(2000);

        log.w("Started");
        Scanner in = new Scanner(System.in);
        int num_threads = -1;
        while (num_threads < 0 || num_threads > 30){
            printf("Enter the number of threads: ");
            num_threads = in.nextInt();
            if (num_threads < 0 || num_threads > 30){
                printf("%d is not a valid number\n" , num_threads);
            }
        }


        Indexer indexer = new Indexer(num_threads);

        log.i("Indexer started");
        if (new File(Defaults.INDEXER_LOCAL_FILE).exists())
            indexer.start(Defaults.INDEXER_LOCAL_FILE);
        else {
            OnlineDB.base.getCollection(Defaults.INDEXER_INDEXED_PARAGRAPHS).drop(); //delete the old data if any
            indexer.start(null);
        }

        while (indexer.isRunning()){}
        log.i("Indexer Finished");

        IndexerIO.WriteToFile(Defaults.INDEXER_LOCAL_FILE , indexer);

        log.w("Finished");
    }
}

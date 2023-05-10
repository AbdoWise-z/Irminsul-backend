package uni.apt;

import uni.apt.core.Log;
import uni.apt.core.OnlineDB;
import uni.apt.engine.Indexer;
import uni.apt.engine.IndexerIO;
import uni.apt.engine.MongoDBIndexerStorage;

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


        Indexer indexer = new Indexer(num_threads , new MongoDBIndexerStorage());

        log.i("Indexer started");
        indexer.start(Defaults.INDEXER_MONGO_NAME);


        while (indexer.isRunning()){}
        log.i("Indexer Finished");

        indexer.getStorage().save(null);

        log.w("Finished");
    }
}

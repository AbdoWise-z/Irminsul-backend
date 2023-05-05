package uni.apt;

import uni.apt.engine.*;
import uni.apt.core.Log;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;

public class Main {

    public static final Log log = Log.getLog(Main.class);
    public static void main(String[] args) throws IOException {
        log.i("Starting...");
        Queue<String> seed = new LinkedList<>();
        seed.add("https://google.com");
        //seed.add("https://wikipedia.com");
        //seed.add("https://yahoo.com");

        Crawler.log.setEnabled(false);

        Crawler crawler = new Crawler(12);
        crawler.start(1000 , seed , true);

        int c = 0;

        log.i("Crawler started");
        while (crawler.isRunning()){
            if (c != crawler.getCrawledCount()) {
                log.i(crawler.getCrawledCount() + "/" + crawler.getLimit());
                c = crawler.getCrawledCount();
            }
        }
        log.i("Crawler finished");


        Indexer indexer = new Indexer(12);
        indexer.start(crawler);

        log.i("Indexer started");

        while (indexer.isRunning()){}

        log.i("Indexer finished");

        IndexedDatabase database = new IndexedDatabase();
        database.insert(indexer);

        database.WriteToFile("base.idx");


        for (Map.Entry<String , WordProps> et : indexer.getIndexedWords().entrySet()){
            if (!et.getKey().equals("Dream,")) continue;
            System.out.println();
            System.out.println(et.getKey());
            for (int i = 0;i < et.getValue().indices.size();i++){
                System.out.println("   " + et.getValue().links.get(i));
                for (int j = 0;j < et.getValue().indices.get(i).size();j++){
                    WordRecord idx = et.getValue().indices.get(i).get(j);
                    System.out.println("   " + "   " + idx.tagIndex + "  " + idx.pos + "  " + idx.type);
                }
            }
        }

        while (true){
            Scanner sr = new Scanner(System.in);
            System.out.println("Enter a word to search for: ");
            String word = sr.next();
            if (word.equals("!exit")) break;
            int wi = 0;
            for (Map.Entry<String , WordProps> et : indexer.getIndexedWords().entrySet()){
                if (!et.getKey().toLowerCase().contains(word.toLowerCase())) continue;
                wi++;
                if (wi > 10) break;
                System.out.println();
                System.out.println(et.getKey());
                int wj = 0;
                for (int i = 0;i < et.getValue().indices.size();i++){
                    System.out.println("   " + et.getValue().links.get(i));
                    wj++;
                    if (wj > 4) break;
                    for (int j = 0;j < et.getValue().indices.get(i).size();j++){
                        WordRecord idx = et.getValue().indices.get(i).get(j);
                        System.out.println("   " + "   " + indexer.getParagraphList().get(idx.paragraphIndex));
                    }
                }
            }

        }

        log.i("Finished.");

    }
}
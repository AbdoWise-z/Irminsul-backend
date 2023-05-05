package uni.apt;

import uni.apt.engine.IndexedDatabase;
import uni.apt.engine.WordProps;
import uni.apt.engine.WordRecord;

import java.util.Map;
import java.util.Scanner;

public class LoaderTest {
    public static void main(String[] args){
        long start = System.currentTimeMillis();
        IndexedDatabase db = new IndexedDatabase();

        if (db.getFromFile("base.idx")){
            System.out.println("time: " + (System.currentTimeMillis() - start) + " ms");
            System.out.println("loaded!\n");


            while (true){
                Scanner sr = new Scanner(System.in);
                System.out.print("Enter a word to search for: ");
                String word = sr.next();

                start = System.currentTimeMillis();

                if (word.equals("!exit")) break;
                int wi = 0;
                for (Map.Entry<String , WordProps> et : db.getIndexedWords().entrySet()){
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
                            System.out.println("   " + "   " + db.getParagraphList().get(idx.paragraphIndex));
                        }
                    }
                }

                System.out.println();
                System.out.println("Search done in: " + (System.currentTimeMillis() - start) + " ms");
            }
        }
    }
}

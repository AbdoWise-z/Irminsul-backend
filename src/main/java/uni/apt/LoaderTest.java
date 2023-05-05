package uni.apt;

import uni.apt.engine.IndexedDatabase;

public class LoaderTest {
    public static void main(String[] args){
        long start = System.currentTimeMillis();
        IndexedDatabase db = new IndexedDatabase();

        if (db.getFromFile("base.idx")){
            System.out.println("time: " + (System.currentTimeMillis() - start) + " ms");
            System.out.println("loaded!\n");
        }
    }
}

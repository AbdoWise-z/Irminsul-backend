package uni.apt.engine;

import java.io.*;
import java.util.*;

public class IndexedDatabase {
    public static class IndexDatabaseSnapshot implements Serializable {
        public String[] paragraph;
        public String[] words;
        public String[][] words_links;
        public WordRecord[][][] words_records;
    }

    private final Map<String , WordProps> indexedWords = new HashMap<>();
    private final ArrayList<String> paragraphList = new ArrayList<>();

    public IndexedDatabase(){

    }

    public void insert(Indexer idx){
        if (idx.isRunning()){
            throw new IllegalStateException("Indexer is still running.");
        }

        Map<String , WordProps> words = idx.getIndexedWords();
        LinkedList<String> paragraphs = idx.getParagraphList();

        for (Map.Entry<String , WordProps> ent : words.entrySet()){
            WordProps props;
            if (( props = indexedWords.get( ent.getKey()) ) == null){
                props = new WordProps();
            }

            //update the thing
            List<String> ent_links = ent.getValue().links;
            List<List<WordRecord>> ent_records = ent.getValue().indices;

            for (int i = 0; i < ent_links.size(); i++) {
                String link = ent_links.get(i);
                List<WordRecord> wordRecord = ent_records.get(i);

                int pos = props.links.indexOf(link);
                if (pos > 0){
                    props.links.remove(pos);
                    props.indices.remove(pos); //remove the old data
                }


                for (WordRecord w : wordRecord){
                    w.paragraphIndex += paragraphList.size();
                }

                props.links.add(link);
                props.indices.add(wordRecord);
            }

            indexedWords.put(ent.getKey() , props);
        }

        paragraphList.addAll(paragraphs);
    }

    public void clear(){
        indexedWords.clear();
        paragraphList.clear();
    }

    public IndexDatabaseSnapshot getSnapshot() {
        IndexDatabaseSnapshot snap = new IndexDatabaseSnapshot();
        snap.paragraph = paragraphList.toArray(new String[0]);

        snap.words = new String[indexedWords.size()];
        snap.words_links = new String[indexedWords.size()][];
        snap.words_records = new WordRecord[indexedWords.size()][][];

        int pos = 0;
        for (Map.Entry<String, WordProps> ent : indexedWords.entrySet()) {
            snap.words[pos] = ent.getKey();

            List<String> ent_links = ent.getValue().links;
            List<List<WordRecord>> ent_records = ent.getValue().indices;

            snap.words_links[pos] = new String[ent_links.size()];
            snap.words_records[pos] = new WordRecord[ent_links.size()][];

            for (int i = 0; i < ent_links.size(); i++) {
                String link = ent_links.get(i);
                List<WordRecord> wordRecords = ent_records.get(i);
                snap.words_links[pos][i] = link;
                snap.words_records[pos][i] = wordRecords.toArray(new WordRecord[0]);
            }

            pos++;
        }

        return snap;

    }

    public void set(IndexDatabaseSnapshot snap){
        paragraphList.clear();
        indexedWords.clear();

        paragraphList.addAll(Arrays.asList(snap.paragraph));

        for (int i = 0; i < snap.words.length; i++) {
            String word = snap.words[i];
            WordProps props = new WordProps();
            for (int j = 0; j < snap.words_links[i].length; j++) {
                props.links.add(snap.words_links[i][j]);

                LinkedList<WordRecord> records = new LinkedList<>(Arrays.asList(snap.words_records[i][j]));

                props.indices.add(records);
            }

            indexedWords.put(word , props);
        }
    }


    public boolean WriteToFile(String f){
        IndexDatabaseSnapshot snap = getSnapshot();
        try{
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f));
            out.writeObject(snap);
            out.close();
            return true;
        } catch (Exception e){
            e.printStackTrace();
        }

        return false;
    }

    public boolean getFromFile(String f){

        try{
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(f));
            set( ((IndexDatabaseSnapshot) in.readObject()) );
            return true;
        } catch (Exception e){
            e.printStackTrace();
        }

        return false;
    }
}

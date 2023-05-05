package uni.apt.engine;

import uni.apt.core.Log;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class IndexedDatabase {

    private static final Log log = Log.getLog(IndexedDatabase.class);

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

    public synchronized void insert(Indexer idx){
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

    public synchronized void clear(){
        indexedWords.clear();
        paragraphList.clear();
    }

    public synchronized IndexDatabaseSnapshot getSnapshot() {
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

    public synchronized void set(IndexDatabaseSnapshot snap){
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

    private static void write_int(OutputStream out , int v) throws IOException{
        out.write( (byte) ((v     )  & 0xff) );
        out.write( (byte) ((v >> 8)  & 0xff) );
        out.write( (byte) ((v >> 16) & 0xff) );
        out.write( (byte) ((v >> 24) & 0xff) );
    }

    private static final Charset CHARSET = StandardCharsets.UTF_8;



    public synchronized boolean WriteToFile(String f){
        try{
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f) , 16 * 1024);

            write_int(out , paragraphList.size());
            for (String s : paragraphList) {
                byte[] p = s.getBytes(CHARSET);
                write_int(out, p.length);
                out.write(p);
            }

            write_int(out , indexedWords.size());

            for (Map.Entry<String, WordProps> ent : indexedWords.entrySet()) {
                byte[] np = ent.getKey().getBytes(CHARSET);

                write_int(out , np.length);
                out.write(np);

                WordProps props = ent.getValue();

                write_int(out , props.links.size());
                for (String s : props.links){
                    byte[] p = s.getBytes(CHARSET);
                    write_int(out , p.length);
                    out.write(p);
                }

                for (List<WordRecord> prop : props.indices){
                    write_int(out , prop.size());
                    for (WordRecord r : prop){
                        write_int(out , r.paragraphIndex);
                        write_int(out , r.pos);
                        write_int(out , r.tagIndex);
                        write_int(out , r.type);
                    }
                }
            }

            out.flush();
            out.close();

            return true;
        }catch (Exception e){
            e.printStackTrace();
        }

        return false;
    }

    private static void readUntil(InputStream in , byte[] buff , int off , int size) throws IOException{
        int s = size;
        while (s > 0){
            int rs = in.read(buff , off , s);

            if (rs == -1){
                throw new RuntimeException("EOF");
            }

            s -= rs;
            off += rs;
        }
    }

    private static int read_int(InputStream in) throws IOException{
        return (in.read() & 0xff) | ((in.read() & 0xff) << 8) | ((in.read() & 0xff) << 16) | ((in.read() & 0xff) << 24);
    }

    public synchronized boolean getFromFile(String f){
        indexedWords.clear();
        paragraphList.clear();

        try{
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(f) , 8 * 1024);
            byte[] buff = new byte[264 * 1024];

            int paragraphSize = read_int(in);

            for (int i = 0; i < paragraphSize; i++) {
                int s = read_int(in);
                readUntil(in , buff , 0 , s);
                paragraphList.add(new String(buff , 0 , s , CHARSET));
            }

            int words_size = read_int(in);

            for (int i = 0; i < words_size; i++) {
                int ws = read_int(in);
                readUntil(in , buff , 0  , ws);
                String word = new String(buff , 0 , ws , CHARSET);

                int ls = read_int(in);

                WordProps props = new WordProps();
                props.links = new ArrayList<>(ls); //to avoid too many resizes
                props.indices = new ArrayList<>(ls);

                for (int j = 0; j < ls; j++) {
                    int link_size = read_int(in);
                    readUntil(in , buff , 0 , link_size);
                    props.links.add(new String(buff , 0 , link_size , CHARSET));
                }

                for (int j = 0; j < ls; j++) {
                    int rs = read_int(in);
                    LinkedList<WordRecord> records = new LinkedList<>();
                    for (int k = 0; k < rs; k++) {
                        WordRecord record = new WordRecord();
                        record.paragraphIndex = read_int(in);
                        record.pos            = read_int(in);
                        record.tagIndex       = read_int(in);
                        record.type           = read_int(in);

                        records.add(record);
                    }

                    props.indices.add(records);
                }

                indexedWords.put(word , props);
            }
            in.close();

            return true;
        }catch (Exception e){
            e.printStackTrace();
        }

        return false;
    }

    public synchronized boolean WriteToObjectFile(String f){
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

    public synchronized boolean getFromObjectFile(String f){
        indexedWords.clear();
        paragraphList.clear();

        try{
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(f));
            set( ((IndexDatabaseSnapshot) in.readObject()) );
            return true;
        } catch (Exception e){
            e.printStackTrace();
        }

        return false;
    }

    public synchronized Map<String , WordProps> getIndexedWords(){
        return indexedWords;
    }

    public synchronized ArrayList<String> getParagraphList(){
        return paragraphList;
    }
}

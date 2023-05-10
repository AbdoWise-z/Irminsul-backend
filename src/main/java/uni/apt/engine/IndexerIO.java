package uni.apt.engine;

import uni.apt.core.Log;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class IndexerIO {

//    private static final Log log = Log.getLog(IndexerIO.class);
//    private static final Charset CHARSET = StandardCharsets.UTF_8;
//
//    private static void readUntil(InputStream in , byte[] buff , int off , int size) throws IOException{
//        int s = size;
//        while (s > 0){
//            int rs = in.read(buff , off , s);
//
//            if (rs == -1){
//                throw new RuntimeException("EOF");
//            }
//
//            s -= rs;
//            off += rs;
//        }
//    }
//
//    private static int read_int(InputStream in) throws IOException {
//        return (in.read() & 0xff) | ((in.read() & 0xff) << 8) | ((in.read() & 0xff) << 16) | ((in.read() & 0xff) << 24);
//    }
//
//    private static void write_int(OutputStream out , int v) throws IOException{
//        out.write( (byte) ((v     )  & 0xff) );
//        out.write( (byte) ((v >> 8)  & 0xff) );
//        out.write( (byte) ((v >> 16) & 0xff) );
//        out.write( (byte) ((v >> 24) & 0xff) );
//    }
//
//
//    private static long read_long(InputStream in) throws IOException {
//        return (in.read() & 0xff) | (((long) (in.read() & 0xff)) << 8) | (((long) (in.read() & 0xff)) << 16) | (((long) (in.read() & 0xff)) << 24)
//                | (((long) (in.read() & 0xff)) << 32) | (((long) (in.read() & 0xff)) << 40) | (((long) (in.read() & 0xff)) << 48) | (((long) (in.read() & 0xff)) << 56) ;
//    }
//
//    private static void write_long(OutputStream out , long v) throws IOException{
//        out.write( (byte) ((v     )  & 0xff) );
//        out.write( (byte) ((v >> 8)  & 0xff) );
//        out.write( (byte) ((v >> 16) & 0xff) );
//        out.write( (byte) ((v >> 24) & 0xff) );
//        out.write( (byte) ((v >> 32) & 0xff) );
//        out.write( (byte) ((v >> 40) & 0xff) );
//        out.write( (byte) ((v >> 48) & 0xff) );
//        out.write( (byte) ((v >> 56) & 0xff) );
//    }
//
//
//    public static boolean getFromFile(String f , Indexer indexer){
//        indexer.indexedWords.clear();
//
//        log.i("Reading indexer from file: " + f);
//
//        try{
//            BufferedInputStream in = new BufferedInputStream(new FileInputStream(f) , 8 * 1024);
//            byte[] buff = new byte[8 * 1024];
//
//            int words_size = read_int(in);
//
//            for (int i = 0; i < words_size; i++) {
//                int ws = read_int(in);
//                readUntil(in , buff , 0  , ws);
//                String word = new String(buff , 0 , ws , CHARSET);
//
//                int ls = read_int(in);
//
//                WordProps props = new WordProps();
//                props.links = new ArrayList<>(ls); //to avoid too many resizes
//                props.indices = new ArrayList<>(ls);
//
//                for (int j = 0; j < ls; j++) {
//                    int link_size = read_int(in);
//                    readUntil(in , buff , 0 , link_size);
//                    props.links.add(new String(buff , 0 , link_size , CHARSET));
//                }
//
//                for (int j = 0; j < ls; j++) {
//                    int rs = read_int(in);
//                    LinkedList<WordRecord> records = new LinkedList<>();
//                    for (int k = 0; k < rs; k++) {
//                        WordRecord record = new WordRecord();
//                        record.paragraphIndex = read_long(in);
//                        record.pos            = read_int(in);
//                        record.tagIndex       = read_int(in);
//                        record.type           = read_int(in);
//
//                        records.add(record);
//                    }
//
//                    props.indices.add(records);
//                }
//
//                indexer.indexedWords.put(word , props);
//            }
//            in.close();
//
//            log.i("Reading done , size: " + indexer.indexedWords.size());
//
//            return true;
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//
//        log.i("Failed to read");
//
//        return false;
//    }
//
//
//    public static boolean WriteToFile(String f , Indexer indexer){
//
//        log.i("Writing indexer to file: " + f);
//
//        try{
//            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f) , 16 * 1024);
//
//            write_int(out , indexer.indexedWords.size());
//
//            for (Map.Entry<String, WordProps> ent : indexer.indexedWords.entrySet()) {
//                byte[] np = ent.getKey().getBytes(CHARSET);
//
//                write_int(out , np.length);
//                out.write(np);
//
//                WordProps props = ent.getValue();
//
//                write_int(out , props.links.size());
//                for (String s : props.links){
//                    byte[] p = s.getBytes(CHARSET);
//                    write_int(out , p.length);
//                    out.write(p);
//                }
//
//                for (List<WordRecord> prop : props.indices){
//                    write_int(out , prop.size());
//                    for (WordRecord r : prop){
//                        write_long(out , r.paragraphIndex);
//                        write_int(out , r.pos);
//                        write_int(out , r.tagIndex);
//                        write_int(out , r.type);
//                    }
//                }
//            }
//
//            out.flush();
//            out.close();
//
//            log.i("Writing done");
//
//            return true;
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//
//        log.i("Failed to write to file");
//
//        return false;
//    }
//    private IndexerIO(){}
}

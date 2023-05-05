package uni.apt.engine;

import uni.apt.engine.Indexer;

import java.io.*;

public class IdxFile {
    public static boolean writeIdx(Indexer idx , String file){
        try {
            FileOutputStream fout = new FileOutputStream(new File(file));
            ObjectOutputStream out = new ObjectOutputStream(fout);
            out.writeObject(idx);
            out.close();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Indexer loadIdx(String file){
        try {
            FileInputStream fin = new FileInputStream(new File(file));
            ObjectInputStream in = new ObjectInputStream(fin);
            Indexer ret = (Indexer) in.readObject();
            in.close();
            return ret;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}

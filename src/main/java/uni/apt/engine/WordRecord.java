package uni.apt.engine;

import java.io.Serializable;

public class WordRecord implements Serializable {
    public int pos;
    public int type;
    public int tagIndex;
    public long paragraphIndex;

    public static final int PARAGRAPH = 0;
    public static final int HEADER    = 1;
}

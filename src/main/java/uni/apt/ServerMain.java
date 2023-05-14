package uni.apt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uni.apt.core.OnlineDB;
import uni.apt.engine.Ranker;

@SpringBootApplication
public class ServerMain {
    public static Ranker ranker;
    public static void main(String[] args){
        OnlineDB.init();
        ranker = new Ranker();
        SpringApplication.run(ServerMain.class , args);
    }
}

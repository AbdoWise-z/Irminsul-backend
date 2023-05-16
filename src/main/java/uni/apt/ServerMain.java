package uni.apt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import uni.apt.core.OnlineDB;
import uni.apt.engine.Ranker;
import uni.apt.server.ServerCfg;

@SpringBootApplication
public class ServerMain {
    public static Ranker ranker;
    public static void main(String[] args){
        OnlineDB.init();
        ranker = new Ranker();
        SpringApplication.run(ServerMain.class , args);
    }
}

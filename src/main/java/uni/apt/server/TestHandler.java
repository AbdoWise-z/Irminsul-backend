package uni.apt.server;

import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class TestHandler {

    @PostMapping("/test")
    public String lol(@RequestBody String str){
        System.out.println(str);

        return "nothing for you";
    }
}

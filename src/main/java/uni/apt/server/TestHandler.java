package uni.apt.server;

import org.springframework.http.MediaType;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class TestHandler {

    @PostMapping(
            value = "/test",
            consumes = {MediaType.ALL_VALUE}
    )
    public String lol(@RequestBody String str){
        System.out.println(str);

        return "nothing for you";
    }
}

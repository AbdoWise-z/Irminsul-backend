package uni.apt.server;

import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestHandler {
    static int id = 0;
    @GetMapping("/test")
    public String lol(@RequestParam Map<String,String> allRequestParams, ModelMap model){
        System.out.println("Request received " + (id++) + "\n");
        return "get gud: " + allRequestParams.get("q");
    }
}

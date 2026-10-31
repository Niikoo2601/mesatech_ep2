package cl.duoc.api_cloud_native.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/public")
public class PublicController {

    @GetMapping("/hola")
    public Map<String, String> hola() {
        return Map.of("mensaje", "API Spring Boot funcionando");
    }
}

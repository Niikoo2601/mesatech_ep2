package cl.duoc.api_cloud_native.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UsuarioController {

    @GetMapping("/usuario")
    public Map<String, Object> usuario(@AuthenticationPrincipal Jwt jwt) {

        Map<String, Object> respuesta = new LinkedHashMap<>();

        respuesta.put("mensaje", "Token JWT validado correctamente");
        respuesta.put("nombre", jwt.getClaimAsString("name"));
        respuesta.put("usuario", jwt.getClaimAsString("preferred_username"));
        respuesta.put("oid", jwt.getClaimAsString("oid"));
        respuesta.put("scopes", jwt.getClaimAsString("scp"));
        respuesta.put("roles", jwt.getClaimAsStringList("roles"));
        respuesta.put("audience", jwt.getAudience());

        return respuesta;
    }
}

package br.com.fiap.gs.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Rotas do app web (Expo Router, build em frontend/ copiado para classpath:/static).
 * O app é uma SPA: ao abrir ou recarregar uma rota como /login ou /detalhe/10,
 * o navegador precisa receber o index.html, e o roteamento acontece no cliente.
 * A raiz "/" é atendida pela welcome page padrão do Spring Boot (static/index.html).
 */
@Hidden
@Controller
public class FrontendController {

    public static final String[] ROUTES = {
            "/login",
            "/cadastro",
            "/perfil",
            "/sobre",
            "/gestor",
            "/nova-ocorrencia",
            "/detalhe/{id}",
            "/editar/{id}"
    };

    @GetMapping({
            "/login",
            "/cadastro",
            "/perfil",
            "/sobre",
            "/gestor",
            "/nova-ocorrencia",
            "/detalhe/{id}",
            "/editar/{id}"
    })
    public String app() {
        return "forward:/index.html";
    }
}

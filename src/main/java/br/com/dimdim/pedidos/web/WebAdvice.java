package br.com.dimdim.pedidos.web;

import br.com.dimdim.pedidos.domain.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class WebAdvice {
    private final Environment environment;
    public WebAdvice(Environment environment) { this.environment = environment; }

    @ModelAttribute("modoDemo")
    public boolean modoDemo() { return environment.acceptsProfiles(Profiles.of("demo")); }

    @ExceptionHandler(RegistroNaoEncontradoException.class)
    public String naoEncontrado(RegistroNaoEncontradoException ex, Model model, HttpServletResponse response) {
        return erro(model, response, 404, "Registro não encontrado", ex.getMessage());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public String regra(RegraNegocioException ex, Model model, HttpServletResponse response) {
        return erro(model, response, 409, "Não foi possível concluir", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String parametroInvalido(Model model, HttpServletResponse response) {
        return erro(model, response, 400, "Endereço inválido", "Verifique o identificador informado e tente novamente.");
    }

    private static String erro(Model model, HttpServletResponse response, int status, String titulo, String mensagem) {
        response.setStatus(status);
        model.addAttribute("tituloErro", titulo);
        model.addAttribute("mensagemErro", mensagem);
        return "erro";
    }
}

package fr.kernelis.illico.securite.api;

import fr.kernelis.illico.securite.application.ConnexionService;
import fr.kernelis.illico.securite.application.EtapeMotDePasseValidee;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthentificationController {

    static final String ATTRIBUT_ETAPE_MOT_DE_PASSE = "illico.connexion.etape-mot-de-passe";

    private final ConnexionService connexionService;

    AuthentificationController(ConnexionService connexionService) {
        this.connexionService = connexionService;
    }

    /** Appelé par Angular au démarrage : sa lecture dépose le cookie XSRF-TOKEN. */
    @GetMapping("/csrf")
    JetonCsrf jetonCsrf(CsrfToken jeton) {
        return new JetonCsrf(jeton.getHeaderName(), jeton.getToken());
    }

    /** Première étape : identifiant et mot de passe. */
    @PostMapping("/connexion")
    ResponseEntity<ReponseConnexion> connexion(@Valid @RequestBody DemandeConnexion demande, HttpServletRequest requete) {
        return connexionService.verifierMotDePasse(demande.identifiant(), demande.motDePasse())
            .map(utilisateurId -> {
                requete.getSession(true);
                // Nouvel identifiant de session : protège contre la fixation de session
                requete.changeSessionId();
                requete.getSession().setAttribute(
                    ATTRIBUT_ETAPE_MOT_DE_PASSE, new EtapeMotDePasseValidee(utilisateurId, Instant.now()));
                return ResponseEntity.ok(ReponseConnexion.secondFacteurRequis());
            })
            .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ReponseConnexion.refusee()));
    }
}

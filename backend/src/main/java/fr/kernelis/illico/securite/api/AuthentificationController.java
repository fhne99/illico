package fr.kernelis.illico.securite.api;

import fr.kernelis.illico.securite.application.ConnexionService;
import fr.kernelis.illico.securite.application.EtapeMotDePasseValidee;
import fr.kernelis.illico.securite.application.UtilisateurConnecte;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
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
    private final SecurityContextRepository depotContexte;
    private final Clock horloge;
    private final SecurityContextHolderStrategy strategieContexte = SecurityContextHolder.getContextHolderStrategy();

    AuthentificationController(ConnexionService connexionService, SecurityContextRepository depotContexte, Clock horloge) {
        this.connexionService = connexionService;
        this.depotContexte = depotContexte;
        this.horloge = horloge;
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
                    ATTRIBUT_ETAPE_MOT_DE_PASSE, new EtapeMotDePasseValidee(utilisateurId, horloge.instant()));
                return ResponseEntity.ok(ReponseConnexion.secondFacteurRequis());
            })
            .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ReponseConnexion.refusee()));
    }

    /** Seconde étape : code de l'application d'authentification. */
    @PostMapping("/second-facteur")
    ResponseEntity<ReponseConnexion> secondFacteur(@Valid @RequestBody DemandeSecondFacteur demande,
                                                  HttpServletRequest requete, HttpServletResponse reponse) {
        HttpSession session = requete.getSession(false);
        EtapeMotDePasseValidee etape = session == null
            ? null
            : (EtapeMotDePasseValidee) session.getAttribute(ATTRIBUT_ETAPE_MOT_DE_PASSE);

        return connexionService.verifierSecondFacteur(etape, demande.code())
            .map(utilisateur -> {
                session.removeAttribute(ATTRIBUT_ETAPE_MOT_DE_PASSE);
                // Encore un nouvel identifiant de session, cette fois pour la session connectée
                requete.changeSessionId();
                ouvrirSession(utilisateur, requete, reponse);
                return ResponseEntity.ok(ReponseConnexion.connecte());
            })
            .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ReponseConnexion.codeRefuse()));
    }

    /** Permet à Angular de savoir qui est connecté, sans jamais manipuler de jeton. */
    @GetMapping("/moi")
    ProfilConnecte moi(@AuthenticationPrincipal UtilisateurConnecte utilisateur) {
        return new ProfilConnecte(utilisateur.identifiant(), utilisateur.role());
    }

    private void ouvrirSession(UtilisateurConnecte utilisateur, HttpServletRequest requete, HttpServletResponse reponse) {
        var authentification = UsernamePasswordAuthenticationToken.authenticated(
            utilisateur, null, List.of(new SimpleGrantedAuthority("ROLE_" + utilisateur.role().name())));
        SecurityContext contexte = strategieContexte.createEmptyContext();
        contexte.setAuthentication(authentification);
        strategieContexte.setContext(contexte);
        depotContexte.saveContext(contexte, requete, reponse);
    }
}

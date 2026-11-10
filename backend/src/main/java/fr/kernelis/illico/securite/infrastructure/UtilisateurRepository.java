package fr.kernelis.illico.securite.infrastructure;

import fr.kernelis.illico.securite.domain.Utilisateur;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID> {

    /** L'identifiant doit être fourni normalisé (voir Utilisateur.normaliser). */
    Optional<Utilisateur> findByIdentifiant(String identifiant);
}

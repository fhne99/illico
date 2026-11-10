package fr.kernelis.illico.securite.infrastructure;

import fr.kernelis.illico.securite.domain.EvenementSecurite;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Dépôt en ajout seul : il hérite de Repository, et non de JpaRepository,
 * pour n'exposer ni modification ni suppression. Le journal ne peut qu'être complété ou consulté.
 */
public interface EvenementSecuriteRepository extends Repository<EvenementSecurite, UUID> {

    EvenementSecurite save(EvenementSecurite evenement);

    List<EvenementSecurite> findByUtilisateurIdOrderBySurvenuLeAsc(UUID utilisateurId);

    List<EvenementSecurite> findByIdentifiantSaisiOrderBySurvenuLeAsc(String identifiantSaisi);
}

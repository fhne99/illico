package fr.kernelis.illico;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.web.bind.annotation.RestController;

/**
 * Règles d'architecture du monolithe modulaire.
 * Organisation attendue : fr.kernelis.illico.<module>.{api, application, domain, infrastructure}
 * allowEmptyShould(true) : les règles restent valides tant que les modules ne sont pas encore écrits.
 */
@AnalyzeClasses(packages = "fr.kernelis.illico", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /** Les modules (demandes, interventions, referentiel, securite...) ne forment aucun cycle de dépendances. */
    @ArchTest
    static final ArchRule modules_sans_dependance_circulaire = slices()
        .matching("fr.kernelis.illico.(*)..")
        .should().beFreeOfCycles()
        .allowEmptyShould(true);

    /** Le domaine porte les règles métier : il ne dépend ni du web, ni de l'infrastructure, ni de Spring. */
    @ArchTest
    static final ArchRule domaine_independant_des_couches_techniques = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("..api..", "..infrastructure..", "org.springframework..")
        .allowEmptyShould(true);

    /** Les contrôleurs passent par la couche application : jamais d'accès direct aux dépôts de données. */
    @ArchTest
    static final ArchRule controleurs_sans_acces_direct_aux_depots = noClasses()
        .that().resideInAPackage("..api..")
        .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
        .allowEmptyShould(true);

    /** Les points d'entrée REST sont regroupés dans les packages api, ce qui facilite leur sécurisation et leur revue. */
    @ArchTest
    static final ArchRule controleurs_rest_dans_les_packages_api = classes()
        .that().areAnnotatedWith(RestController.class)
        .should().resideInAPackage("..api..")
        .allowEmptyShould(true);
}

package com.example.demo.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * ArchUnit tests enforcing Clean Architecture rules for BFF Service.
 *
 * <p>ARCHITECTURE ADAPTATION NOTE:
 * BFF architecture fundamentally differs from domain services:
 * <ul>
 *   <li>BFF has NO domain layer (thin orchestration/aggregation only)
 *   <li>Domain logic resides in downstream services (Claims Service)
 *   <li>BFF uses application layer for use case orchestration
 * </ul>
 *
 * <p>Therefore, these tests are ADAPTED (not identical) to validate:
 * <ul>
 *   <li>Application layer independence from infrastructure concerns
 *   <li>Dependencies flow INWARD: adapter → application
 *   <li>No JPA in application layer (BFF doesn't manage persistence)
 *   <li>Naming conventions (no prohibited abbreviations)
 *   <li>Constructor injection only (no field injection)
 *   <li>No circular dependencies
 *   <li>No static mutable state
 * </ul>
 */
@AnalyzeClasses(packages = "com.example.demo")
public class ArchitectureTest {

    // ==================== LAYER DEPENDENCY RULES ====================

    /**
     * TC-037 (adapted for BFF): Application layer should not depend on adapters.
     */
    @ArchTest
    static final ArchRule applicationLayerShouldNotDependOnInfrastructure =
        noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..adapter..")
            .allowEmptyShould(true)
            .as("Application layer should not depend on adapter implementations");

    /**
     * TC-038 (adapted for BFF): Application layer should NOT have JPA dependencies.
     */
    @ArchTest
    static final ArchRule applicationLayerShouldNotDependOnJPA =
        noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("jakarta.persistence..", "javax.persistence..")
            .allowEmptyShould(true)
            .as("BFF application layer should not manage persistence directly");

    /**
     * Layered Architecture: Dependencies must flow INWARD.
     */
    @ArchTest
    static final ArchRule dependenciesFlowInward =
        layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .optionalLayer("Application").definedBy("..application..")
            .optionalLayer("Adapter").definedBy("..adapter..")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter")
            .as("Dependencies must flow inward: adapter → application");

    // ==================== NAMING CONVENTIONS ====================

    /**
     * No "Repo" abbreviation - use "Repository".
     * Allows full word "Repository" but catches "Repo" used as abbreviation.
     */
    @ArchTest
    static final ArchRule no_repo_abbreviation =
        noClasses().that().resideOutsideOfPackage("..generated..")
            .should(new com.tngtech.archunit.lang.ArchCondition<com.tngtech.archunit.core.domain.JavaClass>("not use 'Repo' abbreviation") {
                @Override
                public void check(com.tngtech.archunit.core.domain.JavaClass javaClass, com.tngtech.archunit.lang.ConditionEvents events) {
                    String name = javaClass.getSimpleName();
                    String stripped = name.replace("Repository", "").replace("Report", "").replace("Reporter", "");
                    if (stripped.contains("Repo")) {
                        events.add(com.tngtech.archunit.lang.SimpleConditionEvent.violated(
                            javaClass, "Class " + name + " uses 'Repo' abbreviation (use Repository)"));
                    }
                }
            })
            .allowEmptyShould(true)
            .as("Class names must not contain 'Repo' abbreviation (use Repository)");

    /**
     * No "Svc" abbreviation - use "Service".
     */
    @ArchTest
    static final ArchRule no_svc_abbreviation =
        noClasses().that().resideOutsideOfPackage("..generated..")
            .should().haveSimpleNameContaining("Svc")
            .allowEmptyShould(true)
            .as("Class names must not contain 'Svc' abbreviation (use Service)");

    /**
     * No "Usr" abbreviation - use "User".
     */
    @ArchTest
    static final ArchRule no_usr_abbreviation =
        noClasses().that().resideOutsideOfPackage("..generated..")
            .should().haveSimpleNameContaining("Usr")
            .allowEmptyShould(true)
            .as("Class names must not contain 'Usr' abbreviation (use User)");

    /**
     * No "Impl" abbreviation - use descriptive names.
     * Excludes MapStruct-generated (*MapperImpl).
     *
     * <p>TODO: Existing *UseCaseImpl and adapter *Impl classes should be renamed to
     * descriptive names (e.g., KeycloakLogoutUseCase, KeycloakTokenProvider).
     * These are excluded temporarily to allow incremental adoption.
     */
    @ArchTest
    static final ArchRule no_impl_abbreviation =
        noClasses().that().resideOutsideOfPackage("..generated..")
            .and().haveSimpleNameNotEndingWith("MapperImpl")
            .and().haveSimpleNameNotEndingWith("UseCaseImpl")
            .and().haveSimpleNameNotEndingWith("ProviderImpl")
            .and().haveSimpleNameNotEndingWith("FacadeImpl")
            .should().haveSimpleNameContaining("Impl")
            .allowEmptyShould(true)
            .as("Class names must not contain 'Impl' abbreviation (use descriptive names)");

    // ==================== CONSTRUCTOR INJECTION ====================

    /**
     * No @Autowired field injection allowed.
     * Test classes are excluded.
     */
    @ArchTest
    static final ArchRule no_field_injection_with_autowired =
        fields().that()
            .areDeclaredInClassesThat().resideOutsideOfPackage("..generated..")
            .and().areDeclaredInClassesThat().resideOutsideOfPackage("..integration..")
            .and().areDeclaredInClassesThat().resideOutsideOfPackage("..architecture..")
            .and().areDeclaredInClassesThat().haveSimpleNameNotEndingWith("Test")
            .and().areDeclaredInClassesThat().haveSimpleNameNotEndingWith("IntegrationTest")
            .should().notBeAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
            .allowEmptyShould(true)
            .as("Production fields must not use @Autowired - use constructor injection instead (test classes excluded)");

    /**
     * No @Inject field injection allowed.
     */
    @ArchTest
    static final ArchRule no_field_injection_with_inject =
        fields().that()
            .areDeclaredInClassesThat().resideOutsideOfPackage("..generated..")
            .and().areDeclaredInClassesThat().haveSimpleNameNotEndingWith("Test")
            .and().areDeclaredInClassesThat().haveSimpleNameNotEndingWith("IntegrationTest")
            .should().notBeAnnotatedWith("jakarta.inject.Inject")
            .allowEmptyShould(true)
            .as("Production fields must not use @Inject - use constructor injection instead (test classes excluded)");

    // ==================== STRUCTURAL INTEGRITY ====================

    /**
     * No circular dependencies in application packages.
     */
    @ArchTest
    static final ArchRule no_circular_dependencies_in_application =
        com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices()
            .matching("com.example.demo.application.(**)")
            .should().beFreeOfCycles()
            .allowEmptyShould(true)
            .as("Application packages should not have circular dependencies");

    /**
     * No static mutable state in production code.
     */
    @ArchTest
    static final ArchRule no_static_mutable_state =
        fields().that()
            .areDeclaredInClassesThat().resideOutsideOfPackage("..generated..")
            .and().areDeclaredInClassesThat().haveSimpleNameNotEndingWith("Test")
            .and().areDeclaredInClassesThat().haveSimpleNameNotEndingWith("IntegrationTest")
            .and().areStatic()
            .should().beFinal()
            .allowEmptyShould(true)
            .as("Static fields must be final (constants only) - no mutable static state allowed (test classes excluded)");

    // ==================== CONTRACT BOUNDARY RULES ====================
    // Derived from CMX Mobile BFF contract mismatch audit (2026-03-08).
    // These rules prevent the exact anti-patterns that caused runtime crashes
    // and silent data loss in a real production service despite 1600+ passing tests.
    // See: docs/devstandards/backend/knowledge/contract-boundary-rules.md

    /**
     * Outbound adapter classes (adapter.out.*) must NOT import inbound web adapter classes
     * (adapter.in.*). The Claims Service client deserialises into generated DTOs and maps
     * to domain records. Only the web adapter layer (adapter.in) maps to response DTOs.
     *
     * Violation: using a BFF web response DTO as the deserialization target for a
     * Claims Service HTTP response — this causes runtime Jackson failures when types diverge.
     */
    @ArchTest
    static final ArchRule outbound_adapters_must_not_import_inbound_web_dtos =
        noClasses().that()
            .resideInAPackage("..adapter.out..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapter.in.web..")
            .allowEmptyShould(true)
            .as("Outbound adapter classes must not import inbound web adapter classes. "
                + "Claims Service clients must deserialise into generated DTOs "
                + "(com.example.bff.adapter.out.claims.generated.model.*), not web response DTOs. "
                + "See: docs/devstandards/backend/knowledge/contract-boundary-rules.md");

    /**
     * Prohibit UUID.hashCode() as an ID conversion strategy.
     * UUID.hashCode() is lossy (hash collisions), non-reversible, and non-unique —
     * clients cannot use the returned ID to call back (e.g. getClaim, updateStatus).
     */
    @ArchTest
    static final ArchRule no_uuid_hashcode_for_id_conversion =
        noClasses().that()
            .resideInAPackage("..adapter.out..")
            .should()
            .callMethod(java.util.UUID.class, "hashCode")
            .allowEmptyShould(true)
            .as("UUID.hashCode() must not be used for ID conversion — it is lossy and non-reversible. "
                + "If Claims Service returns UUID and BFF expects Long, HALT and report the mismatch. "
                + "See: docs/devstandards/backend/knowledge/contract-boundary-rules.md");
}

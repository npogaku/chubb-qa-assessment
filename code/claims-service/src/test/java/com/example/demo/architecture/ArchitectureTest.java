package com.example.demo.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * ArchUnit tests enforcing Clean Architecture rules for Claims Service.
 *
 * <p>Validates:
 * <ul>
 *   <li>Layer dependency rules (Adapter → Application → Domain)
 *   <li>Domain purity (no framework dependencies)
 *   <li>Naming conventions (no prohibited abbreviations)
 *   <li>Constructor injection only (no field injection)
 *   <li>No circular dependencies between packages
 *   <li>No static mutable state
 *   <li>JPA entity separation from domain entities
 *   <li>Ports & Adapters pattern enforcement
 * </ul>
 *
 * <p>Test Cases:
 * <ul>
 *   <li>TC-037: Domain layer independence from Spring
 *   <li>TC-038: Domain layer independence from JPA
 *   <li>TC-039: Repository adapters implement domain interfaces
 *   <li>Layered architecture: Dependencies flow inward
 * </ul>
 */
@AnalyzeClasses(packages = "com.example.demo")
public class ArchitectureTest {

    // ==================== LAYER DEPENDENCY RULES ====================

    /**
     * Validates Clean Architecture layer dependencies.
     *
     * <p>Rules:
     * <ul>
     *   <li>Domain layer may only be accessed by Application, Adapter, Config, Fixtures, TestUtil
     *   <li>Application layer may only be accessed by Adapter, Config, Fixtures
     *   <li>Adapter layer is a leaf layer
     * </ul>
     *
     * <p>Config and Fixtures are infrastructure-level packages that legitimately
     * reference all layers. Empty layers allowed for greenfield development.
     */
    @ArchTest
    static final ArchRule layer_dependencies_must_be_respected =
        layeredArchitecture()
            .consideringAllDependencies()
            .optionalLayer("Domain").definedBy("..domain..")
            .optionalLayer("Application").definedBy("..application..")
            .optionalLayer("Adapter").definedBy("..adapter..")
            .optionalLayer("Config").definedBy("..config..")
            .optionalLayer("Fixtures").definedBy("..fixtures..")
            .optionalLayer("TestUtil").definedBy("..testutil..")
            .optionalLayer("Contract").definedBy("..contract..")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapter", "Config", "Fixtures", "TestUtil")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter", "Config", "Fixtures")
            .ignoreDependency("..DemoApplication", "org.springframework.boot.SpringApplication")
            .as("Clean Architecture layer dependencies must be respected: Adapter → Application → Domain");

    // ==================== DOMAIN PURITY ====================

    /**
     * TC-037: Domain layer must have ZERO framework dependencies.
     *
     * <p>Domain must not depend on:
     * <ul>
     *   <li>Spring Framework (org.springframework)
     *   <li>JPA/Hibernate (javax.persistence, jakarta.persistence)
     *   <li>Jackson (com.fasterxml.jackson)
     * </ul>
     *
     * <p>This ensures domain remains framework-agnostic and portable.
     * Lombok is excluded from this rule as it is compile-time only and generates
     * no runtime dependency. However, pure domain POJOs are preferred.
     */
    @ArchTest
    static final ArchRule domain_must_not_depend_on_frameworks =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "org.springframework..",
                "javax.persistence..",
                "jakarta.persistence..",
                "com.fasterxml.jackson..")
            .allowEmptyShould(true)
            .as("Domain layer must have ZERO framework dependencies (no Spring, JPA, Jackson)");

    /**
     * TC-038: Domain entities must NOT be JPA entities.
     *
     * <p>Domain entities should be pure POJOs without persistence annotations.
     * JPA entities belong in infrastructure.adapter.persistence layer.
     */
    @ArchTest
    static final ArchRule domain_entities_must_not_be_jpa_entities =
        noClasses().that().resideInAPackage("..domain.model..")
            .should().beAnnotatedWith("jakarta.persistence.Entity")
            .orShould().beAnnotatedWith("javax.persistence.Entity")
            .allowEmptyShould(true)
            .as("Domain entities must be pure POJOs (JPA entities belong in infrastructure layer)");

    // ==================== PORTS & ADAPTERS ====================

    /**
     * TC-039: Repository adapters should implement domain interfaces.
     *
     * <p>Enforces the port-adapter pattern where persistence implementations
     * (adapters) must implement interfaces from the domain layer.
     */
    @ArchTest
    static final ArchRule repositoryAdaptersShouldImplementDomainInterfaces =
        classes().that().resideInAPackage("..adapter.out.persistence..")
            .and().haveSimpleNameEndingWith("Adapter")
            .should().beAssignableTo(new DescribedPredicate<JavaClass>("domain interfaces") {
                @Override
                public boolean test(JavaClass javaClass) {
                    return javaClass.getInterfaces().stream()
                        .anyMatch(iface -> iface.getName().contains(".domain."));
                }
            })
            .allowEmptyShould(true)
            .as("Persistence adapters must implement domain repository interfaces");

    /**
     * Output ports must be interfaces.
     */
    @ArchTest
    static final ArchRule ports_and_adapters_pattern_must_be_followed =
        classes().that().resideInAPackage("..domain.port.output..")
            .should().beInterfaces()
            .allowEmptyShould(true)
            .as("Output ports must be interfaces (implemented by infrastructure adapters)");

    /**
     * Use cases must depend on port interfaces, not directly on infrastructure adapters.
     */
    @ArchTest
    static final ArchRule use_cases_must_not_depend_on_infrastructure_adapters =
        noClasses().that().resideInAPackage("..application.usecase..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapter..")
            .allowEmptyShould(true)
            .as("Use cases must depend on port interfaces, not directly on infrastructure adapters");

    // ==================== NAMING CONVENTIONS ====================

    /**
     * No "Repo" abbreviation - use "Repository".
     * Allows full word "Repository" but catches "Repo" used as abbreviation.
     */
    @ArchTest
    static final ArchRule no_repo_abbreviation =
        noClasses().that().resideOutsideOfPackage("..generated..")
            .should(new com.tngtech.archunit.lang.ArchCondition<JavaClass>("not use 'Repo' abbreviation") {
                @Override
                public void check(JavaClass javaClass, com.tngtech.archunit.lang.ConditionEvents events) {
                    String name = javaClass.getSimpleName();
                    // Match "Repo" but not "Repository" or "Report"
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
     * Excludes MapStruct-generated classes (*MapperImpl).
     */
    @ArchTest
    static final ArchRule no_impl_abbreviation =
        noClasses().that().resideOutsideOfPackage("..generated..")
            .and().haveSimpleNameNotEndingWith("MapperImpl")
            .should().haveSimpleNameContaining("Impl")
            .allowEmptyShould(true)
            .as("Class names must not contain 'Impl' abbreviation (use descriptive names)");

    // ==================== CONSTRUCTOR INJECTION ====================

    /**
     * No @Autowired field injection allowed.
     *
     * <p>All Spring dependencies must be injected via constructor parameters with final fields.
     * Test classes are excluded as they use @Autowired for Spring Boot test injection.
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
     * No circular dependencies between domain aggregates.
     *
     * <p>Uses single-level matching so that sub-packages within an aggregate
     * (e.g., domain.claim and domain.claim.events) are treated as one slice.
     * This correctly allows aggregate roots to create their own domain events.
     */
    @ArchTest
    static final ArchRule no_circular_dependencies_between_domain_aggregates =
        com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices()
            .matching("com.example.demo.domain.(*)")
            .should().beFreeOfCycles()
            .allowEmptyShould(true)
            .as("Domain aggregates should not have circular dependencies");

    /**
     * No circular dependencies between application use cases.
     */
    @ArchTest
    static final ArchRule no_circular_dependencies_in_application =
        com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices()
            .matching("com.example.demo.application.(*)")
            .should().beFreeOfCycles()
            .allowEmptyShould(true)
            .as("Application packages should not have circular dependencies");

    /**
     * No static mutable state in production code.
     *
     * <p>Static mutable state (non-final static fields) creates global state that is
     * hard to test, causes concurrency issues, and violates singleton patterns.
     * Constants (static final) are allowed.
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
}

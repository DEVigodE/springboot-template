package com.template.auth;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Clean Architecture boundaries, enforced at build time rather than by
 * convention. Dependencies only point inward; `config` is the
 * composition root and the only package allowed to see everything.
 */
@AnalyzeClasses(packages = "com.template.auth", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layers = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy("..domain..")
            .layer("Application").definedBy("..application..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            .layer("Presentation").definedBy("..presentation..")
            .layer("Config").definedBy("..config..")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Presentation", "Config")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure", "Presentation", "Config")
            .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Config")
            .whereLayer("Presentation").mayOnlyBeAccessedByLayers("Config")
            // Only typed settings (AppProperties) are consumed from here by the outer layers.
            .whereLayer("Config").mayOnlyBeAccessedByLayers("Infrastructure", "Presentation");

    @ArchTest
    static final ArchRule domainIsFrameworkFree = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "jakarta.validation..", "tools.jackson..",
                    "com.fasterxml..", "..application..", "..infrastructure..", "..presentation..", "..config..");

    @ArchTest
    static final ArchRule applicationKnowsNoAdapters = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..", "org.springframework.data..", "org.springframework.web..",
                    "org.springframework.security..", "org.springframework.http..", "jakarta.servlet..",
                    "tools.jackson..", "com.fasterxml..");
}

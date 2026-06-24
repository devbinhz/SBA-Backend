package com.bookverse.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.bookverse")
class LayerDependencyTest {

    @ArchTest
    static final ArchRule controller_must_not_depend_on_repository = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAnyPackage("..repository..");

    @Test
    void architectureRuleLoadsWithoutErrors() {
        new ClassFileImporter().importPackages("com.bookverse");
    }
}


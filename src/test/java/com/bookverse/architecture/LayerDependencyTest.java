package com.bookverse.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

@AnalyzeClasses(packages = "com.bookverse")
class LayerDependencyTest {

    @ArchTest
    static final ArchRule controller_must_not_depend_on_repository = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAnyPackage("..repository..");

    @ArchTest
    static final ArchRule controller_must_not_depend_on_entity = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAnyPackage("..entity..");

    @ArchTest
    static final ArchRule controller_methods_must_not_return_entity = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..controller..")
            .should().haveRawReturnType(resideInEntityPackage());

    @ArchTest
    static final ArchRule service_impl_classes_must_reside_in_impl_package = classes()
            .that().haveSimpleNameEndingWith("ServiceImpl")
            .should().resideInAPackage("..service..impl..");

    @ArchTest
    static final ArchRule service_interfaces_must_not_reside_in_impl_package = classes()
            .that().haveSimpleNameEndingWith("Service")
            .and().areInterfaces()
            .should().resideOutsideOfPackage("..service..impl..");

    private static com.tngtech.archunit.base.DescribedPredicate<com.tngtech.archunit.core.domain.JavaClass> resideInEntityPackage() {
        return com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage("..entity..");
    }

    @Test
    void architectureRuleLoadsWithoutErrors() {
        new ClassFileImporter().importPackages("com.bookverse");
    }
}


package com.bookverse.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

public class LayerDependencyTest {

    private static final JavaClasses PROJECT_CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.bookverse");

    private static final ArchRule CONTROLLER_MUST_NOT_DEPEND_ON_REPOSITORY = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAnyPackage("..repository..");

    private static final ArchRule CONTROLLER_MUST_NOT_DEPEND_ON_ENTITY = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAnyPackage("..entity..");

    private static final ArchRule CONTROLLER_METHODS_MUST_NOT_RETURN_ENTITY = noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..controller..")
            .should().haveRawReturnType(resideInEntityPackage());

    private static final ArchRule SERVICE_IMPL_CLASSES_MUST_RESIDE_IN_IMPL_PACKAGE = classes()
            .that().haveSimpleNameEndingWith("ServiceImpl")
            .should().resideInAPackage("..service..impl..");

    private static final ArchRule SERVICE_INTERFACES_MUST_NOT_RESIDE_IN_IMPL_PACKAGE = classes()
            .that().haveSimpleNameEndingWith("Service")
            .and().areInterfaces()
            .should().resideOutsideOfPackage("..service..impl..");

    private static com.tngtech.archunit.base.DescribedPredicate<com.tngtech.archunit.core.domain.JavaClass> resideInEntityPackage() {
        return com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage("..entity..");
    }

    @Test
    void controllerMustNotDependOnRepository() {
        CONTROLLER_MUST_NOT_DEPEND_ON_REPOSITORY.check(PROJECT_CLASSES);
    }

    @Test
    void controllerMustNotDependOnEntity() {
        CONTROLLER_MUST_NOT_DEPEND_ON_ENTITY.check(PROJECT_CLASSES);
    }

    @Test
    void controllerMethodsMustNotReturnEntity() {
        CONTROLLER_METHODS_MUST_NOT_RETURN_ENTITY.check(PROJECT_CLASSES);
    }

    @Test
    void serviceImplementationsMustResideInImplPackage() {
        SERVICE_IMPL_CLASSES_MUST_RESIDE_IN_IMPL_PACKAGE.check(PROJECT_CLASSES);
    }

    @Test
    void serviceInterfacesMustNotResideInImplPackage() {
        SERVICE_INTERFACES_MUST_NOT_RESIDE_IN_IMPL_PACKAGE.check(PROJECT_CLASSES);
    }
}

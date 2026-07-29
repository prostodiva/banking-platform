package com.bankapp;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "com.bankapp",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_has_no_framework_behavior = noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("org.springframework..", "jakarta.transaction..");

    @ArchTest
    static final ArchRule domain_does_not_depend_on_outer_layers = noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..application..", "..api..", "..infrastructure..");

    @ArchTest
    static final ArchRule slices_do_not_depend_on_each_other = slices()
        .matching("com.bankapp.(*)..")
        .should()
        .notDependOnEachOther()
        .ignoreDependency(
            DescribedPredicate.alwaysTrue(),
            JavaClass.Predicates.resideInAnyPackage("com.bankapp.shared..")
        );
}

package com.bankapp;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;

@AnalyzeClasses(
    packages = "com.bankapp",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

    private static final String ROOT = "com.bankapp.";

    /** The context every other context is allowed to depend on. */
    private static final String SHARED = "shared";

    /** Layers a context keeps to itself. Everything else it may publish. */
    private static final Set<String> INTERNAL_LAYERS = Set.of(
        "domain",
        "infrastructure",
        "api"
    );

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

    /**
     * Contexts stay independent, with two carve-outs: {@code shared}, and another
     * context's published inbound ports (ADR-003 §2, e.g. payments calling
     * {@code accounts.application.port.AccountLedger}).
     */
    @ArchTest
    static final ArchRule slices_do_not_depend_on_each_other = slices()
        .matching("com.bankapp.(*)..")
        .should()
        .notDependOnEachOther()
        .ignoreDependency(
            DescribedPredicate.alwaysTrue(),
            JavaClass.Predicates.resideInAnyPackage("com.bankapp.shared..")
        )
        .ignoreDependency(
            DescribedPredicate.alwaysTrue(),
            JavaClass.Predicates.resideInAnyPackage(
                "com.bankapp.*.application.port.."
            )
        );

    /**
     * The other half of ADR-001's boundary rule, which the slice rule above only
     * ever expressed as a side effect: the carve-out is for published ports and
     * nothing else. A context reaching into another's {@code domain},
     * {@code infrastructure} or {@code api} is forbidden explicitly, so loosening
     * the ignore above cannot quietly re-permit it.
     */
    @ArchTest
    static final ArchRule contexts_reach_each_other_only_through_published_ports =
        noClasses()
            .should(dependOnAnotherContextsInternals())
            .because(
                "a context may depend on another's application.port.., never on " +
                "its domain.., infrastructure.. or api.. (ADR-003 decision 2)"
            );

    private static ArchCondition<JavaClass> dependOnAnotherContextsInternals() {
        return new ArchCondition<>("depend on another context's internals") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                String origin = contextOf(item);
                if (origin == null) {
                    return;
                }
                for (Dependency dependency : item.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    String targetContext = contextOf(target);
                    if (
                        targetContext == null ||
                        targetContext.equals(origin) ||
                        targetContext.equals(SHARED)
                    ) {
                        continue;
                    }
                    if (INTERNAL_LAYERS.contains(layerOf(target))) {
                        events.add(
                            SimpleConditionEvent.satisfied(
                                dependency,
                                dependency.getDescription()
                            )
                        );
                    }
                }
            }
        };
    }

    /** {@code com.bankapp.accounts.domain.Account} -> {@code "accounts"}. */
    private static String contextOf(JavaClass javaClass) {
        return segmentAfterRoot(javaClass, 0);
    }

    /** {@code com.bankapp.accounts.domain.Account} -> {@code "domain"}. */
    private static String layerOf(JavaClass javaClass) {
        return segmentAfterRoot(javaClass, 1);
    }

    private static String segmentAfterRoot(JavaClass javaClass, int index) {
        String packageName = javaClass.getPackageName();
        if (!packageName.startsWith(ROOT)) {
            return null;
        }
        String[] segments = packageName.substring(ROOT.length()).split("\\.");
        return index < segments.length ? segments[index] : null;
    }
}

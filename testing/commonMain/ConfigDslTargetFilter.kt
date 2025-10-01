package dev.dokky.zerojson.framework

fun TestConfigBuilder.includeOnly(target0: TestTarget, vararg targets: TestTarget) {
    includeOnly(targets.toList() + target0)
}

inline fun TestConfigBuilder.includeTargetIf(
    targetFilter: (TestTarget) -> Boolean
) {
    includeOnly(TestTarget.entries.filter(targetFilter))
}

fun TestConfigBuilder.exclude(target0: TestTarget, vararg targets: TestTarget) {
    exclude(targets.toList() + target0)
}

inline fun TestConfigBuilder.excludeTargetIf(
    targetFilter: (TestTarget) -> Boolean
) {
    exclude(TestTarget.entries.filter(targetFilter))
}
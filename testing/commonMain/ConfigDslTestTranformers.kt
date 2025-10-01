package dev.dokky.zerojson.framework

inline fun <reified T: TestInputTransformer> TestConfigBuilder.disable() {
    disableTransformerIf { it is T }
}

inline fun <reified T: TestInputTransformer> TestConfigBuilder.disableIf(crossinline predicate: (T) -> Boolean) {
    disableTransformerIf { it is T && predicate(it) }
}
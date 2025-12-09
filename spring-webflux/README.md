# Spring WebFlux integration

## Setup

```kotlin
implementation("io.github.adokky:zero-json-spring-webflux:0.2.0")
```

Compatible with Spring Framework 5.3+

## Usage

```kotlin
@Configuration
interface ZeroJsonConfig {
    @Bean
    fun webFluxConfigurer() = object : WebFluxConfigurer {
        override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
            // it is highly recommended to limit the payload size upfront
            configurer.defaultCodecs().maxInMemorySize(256 * 1024) // 256 Kb
            
            val json = ZeroJson {
                // ...
            }
            val reader = ZeroJsonHttpMessageReader(json)
            configurer.customCodecs().register(reader)
        }
    }
}
```
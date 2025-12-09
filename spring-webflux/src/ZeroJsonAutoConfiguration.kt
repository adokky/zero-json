//package dev.dokky.zerojson
//
//import org.springframework.http.codec.HttpMessageReader
//import org.springframework.http.codec.ServerCodecConfigurer
//
//@Configuration
//class WebConfig : WebFluxConfigurer {
//
//    override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
//        configurer.defaultCodecs().maxInMemorySize(1024 * 1024) // 1MB
//    }
//}
//
//@Configuration
//class ZeroJsonConfiguration {
//
//    @Bean
//    fun zeroJsonMessageReader(): HttpMessageReader<Any> {
//        return ZeroJsonHttpMessageReader()
//    }
//}
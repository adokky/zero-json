package dev.dokky.zerojson.framework

@Suppress("ObjectInheritsException", "JavaIoSerializableObjectMustHaveReadResolve")
private object TestFail: AssertionError("test failed") {
    override fun fillInStackTrace(): Throwable? = null
}

actual fun prettyFail(message: String) {
    System.err.println(message)
    throw TestFail
}
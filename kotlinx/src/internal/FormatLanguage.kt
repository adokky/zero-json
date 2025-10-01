package kotlinx.serialization.json.internal

import kotlinx.serialization.InternalSerializationApi

/**
 * Specifies that an element of the program represents a string that is a source code on a specified language.
 * Code editors may use this annotation to enable syntax highlighting, code completion and other features
 * inside the literals that assigned to the annotated variables, passed as arguments to the annotated parameters,
 * or returned from the annotated methods.
 * <p>
 * This annotation also could be used as a meta-annotation, to define derived annotations for convenience.
 * E.g. the following annotation could be defined to annotate the strings that represent Java methods:
 *
 * <pre>
 *   &#64;Language(value = "JAVA", prefix = "class X{", suffix = "}")
 *   &#64;interface JavaMethod {}
 * </pre>
 * <p>
 * Note that using the derived annotation as meta-annotation is not supported.
 * Meta-annotation works only one level deep.
 */
@InternalSerializationApi
public typealias FormatLanguage = org.intellij.lang.annotations.Language
package io.mockk.proxy.jvm

import io.mockk.proxy.MockKAgentLogger
import io.mockk.proxy.MockKInstantiatior
import io.mockk.proxy.jvm.transformation.CacheKey
import net.bytebuddy.ByteBuddy
import net.bytebuddy.TypeCache
import org.objenesis.ObjenesisStd
import org.objenesis.instantiator.ObjectInstantiator
import java.lang.reflect.Modifier
import java.util.*

class ObjenesisInstantiator(
    private val log: MockKAgentLogger,
    private val byteBuddy: ByteBuddy
) : MockKInstantiatior {
    private val objenesis = ObjenesisStd(false)

    private val typeCache = TypeCache<CacheKey>(TypeCache.Sort.WEAK)

    private val instantiators = Collections.synchronizedMap(WeakHashMap<Class<*>, ObjectInstantiator<*>>())

    override fun <T> instance(cls: Class<T>): T {
        if (cls == Any::class.java) {
            @Suppress("UNCHECKED_CAST")
            return Any() as T
        } else if (cls.isSealedSafe()) {
            cls.getPermittedSubclassesSafe().firstNotNullOfOrNull { subCls ->
                runCatching { instance(subCls) }.getOrNull()
            } ?: error("could not find subclass for sealed class $cls")
        } else if (!Modifier.isFinal(cls.modifiers)) {
            try {
                val instance = instantiateViaProxy(cls)
                if (instance != null) {
                    return instance
                }
            } catch (ex: Exception) {
                log.trace(
                    ex, "Failed to instantiate via proxy " + cls + ". " +
                            "Doing objenesis instantiation"
                )
            }

        }

        return instanceViaObjenesis(cls)
    }

    /**
     * `boolean Class.isSealed()` is only available with JDK 17+. Used via reflection to support
     * builds with previous Java versions as well. This should be refactored to use the actual
     * method once the minimum Java version is 17.
     */
    private fun Class<*>.isSealedSafe(): Boolean =
        javaClass.methods.firstOrNull { it.name == "isSealed" }?.invoke(this) == true

    /**
     * `Class<?>[] Class.getPermittedSubclasses` is only available with JDK 17+. Used via reflection
     * to support builds with previous Java versions as well. This should be refactored to use the
     * actual method once the minimum Java version is 17.
     */
    private fun Class<*>.getPermittedSubclassesSafe(): Array<Class<*>> =
        javaClass.methods.firstOrNull { it.name == "getPermittedSubclasses" }
            ?.let {
                @Suppress("UNCHECKED_CAST")
                it.invoke(this) as Array<Class<*>>
            }
            ?: emptyArray()

    private fun <T> instantiateViaProxy(cls: Class<T>): T? {
        val proxyCls = if (!Modifier.isAbstract(cls.modifiers)) {
            log.trace("Skipping instantiation subsclassing $cls because class is not abstract.")
            cls
        } else {
            log.trace("Instantiating $cls via subclass proxy")

            val classLoader = cls.classLoader
            typeCache.findOrInsert(
                classLoader,
                CacheKey(cls, setOf()),
                {
                    byteBuddy.subclass(cls)
                        .annotateType(*cls.annotations)
                        .make()
                        .load(classLoader)
                        .loaded
                }, classLoader ?: bootstrapMonitor
            )
        }

        return cls.cast(instanceViaObjenesis(proxyCls))
    }


    private fun <T> instanceViaObjenesis(clazz: Class<T>): T {
        log.trace("Creating new empty instance of $clazz")

        return clazz.cast(
            getOrCreateInstantiator(clazz)
                .newInstance()
        )
    }

    private fun <T> getOrCreateInstantiator(clazz: Class<T>) =
        instantiators[clazz] ?: let {
            objenesis.getInstantiatorOf(clazz).also {
                instantiators[clazz] = it
            }
        }

    companion object {
        private val bootstrapMonitor = Any()
    }
}

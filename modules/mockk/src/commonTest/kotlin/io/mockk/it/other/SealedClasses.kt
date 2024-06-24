package io.mockk.it.other


sealed class OtherNode

abstract class AbstractOtherNode : OtherNode()
class OtherRoot(val id: Int) : AbstractOtherNode()
class OtherLeaf(val id: Int) : AbstractOtherNode()

interface OtherFactory {
    fun create(): OtherNode
}

class OtherFactoryImpl : OtherFactory {
    override fun create(): OtherNode = OtherRoot(0)
}

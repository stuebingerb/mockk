package io.mockk.it.other


interface RootInterface
interface LeafInterface

sealed class OtherNode(val id: Int)

abstract class AbstractOtherNode(id: Int) : OtherNode(id)
class OtherRoot(id: Int) : AbstractOtherNode(id), RootInterface
class OtherLeaf(id: Int) : AbstractOtherNode(id), LeafInterface

interface OtherFactory {
    fun create(): OtherNode
}

class OtherFactoryImpl : OtherFactory {
    override fun create(): OtherNode = OtherRoot(0)
}

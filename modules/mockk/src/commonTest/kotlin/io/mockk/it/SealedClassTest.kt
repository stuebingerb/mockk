package io.mockk.it

import io.mockk.every
import io.mockk.it.other.OtherFactory
import io.mockk.it.other.OtherLeaf
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals


class SealedClassTest {

    @Test
    fun serviceReturnsSealedClassImpl() {
        val factory = mockk<Factory> {
            every { create() } returns Leaf(1)
        }

        val result = factory.create()

        assertEquals(Leaf(1), result)
    }

    @Test
    fun serviceAnswersSealedClassImpl() {
        val factory = mockk<Factory> {
            every { create() } answers { Leaf(1) }
        }

        val result = factory.create()

        assertEquals(Leaf(1), result)
    }

    @Test
    fun serviceTakesSealedClassAsInput() {
        val formattedNode = "Formatted node"
        val factory = mockk<Factory> {
            every { format(any()) } answers { formattedNode }
        }

        val result = factory.format(Root(0))

        assertEquals(formattedNode, result)
    }

    companion object {

        sealed class Node

        data class Root(val id: Int) : Node()
        data class Leaf(val id: Int) : Node()

        interface Factory {
            fun create(): Node

            fun format(node: Node): String
        }

        class FactoryImpl : Factory {
            override fun create(): Node = Root(0)

            override fun format(node: Node): String = node.toString()
        }

    }

    @Test
    fun serviceReturnsOtherSealedClassImpl() {
        val otherLeaf = OtherLeaf(1)
        val factory = mockk<OtherFactory> {
            every { create() } returns otherLeaf
        }

        val result = factory.create()

        assertEquals(otherLeaf, result)
    }

    @Test
    fun serviceAnswersOtherSealedClassImpl() {
        val otherLeaf = OtherLeaf(1)
        val factory = mockk<OtherFactory> {
            every { create() } answers { otherLeaf }
        }

        val result = factory.create()

        assertEquals(otherLeaf, result)
    }
}

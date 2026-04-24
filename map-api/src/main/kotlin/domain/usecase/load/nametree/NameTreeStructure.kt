package cz.cvut.fit.gaierda1.domain.usecase.load.nametree

class NameTreeStructure: Iterable<Map.Entry<String, NameTreeNode>> {
    private val treeChunks = mutableMapOf<Char, MutableMap<String, NameTreeNode>>()
    operator fun get(namePart: String): NameTreeNode? = treeChunks[namePart.first()]?.get(namePart)
    operator fun set(namePart: String, node: NameTreeNode) {
        val nodeChildren = treeChunks.getOrPut(namePart.first()) { mutableMapOf() }
        nodeChildren[namePart] = node
    }
    fun getOrPut(namePart: String, defaultValue: () -> NameTreeNode): NameTreeNode =
        treeChunks.getOrPut(namePart.first()) { mutableMapOf() }.getOrPut(namePart, defaultValue)
    fun getChunk(namePart: String) = treeChunks[namePart.first()] ?: emptyMap()

    override fun iterator(): Iterator<Map.Entry<String, NameTreeNode>> = object : Iterator<Map.Entry<String, NameTreeNode>> {
        private val chunkIterator = treeChunks.values.iterator()
        private var currentNodeIterator: Iterator<Map.Entry<String, NameTreeNode>>? = null

        override fun hasNext(): Boolean {
            while (currentNodeIterator == null || !currentNodeIterator!!.hasNext()) {
                if (!chunkIterator.hasNext()) return false
                currentNodeIterator = chunkIterator.next().iterator()
            }
            return true
        }

        override fun next(): Map.Entry<String, NameTreeNode> {
            if (!hasNext()) throw NoSuchElementException()
            return currentNodeIterator!!.next()
        }
    }

    val chunks: Int get() = treeChunks.size
    val chunkSizes: List<Int> get() = treeChunks.values.map { it.size }
}
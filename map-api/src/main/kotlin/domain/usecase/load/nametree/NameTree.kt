package cz.cvut.fit.gaierda1.domain.usecase.load.nametree

class NameTree(names: List<String>) {
    companion object {
        private const val MAX_SIMILARITY_SEARCH_LEVEL = 1
    }
    private fun String.toNameParts(): List<String> = split(",").reversed()

    private val tree = NameTreeStructure()

    private fun save(name: String) {
        var nodeChildren = tree
        val nameParts = name.toNameParts()
        for ((idx, part) in nameParts.withIndex()) {
            val nodeEntry = nodeChildren.getOrPut(part) { NameTreeNode(part, false, NameTreeStructure()) }
            if (idx == nameParts.size - 1) {
                nodeEntry.isLastPart = true
            }
            nodeChildren = nodeEntry.children
        }
    }

    init {
        for (name in names) {
            save(name)
        }
        println("Chunks: ${tree.chunks}")
        println("Chunk sizes: ${tree.chunkSizes}")
    }

    private fun returnNamesRecFromNode(
        node: NameTreeNode,
        namePartsStack: ArrayDeque<String>,
        result: MutableList<String>,
    ) {
        if (node.isLastPart) {
            result.add(namePartsStack.joinToString(","))
        }
        for ((namePart, child) in node.children) {
            namePartsStack.addFirst(namePart)
            returnNamesRecFromNode(child, namePartsStack, result)
            namePartsStack.removeFirst()
        }
    }

    fun findAllPreciseMatches(name: String): List<String> {
        val nameParts = name.toNameParts()
        val firstPart = nameParts.first()
        if (firstPart.isEmpty()) println(name)
        var node: NameTreeNode = tree[firstPart] ?: return emptyList()
        val namePartsStack = ArrayDeque<String>(8)
        namePartsStack.addFirst(firstPart)
        for (part in nameParts.drop(1)) {
            node = node.children[part] ?: return emptyList()
            namePartsStack.addFirst(part)
        }
        val result = mutableListOf<String>()
        returnNamesRecFromNode(node, namePartsStack, result)
        return result
    }

    private fun String.toRegexQuery(): Regex {
        val pattern = this
//            .replace(Regex("""(aut\. nádr\.|aut\. st\.)"""), "(aut. nádr.|aut. st.)")
//            .replace(Regex("""(žel\. zast\.|žel\. st\.)"""), "(žel. zast.|žel. st.)")
            .replace(Regex("""([^IVX0-9])\."""), """$1[^\s,]*""")
            .replace(Regex("""\."""), """\.""")
        return Regex("^${pattern}$")
    }

    private fun findAllNamesSimilarToMatchRec(
        node: NameTreeNode,
        namePartsQueue: ArrayDeque<String>,
        namePartsStack: ArrayDeque<String>,
        result: MutableList<String>,
    ) {
        val queryNamePart = namePartsQueue.first()
        if (!node.namePart.matches(queryNamePart.toRegexQuery())) {
            return
        }
        if (namePartsQueue.size == 1) {
            namePartsStack.addFirst(node.namePart)
            returnNamesRecFromNode(node, namePartsStack, result)
            namePartsStack.removeFirst()
            return
        }

        namePartsQueue.removeFirst()
        namePartsStack.addFirst(node.namePart)
        for ((_, child) in node.children.getChunk(namePartsQueue.first())) {
            findAllNamesSimilarToMatchRec(child, namePartsQueue, namePartsStack, result)
        }
        namePartsStack.removeFirst()
        namePartsQueue.addFirst(queryNamePart)
    }

    private fun findAllNamesSimilarToSkipRec(
        level: Int,
        subTree: NameTreeStructure,
        namePartsQueue: ArrayDeque<String>,
        namePartsStack: ArrayDeque<String>,
        result: MutableList<String>,
    ) {
        if (level == MAX_SIMILARITY_SEARCH_LEVEL) return
        for ((namePart, node) in subTree) {
            namePartsStack.addFirst(namePart)
            for ((_, child) in node.children.getChunk(namePartsQueue.first())) {
                findAllNamesSimilarToMatchRec(child, namePartsQueue, namePartsStack, result)
            }
            findAllNamesSimilarToSkipRec(level + 1, node.children, namePartsQueue, namePartsStack, result)
            namePartsStack.removeFirst()
        }
    }

    fun findAllSimilar(name: String): List<String> {
        val namePartsStack = ArrayDeque<String>(8)
        val result = mutableListOf<String>()
        val namePartsQueue = ArrayDeque(name.toNameParts())
        for ((_, node) in tree.getChunk(namePartsQueue.first())) {
            findAllNamesSimilarToMatchRec(node, namePartsQueue, namePartsStack, result)
        }
        if (result.isEmpty()) {
            findAllNamesSimilarToSkipRec(0, tree, namePartsQueue, namePartsStack, result)
        }
        return result
    }
}

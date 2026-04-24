package cz.cvut.fit.gaierda1.domain.usecase.load.nametree

data class NameTreeNode(
    val namePart: String,
    var isLastPart: Boolean,
    val children: NameTreeStructure,
)

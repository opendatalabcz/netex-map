package cz.cvut.fit.gaierda1.data.orm.adapter

fun <T: Comparable<T>> List<T>.compareTo(other: List<T>): Int {
    val thisIterator = iterator()
    val otherIterator = other.iterator()
    while (true) {
        if (!thisIterator.hasNext() && !otherIterator.hasNext()) return 0
        if (!thisIterator.hasNext()) return -1
        if (!otherIterator.hasNext()) return 1
        val compare = thisIterator.next().compareTo(otherIterator.next())
        if (compare != 0) return compare
    }
}

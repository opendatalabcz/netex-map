package cz.cvut.fit.gaierda1.data.orm.adapter

infix fun <T, R> Iterable<T>.zipWithFill(other: Iterable<R>): List<Pair<T?, R?>> {
    val list = mutableListOf<Pair<T?, R?>>()
    val thisIterator = this.iterator()
    val otherIterator = other.iterator()
    while (true) {
        val thisNext = if (thisIterator.hasNext()) thisIterator.next() else null
        val otherNext = if (otherIterator.hasNext()) otherIterator.next() else null
        if (thisNext == null && otherNext == null) break
        list.add(thisNext to otherNext)
    }
    return list
}

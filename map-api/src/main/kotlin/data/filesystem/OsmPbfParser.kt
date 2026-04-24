package cz.cvut.fit.gaierda1.data.filesystem

import crosby.binary.osmosis.OsmosisReader
import cz.cvut.fit.gaierda1.data.orm.model.PhysicalStop
import cz.cvut.fit.gaierda1.domain.port.OsmParserPort
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType
import org.openstreetmap.osmosis.core.domain.v0_6.Node
import org.openstreetmap.osmosis.core.domain.v0_6.Relation
import org.openstreetmap.osmosis.core.domain.v0_6.Tag
import org.openstreetmap.osmosis.core.task.v0_6.Sink
import org.springframework.stereotype.Component
import java.io.File

@Component
class OsmPbfParser: OsmParserPort {
    companion object {
        private const val OFFICIAL_NAME_TAG = "official_name"
        private const val NAME_TAG = "name"
    }
    private val geometryFactory = GeometryFactory()

    private class SimpleFilteringSink(
        private val stopNodes: MutableMap<Long, Node>,
        private val stopRelations: MutableList<Relation>,
    ): Sink, EntityProcessor {
        override fun process(entityContainer: EntityContainer) {
            entityContainer.process(this)
        }
        override fun process(node: NodeContainer) {
            val nodeEntity = node.entity
            if (nodeEntity.tags.any { tag ->
                tag.key == "public_transport" && (tag.value == "stop_position" || tag.value == "platform")
                || tag.key == "highway" && tag.value == "bus_stop"
                || tag.key == "railway" && (tag.value == "halt" || tag.value == "station" || tag.value == "stop" || tag.value == "tram_stop")
            }) {
                stopNodes[nodeEntity.id] = nodeEntity
            }
        }
        override fun process(relation: RelationContainer) {
            val relationEntity = relation.entity
            if (relationEntity.tags.any { tag -> tag.key == "public_transport" && tag.value == "stop_area" }) {
                stopRelations.add(relationEntity)
            }
        }
        override fun initialize(metaData: Map<String, Any>) {}
        override fun complete() {}
        override fun close() {}
        override fun process(bound: BoundContainer) {}
        override fun process(way: WayContainer) {}
    }

    private fun Collection<Tag>.getName(): String? {
        var name: String? = null
        for (tag in this) {
            if (tag.key == OFFICIAL_NAME_TAG) {
                return tag.value
            } else if (tag.key == NAME_TAG) {
                name = tag.value
            }
        }
        return name
    }
    private fun Collection<Tag>.toMap(): Map<String, String> = associate { it.key to it.value }

    override fun parseOsmFile(osmFile: File): List<PhysicalStop> {
        val stopNodes = mutableMapOf<Long, Node>()
        val stopRelations = mutableListOf<Relation>()
        val reader = OsmosisReader(osmFile)
        val sink = SimpleFilteringSink(stopNodes, stopRelations)
        reader.setSink(sink)
        reader.run()
        for (stopRelation in stopRelations) {
            val relationName = stopRelation.tags.getName() ?: continue
            for (member in stopRelation.members) {
                if (member.memberType != EntityType.Node) continue
                val stop = stopNodes[member.memberId] ?: continue
                if (stop.tags.getName() == null) {
                    stop.tags.add(Tag(NAME_TAG, relationName))
                }
            }
        }
        return stopNodes.map { PhysicalStop(
            relationalId = null,
            externalId = it.value.id.toString(),
            name = it.value.tags.getName(),
            position = geometryFactory.createPoint(Coordinate(it.value.longitude, it.value.latitude)),
            tags = it.value.tags.toMap(),
        ) }
    }
}

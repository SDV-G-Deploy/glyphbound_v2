package com.sdvgdeploy.glyphbound.core.procgen

import com.sdvgdeploy.glyphbound.core.model.Level
import com.sdvgdeploy.glyphbound.core.model.Pos
import java.util.ArrayDeque

enum class DisjointMode { EDGE, NODE }

data class PathValidationConfig(
    val minDisjointPaths: Int = 2,
    val mode: DisjointMode = DisjointMode.EDGE
)

data class PathValidationResult(
    val connected: Boolean,
    val disjointPathCount: Int,
    val requiredDisjointPaths: Int,
    val mode: DisjointMode
) {
    val isValid: Boolean get() = connected && disjointPathCount >= requiredDisjointPaths
}

object PathValidator {
    private val deltas = listOf(Pos(1, 0), Pos(-1, 0), Pos(0, 1), Pos(0, -1))

    fun validate(level: Level, config: PathValidationConfig = PathValidationConfig()): PathValidationResult {
        val connected = isConnected(level)
        if (!connected) return PathValidationResult(false, 0, config.minDisjointPaths, config.mode)

        val count = when (config.mode) {
            DisjointMode.EDGE -> maxEdgeDisjointPaths(level)
            DisjointMode.NODE -> maxNodeDisjointPaths(level)
        }

        return PathValidationResult(connected, count, config.minDisjointPaths, config.mode)
    }

    private fun walkableNodes(level: Level): List<Pos> {
        val nodes = mutableListOf<Pos>()
        for (y in 0 until level.height) {
            for (x in 0 until level.width) {
                val p = Pos(x, y)
                if (level.isWalkable(p)) nodes += p
            }
        }
        return nodes
    }

    private fun neighbors(level: Level, pos: Pos): List<Pos> =
        deltas.map { Pos(pos.x + it.x, pos.y + it.y) }.filter { level.isWalkable(it) }

    private fun isConnected(level: Level): Boolean {
        val q = ArrayDeque<Pos>()
        val visited = mutableSetOf<Pos>()
        q += level.entry
        visited += level.entry

        while (q.isNotEmpty()) {
            val current = q.removeFirst()
            if (current == level.exit) return true
            for (n in neighbors(level, current)) {
                if (visited.add(n)) q += n
            }
        }

        return false
    }

    // Matches the previous dense implementation, which stopped counting at 2 paths.
    private const val FLOW_LIMIT = 2

    private fun maxEdgeDisjointPaths(level: Level): Int {
        val nodes = walkableNodes(level)
        val index = nodes.withIndex().associate { it.value to it.index }
        val graph = FlowGraph(nodes.size)
        for (p in nodes) {
            val i = index.getValue(p)
            for (nPos in neighbors(level, p)) graph.addEdge(i, index.getValue(nPos), 1)
        }
        return graph.maxFlow(index.getValue(level.entry), index.getValue(level.exit), FLOW_LIMIT)
    }

    private fun maxNodeDisjointPaths(level: Level): Int {
        val nodes = walkableNodes(level)
        val index = nodes.withIndex().associate { it.value to it.index }
        val graph = FlowGraph(nodes.size * 2)

        fun inId(i: Int) = i * 2
        fun outId(i: Int) = i * 2 + 1

        val startIdx = index.getValue(level.entry)
        val endIdx = index.getValue(level.exit)

        for (p in nodes) {
            val i = index.getValue(p)
            val internalCapacity = if (i == startIdx || i == endIdx) 2 else 1
            graph.addEdge(inId(i), outId(i), internalCapacity)
            for (nPos in neighbors(level, p)) graph.addEdge(outId(i), inId(index.getValue(nPos)), 1)
        }

        return graph.maxFlow(outId(startIdx), inId(endIdx), FLOW_LIMIT)
    }

    /**
     * Sparse residual graph for Edmonds-Karp. Every walkable tile has at most 4 neighbours, so the
     * previous dense V x V matrices (and O(V^2) BFS) were wasted work on every generation attempt.
     */
    private class FlowGraph(size: Int) {
        private val heads = IntArray(size) { -1 }
        private var to = IntArray(64)
        private var cap = IntArray(64)
        private var next = IntArray(64)
        private var edgeCount = 0

        fun addEdge(u: Int, v: Int, c: Int) {
            push(u, v, c)
            push(v, u, 0)
        }

        private fun push(u: Int, v: Int, c: Int) {
            if (edgeCount == to.size) {
                to = to.copyOf(edgeCount * 2)
                cap = cap.copyOf(edgeCount * 2)
                next = next.copyOf(edgeCount * 2)
            }
            to[edgeCount] = v
            cap[edgeCount] = c
            next[edgeCount] = heads[u]
            heads[u] = edgeCount
            edgeCount++
        }

        /** Edge e and its reverse are stored at e and e xor 1. Stops once [limit] flow is found. */
        fun maxFlow(source: Int, sink: Int, limit: Int): Int {
            var flow = 0
            val parentEdge = IntArray(heads.size)
            val queue = IntArray(heads.size)
            while (flow < limit) {
                parentEdge.fill(-1)
                var qHead = 0
                var qTail = 0
                queue[qTail++] = source
                var found = false
                while (qHead < qTail && !found) {
                    val u = queue[qHead++]
                    var e = heads[u]
                    while (e != -1) {
                        val v = to[e]
                        if (cap[e] > 0 && v != source && parentEdge[v] == -1) {
                            parentEdge[v] = e
                            if (v == sink) {
                                found = true
                                break
                            }
                            queue[qTail++] = v
                        }
                        e = next[e]
                    }
                }
                if (!found) break

                var bottleneck = Int.MAX_VALUE
                var v = sink
                while (v != source) {
                    val e = parentEdge[v]
                    bottleneck = minOf(bottleneck, cap[e])
                    v = to[e xor 1]
                }
                v = sink
                while (v != source) {
                    val e = parentEdge[v]
                    cap[e] -= bottleneck
                    cap[e xor 1] += bottleneck
                    v = to[e xor 1]
                }
                flow += bottleneck
            }
            return flow
        }
    }
}

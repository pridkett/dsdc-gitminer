import net.wagstrom.research.github.EdgeType
import net.wagstrom.research.github.VertexType
import net.wagstrom.research.github.PropertyName
import net.wagstrom.research.github.IndexNames
import net.wagstrom.research.github.IdCols

REPONAME = "JuliaLang/julia"

def exportWatchingSubgraph(users, outfile, edgetype, sourceprop, targetprop) {
    vertexCache = [:]
    to = new TinkerGraph()
    System.out.println("processing users")
    for (vertex in users) {
        System.out.println("user: " + vertex.getProperty(sourceprop))
        toVertex = to.addVertex(vertex.getId())
        ElementHelper.copyProperties(vertex, toVertex)

        // cache the created nodes
        vertexCache[vertex.getId()] = toVertex;
    }

    System.out.println("processing edges")
    for (vertex in users) {
        System.out.println("edges for user: " + vertex.getProperty(sourceprop))
        for (edge in vertex.outE(edgetype)) {
            System.out.println("edge: " + edge)
            if (edge == null) continue
            inVertex = edge.inV().next()
            System.out.println("edge: " + edge)
            if (!vertexCache.containsKey(inVertex.getId())) {
                System.out.println("Creating new element: " + inVertex.getProperty(targetprop))
                toInVertex = to.addVertex(inVertex.getId())
                ElementHelper.copyProperties(inVertex, toInVertex)
                vertexCache[inVertex.getId()] = toInVertex
            }

            toEdge = to.addEdge(edge.getId(), vertexCache[vertex.getId()],
                vertexCache[inVertex.getId()], edge.getLabel())
            ElementHelper.copyProperties(edge, toEdge)
        }
    }

    System.out.println("Writing graphml file")
    GraphMLWriter writer = new GraphMLWriter()
    writer.outputGraph(to, new FileOutputStream(outfile))
}

System.out.println("Connecting...")
g = Helpers.connect()
System.out.println("Getting Repo....")
repo = g.idx(IndexNames.REPOSITORY).get(IdCols.REPOSITORY, "JuliaLang/julia")._().next()
users = []
System.out.println("Getting Users...")
users = []
repo.out(EdgeType.REPOCONTRIBUTOR).fill(users)
System.out.println("Making Graph...")
exportWatchingSubgraph(users, "contributor-watching.graphml", EdgeType.REPOWATCHED, PropertyName.LOGIN, PropertyName.FULLNAME)
g.shutdown()

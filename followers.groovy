import net.wagstrom.research.github.EdgeType
import net.wagstrom.research.github.VertexType
import net.wagstrom.research.github.PropertyName
import net.wagstrom.research.github.IndexNames
import net.wagstrom.research.github.IdCols

REPONAME = "JuliaLang/julia"

def exportFollowerSubgraph(users, outfile, edgetype) {
    userMap = [:]
    to = new TinkerGraph()
    System.out.println("processing users")
    for (vertex in users) {
        System.out.println("user: " + vertex["login"])
        toVertex = to.addVertex(vertex.getId())
        ElementHelper.copyProperties(vertex, toVertex)

        // cache the created nodes
        userMap[vertex["login"]] = toVertex.getId(); 
    }

    System.out.println("processing edges")
    for (vertex in users) {
        System.out.println("edges for user: " + vertex["login"])
        for (edge in vertex.outE(edgetype)) {
            System.out.println("edge: " + edge)
            if (edge == null) continue
            inVertex = edge.inV().next()
            System.out.println("edge: " + vertex["login"] + " => " + inVertex["login"])
            System.out.println("inVertex: " + inVertex)
            if (!userMap.containsKey(inVertex.getProperty(PropertyName.LOGIN))) {
                System.out.println("Creating new element: " + inVertex.getProperty(PropertyName.LOGIN))
                toInVertex = to.addVertex(inVertex.getId())
                ElementHelper.copyProperties(inVertex, toInVertex)
                userMap[toInVertex.getProperty(PropertyName.LOGIN)] = toInVertex.getId()
            }

            toEdge = to.addEdge(edge.getId(), to.v(userMap[vertex.getProperty(PropertyName.LOGIN)]),
                to.v(userMap[inVertex.getProperty(PropertyName.LOGIN)]), edge.getLabel())
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
repo.in(EdgeType.REPOWATCHED).has(PropertyName.TYPE, VertexType.USER).fill(users)
System.out.println("Making Graph...")
exportFollowerSubgraph(users, "watcher-followers.graphml", EdgeType.FOLLOWER)
System.out.println("Making Graph...")
users = []
repo.out(EdgeType.REPOCONTRIBUTOR).fill(users)
exportFollowerSubgraph(users, "contributor-followers.graphml", EdgeType.FOLLOWER)
g.shutdown()

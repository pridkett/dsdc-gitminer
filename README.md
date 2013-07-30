dsdc-gitminer
===================

This is the collection of scripts and other fun stuff to go along with my
July 30, 2013 talk at Data Science DC entitled "Abusing the GitHub API and Graph
Databases to Gain Insight About Your Project".

<iframe src="https://docs.google.com/presentation/d/1zsql-8WPX52tB_ghxxDYga-_bKQESZKcCzjpl-qmO7s/embed?start=false&loop=false&delayms=3000" frameborder="0" width="960" height="749" allowfullscreen="true" mozallowfullscreen="true" webkitallowfullscreen="true"></iframe>

A [GitMiner][GitMiner] sample dataset collected around the [Julia][Julia]
programming language. This data was originally collected for the July 2013 Data
Science DC Meetup.

Getting Setup
=============

There's some other infrastructure that you'll need to get this going. In
particular, you'll need to have [Gremlin][Gremlin], [GitMiner][GitMiner], and
that data set for the [Julia][Julia] project downloaded. With any luck these
instructions should do everything you need.

For this to work properly start out at a command prompt and enter the commands
as follows. Don't go changing directories. When you're done you should have a
hierarchy as follows:

    ~/dsdc-gitminer
    ~/gremlin
    ~/gitminer
    ~/dsdc-gitminer/julia.graph.db

It doesn't matter what your root directory is. Just make sure that `dsdc-gitminer`,
`gremlin`, and `gitminer` are all peers.

Installing dsdc-gitminer
------------------------

The first step is to install the scripts for analysis. That's just a git clone:

    git clone https://github.com/pridkett/dsdc-julia


Installing Gremlin
------------------

Gremlin is a domain specific language for traversing graph databases. It's pretty
neat and is written in Java. You'll need [maven][maven] to compile it, however.

    git clone https://github.com/tinkerpop/gremlin
    cd gremlin
    mvn clean compile package
    cd ..

Installing GitMiner
-------------------

GitMiner is the tool that I wrote for fetching data from GitHub. Although we're not
actually fetching data here, there's some constants from the project that make analysis
a lot easier.

    git clone https://github.com/pridkett/gitminer
    cd gitminer
    mvn clean compile package

Getting the Julia Project Data
------------------------------

The data is contained in a 500MiB tarball. Unfortuantely, GitHub no longer allows me to
store these files there, so it's up on S3. Running the following commands should download
and unpack the data:

    curl http://s3.wagstrom.net/julia.graph.db.tar.gz -o julia.graph.db.tar.gz
    tar -zxvf julia.graph.db.tar.gz

Configuring the Scripts
-----------------------

There's one setting that you'll need to change in the script `Defaults.groovy`. Open it up
in a text editor and navigate to line 22. There you'll see a line that reads:

    static String BASEPATH=[System.getenv("HOME"), "Documents", "workspace", "gitminer-data-julia"].join(File.separator)

This needs to be changed to where everything on your system. So, if you checked everything
out into `~/Documents/dsdc-gitminer` you'd change the line to:

    static String BASEPATH=[System.getenv("HOME"), "Documents", "dsdc-gitminer"].join(File.separator)

Running the Scripts
===================

Within the presentation I really only made use of a single script - the one that created a GraphML file to show the relationship
between the Julia core contributors and what projects they watched. Executing this script from the command line will generate
a graphml file called `contributor-watching.groovy` that links contributors to the projects they watched. For your convenience,
this network is already included along with a Cytoscape session file that was used for the visualization.

To run the script simply execute:

    ./gremlin.sh -e contributor-watching.groovy

It will take a little while and spam the console with a ton of stuff, but after a while you should have the graph file.

If you're interested, there's another file, `followers.groovy` that generates two networks `watcher-followers.graphml` and
`contributor-followers.graphml`. These networks map individuals to the other individuals they follow. In the first file
the seed set of users is all users who watch Julia. In the second file it is only the individuals who have contributed to
Julia.

You can run that script as follows:

    ./gremlin.sh -e followers.groovy

These files can be easily opened up in [Gephi][Gephi] or in [Cytoscape][Cytoscape] (you'll need the GraphML plugin for Cytoscape, however).

Gremlin REPL Fun
================

Gremlin provides a REPL to explore the data too. Here's some helpful code to
get you going.

First, start up the Gremlin REPL interface by running:

    ./gremlin.sh

Next, run the following commands. This will import the appropriate constants,
connect to the database, and give you a pointer to the node in the database
that corresponds to the `JuliaLang/julia` repository.

    import net.wagstrom.research.github.IndexNames
    import net.wagstrom.research.github.IdCols
    import net.wagstrom.research.github.VertexName
    import net.wagstrom.research.github.EdgeType
    import net.wagstrom.research.github.PropertyName

    g = Helpers.connect()
    repo = g.idx(IndexNames.REPOSITORY).get(IdCols.REPOSITORY, "JuliaLang/julia")._().next()

Now, you've got an element called `repo` that is the node for Julia. Let's look
at the repos that are watched by individuals marked as contributors to Julia:

    repo.out(EdgeType.REPOCONTRIBUTOR).out(EdgeType.REPOWATCHED).iterate()

That's a lot of data that it just spit out, and in no particular order.
Probably too much, what if we just want to know the total number of watched
repositories:

    repo.out(EdgeType.REPOCONTRIBUTOR).out(EdgeType.REPOWATCHED).count()

That should return 4819 different repositories that are watched. Let's go a bit
deeper and look at the top repositories that are watched.

    m = [:]; repo.out(EdgeType.REPOCONTRIBUTOR).out(EdgeType.REPOWATCHED).groupCount(m).iterate(); null
    m.sort{a,b -> a.value<=>b.value}.each{key, value -> print key.fullname + " " + value + "\n"}; null

The last couple of lines there should look something like this:

    forio/julia-studio 8
    stevengj/PyCall.jl 9
    dcjones/Gadfly.jl 10
    nolta/Winston.jl 10
    johnmyleswhite/Optim.jl 10
    HarlanH/DataFrames.jl 12
    xianyi/OpenBLAS 12
    JuliaLang/julialang.github.com 12
    JuliaLang/julia 99

Thus we see that most popular watched project by Julia core contributors is
Julia itself, follwed by the github pages site for Julia, and then later
OpenBLAS and DataFrames.jl.

Now, we can go a step further and see what the most popular projects are for
the people who watch Julia:

    m = [:]; repo.in(EdgeType.REPOWATCHED).out(EdgeType.REPOWATCHED).groupCount(m).iterate(); null
    m.sort{a,b -> a.value<=>b.value}.each{key, value -> print key.fullname + " " + value + "\n"}; null

The last couple of lines should look something like this:

    documentcloud/backbone 346
    nathanmarz/storm 367
    mojombo/jekyll 369
    mxcl/homebrew 370
    mrdoob/three.js 398
    bartaz/impress.js 445
    joyent/node 491
    mbostock/d3 618
    twitter/bootstrap 644
    JuliaLang/julia 2225

None of these are really any surprise - they're largely the most popular
projects on GitHub.

[GitMiner]: https://github.com/pridkett/gitminer
[Julia]: http://julialang.org/
[cytoscape]: http://www.cytoscape.org/
[gephi]: http://www.gephi.org/
[maven]: http://maven.apache.org/

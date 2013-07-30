class Defaults {
    static init() {
        def libs = []
        [GREMLINPATH, GITMINERPATH].each { thisPath ->
            def f = new File(thisPath.getPath() + File.separator + "target");
            if (f.exists()) {
                f.eachFileMatch(~/.*-standalone/) { dir ->
                    dir.eachFileRecurse{ fn -> 
                        if (fn.name =~ /.*\.jar$/) {
                            libs.add(dir.getPath() + File.separator + fn.name)
                        }
                    }
                }
            }
        }
        libs.each{libName ->
            getClassLoader().getRootLoader().addURL(new File(libName).toURL())
        }
    }

    // CHANGE THIS TO THE BASE PATH OF THE CHECKOUT ON YOUR SYSTEM
    static String BASEPATH=[System.getenv("HOME"), "Documents", "workspace", "gitminer-data-julia"].join(File.separator)

    // Settings below should not need to be changed
    static String DBPATH=[BASEPATH, "julia.graph.db"].join(File.separator)

    static File GREMLINPATH=new File(new File(BASEPATH).getParent() + File.separator + "gremlin")
    static File GITMINERPATH=new File(new File(BASEPATH).getParent() + File.separator + "gitminer")
}

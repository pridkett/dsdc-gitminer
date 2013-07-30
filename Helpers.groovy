import net.wagstrom.research.github.EdgeType
import net.wagstrom.research.github.VertexType
import net.wagstrom.research.github.PropertyName
import net.wagstrom.research.github.IndexNames
import net.wagstrom.research.github.IdCols
import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.blueprints.Element
import com.tinkerpop.blueprints.Graph
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph
import com.tinkerpop.pipes.Pipe
import java.security.MessageDigest

class Helpers {
    static Graph connect() {
        return new Neo4jGraph(Defaults.DBPATH)
    }

    static printSortedMap(Map inmap) {
        for (p in inmap.sort{a,b -> a.key <=> b.key}) {
            println "==>" + p.key + "=" + p.value
        }
    }

    static setDifference(Collection s1, Collection s2) {
        def set1 = s1.clone().toSet()
        set1.removeAll(s1.toSet().intersect(s2.toSet()))
        return set1
    }

    
    static setDifferenceLeft(Collection s1, Collection s2) {
        return s1.intersect(setDifference(s1, s2))
    }
    
    static Date timestampToDate(String s) {
        return Date.parse(Defaults.DATE_FORMAT, s)
    }
    
    static Date timestampToDate(int i) {
        return new java.util.Date(i*1000L)
    }

    static Date timestampToDate(long l) {
        return new java.util.Date(l)
    }

    static int parseDate(long l) {
        return l/1000
    }

    static int parseDate(int i) {
        return i
    }

    static int parseDate(String s) {
        return Date.parse(Defaults.DATE_FORMAT, s).getTime()/1000
    }

    static void updateDate(Element v, String s) {
        if (v.getProperty(s) != null) {
            v.setProperty(s, parseDate(v.getProperty(s)))
        }
    }

    static int dateDifference(Date d1, Date d2) {
        return (int)((d1.getTime() - d2.getTime())/1000L)
    }
    
    static int dateDifferenceAbs(Date d1, Date d2) { 
        return Math.abs(dateDifference(d1, d2))
    }
    
    static Pipe getRepositoryWatchers(Vertex repo) {
        return repo.in(EdgeType.REPOWATCHED)
    }
	
	static Pipe getRepositoryOrganizationMembers(Vertex repo) {
		return repo.in(EdgeType.REPOOWNER). \
                    in(EdgeType.ORGANIZATIONMEMBER)
	}

    static Pipe getRepositoryCollaborators(Vertex repo) {
        return repo.out(EdgeType.REPOCOLLABORATOR)
    }

    /**
     * This gets the contributors and owners
     *  
     * This is a gross abuse of copySplit pipe but is required because
     * _() doesn't seem to be defined in this context, therefore it cannot
     * be used. Yech...
     *
     * @param repo
     * @return
     */
    static Pipe getRepositoryContributors(Vertex repo) {
        return repo.copySplit(repo.out(EdgeType.REPOCONTRIBUTOR), \
                              repo.in(EdgeType.REPOOWNER)).exhaustMerge(). \
                    dedup()
    }

    static Pipe getRepositoryIssueOwners(Vertex repo) {
        return repo.out(EdgeType.ISSUE). \
                    in(EdgeType.ISSUEOWNER). \
                    dedup()
    }

    static Pipe getRepositoryIssueCommenters(Vertex repo) {
        return repo.out(EdgeType.ISSUE). \
                    out(EdgeType.ISSUECOMMENT). \
                    in(EdgeType.ISSUECOMMENTOWNER).dedup()
    }

    static Pipe getRepositoryIssueClosers(Vertex repo) {
        return repo.out(EdgeType.ISSUE).out(EdgeType.ISSUEEVENT). \
                    has(PropertyName.EVENT, "closed"). \
                    in(EdgeType.ISSUEEVENTACTOR).dedup()
    }
    
    static Pipe getRepositoryIssueSubscribers(Vertex repo) {
        return repo.out(EdgeType.ISSUE).out(EdgeType.ISSUEEVENT). \
                    has(PropertyName.EVENT, "subscribed"). \
                    in(EdgeType.ISSUEEVENTACTOR).dedup()
    }

    static Pipe getRepositoryPullRequestOwners(Vertex repo) {
        return repo.out(EdgeType.PULLREQUEST). \
                    in(EdgeType.PULLREQUESTOWNER).dedup()
    }

    static Pipe getRepositoryOpenPullRequestOwners(Vertex repo) {
        return repo.out(EdgeType.PULLREQUEST). \
                    has(PropertyName.CLOSED_AT, null). \
                    in(EdgeType.PULLREQUESTOWNER).dedup()
    }

    static Pipe getRepositoryClosedPullRequestOwners(Vertex repo) {
        return repo.out(EdgeType.PULLREQUEST). \
                    hasNot(PropertyName.CLOSED_AT, null). \
                    in(EdgeType.PULLREQUESTOWNER).dedup()
    }

    static Pipe getRepositoryMergedPullRequestOwners(Vertex repo) {
        return repo.out(EdgeType.PULLREQUEST). \
                    hasNot(PropertyName.MERGED_AT, null). \
                    in(EdgeType.PULLREQUESTOWNER).dedup()
    }

    static Pipe getRepositoryPullRequestMergers(Vertex repo) {
        return repo.out(EdgeType.PULLREQUEST). \
                    out(EdgeType.PULLREQUESTMERGEDBY). \
                    dedup()
    }
    
    static Pipe getRepositoryPullRequestCommenters(Vertex repo) {
        return repo.out(EdgeType.PULLREQUEST). \
                    out(EdgeType.PULLREQUESTISSUECOMMENT). \
                    in(EdgeType.PULLREQUESTCOMMENTOWNER).dedup()
    }

    /**
     * returns all of the owners of forks
     *
     * Fundamentally this is just a matter of going from the repository to each
     * fork to the owner. However we also filter out organizations as they're
     * not people
     */
    static Pipe getRepositoryForkOwners(Vertex repo) {
        return repo.out(EdgeType.REPOFORK). \
                    in(EdgeType.REPOOWNER). \
                    hasNot(PropertyName.USER_TYPE, "Organization").dedup()
    }
    
    static Pipe getRepositoryCommitters(Vertex repo) {
        return repo.in(EdgeType.REPOSITORY). \
                    out(EdgeType.COMMITAUTHOR). \
                    has(PropertyName.TYPE, VertexType.GIT_USER). \
                    out(EdgeType.EMAIL).dedup(). \
                    in(EdgeType.EMAIL). \
                    has(PropertyName.TYPE, VertexType.USER).dedup()
    }
    
    static Pipe getRepositoryIssues(Vertex repo) {
        return repo.out(EdgeType.ISSUE).dedup();
    }    

    static Pipe getRepositoryPullRequests(Vertex repo) {
        return repo.out(EdgeType.PULLREQUEST).dedup();
    }

    static Pipe getRepositoryCommits(Vertex repo) {
        return repo.in(EdgeType.REPOSITORY).has(PropertyName.TYPE, VertexType.COMMIT).dedup()
    }
    /**
     * Combined method to return a list of all users on the project
     * 
     * @param repo Vertex for the repository of interest
     * @return a Set of the users on the project
     */
    static Collection getAllRepositoryUsers(repo) {
        def watchers = getRepositoryWatchers(repo).toList()
        
        // collaborators: have admin rights on projects
        def collaborators = getRepositoryCollaborators(repo).toList()
        // organizationMembers were included in the previous definition of roles(.groovy)
        def organizationMembers = getRepositoryOrganizationMembers(repo).toList()
        // contributors: have committed code to project
        def contributors = getRepositoryContributors(repo).toList()
        
        def issueOwners = getRepositoryIssueOwners(repo).toList()
        def issueCommenters = getRepositoryIssueCommenters(repo).toList()
        def issueClosers = getRepositoryIssueClosers(repo).toList()
        def issueSubscribers = getRepositoryIssueSubscribers(repo).toList()
        // pull request owners encompasses open/closed/merged, simplifying
        def pullRequestOwners = getRepositoryPullRequestOwners(repo).toList()
        //def openPullRequestOwners = getRepositoryOpenPullRequestOwners(repo).toList()
        //def closedPullRequestOwners = getRepositoryClosedPullRequestOwners(repo).toList()
        //def mergedPullRequestOwners = getRepositoryMergedPullRequestOwners(repo).toList()
        def pullRequestCommenters = getRepositoryPullRequestCommenters(repo).toList()
        def mergers = getRepositoryPullRequestMergers(repo).toList()
        def forkOwners = getRepositoryForkOwners(repo).toList()
        def committers = getRepositoryCommitters(repo).toList()
        
        def allActive = (collaborators \
                         + organizationMembers \
                         + contributors \
                         + issueOwners \
                         + issueCommenters \
                         + issueClosers \
                         + issueSubscribers \
                         + pullRequestOwners \
                         //+ openPullRequestOwners \
                         //+ closedPullRequestOwners \ 
                         //+ mergedPullRequestOwners \ 
                         + pullRequestCommenters \
                         + mergers \
                         + forkOwners \
                         + committers).unique()
        def allUsers = (allActive as Set) + watchers
        return allUsers
    }

    /**
     * Given a set of users gets all of the projects those users are affiliated with
     *
     * Here are the ways a user can be affiliated with a project:
     *   - User created a milsteon which is used on issues in the project
     *   - User is expressly marked as a contributor to a repository
     *   - User is expressly marked as a collaborator to a repository
     *   - User has commented on issues in a repository
     *   - User has been assigned to issues in a repository
     *   - User is owner of issues in a repository
     *   - User owns pull requests in a repository
     *   - User owns a repository
     *   - User watches a repository
     *   - User has made commits to a repository
     */
    static Collection getUserAffiliatedProjects(Graph g, Collection users) {
        def allProjects = new HashSet()
        [
        // goes from USER->MILESTONE->REPOSITORY
        // users._().in(EdgeType.CREATOR).in(EdgeType.MILESTONE).in(EdgeType.ISSUE).has(PropertyName.TYPE, VertexType.REPOSITORY),
        // goes from USER->REPOSITORY
         users._().in(EdgeType.REPOCONTRIBUTOR).has(PropertyName.TYPE, VertexType.REPOSITORY),
        // goes from USER->REPOSITORY
         users._().in(EdgeType.REPOCOLLABORATOR).has(PropertyName.TYPE, VertexType.REPOSITORY),
        // goes from USER<-ISSUE_COMMENT-?-ISSUE-?-REPOSITORY
        // users._().out(EdgeType.ISSUECOMMENTOWNER).in(EdgeType.ISSUECOMMENT).in(EdgeType.ISSUE).has(PropertyName.TYPE, VertexType.REPOSITORY),
        // goes from USER<-ISSUE-?-REPOSITORY
        // users._().out(EdgeType.ISSUEASSIGNEE).in(EdgeType.ISSUE).has(PropertyName.TYPE, VertexType.REPOSITORY),
        // goes from USER<-ISSUE-?-REPOSITORY
        // users._().out(EdgeType.ISSUEOWNER).in(EdgeType.ISSUE).has(PropertyName.TYPE, VertexType.REPOSITORY),
        // goes from USER<-PULL_REQUEST_COMMENT-?-PULLREQUEST-?-REPOSITORY
        // users._().out(EdgeType.PULLREQUESTCOMMENTOWNER).in(EdgeType.PULLREQUESTISSUECOMMENT).in(EdgeType.PULLREQUEST).has(PropertyName.TYPE, VertexType.REPOSITORY),
        // goes from USER<-PULLREQUEST-?-REPOSITORY
        // users._().out(EdgeType.PULLREQUESTOWNER).in(EdgeType.PULLREQUEST).has(PropertyName.TYPE, VertexType.REPOSITORY),
        // goes from USER<-REPOSITORY
         users._().out(EdgeType.REPOOWNER).has(PropertyName.TYPE, VertexType.REPOSITORY),
        // goes from USER<-REPOSITORY
         users._().out(EdgeType.REPOWATCHED).has(PropertyName.TYPE, VertexType.REPOSITORY)].each{pipe ->
            allProjects.addAll(pipe.toSet())
         }
        // get all the stuff for each commit
        // users.each{user ->
        //    def accounts = getAllGitAccounts(g, user)
        //    allProjects.addAll(accounts._().in.has(PropertyName.TYPE, VertexType.COMMIT).out(EdgeType.REPOSITORY).has(PropertyName.TYPE, VertexType.REPOSITORY).toSet())
        // }
        return allProjects
    }
    
    static String gravatarHash(String email) {
        def m = MessageDigest.getInstance("MD5")
        m.update(email.trim().toLowerCase().getBytes())
        def l = new BigInteger(1, m.digest())
        return l.toString(16)
    }
    
    /**
     * gravatarId's can be problematic as they can be either of:
     * https://secure.gravatar.com/avatar/ee85853909657f47c8a68e8a9bc7d992?d=https://a248.e.akamai.net/assets.github.com%2Fimages%2Fgravatars%2Fgravatar-140.png
     * e3e98bfa99e82ac8b0cb63660dc23b14
     * 
     * This function extracts the proper value
     */
    static String gravatarIdExtract(String gravatarId) {
        try {
            return (gravatarId =~ /([a-f0-9]{32})/)[0][0]
        } catch (e) {
            return null;
        }
    }
    
    /**
     * There are multiple different ways that we can tie a git account to a github
     * account. This iterates through three ways in an attempt to map the commit with the user
     * 
     * 1. direct links via email addresses
     * 2. gravatar links
     * 3. referenced commits from issue events
     * 
     */
    static getUserFromGitUser(Vertex gituser) {
        try {
            return gituser.out(EdgeType.EMAIL).in(EdgeType.EMAIL).has(PropertyName.TYPE, VertexType.USER).next()
        } catch (e) {}

        try {
            return gituser.out(EdgeType.EMAIL).out(EdgeType.GRAVATARHASH).in(EdgeType.GRAVATAR).next()
        } catch (e) {}

        try {
            return gituser.in(EdgeType.COMMITTER).out(EdgeType.AUTHOR).filter{it==gituser}.back(2).in(EdgeType.EVENTCOMMIT).in(EdgeType.ISSUEEVENTACTOR).dedup().next()
        } catch (e) {}
        return null;
    }
    
    static getAllGitAccounts(g, Vertex user) {
        // getting all of a users git accounts is tricky because they don't make all of their email addresses
        // public. Luckily, using these two methods we do a pretty good job of getting all of a users git_user
        // accounts
        def gitAccounts = user.out(EdgeType.EMAIL). \
                           in(EdgeType.EMAIL). \
                           has("type", VertexType.GIT_USER). \
                           dedup().toSet()
    
        // this code has been superseded as it isn't always that accurate and can
        // grab accounts that don't belong to this user
        // gitAccounts = (gitAccounts as Set) + user.out(EdgeType.ISSUEEVENTACTOR). \
        //                  in(EdgeType.ISSUEEVENT).in(EdgeType.ISSUE). \
        //                  filter{it == repo}.back(3).out(EdgeType.EVENTCOMMIT). \
        //                  out(EdgeType.COMMITTER).dedup().toList()
        
        // here we need to be a little careful with finding additional accounts
        // this pipe takes all of the commits this person has tied to an issue,
        // and filters for those email addresses which are not associated with
        // a user yet. It assumes, and this is a big assumption, that if one of
        // these unparented links shows up in both COMMITTERS and PARENTS then
        // the user probably owns that account
        // traceAccountsCommitter = user.out(EdgeType.ISSUEEVENTACTOR).out(EdgeType.EVENTCOMMIT).out(EdgeType.COMMITTER). \
        //                          filter{it.type=="GIT_USER"}.out("EMAIL").dedup().filter{it.in("EMAIL").filter{it.type == "USER"}.count() == 0}.back(4).toList()
        // traceAccountsAuthor = user.out(EdgeType.ISSUEEVENTACTOR).out(EdgeType.EVENTCOMMIT).out(EdgeType.COMMITAUTHOR). \
        //                          filter{it.type=="GIT_USER"}.out("EMAIL").dedup().filter{it.in("EMAIL").filter{it.type == "USER"}.count() == 0}.back(4).toList()
        // traceAccounts = (traceAccountsCommitter as Set) + traceAccountsAuthor
        // a slightly more complicated but more accurate version of the above commands
        // this version requires that the supposedly unattached commit have the same author
        // and committer.
        def traceAccounts = user.out(EdgeType.ISSUEEVENTACTOR).out(EdgeType.EVENTCOMMIT). \
             filter{it.out(EdgeType.COMMITTER).filter{it.type==VertexType.GIT_USER}.out(EdgeType.EMAIL).next() == \
                    it.out(EdgeType.COMMITAUTHOR).filter{it.type==VertexType.GIT_USER}.out(EdgeType.EMAIL).next()}. \
             out(EdgeType.COMMITTER). \
             filter{it.type==VertexType.GIT_USER}.out(EdgeType.EMAIL).dedup().filter{it.in(EdgeType.EMAIL).filter{it.type == VertexType.USER}.count() == 0}. \
             back(4).dedup().toSet()
        
        def gravatars = user.out(EdgeType.GRAVATAR). \
                         in(EdgeType.GRAVATARHASH). \
                         has(PropertyName.TYPE, VertexType.EMAIL). \
                         in(VertexType.EMAIL). \
                         has(PropertyName.TYPE, VertexType.GIT_USER).toSet()
                
        gitAccounts = gitAccounts + traceAccounts + gravatars
         
        def allGitAccounts = [] as Set
        for (email in gitAccounts._().out(EdgeType.EMAIL).email.dedup().toSet()) {
            allGitAccounts += g.idx(IndexNames.EMAIL).get(IdCols.EMAIL, email)._().in(EdgeType.EMAIL).has(PropertyName.TYPE, VertexType.GIT_USER).toSet()
        }
    
        return allGitAccounts
    }
}

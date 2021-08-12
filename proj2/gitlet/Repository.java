package gitlet;

import java.io.File;
import java.util.*;

import static gitlet.Utils.*;

/** Represents a gitlet repository.
 *  It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Hongfa You
 */
public class Repository {
    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** The .gitlet/staged_obj directory. */
    public static final File STAGE_DIR = join(GITLET_DIR, "staged_obj");
    /** The .gitlet/commited_obj directory. */
    public static final File COMMITED_DIR = join(GITLET_DIR, "commited_obj");
    /** The .gitlet/unstaged_obj directory. */
    public static final File UNSTAGE_DIR = join(GITLET_DIR, "unstaged_obj");
    /** The .gitlet directory. */
    public static final File INFOCOMMIT_DIR = join(GITLET_DIR, "infocommit");
    /** The .gitlet/infostaged_dir directory. */
    public static final File INFOSTAGE_DIR = join(GITLET_DIR, "infostaged_dir");
    /** The .gitlet/branch_dir directory. */
    public static final File BRANCH_DIR = join(GITLET_DIR, "branch_dir");

    /** Create a new Gitlet version-control system in the current directory.
     *
     *  Start a initial commit automatically with a message "initial commit",
     *  a single branch "master" which initially points to this inital commit.
     *  And create a HEAD, which indicates where current branch are now.
     *
     *  In addition, save the new created initial commit, master branch,
     *  HEAD branch to the file system for future use.
     *  TIME COMPLEXITY : O(1).
     */
    public static void init() {
        String errMsg = "A Gitlet version-control system already exists in the current directory.";
        if (GITLET_DIR.exists()) {
            abort(errMsg);
        } else {
            mkalldir();
        }
        Commit initial = new Commit("initial commit", null, null);
        initial.saveCommit();
        HEAD.initialize(initial.getSHA1());
    }

    /** Adds a copy of the file as it currently exists to the staging area.
     *
     *  If file named filename doesn't exists, or it is a directory, just exit.
     *  If the file is identical to the version in the current commit, do not stage
     *  it, and remove it from the staging area if already there.
     *  If the new-added file is already unstaged, just unremove it.
     *  @param filename : name of File to be added (staged).
     *  TIME COMPLEXITY : O(M + logN)
     *  M:the size of the file being added   N:the number of files in the commit.
     */
    public static void add(String filename) {
        File destFile = join(CWD, filename);
        /** File named filename doesn't exists, or it is a directory, exit. */
        if (!destFile.exists()) {
            abort("File does not exist.");
        } else if (destFile.isDirectory()) {
            abort(filename + "is a directory.");
        }

        /** If the new-added file is already unstaged, just unremove it. */
        if (Blob.isRemovalContains(filename)) {
            Blob.unremove(filename);
            return;
        }

        /** Load current Commit and files in it. */
        Commit lastCommit = Commit.readCommitFromFile(HEAD.whichCommit());
        File commitedFile = lastCommit.loadfile(filename); //TC: O(logN)

        /** Check if file staged is the same as the file in CWD. */
        /** If it is, remove it from staging area. */
        if (commitedFile != null && isFileSame(commitedFile, destFile)) {
            String sha1 = Utils.sha1(filename);
            File stagedfile = join(STAGE_DIR, sha1);
            if (stagedfile.exists()) {
                restrictedDelete(stagedfile);
            }
            return;
        }
        Blob.add(filename);
    }

    /** Create a new Commit and saves a snapshot of tracked files in the current
     *  Commit and staging area, so they can be restored later.
     *
     *  By default, each commit’s snapshot of files will be the same as its parent
     *  commit’s snapshot; it'll keep versions of files exactly as they are, and not
     *  update them.
     *
     *  A commit will only update the contents of files it's tracking that have been staged
     *  for addition at the time of commit, in which case the commit will include the
     *  version of the file that was staged instead of the old version got from its parent.
     *  A commit will save and start tracking any files that were staged for addition
     *  but weren’t tracked by its parent.
     *
     *  Finally, files tracked in current commit may be untracked in new commit as a
     *  result of being staged for removal by the rm command.
     * @param msg : message for new commit.
     * @param sp : second parent.
     * TIME COMPLEXITY : O(N) , N : total number of files the commit is tracking.
     * SPACE COMPLEXITY : O(M) , M : total size of the files staged for addition when "commit".
     */
    public static void commit(String msg, String sp) {
        if (msg.length() == 0) {
            abort("Please enter a commit message.");
        }

        Commit commit = new Commit(msg, HEAD.whichCommit(), sp);
        CommitHelper.copySnapshot(commit); // TC : O(N).
        CommitHelper.staged2Commited(commit); // SC : O(M).
        HEAD.switch2commit(commit.getSHA1());
        commit.saveCommit();
    }

    /** Unstage the file if it is currently staged for addition. If the file is
     * tracked in the current commit, stage it for removal and remove the file from
     * the working directory if the user has not already done so (don't remove it
     * unless it is tracked in the current commit).
     * @param filename : name of file to "rm".
     * TIME COMPLEXITY : O(logN). N : total number of files the commit is tracking.
     */
    public static void rm(String filename) {
        boolean iSinCommit = StageHelper.checkCommit2Unstaged(filename);
        boolean iSinStage = StageHelper.unstageOne(filename);
        if (!iSinCommit && !iSinStage) {
            abort("No reason to remove the file.");
        }
        Blob.remove(filename, iSinCommit);
    }

    /** Starting at the current head commit, display information about each commit
     * backwards along the commit tree until the initial commit, following the first
     * parent commit links, ignoring any second parents found in merge commits.
     * (In regular Git, this is what you get with git log --first-parent). This set
     * of commit nodes is called the commit’s history. The information it should display
     * is the commit id, the time the commit was made, and the commit message.
     * TIME COMPLEXITY : O(N).  N : the number of nodes in head’s history.
     */
    public static void log() {
        Commit commit = Commit.readCommitFromFile(HEAD.whichCommit());
        String parent;
        while (true) {
            parent = commit.getfirstParent();
            commit.printCommitInfo();
            if (parent == null) {
                break;
            }
            commit = Commit.readCommitFromFile(parent);
        }
    }

    /** Like log, except displays information about all commits ever made. The order of
     *  the commits does not matter. Hint: there is a useful method in gitlet.Utils that
     *  will help you iterate over files within a directory.
     *  TIME COMPLEXITY : O(N).  N : the number of commits ever made.
     */
    public static void globalLog() {
        List<String> fileList = Utils.plainFilenamesIn(INFOCOMMIT_DIR);
        Commit commit;

        for (String filename: fileList) {
            commit = Commit.readCommitFromFile(filename);
            commit.printCommitInfo();
        }
    }

    /** Prints out the ids of all commits that have the given commit message, one per line.
     * If there are multiple such commits, it prints the ids out on separate lines. The
     * commit message is a single operand; to indicate a multiword message, put the operand
     * in quotation marks, as for the commit command below.
     * @param msg : commit message to find.
     * TIME COMPLEXITY : O(N).  N : the number of commits ever made.
     */
    public static void find(String msg) {
        if (msg == null) {
            abort("Please specify a message to find.");
        }
        List<String> fileList = Utils.plainFilenamesIn(INFOCOMMIT_DIR);
        Commit commit;
        boolean isFinded = false;
        for (String filename: fileList) {
            commit = Commit.readCommitFromFile(filename);
            if (commit.getMessage().equals(msg)) {
                message(commit.getSHA1());
                isFinded = true;
            }
        }
        if (!isFinded) {
            message("Found no commit with that message.");
        }
    }

    /** Displays what branches currently exist, and marks the current branch with a *.
     *  Also displays what files have been staged for addition or removal. An example of
     *  the exact format it should follow is as follows.
     */
    public static void status() {
        List<String> commitList = Utils.plainFilenamesIn(Repository.INFOCOMMIT_DIR);
        if (commitList.isEmpty()) {
            Utils.abort("Not in an initialized Gitlet directory.");
        }
        StatusHelper.printStatus();
    }

    /** Takes the version of the file as it exists in the head commit and puts it in the
     * working directory, overwriting the version of the file that’s already there if there
     * is one. The new version of the file is not staged.
     * @param operand : args[1].
     * @param filename : name of file to checkout.
     */
    public static void checkout(String operand, String filename) {
        // checkout -- [file name]
        if (!operand.equals("--")) {
            Utils.abort("Incorrect operands.");
        }
        String commitId = HEAD.whichCommit();
        CheckoutHelper.overwriteOne(commitId, filename);
        // unstageOne(filename);
        /** Only version 3 (checkout of a full branch) modifies the staging area:
        * otherwise files scheduled for addition or removal remain so. */
    }


    /** Takes all files in the commit at the head of the given branch, and puts them in the
     *  working directory, overwriting the versions of the files that are already there if
     *  they exist.
     *  Also, at the end of this command, the given branch will now be considered the
     *  current branch (HEAD). Any files that are tracked in the current branch but are not
     *  present in the checked-out branch are deleted.
     *  The staging area is cleared, unless the checked-out branch is the current branch
     *  (see Failure cases below).
     * @param branchName : name of branch to checkout.
     */
    public static void checkout(String branchName) {
        // checkout [branch name]
        HEAD.readHEAD();
        if (branchName.equals(HEAD.getPointBranch())) {
            abort("No need to checkout the current branch.");
        }
        List<String> branchList = Utils.plainFilenamesIn(BRANCH_DIR);
        if (!branchList.contains(branchName)) {
            abort("No such branch exists.");
        }

        CheckoutHelper.checkUntracked();
        HEAD.switchHEAD(branchName);
        deleteCWDall();
        CheckoutHelper.overwriteAll(HEAD.whichCommit());
        CheckoutHelper.unstageAll();
    }

    /** Takes the version of the file as it exists in the commit with the given id, and puts
     * it in the working directory, overwriting the version of the file that’s already there
     * if there is one. The new version of the file is not staged.
     * checkout [commit id] -- [file name]
     * @param commitName : commit id to checkout.
     * @param operand : args[2].
     * @param fileName : name of file to checkout.
     */
    public static void checkout(String commitName, String operand, String fileName) {
        if (!operand.equals("--")) {
            Utils.abort("Incorrect operands.");
        }
        String commitId;
        if (commitName.length() < 40) {
            commitId = CheckoutHelper.find(commitName);
            if (commitId == null) {
                abort("CommitId of " + commitName + " doesn't exist.");
            }
        } else {
            commitId = commitName;
        }
        CheckoutHelper.overwriteOne(commitId, fileName);
        // unstageOne(fileName);
        /* Only version 3 (checkout of a full branch) modifies the staging area:
        * otherwise files scheduled for addition or removal remain so. */
    }

    /** Creates a new branch with the given name, and points it at the current head commit.
     * A branch is nothing more than a name for a reference (a SHA-1 identifier) to a commit
     * node. This command does NOT immediately switch to the newly created branch (just as
     * in real Git). Before you ever call branch, your code should be running with a default
     * branch called master.
     * @param branchName : name of new branch.
     * TIME COMPLEXITY : O(1).
     */
    public static void branch(String branchName) {
        if (Branch.isBranchExist(branchName)) {
            abort("A branch with that name already exists.");
        }
        Branch newone = new Branch(branchName, HEAD.whichCommit());
        newone.saveBranch();
    }

    /** Deletes the branch with the given name. This only means to delete the pointer associated
     *  with the branch; it does not mean to delete all commits that were created under the branch,
     *  or anything like that.
     * @param branchName : name of branch to remove.
     *  TIME COMPLEXITY : O(1).
     */
    public static void rmBranch(String branchName) {
        Branch.deleteBranch(branchName);
    }

    /** Checks out all the files tracked by the given commit. Removes tracked files that are not
     *  present in that commit. Also moves the current branch’s head to that commit node. See the
     *  intro for an example of what happens to the head pointer after using reset. The [commit id]
     *  may be abbreviated as for checkout. The staging area is cleared. The command is essentially
     *  checkout of an arbitrary commit that also changes the current branch head.
     * @param commitID : SHA1 String of commit to reset to.
     * TIME COMPLEXITY : O(N). N : total size of files tracked by the given commit.
     *  O(1), be constant with respect to any measure involving number of commits.
    */
    public static void reset(String commitID) {
        CheckoutHelper.checkUntracked();
        Commit commit = Commit.readCommitFromFile(commitID);
        deleteCWDall();
        HEAD.switch2commit(commitID);
        CheckoutHelper.overwriteAll(commitID); // TC : O(1).
        CheckoutHelper.unstageAll();
    }

    /** Driver method for merge.
     * @param branchName : name of the other branch.
     * TIME COMPLEXITY : O(NlgN+D).
     *   N : the total number of ancestor commits for the two branches
     *   D : total amount of data in all the files under these commits.
     */
    public static void merge(String branchName) {
        String splitCommitSha1 = MergeHelper.findSplitPoint(branchName); // TC: O(NlogN)
        String commitSHA1 = Branch.readBranchIn(branchName, true).whichCommit();
        MergeHelper.mergeCheck(branchName, splitCommitSha1, commitSHA1);
        MergeHelper.doMerge(splitCommitSha1, commitSHA1);
        String commitMsg = "Merged " + branchName + " into " + HEAD.getPointBranch() + ".";
        commit(commitMsg, commitSHA1);
    }


    /** Delete all of the files in current working directory. */
    private static void deleteCWDall() {
        List<String> fileList = Utils.plainFilenamesIn(CWD);
        File file;
        for (String filename: fileList) {
            file = join(CWD, filename);
//            Utils.restrictedDelete(filename);
            file.delete();
        }
    }

    /** Touch the directories required. */
    public static void mkalldir() {
        restrictCreateDir(GITLET_DIR);
        restrictCreateDir(STAGE_DIR);
        restrictCreateDir(COMMITED_DIR);
        restrictCreateDir(UNSTAGE_DIR);
        restrictCreateDir(INFOSTAGE_DIR);
        restrictCreateDir(INFOCOMMIT_DIR);
        restrictCreateDir(BRANCH_DIR);
    }

}

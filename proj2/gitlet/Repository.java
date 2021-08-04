package gitlet;

// import com.sun.source.tree.Tree;

import jdk.jshell.execution.Util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static gitlet.Utils.*;


/** Represents a gitlet repository.
 *  It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Hongfa You
 */
public class Repository {
    /**
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

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

    /**
     *  Create a new Gitlet version-control system in the current directory.
     *
     *  Start a initial commit automatically with a message "initial commit",
     *  a single branch "master" which initially points to this inital commit.
     *  And create a HEAD, which indicates where current branch we are now.
     *
     *  In addition, save the new created initial Commit, master branch,
     *  HEAD branch to the file system for future use.
     *
     *  The timestamp for this initial commit will be 00:00:00 UTC, Thursday,
     *  1 January 1970 in whatever format you choose for dates.
     */
    public static void init() {
        if (Repository.GITLET_DIR.exists()) {
            Repository.abort("A Gitlet version-control system already exists "
                    + "in the current directory.");
        } else {
            Repository.mkalldir();
        }
        Commit initial = new Commit("initial commit", null);
        initial.saveCommit();
        HEAD.initialize(initial.getSHA1());
    }

    /**
     *  Adds a copy of the file as it currently exists to the staging area.
     *
     *  If file named filename doesn't exists, or it is a directory, just exit.
     *
     *  If the file is identical to the version in the current commit,
     *  do not stage it to be added, and remove it from the staging area if it is already there.
     *
     *  @param filename
     */
    public static void add(String filename) {
        File addedfile = join(CWD, filename);
        /** File named filename doesn't exists, or it is a directory, just exit. */
        if (!addedfile.exists()) {
            abort("File does not exist.");
        } else if (addedfile.isDirectory()) {
            abort(filename + "is a directory.");
        }

        /** If the new-added file is already unstaged, just unremove the file. */
        if (Blob.isRemovalContains(filename)) {
            Blob.unremove(filename);
            return;
        }

        /** Load current Commit and files in it. */
        Commit lastCommit = Commit.readCommitFromFile(HEAD.whichCommit());
        File commitedFile = lastCommit.loadfile(filename);

        /** Check if file staged is the same as the file in CWD. */
        /** If it is, remove it from staging area. */
        if (commitedFile != null && isFileSame(commitedFile, addedfile)) {
            String sha1 = Utils.sha1(filename);
            File stagedfile = join(STAGE_DIR, sha1);
            if (stagedfile.exists()) {
                restrictedDelete(stagedfile);
            }
            return;
        }
        Blob.add(filename);
    }

    /**
     *  Create a new Commit and saves a snapshot of tracked files in the current
     *  Commit and staging area, so they can be restored later.
     *
     *  By default, each commit’s snapshot of files will be the same as its parent
     *  commit’s snapshot of files; it will keep versions of files exactly as they
     *  are, and not update them.
     *
     *  A commit will only update the contents of files it is tracking that have
     *  been staged for addition at the time of commit, in which case the commit
     *  will now include the version of the file that was staged instead of the
     *  version it got from its parent.
     *
     *  A commit will save and start tracking any files that were staged for addition
     *  but weren’t tracked by its parent.
     *
     *  Finally, files tracked in the current commit may be untracked in the new commit
     *  as a result being staged for removal by the rm command.
     * @param msg
     */
    public static void commit(String msg) {
        if (msg.length() == 0) {
            Repository.abort("Please enter a commit message.");
        }

        Commit commit = new Commit(msg, HEAD.whichCommit());
        copySnapshot(commit);
        staged2Commited(commit);
        HEAD.switch2commit(commit.getSHA1());
        commit.saveCommit();
    }

    /**
     * Unstage the file if it is currently staged for addition. If the file is
     * tracked in the current commit, stage it for removal and remove the file from
     * the working directory if the user has not already done so (do not remove it
     * unless it is tracked in the current commit).
     */
    public static void rm(String filename) {
        boolean iSinCommit = checkCommit2Unstaged(filename);
        boolean iSinStage = unstageOne(filename, iSinCommit);
        if (!iSinCommit && !iSinStage) {
            abort("No reason to remove the file.");
        }
        Blob.remove(filename, iSinCommit);
    }

    /**
     * Starting at the current head commit, display information about each commit
     * backwards along the commit tree until the initial commit, following the first
     * parent commit links, ignoring any second parents found in merge commits.
     * (In regular Git, this is what you get with git log --first-parent). This set
     * of commit nodes is called the commit’s history. For every node in this history,
     * the information it should display is the commit id, the time the commit was made,
     * and the commit message.
     */
    public static void log() {
        Commit commit = Commit.readCommitFromFile(HEAD.whichCommit());
        String parent;
        while (true) {
            parent = commit.getParent();
            printCommitInfo(commit);
            if (parent == null) {
                break;
            }
            commit = Commit.readCommitFromFile(parent);
        }
    }

    /**
     *  Like log, except displays information about all commits ever made. The order of
     *  the commits does not matter. Hint: there is a useful method in gitlet.Utils that
     *  will help you iterate over files within a directory.
     */
    public static void globalLog() {
        List<String> fileList = Utils.plainFilenamesIn(INFOCOMMIT_DIR);
        Commit commit;

        for (String filename: fileList) {
            commit = Commit.readCommitFromFile(filename);
            printCommitInfo(commit);
        }
    }

    /**
     * Prints out the ids of all commits that have the given commit message, one per line.
     * If there are multiple such commits, it prints the ids out on separate lines. The
     * commit message is a single operand; to indicate a multiword message, put the operand
     * in quotation marks, as for the commit command below.
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
                System.out.println(commit.getSHA1());
                isFinded = true;
            }
        }
        if (!isFinded) {
            System.out.println("Found no commit with that message.");
        }
    }

    /**
     *  Displays what branches currently exist, and marks the current branch with a *.
     *  Also displays what files have been staged for addition or removal. An example of
     *  the exact format it should follow is as follows.
     */
    public static void status() {
        Set<String> nameSet = new HashSet<>();
        List<String> commitList = Utils.plainFilenamesIn(Repository.INFOCOMMIT_DIR);
        if (commitList.isEmpty()) {
            Repository.abort("Not in an initialized Gitlet directory.");
        }
//        getUntracked();
//        getModifiedNotStaged();
        printBranch();
        printStagedFiles(nameSet);
        printRemovedFiles(nameSet);
        printModifications();
        printUntrackedFiles();
    }

    /**
     * Takes the version of the file as it exists in the head commit and puts it in the
     * working directory, overwriting the version of the file that’s already there if there
     * is one. The new version of the file is not staged.
     */
    public static void checkout(String operand, String filename) {
        // checkout -- [file name]
        if (!operand.equals("--")) {
            Repository.abort("Incorrect operands.");
        }
        String commitId = HEAD.whichCommit();
        overwriteOne(commitId, filename);
        // unstageOne(filename);
        // Only version 3 (checkout of a full branch) modifies the staging area:
        // otherwise files scheduled for addition or removal remain so.
    }


    /**
     *  Takes all files in the commit at the head of the given branch, and puts them in the
     *  working directory, overwriting the versions of the files that are already there if
     *  they exist.
     *  Also, at the end of this command, the given branch will now be considered the
     *  current branch (HEAD). Any files that are tracked in the current branch but are not
     *  present in the checked-out branch are deleted.
     *  The staging area is cleared, unless the checked-out branch is the current branch
     *  (see Failure cases below).
     */
    public static void checkout(String branchName) {
        // checkout [branch name]
        HEAD.readHEAD();
        if (branchName.equals(HEAD.pointBranchName)) {
            abort("No need to checkout the current branch.");
        }
        List<String> branchList = Utils.plainFilenamesIn(BRANCH_DIR);
        if (!branchList.contains(branchName)) {
            abort("No such branch exists.");
        }

        checkUntracked();
        HEAD.switchHEAD(branchName);
        HEAD.saveHEAD();
        deleteCWDall();
        overwriteAll(HEAD.whichCommit());
        unstageAll();
    }

    /**
     * Takes the version of the file as it exists in the commit with the given id, and puts
     * it in the working directory, overwriting the version of the file that’s already there
     * if there is one. The new version of the file is not staged.
     */
    public static void checkout(String commitName, String operand, String fileName) {
        // checkout [commit id] -- [file name]
        if (!operand.equals("--")) {
            Repository.abort("Incorrect operands.");
        }
        overwriteOne(commitName, fileName);
        // unstageOne(fileName);
        // Only version 3 (checkout of a full branch) modifies the staging area:
        // otherwise files scheduled for addition or removal remain so.
    }

    /**
     * Creates a new branch with the given name, and points it at the current head commit.
     * A branch is nothing more than a name for a reference (a SHA-1 identifier) to a commit
     * node. This command does NOT immediately switch to the newly created branch (just as
     * in real Git). Before you ever call branch, your code should be running with a default
     * branch called master.
     */
    public static void branch(String branchName) {
        if (Branch.isBranchExist(branchName)) {
            abort("A branch with that name already exists.");
        }

        Branch newone = new Branch(branchName, HEAD.whichCommit());
        newone.saveBranch();
//        HEAD.switchHEAD(branchName);
    }

    /**
     *  Deletes the branch with the given name. This only means to delete the pointer associated
     *  with the branch; it does not mean to delete all commits that were created under the branch,
     *  or anything like that.
     */
    public static void rmBranch(String branchName) {
        Branch.deleteBranch(branchName);
    }

    /**
     *  Checks out all the files tracked by the given commit. Removes tracked files that are not
     *  present in that commit. Also moves the current branch’s head to that commit node. See the
     *  intro for an example of what happens to the head pointer after using reset. The [commit id]
     *  may be abbreviated as for checkout. The staging area is cleared. The command is essentially
     *  checkout of an arbitrary commit that also changes the current branch head.
    */
    public static void reset(String commitID) {
        deleteCWDall();
        HEAD.switch2commit(commitID);
        overwriteAll(commitID);
        unstageAll();
    }

    public static void merge() {

    }


    /** helper function for add().
     * Compare file in CWD with file in HEAD
     * @return
     */
//    private static Boolean comparefileCWD2HEAD(String name) {
//        HEAD = readHEAD();
//        String SHA1 = HEAD.whichCommit();
//        Commit currenCommit = Commit.readCommitFromFile(SHA1);
//
//        return false;
//    }

    /** helper function for commit().
     *  Move the files in the directory .gitlet/staged_obj/ to the directory .gitlet/commited_obj
     * @param commit
     */
    private static void staged2Commited(Commit commit) {
        moveFromStaged2Commited(commit);
        Blob.deleteBlobMap();
        Blob.deleteRemoval();
    }

    /** helper function for Staged2Commited().
     * @param commit
     */
    private static void moveFromStaged2Commited(Commit commit) {
        List<String> listOfStaged = plainFilenamesIn(STAGE_DIR);
        Blob.loadremoval();
        Blob.loadBlobMap();
        if (Blob.blobMap.isEmpty() && Blob.isRemovalEmpty()) {
            abort("No changes added to the commit.");
        }

        File tmpfile;
        File destfile;
        Boolean isRmNotEmpty = !Blob.isRemovalEmpty();
        for (String file: listOfStaged) { // file is a SHA1 String
            tmpfile = Utils.join(STAGE_DIR, file);
            String name = Blob.blobMap.get(file); // name: hello.c (for example)

            Calendar calendar = Calendar.getInstance();
            Date date = calendar.getTime();
            String shaId = Utils.sha1(name + date.toString());

            if (commit.fileMap.containsKey(name)) {
                commit.fileMap.replace(name, shaId);
            } else {
                commit.fileMap.put(name, shaId); //map from file name (hello.c) to SHA1 String
            }
            destfile = Utils.join(COMMITED_DIR, shaId);
//            tmpfile.renameTo(destfile);
            try {
                Files.copy(tmpfile.toPath(), destfile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException excp) {
                System.out.println(excp.getMessage());
            }
            tmpfile.delete();
        }

        if (!isRmNotEmpty) {
            return;
        }
        /** Remove the entry of unstaged files from fileMap. */
        for (Map.Entry<String, String> entry : Blob.removal.entrySet()) {
            if (commit.fileMap.containsKey(entry.getKey())) {
                commit.fileMap.remove(entry.getKey());
            }
        }
    }

    /**
     *   Copy snapshots of current Commit to the new Commit commit.
     *   If no snapshots, namely fileMap of current Commit is empty,
     *   just create a new TreeMap.
     */
    private static void copySnapshot(Commit commit) {
        Commit lastestCommit = Commit.readCommitFromFile(HEAD.whichCommit());
        if (lastestCommit.fileMap != null) {
            commit.fileMap = (TreeMap<String, String>) lastestCommit.fileMap.clone();
        } else {
            commit.fileMap = new TreeMap<>();
        }
    }

    /**
     *  Check current Commit for file to unstage from it.
     *  If the file exists in the Commit, move it from .gitlet/commited_obj to .gitlet/unstaged_obj.
     *  --And do not remove the key-value pair from fileMap.--
     *  If it is in the Commit, remove the file from CWD.
     */
    private static boolean checkCommit2Unstaged(String filename) {
        Commit commit = Commit.readCommitFromFile(HEAD.whichCommit());
        if (commit.fileMap == null) {
            return false;
        }
        if (commit.fileMap.containsKey(filename)) {
            File cwdfile = Utils.join(CWD, filename);
            cwdfile.delete();
            return true;
        }
        return false;
    }

    /**
     *  Unstaged the file if it is currently staged for addition.
     *  Delete the file and update the blobMap.
     */
    private static boolean unstageOne(String filename, boolean isInCommit) {
        String shaId = Utils.sha1(filename);
        File destfile = join(STAGE_DIR, shaId);
        if (destfile.exists()) {
            destfile.delete();
            Blob.deteleItem(filename);
            return true;
        }
        return false;
    }

    /**
     *  Unstaged all of the files if it is currently staged for addition.
     *  Delete them and update the blobMap.
     */
    private static void unstageAll() {
        List<String> fileList = Utils.plainFilenamesIn(STAGE_DIR);
        File file;
        for (String filename: fileList) {
            file = Utils.join(STAGE_DIR, filename);
            file.delete();
        }
        Blob.deleteBlobMap();
    }

    /**
     *  Helper function for log(), global-log().
     *  Print the commit information.
     */
    private static void printCommitInfo(Commit commit) {
        System.out.println("===");
        System.out.println("commit " + commit.getSHA1());
        System.out.println("Date: " + commit.getDate());
        System.out.println(commit.getMessage());
        System.out.println();
    }

    /** Helper function for status(). */
    private static void printBranch() {
        System.out.println("=== Branches ===");
        List<String> branchList = Utils.plainFilenamesIn(BRANCH_DIR);
        HEAD.readHEAD();

        for (String branchName: branchList) {
            if (branchName.equals("HEAD")) {
                continue;
            }
            if (branchName.equals(HEAD.pointBranchName)) {
                System.out.print('*');
            }
            System.out.println(branchName);
        }
        System.out.println();
    }

    /** Helper function for status(). */
    private static void printStagedFiles(Set<String> nameSet) {
        System.out.println("=== Staged Files ===");
        TreeMap<String, String> blobMap = Blob.getTreeMap(Blob.blobMap, false);
        if (blobMap == null) {
            abort("");
        }
        String value;
        for (Map.Entry<String, String> entry : blobMap.entrySet()) {
            value = entry.getValue();
            nameSet.add(value);
            System.out.println(value);
        }

        System.out.println();
    }

    /** Helper function for status(). */
    private static void printRemovedFiles(Set<String> nameSet) {
        System.out.println("=== Removed Files ===");
        TreeMap<String, String> removal = Blob.getTreeMap(Blob.removal, true);
        String key;
        for (Map.Entry<String, String> entry : removal.entrySet()) {
            key = entry.getKey();
//            nameSet.add(key);
            System.out.println(key);
        }
        System.out.println();
    }

    /** Helper function for status().
     *  modified but not staged.
     *  Tracked in the current commit, changed in the working directory, but not staged.
     *  Staged for addition, but with different contents than in the working directory.
     *  Staged for addition, but deleted in the working directory.
     *  Not staged for removal, but tracked in the current commit and deleted from the
     *  working directory.
     * */
    private static void printModifications() {
        System.out.println("=== Modifications Not Staged For Commit ===");

        Commit currentCommit = Commit.readCommitFromFile(HEAD.whichCommit());
        List<String> fileList = Utils.plainFilenamesIn(Repository.CWD);
        Blob.loadBlobMap();
        Blob.loadremoval();
        for (String file : fileList) {
            if ((Blob.blobMap == null  || !Blob.isBlobmapContains(file)) && currentCommit.isFilemapContains(file)) {
                /** Committed but changed and unstaged. */
                File rawfile = Utils.join(COMMITED_DIR, currentCommit.fileMap.get(file));
                File cwdfile= Utils.join(CWD, file);
                if (!isFileSame(cwdfile, rawfile)) {
                    System.out.println(file + "(modified)");
                }
            } else if (!currentCommit.isFilemapContains(file) && Blob.blobMap != null && Blob.isBlobmapContains(file)) {
                /** Staged but not commited and changed.*/
                String sha1 = Utils.sha1(file);
                File rawfile = Utils.join(STAGE_DIR, sha1);
                File cwdfile= Utils.join(CWD, file);
                if (!isFileSame(cwdfile, rawfile)) {
                    System.out.println(file + "(modified)");
                }
            }
        }

        if (Blob.blobMap != null) {
            for (Map.Entry<String, String> entry : Blob.blobMap.entrySet()) {
                if (!fileList.contains(entry.getValue())) {
                    /** file is deleted. */
                    System.out.println(entry.getValue() + "(deleted)");
                }
            }
        }

        if (currentCommit.fileMap != null) {
            for (Map.Entry<String, String> entry : currentCommit.fileMap.entrySet()) {
                if (!fileList.contains(entry.getKey())  &&
                        (Blob.removal == null || !Blob.isRemovalContains(entry.getKey()))) {
                    /** Committed and deleted but not unstaged. **/
                    System.out.println(entry.getKey() + "(deleted)");
                }
            }
        }
        System.out.println();
    }

    /** Helper function for status(). */
    private static void printUntrackedFiles() {
        System.out.println("=== Untracked Files ===");
        Blob.loadBlobMap();
        Commit currentCommit = Commit.readCommitFromFile(HEAD.whichCommit());
        List<String> fileList = Utils.plainFilenamesIn(CWD);
        for (String file : fileList) {
            /** Files presents in CWD, but neither staged nor tracked. **/
            if (!Blob.isBlobmapContains(file))
                if ( currentCommit.fileMap == null || !currentCommit.isFilemapContains(file)) {
                    System.out.println(file);
                }
        }
        System.out.println();
    }

    /** Overwrite file named filename in commitSHA. */
    private static void overwriteOne(String commitSHA, String filename) {
        Commit commit = Commit.readCommitFromFile(commitSHA);
        if (!commit.isFilemapContains(filename)) {
            abort("File does not exist in that commit.");
        }
        String shaId = commit.fileMap.get(filename);
        File dir = Utils.join(Repository.COMMITED_DIR, shaId);
        File dest = join(CWD, filename);
        Utils.secureCopyFile(dir, dest);
    }

    /** Overwrite all files in CWD in commitSHA. */
    private static void overwriteAll(String commitSHA) {
        Commit commit = Commit.readCommitFromFile(commitSHA);
        if (commit.fileMap == null) {
            deleteCWDall();
            return;
        }
        for (Map.Entry<String, String> entry : commit.fileMap.entrySet()) {
            String shaId = commit.fileMap.get(entry.getKey());
            File dir = Utils.join(Repository.COMMITED_DIR, shaId);
            File dest = join(CWD, entry.getKey());
            Utils.secureCopyFile(dir, dest);
        }
    }

    /** If there are some files untracked, just abort. */
    private static void checkUntracked() {
        List<String> fileList = Utils.plainFilenamesIn(CWD);
        Commit currentCommit = Commit.readCommitFromFile(HEAD.whichCommit());
        for (String file : fileList) {
            if (!currentCommit.isFilemapContains(file)) {
                abort("There is an untracked file in the way; delete it, or add and commit it first.");
            }
        }
    }

    /**
     */
    private static void deleteCWDall() {
        List<String> fileList = Utils.plainFilenamesIn(CWD);
        File file;
        for (String filename: fileList) {
            file = join(CWD, filename);
            Utils.restrictedDelete(filename);
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

    /** Abort with message printed.
     * @param msg message.
     */
    public static void abort(String msg) {
        System.out.println(msg);
        System.exit(0);
    }

    /**
     *  Debug helper function.
     *  Print fileMap of the Commit pointed by HEAD.
     */
    private static void testCommit() {
        Commit tmp = Commit.readCommitFromFile(HEAD.whichCommit());
        for (Map.Entry<String, String> entry : tmp.fileMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            System.out.println(key + " => " + value);
        }

    }

    /**
     *  Debug helper function.
     */
    public static void testBlob() {
        Blob.blobMap = Blob.getTreeMap(Blob.blobMap, false);
        for (Map.Entry<String, String> entry : Blob.blobMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            System.out.println(key + " => " + value);
        }
    }

    private static void traceback() {
        Commit commit = Commit.readCommitFromFile(HEAD.whichCommit());
        String parent;
        while (true) {
            parent = commit.getParent();
            printCommitInfo(commit);
            if (parent == null) {
                break;
            }
            commit = Commit.readCommitFromFile(parent);
        }
    }

    public static void printCommitInfo() {
        traceback();
        HEAD.switchHEAD("fork");
        traceback();
    }

    public static void printStaged() {

    }

    public static void printUnstaged() {

    }

    public static void main(String[] args) {
        Formatter fmt = new Formatter();
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
        fmt.format("%ta %tb %td %tr %tY %tz", cal, cal, cal, cal, cal, cal);
        Date date = cal.getTime();
        System.out.println(fmt);

        if (args.length == 0) {
            System.out.println("select --> blobMap, commit, remove, branch");
        }

        switch (args[0]) {
            case "blobMap":
                testBlob();
                break;
            case "commit":
                testCommit();
                break;
            case "remove":
                break;

            case "branch":
                printCommitInfo();
                break;

            case "stage":
                break;

            case "unstage":
                break;

            default:
                break;
        }
    }

}

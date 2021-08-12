package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import static gitlet.Utils.*;

/***
 *
 * @author Hongfa You
 */
public class Branch implements Serializable {
    /** Name of Brunch, such as "master" **/
    private String branchName;
    /** Indicates the Commit this Branch should point to, using a SHA1 string **/
    private String whichCommit;

    /** Constructor of Branch
     * @param name Name of Brunch such as "master", "HEAD".
     * @param which Indicates the Commit this Branch should point to, using a SHA1 string
     */
    public Branch(String name, String which) {
        branchName = name;
        whichCommit = which;
    }

    /** Saves this Branch for future use, and name of the Object
     * in the File System is branchName
     **/
    public void saveBranch() {
        File outfile = Utils.join(Repository.BRANCH_DIR,  this.branchName);
        writeObject(outfile, this);
    }

    /** Reads in and deserializes a branch from a file.
     * @param name the name of Branch
     * @return the Branch read in.
     */
    public static Branch readBranchIn(String name, Boolean isMerge) {
        File file = join(Repository.BRANCH_DIR, name);
        if (!file.exists() && !isMerge) {
            Utils.abort("No such branch exists.");
        } else if (!file.exists() && isMerge) {
            Utils.abort("A branch with that name does not exist.");
        }
        Branch result = readObject(file, Branch.class);
        return result;
    }


    /** Return the Commit pointed by this Branch. */
    public String whichCommit() {
        return this.whichCommit;
    }

    /** Make this Branch point to another Commit named commit
     * @param commit : SHA1 String of new commit.
     */
    public void resetWhichCommit(String commit) {
        this.whichCommit = commit;
        this.saveBranch();
    }

    /** Check if Branch called name exists or not.
     * @param name : name of branch.
     * @return True if Branch called name exists, or return false.
     */
    public static Boolean isBranchExist(String name) {
        File file = join(Repository.BRANCH_DIR, name);
        return file.exists();
    }

    /** Delete the Branch whose name is branchName.
     *  If the Branch is current Branch, just abort.
     *  If it doesn't exists, just abort.
     * @param branchName : name of Branch.
     */
    protected static void deleteBranch(String branchName) {
        HEAD.readHEAD();
        if (branchName.equals(HEAD.getPointBranch())) { //don't use ==
            Utils.abort("Cannot remove the current branch.");
        }
        List<String> branchList = Utils.plainFilenamesIn(Repository.BRANCH_DIR);
        if (branchList.contains(branchName)) {
            File file = Utils.join(Repository.BRANCH_DIR, branchName);
            file.delete();
        } else {
            Utils.abort("A branch with that name does not exist.");
        }
    }

}


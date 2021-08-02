package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Hongfa You
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // if args is empty, print and exit
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }

        Repository.mkalldir();
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                // handle the `init` command
                Repository.init();
                break;

            case "add":
                // handle the `add [filename]` command
                if (args.length <= 1) {
                    System.out.println("Please specify a file to be staged.");
                    System.exit(0);
                } else {
                    Repository.add(args[1]);
                }
                break;

            case "commit":
                if (args.length < 2) {
                    System.out.println("Please enter a commit message.");
                    System.exit(0);
                }
                Repository.commit(args[1]);
                break;

            case "rm":
                if (args.length < 2) {
                    System.out.println("Please specify a file to be unstaged.");
                    System.exit(0);
                }
                Repository.rm(args[1]);
                break;

            case "log":
                Repository.log();
                break;

            case "global-log":
                Repository.globalLog();
                break;

            case "find":
                Repository.find(args[1]);
                break;

            case "status":
                Repository.status();
                break;

            case "checkout":
                if (args.length == 2) {
                    // checkout [branch name]
                    Repository.checkout(args[1]);
                } else if (args.length == 3) {
                    // checkout -- [file name]
                    Repository.checkout(args[2], true);
                } else if (args.length == 4) {
                    // checkout [commit id] -- [file name]
                    Repository.checkout(args[1], args[3]);
                }
                break;

            case "branch":
                Repository.branch(args[1]);
                break;

            case "rm-branch":
                Repository.rmBranch(args[1]);
                break;

            case "reset":
                Repository.reset(args[1]);
                break;

            case "merge":
                //
                break;
        }
    }
}

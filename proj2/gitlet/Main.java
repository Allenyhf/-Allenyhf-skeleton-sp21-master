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
            Utils.abort("Please enter a command.");
        }

        String firstArg = args[0];
        if (!firstArg.equals("init")) {
            Repository.mkalldir();
        }
        switch(firstArg) {
            case "init":
                // handle the `init` command
                Repository.init();
                break;

            case "add":
                // handle the `add [filename]` command
                if (args.length <= 1) {
                    Utils.abort("Please specify a file to be staged.");
                } else {
                    Repository.add(args[1]);
                }
                break;

            case "commit":
                if (args.length < 2) {
                    Utils.abort("Please enter a commit message.");
                }
                Repository.commit(args[1], null);
                break;

            case "rm":
                if (args.length < 2) {
                    Utils.abort("Please specify a file to be unstaged.");
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
                switch (args.length) {
                    case 2:
                    // checkout [branch name]
                    Repository.checkout(args[1]);
                        break;
                    case 3:
                    // checkout -- [file name]
                    Repository.checkout(args[1], args[2]);
                        break;
                    case 4:
                    // checkout [commit id] -- [file name]
                    Repository.checkout(args[1], args[2], args[3]);
                        break;
                    default:
                        break;
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
                Repository.merge(args[1]);
                break;

            default:
                Utils.abort("No command with that name exists.");
                break;
        }
    }
}

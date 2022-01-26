public class MyStatus {

    protected static String master;
    protected static boolean isRunning;

    public static void initialize(String master) {

        if (!isRunning) {
            MyStatus.master = master;
            System.out.println(master + " call");
            isRunning = true;
        } else {
            System.out.println("Already been initialed");
        }
    }
}

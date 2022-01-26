public class SimpleGoto {

    public SimpleGoto() {

        func1();
    }

    private void func1() {
        int a = 1;
        func2();
        func3();
    }

    private void func2() {
        int b = 2;
        func4();
        func5();
    }

    private void func3() {
        int c = 3;
        func6();
        func7();
    }

    private void func4() {
        int d = 4;
        for (int i = 0; i < 20; i++) {
            d += i;
            if (d > 15) break;
        }
        d = 10;
    }

    private void func5() {
        int e = 5;
        while (e > 0) {
            e--;
        }
        e = -1;
    }

    private void func6() {
        int f = 6;
    }

    private void func7() {
        int g = 7;
    }

    public static void main(String[] args) {

        SimpleGoto simpleGoto = new SimpleGoto();
    }
}

package it.unive.scsr;

public class Main {
    public static void main(String[] args) {
        Parity a = Parity.fromConstant(4); // EVEN
        Parity b = Parity.fromConstant(3); // ODD

        System.out.println("4 + 3 = " + a.plus(b)); // ODD
        System.out.println("4 * 3 = " + a.times(b)); // EVEN
        System.out.println("-3 = " + b.negate()); // ODD
        System.out.println("3 / 2 = " + b.div(Parity.fromConstant(2))); // TOP
    }
}

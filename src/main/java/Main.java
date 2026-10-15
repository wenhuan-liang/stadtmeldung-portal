import database.initialization.DatabaseInitializer;

public class Main {

    public static void main(String[] args) {
        DatabaseInitializer.initialize();
        System.out.println("Anwendung gestartet!");
    }
}
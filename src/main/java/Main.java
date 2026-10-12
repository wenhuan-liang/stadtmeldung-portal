import database.initialization.DatabaseInitializer;
import java.sql.Connection;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {

        DatabaseInitializer.initialize();

        System.out.println(
            "Datenbankinitialisierung abgeschlossen!"
        );
    }
}
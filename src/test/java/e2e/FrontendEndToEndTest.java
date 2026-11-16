package e2e;

import com.stadtmeldung.StadtmeldungApplication;
import model.User;
import model.enums.Status;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import service.interfaces.ReportService;
import service.interfaces.UserService;
import testsupport.TestDatabase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-End-Tests mit Selenium: Die komplette Anwendung wird auf einem
 * freien Port gestartet und ein echter Chrome-Browser (ohne Fenster)
 * klickt sich wie ein Mensch durch die Oberfläche.
 *
 * Start: mvn test -Pe2e
 * Screenshots jedes Schritts: target/e2e-screenshots/
 *
 * Voraussetzungen: Google Chrome installiert und Internet (Leaflet-Karte
 * wird von unpkg.com geladen, ChromeDriver lädt Selenium automatisch).
 */
@Tag("e2e")
@SpringBootTest(classes = StadtmeldungApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("End-to-End (Selenium + Chrome)")
class FrontendEndToEndTest {

    static {
        TestDatabase.init();
    }

    private static final String PASSWORD = "e2e-passwort-123";
    private static final Path SCREENSHOTS = Path.of("target", "e2e-screenshots");

    @LocalServerPort
    private int port;

    @Autowired
    private UserService userService;

    @Autowired
    private ReportService reportService;

    private WebDriver driver;
    private WebDriverWait wait;
    private String testName;
    private int step;

    @BeforeAll
    void setUpUsersAndBrowser() throws IOException {
        TestDatabase.clearAll();
        User admin = userService.provisionAdmin("e2e.admin", PASSWORD);
        userService.createEmployee(admin.getId(), "e2e.mitarbeiter", PASSWORD);
        userService.registerCitizen("e2e.buerger", PASSWORD);

        Files.createDirectories(SCREENSHOTS);
        ChromeOptions options = new ChromeOptions();
        if (!"false".equals(System.getProperty("headless"))) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1400,1000", "--lang=de-DE");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        // Die Liste wird nach Änderungen per JavaScript neu aufgebaut;
        // alte Element-Referenzen werden dabei ungültig -> einfach erneut suchen
        wait.ignoring(StaleElementReferenceException.class);
    }

    @AfterAll
    void closeBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    @BeforeEach
    void freshSession(org.junit.jupiter.api.TestInfo info) {
        TestDatabase.clearReports();
        testName = info.getTestMethod().orElseThrow().getName();
        step = 0;
        driver.manage().deleteAllCookies();
    }

    @AfterEach
    void finalScreenshot() {
        screenshot("ende");
    }

    // ---------- Hilfsmethoden ----------

    private void open(String page) {
        driver.get("http://127.0.0.1:" + port + "/frontend/" + page);
    }

    private WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    private void type(String id, String text) {
        WebElement input = visible(By.id(id));
        input.clear();
        input.sendKeys(text);
    }

    private void screenshot(String label) {
        try {
            Path file = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE).toPath();
            String name = "%s_%02d_%s.png".formatted(testName, ++step, label);
            Files.copy(file, SCREENSHOTS.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {
            // Screenshots sind nur Dokumentation und dürfen keinen Test scheitern lassen
        }
    }

    private void waitForPage(String page) {
        wait.until(ExpectedConditions.urlContains(page));
        // Seite ist bereit, sobald auth.js die Navigationsleiste befüllt hat
        wait.until(d -> !d.findElement(By.id("navbar-auth")).getText().isBlank());
    }

    private void login(String role, String username) {
        open("login.html");
        visible(By.id("btn-role-" + role)).click();
        type(role + "-login-username", username);
        type(role + "-login-password", PASSWORD);
        screenshot("login-" + role);
        driver.findElement(By.id("btn-" + role + "-login")).click();
        waitForPage("index.html");
    }

    private WebElement reportCardWithTitle(String title) {
        By card = By.xpath("//li[contains(@class,'report-card')][.//h3[normalize-space()='" + title + "']]");
        return visible(card);
    }

    private void fillReportForm(String title) {
        type("input-title", title);
        new Select(visible(By.id("input-category"))).selectByValue("Defekt");
        new Select(visible(By.id("input-defect-type"))).selectByValue("POTHOLE");
        type("input-description", "Tiefes Schlagloch, Gefahr für Radfahrer.");
        // Karte muss geladen sein, dann Klick in die Mitte der Karte als Standort
        wait.until(d -> d.findElement(By.id("map")).getAttribute("class").contains("leaflet-container"));
        driver.findElement(By.id("map")).click();
        visible(By.id("coord-display"));
    }

    // ---------- Testfälle ----------

    @Test
    @DisplayName("E2E-1: Besucher meldet ohne Konto einen Schaden und findet ihn in der Übersicht")
    void anonymousCitizenCreatesReport() {
        String title = "E2E Schlagloch " + System.currentTimeMillis();

        open("index.html");
        waitForPage("index.html");
        screenshot("startseite");
        visible(By.id("btn-neue-meldung")).click();

        waitForPage("neue-meldung.html");
        fillReportForm(title);
        screenshot("formular-ausgefuellt");
        driver.findElement(By.cssSelector("#neue-meldung-form button[type=submit]")).click();

        waitForPage("index.html");
        WebElement card = reportCardWithTitle(title);
        screenshot("meldung-in-liste");
        assertTrue(card.getText().contains("Wartet auf Bearbeitung durch die Stadt"));
        assertEquals(1, reportService.findAll().size(), "genau eine Meldung in der Datenbank");
    }

    @Test
    @DisplayName("E2E-2: Formular ohne Standort zeigt eine verständliche Fehlermeldung")
    void reportWithoutLocationShowsError() {
        open("neue-meldung.html");
        waitForPage("neue-meldung.html");
        type("input-title", "Ohne Standort");
        new Select(visible(By.id("input-category"))).selectByValue("Sonstiges");
        type("input-description", "Test");
        driver.findElement(By.cssSelector("#neue-meldung-form button[type=submit]")).click();

        WebElement error = visible(By.id("form-error"));
        screenshot("fehlermeldung");
        assertEquals("Bitte wählen Sie einen Standort auf der Karte aus.", error.getText());
        assertTrue(reportService.findAll().isEmpty());
    }

    @Test
    @DisplayName("E2E-3: Bürger registriert sich, meldet einen Schaden und stimmt dafür ab")
    void citizenRegistersReportsAndVotes() {
        String username = "neu" + System.currentTimeMillis();
        String title = "E2E Laterne " + System.currentTimeMillis();

        open("login.html");
        visible(By.id("link-to-register")).click();
        type("user-register-username", username);
        type("user-register-password", PASSWORD);
        screenshot("registrierung");
        driver.findElement(By.id("btn-user-register")).click();

        waitForPage("index.html");
        assertEquals(username, visible(By.cssSelector("#navbar-auth .nav-user")).getText());
        visible(By.id("btn-neue-meldung")).click();
        waitForPage("neue-meldung.html");
        fillReportForm(title);
        driver.findElement(By.cssSelector("#neue-meldung-form button[type=submit]")).click();

        // Angemeldete Bürger werden zu „Meine Meldungen“ weitergeleitet
        waitForPage("meine-meldungen.html");
        WebElement card = reportCardWithTitle(title);
        assertTrue(card.getText().contains("Von mir gemeldet"));
        screenshot("meine-meldungen");

        card.findElement(By.cssSelector("[data-action=vote]")).click();
        wait.until(d -> reportCardWithTitle(title).getText().contains("1 Stimme"));
        screenshot("abgestimmt");
        assertTrue(reportCardWithTitle(title).getText().contains("Stimme zurücknehmen"));
    }

    @Test
    @DisplayName("E2E-4: Mitarbeiter meldet sich an und setzt eine Meldung auf erledigt")
    void employeeCompletesReport() {
        var report = reportService.createReport(null, "E2E Müll", model.enums.Category.LITTER,
                "", "Sperrmüll am Straßenrand", "", 49.48, 8.46);

        login("staff", "e2e.mitarbeiter");
        WebElement card = reportCardWithTitle("E2E Müll");
        screenshot("dashboard");
        new Select(card.findElement(By.cssSelector("select.report-status"))).selectByValue("resolved");

        wait.until(d -> reportCardWithTitle("E2E Müll")
                .findElement(By.cssSelector(".status-badge")).getText().equals("Erledigt"));
        screenshot("status-erledigt");
        assertEquals(Status.COMPLETED, reportService.findById(report.getId()).getStatus());
    }

    @Test
    @DisplayName("E2E-5: falsches Passwort zeigt Fehlermeldung, kein Login")
    void wrongPasswordShowsError() {
        open("login.html");
        visible(By.id("btn-role-user")).click();
        type("user-login-username", "e2e.buerger");
        type("user-login-password", "falsches-passwort");
        driver.findElement(By.id("btn-user-login")).click();

        WebElement error = visible(By.id("user-login-error"));
        screenshot("fehler");
        assertEquals("Benutzername oder Passwort ungültig.", error.getText());
        assertTrue(driver.getCurrentUrl().contains("login.html"));
    }

    @Test
    @DisplayName("E2E-6: Bürgerkonto kann sich nicht im Mitarbeiterbereich anmelden")
    void citizenCannotUseStaffLogin() {
        open("login.html");
        visible(By.id("btn-role-staff")).click();
        type("staff-login-username", "e2e.buerger");
        type("staff-login-password", PASSWORD);
        driver.findElement(By.id("btn-staff-login")).click();

        WebElement error = visible(By.id("staff-login-error"));
        screenshot("fehler");
        assertEquals("Dieser Account kann sich in diesem Bereich nicht anmelden.", error.getText());
    }

    @Test
    @DisplayName("E2E-7: Admin legt über die Oberfläche ein Mitarbeiterkonto an")
    void adminCreatesEmployee() {
        String username = "kollege" + System.currentTimeMillis();

        login("staff", "e2e.admin");
        visible(By.cssSelector("a.tab[href='mitarbeiter.html']")).click();
        waitForPage("mitarbeiter.html");
        type("employee-username", username);
        type("employee-password", PASSWORD);
        screenshot("formular");
        driver.findElement(By.cssSelector("#employee-form button[type=submit]")).click();

        WebElement success = visible(By.id("employee-success"));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.id("employee-list"), "@" + username));
        screenshot("mitarbeiter-angelegt");
        assertTrue(success.getText().contains(username));
    }
}

package service.implementation;

import model.Report;
import model.User;
import model.enums.Category;
import model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.interfaces.ReportRepository;
import repository.interfaces.ReportVoteRepository;
import repository.interfaces.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit-Tests für {@link ReportVoteServiceImpl}: Bürger unterstützen Meldungen
 * mit einer Stimme.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReportVoteService")
class ReportVoteServiceImplTest {

    @Mock
    private ReportVoteRepository voteRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    // ReportVoteServiceImpl ist final, daher manuell statt mit @InjectMocks erzeugt
    private ReportVoteServiceImpl voteService;

    @BeforeEach
    void setUp() {
        voteService = new ReportVoteServiceImpl(voteRepository, reportRepository, userRepository);
    }

    private static User user(long id, Role role) {
        User user = new User("user" + id, "user" + id, "hash", role);
        user.setId(id);
        return user;
    }

    private void reportExists(long reportId) {
        Report report = new Report(1L, Category.LITTER, "Müll", 49.0, 8.0);
        report.setId(reportId);
        when(reportRepository.findById(reportId)).thenReturn(report);
    }

    @Test
    @DisplayName("Bürger kann für eine Meldung stimmen")
    void citizenCanVote() {
        reportExists(10);
        when(userRepository.findById(1L)).thenReturn(user(1, Role.CITIZEN));
        when(voteRepository.addVote(10L, 1L)).thenReturn(true);

        assertTrue(voteService.vote(10L, 1L));
    }

    @Test
    @DisplayName("zweite Stimme desselben Bürgers zählt nicht (Ergebnis false)")
    void secondVoteIsIgnored() {
        reportExists(10);
        when(userRepository.findById(1L)).thenReturn(user(1, Role.CITIZEN));
        when(voteRepository.addVote(10L, 1L)).thenReturn(false);

        assertFalse(voteService.vote(10L, 1L));
    }

    @Test
    @DisplayName("Bürger kann seine Stimme zurücknehmen")
    void citizenCanRemoveVote() {
        reportExists(10);
        when(userRepository.findById(1L)).thenReturn(user(1, Role.CITIZEN));
        when(voteRepository.removeVote(10L, 1L)).thenReturn(true);

        assertTrue(voteService.removeVote(10L, 1L));
    }

    @ParameterizedTest(name = "Rolle {0} darf nicht abstimmen")
    @EnumSource(value = Role.class, names = {"EMPLOYEE", "ADMIN"})
    @DisplayName("Stadt-Team darf nicht abstimmen")
    void staffCannotVote(Role role) {
        reportExists(10);
        when(userRepository.findById(2L)).thenReturn(user(2, role));

        assertThrows(SecurityException.class, () -> voteService.vote(10L, 2L));
        verify(voteRepository, never()).addVote(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Abstimmung für nicht existierende Meldung wird abgelehnt")
    void cannotVoteForUnknownReport() {
        when(reportRepository.findById(99L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> voteService.vote(99L, 1L));
        verify(voteRepository, never()).addVote(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Stimmenzahl wird aus dem Repository gelesen")
    void countReturnsNumberOfVotes() {
        reportExists(10);
        when(voteRepository.countByReportId(10L)).thenReturn(7);

        assertEquals(7, voteService.count(10L));
    }

    @Test
    @DisplayName("ungültige IDs werden ohne Datenbankzugriff abgelehnt")
    void rejectsInvalidIds() {
        assertThrows(IllegalArgumentException.class, () -> voteService.vote(0L, 1L));
        assertThrows(IllegalArgumentException.class, () -> voteService.hasVoted(1L, -1L));
        assertThrows(IllegalArgumentException.class, () -> voteService.findReportIdsByUserId(0L));
        verifyNoInteractions(voteRepository, reportRepository, userRepository);
    }
}

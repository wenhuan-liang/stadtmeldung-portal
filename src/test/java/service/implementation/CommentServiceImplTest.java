package service.implementation;

import model.Comment;
import model.Report;
import model.User;
import model.enums.Category;
import model.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.interfaces.CommentRepository;
import repository.interfaces.ReportRepository;
import repository.interfaces.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-Tests für {@link CommentServiceImpl}: öffentliche Bürger-Updates
 * und interne Kommentare des Stadt-Teams.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService")
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

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

    private static Comment existingComment(long id, long ownerId) {
        Comment comment = new Comment(10L, ownerId, "Alter Text");
        comment.setId(id);
        return comment;
    }

    @Nested
    @DisplayName("Öffentliche Updates von Bürgern")
    class PublicUpdates {

        @Test
        @DisplayName("Bürger kann ein öffentliches Update mit Text veröffentlichen")
        void citizenAddsPublicUpdate() {
            reportExists(10);
            when(userRepository.findById(1L)).thenReturn(user(1, Role.CITIZEN));

            Comment result = commentService.createComment(10L, 1L, "  Loch ist größer geworden  ");

            assertEquals("Loch ist größer geworden", result.getText());
            assertFalse(result.isInternal());
            verify(commentRepository).save(result);
        }

        @Test
        @DisplayName("Update nur mit Foto und ohne Text ist erlaubt")
        void photoOnlyUpdateIsAllowed() {
            reportExists(10);
            when(userRepository.findById(1L)).thenReturn(user(1, Role.CITIZEN));

            Comment result = commentService.createComment(10L, 1L, null, "uploads/foto.jpg");

            assertEquals("", result.getText());
            assertEquals("uploads/foto.jpg", result.getPhotoPath());
        }

        @Test
        @DisplayName("Update ohne Text und ohne Foto wird abgelehnt")
        void emptyUpdateIsRejected() {
            assertThrows(IllegalArgumentException.class,
                    () -> commentService.createComment(10L, 1L, "   ", ""));
            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Text: 1000 Zeichen erlaubt, 1001 Zeichen abgelehnt")
        void textLengthLimit() {
            reportExists(10);
            when(userRepository.findById(1L)).thenReturn(user(1, Role.CITIZEN));

            commentService.createComment(10L, 1L, "a".repeat(1000));
            assertThrows(IllegalArgumentException.class,
                    () -> commentService.createComment(10L, 1L, "a".repeat(1001)));
            verify(commentRepository).save(any(Comment.class));
        }

        @ParameterizedTest(name = "Rolle {0} darf keine öffentlichen Updates schreiben")
        @EnumSource(value = Role.class, names = {"EMPLOYEE", "ADMIN"})
        @DisplayName("Stadt-Team schreibt keine öffentlichen Bürger-Updates")
        void staffCannotAddPublicUpdate(Role role) {
            reportExists(10);
            when(userRepository.findById(2L)).thenReturn(user(2, role));

            assertThrows(SecurityException.class,
                    () -> commentService.createComment(10L, 2L, "Text"));
            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Update zu nicht existierender Meldung wird abgelehnt")
        void unknownReport() {
            when(reportRepository.findById(99L)).thenReturn(null);

            assertThrows(IllegalArgumentException.class,
                    () -> commentService.createComment(99L, 1L, "Text"));
            verify(commentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Interne Kommentare")
    class InternalComments {

        @ParameterizedTest(name = "Rolle {0} darf interne Kommentare schreiben")
        @EnumSource(value = Role.class, names = {"EMPLOYEE", "ADMIN"})
        @DisplayName("Mitarbeiter und Admins können interne Kommentare speichern")
        void staffAddsInternalComment(Role role) {
            reportExists(10);
            when(userRepository.findById(2L)).thenReturn(user(2, role));

            Comment result = commentService.createInternalComment(10L, 2L, "Bauhof informiert");

            assertTrue(result.isInternal());
            verify(commentRepository).save(result);
        }

        @Test
        @DisplayName("Bürger darf keine internen Kommentare schreiben")
        void citizenCannotAddInternalComment() {
            reportExists(10);
            when(userRepository.findById(1L)).thenReturn(user(1, Role.CITIZEN));

            assertThrows(SecurityException.class,
                    () -> commentService.createInternalComment(10L, 1L, "Text"));
            verify(commentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Kommentare bearbeiten und löschen")
    class EditAndDelete {

        @Test
        @DisplayName("Verfasser kann eigenen Kommentar bearbeiten")
        void ownerCanEdit() {
            Comment comment = existingComment(5, 1);
            when(commentRepository.findById(5L)).thenReturn(comment);

            commentService.updateComment(5L, " Neuer Text ", 1L);

            assertEquals("Neuer Text", comment.getText());
            verify(commentRepository).update(comment);
        }

        @Test
        @DisplayName("fremder Kommentar kann nicht bearbeitet werden")
        void otherUserCannotEdit() {
            when(commentRepository.findById(5L)).thenReturn(existingComment(5, 1));

            assertThrows(SecurityException.class,
                    () -> commentService.updateComment(5L, "Text", 2L));
            verify(commentRepository, never()).update(any());
        }

        @Test
        @DisplayName("Verfasser kann eigenen Kommentar löschen")
        void ownerCanDelete() {
            when(commentRepository.findById(5L)).thenReturn(existingComment(5, 1));

            commentService.deleteComment(5L, 1L);

            verify(commentRepository).deleteById(5L);
        }

        @Test
        @DisplayName("fremder Kommentar kann nicht gelöscht werden")
        void otherUserCannotDelete() {
            when(commentRepository.findById(5L)).thenReturn(existingComment(5, 1));

            assertThrows(SecurityException.class, () -> commentService.deleteComment(5L, 2L));
            verify(commentRepository, never()).deleteById(anyLong());
        }
    }
}

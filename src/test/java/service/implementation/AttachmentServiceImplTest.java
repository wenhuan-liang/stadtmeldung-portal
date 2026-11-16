package service.implementation;

import model.Attachment;
import model.Report;
import model.enums.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import repository.interfaces.AttachmentRepository;
import repository.interfaces.ReportRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit-Tests für {@link AttachmentServiceImpl}: Anhänge dürfen nur vom
 * Eigentümer der Meldung verwaltet werden.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AttachmentService")
class AttachmentServiceImplTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private AttachmentServiceImpl attachmentService;

    private void reportOwnedBy(long reportId, Long ownerId) {
        Report report = new Report(ownerId, Category.LITTER, "Müll", 49.0, 8.0);
        report.setId(reportId);
        when(reportRepository.findById(reportId)).thenReturn(report);
    }

    @Test
    @DisplayName("Eigentümer kann Anhang hinzufügen")
    void ownerAddsAttachment() {
        reportOwnedBy(10, 1L);

        Attachment result = attachmentService.addAttachment(10L, " uploads/bild.png ", 1L);

        assertEquals("uploads/bild.png", result.getFilePath());
        assertEquals(10L, result.getReportId());
        verify(attachmentRepository).save(result);
    }

    @Test
    @DisplayName("fremde Person kann keinen Anhang hinzufügen")
    void otherUserCannotAddAttachment() {
        reportOwnedBy(10, 1L);

        assertThrows(SecurityException.class,
                () -> attachmentService.addAttachment(10L, "uploads/bild.png", 2L));
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("anonyme Meldung erlaubt keine nachträglichen Anhänge")
    void anonymousReportRejectsAttachment() {
        reportOwnedBy(10, null);

        assertThrows(SecurityException.class,
                () -> attachmentService.addAttachment(10L, "uploads/bild.png", 1L));
    }

    @ParameterizedTest(name = "Dateipfad \"{0}\" ist ungültig")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("leerer Dateipfad wird abgelehnt")
    void rejectsEmptyFilePath(String filePath) {
        assertThrows(IllegalArgumentException.class,
                () -> attachmentService.addAttachment(10L, filePath, 1L));
        verifyNoInteractions(attachmentRepository, reportRepository);
    }

    @Test
    @DisplayName("Eigentümer kann Anhang löschen, Fremde nicht")
    void onlyOwnerCanDeleteAttachment() {
        when(attachmentRepository.findById(3L)).thenReturn(new Attachment(10L, "uploads/bild.png"));
        reportOwnedBy(10, 1L);

        assertThrows(SecurityException.class, () -> attachmentService.deleteAttachment(3L, 2L));
        verify(attachmentRepository, never()).deleteById(anyLong());

        attachmentService.deleteAttachment(3L, 1L);
        verify(attachmentRepository).deleteById(3L);
    }
}

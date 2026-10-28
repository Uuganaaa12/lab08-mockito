package mn.edu.must.sqat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    @Mock StudentRepository repository;
    @Mock EmailSender emailSender;
    CourseRegistrationService service;

    static final String SID = "B231234567";
    static final String EMAIL = "b231234567@must.edu.mn";
    static final String COURSE = "CS313";

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    // Idevhtei, CS201 uzsen oyutan
    private Student activeStudent() {
        return new Student(SID, EMAIL, true, List.of("CS201"));
    }

    // A1: when + verify
    @Test
    void register_activeStudentWithPrerequisites_returnsOkSavesAndSendsEmail() {
        when(repository.findStudent(SID)).thenReturn(activeStudent());
        when(repository.courseExists(COURSE)).thenReturn(true);
        when(repository.prerequisites(COURSE)).thenReturn(List.of("CS201"));

        String result = service.register(SID, COURSE);

        assertEquals("OK", result);
        verify(repository).saveRegistration(SID, COURSE);
        verify(emailSender).send(eq(EMAIL), anyString(), anyString());
    }

    // A2: verify(..., never())
    @Test
    void register_unknownStudent_returnsErrorNoStudentAndSavesNothing() {
        when(repository.findStudent(SID)).thenReturn(null);

        String result = service.register(SID, COURSE);

        assertEquals("ERROR_NO_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    // A3: verifyNoInteractions
    @Test
    void register_inactiveStudent_returnsErrorInactiveAndNeverTouchesEmail() {
        when(repository.findStudent(SID))
                .thenReturn(new Student(SID, EMAIL, false, List.of("CS201")));

        String result = service.register(SID, COURSE);

        assertEquals("ERROR_INACTIVE_STUDENT", result);
        verifyNoInteractions(emailSender);
    }

    // A4: when + never
    @Test
    void register_missingPrerequisite_returnsErrorPrerequisitesAndSavesNothing() {
        when(repository.findStudent(SID)).thenReturn(activeStudent());
        when(repository.courseExists(COURSE)).thenReturn(true);
        when(repository.prerequisites(COURSE)).thenReturn(List.of("CS201", "CS202"));

        String result = service.register(SID, COURSE);

        assertEquals("ERROR_PREREQUISITES", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
    }

    // A5: doThrow
    @Test
    void register_emailSenderThrows_stillReturnsOkAndSaves() {
        when(repository.findStudent(SID)).thenReturn(activeStudent());
        when(repository.courseExists(COURSE)).thenReturn(true);
        when(repository.prerequisites(COURSE)).thenReturn(List.of());
        doThrow(new RuntimeException("SMTP unavailable"))
                .when(emailSender).send(anyString(), anyString(), anyString());

        String result = service.register(SID, COURSE);

        assertEquals("OK", result);
        verify(repository).saveRegistration(SID, COURSE);
    }

    // A6: assertThrows, hoyor argumentiin null-iig tus tusad ni
    @Test
    void register_nullArguments_throwsIllegalArgumentAndTouchesNothing() {
        assertThrows(IllegalArgumentException.class, () -> service.register(null, COURSE));
        assertThrows(IllegalArgumentException.class, () -> service.register(SID, null));
        verifyNoInteractions(repository, emailSender);
    }

    // A7: ArgumentCaptor
    @Test
    void register_success_emailBodyContainsCourseId() {
        when(repository.findStudent(SID)).thenReturn(activeStudent());
        when(repository.courseExists(COURSE)).thenReturn(true);
        when(repository.prerequisites(COURSE)).thenReturn(List.of());
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);

        service.register(SID, COURSE);

        verify(emailSender).send(eq(EMAIL), anyString(), body.capture());
        assertTrue(body.getValue().contains(COURSE), "body: " + body.getValue());
    }

    // A8: @ParameterizedTest + @CsvSource, gurwan aldaanii angi
    @ParameterizedTest(name = "{3}")
    @CsvSource({
            "false, false, true,  ERROR_NO_STUDENT",
            "true,  false, true,  ERROR_INACTIVE_STUDENT",
            "true,  true,  false, ERROR_NO_COURSE",
    })
    void register_errorCases_returnsExpectedErrorAndSavesNothing(
            boolean studentExists, boolean active, boolean courseExists, String expected) {
        Student student = studentExists ? new Student(SID, EMAIL, active, List.of()) : null;
        // Mor bur stub-iin zarimiig l ashigladag tul lenient
        lenient().when(repository.findStudent(SID)).thenReturn(student);
        lenient().when(repository.courseExists(COURSE)).thenReturn(courseExists);

        String result = service.register(SID, COURSE);

        assertEquals(expected, result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }
}

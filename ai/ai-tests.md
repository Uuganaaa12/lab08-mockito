Доорх тестийн классыг `src/test/java/mn/edu/must/sqat/CourseRegistrationServiceTest.java` файлд хуулаад `mvn test` гэж ажиллуулна.

**Юуг шалгаж байгаа вэ:**

- `studentId` / `courseId` null бол `IllegalArgumentException` шидэж, repository болон emailSender-т огт хандахгүй.
- Оюутан олдоогүй → `ERROR_NO_STUDENT`, цаашдын шалгалт (courseExists, save, email) хийгдэхгүй.
- Оюутан идэвхгүй → `ERROR_INACTIVE_STUDENT`.
- Хичээл байхгүй → `ERROR_NO_COURSE`, prerequisites дуудагдахгүй.
- Өмнөх холбоо хичээл бүгд дутуу эсвэл нэг нь дутуу → `ERROR_PREREQUISITES`, бүртгэл хадгалагдахгүй, мэйл явахгүй.
- Бүх холбоо хичээл үзсэн / холбоо хичээл огт байхгүй → `OK`, `saveRegistration` зөв аргументаар дуудагдаж, мэйл зөв хаяг, гарчиг, агуулгатай илгээгдэнэ (`ArgumentCaptor`-оор шалгана). `verifyNoMoreInteractions`-оор repository-д илүү дуудлага байхгүйг баталгаажуулна.
- `InOrder`-оор бүртгэл эхлээд хадгалагдаж, дараа нь мэйл явдгийг шалгана.
- Мэйл илгээх үед `RuntimeException` гарсан ч `OK` буцааж, бүртгэл хадгалагдсан хэвээр байна (`doThrow`).

`MockitoExtension` нь strict stubs горимтой тул тест бүр зөвхөн өөрт хэрэгтэй stub-ийг л тодорхойлсон. Ашиглагдаагүй stub байвал `UnnecessaryStubbingException` гарна гэдгийг анхаарна уу.

```java
package mn.edu.must.sqat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    private static final String STUDENT_ID = "b232270004";
    private static final String EMAIL = "student@must.edu.mn";
    private static final String COURSE_ID = "F.CSA313";

    @Mock
    private StudentRepository repository;

    @Mock
    private EmailSender emailSender;

    private CourseRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    private Student activeStudent(List<String> coursesTaken) {
        return new Student(STUDENT_ID, EMAIL, true, coursesTaken);
    }

    // ---------- Оролтын шалгалт ----------

    @Test
    void register_studentIdNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> service.register(null, COURSE_ID));
        verifyNoInteractions(repository, emailSender);
    }

    @Test
    void register_courseIdNull_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> service.register(STUDENT_ID, null));
        verifyNoInteractions(repository, emailSender);
    }

    // ---------- Оюутны шалгалт ----------

    @Test
    void register_studentNotFound_returnsErrorNoStudent() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(null);

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_NO_STUDENT", result);
        verify(repository).findStudent(STUDENT_ID);
        verify(repository, never()).courseExists(anyString());
        verify(repository, never()).prerequisites(anyString());
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    @Test
    void register_inactiveStudent_returnsErrorInactiveStudent() {
        when(repository.findStudent(STUDENT_ID))
                .thenReturn(new Student(STUDENT_ID, EMAIL, false, List.of()));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_INACTIVE_STUDENT", result);
        verify(repository, never()).courseExists(anyString());
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    // ---------- Хичээлийн шалгалт ----------

    @Test
    void register_courseNotFound_returnsErrorNoCourse() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent(List.of()));
        when(repository.courseExists(COURSE_ID)).thenReturn(false);

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_NO_COURSE", result);
        verify(repository, never()).prerequisites(anyString());
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    // ---------- Өмнөх холбоо хичээл ----------

    @Test
    void register_allPrerequisitesMissing_returnsErrorPrerequisites() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent(List.of()));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CSA201", "CSA202"));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_PREREQUISITES", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    @Test
    void register_onePrerequisiteMissing_returnsErrorPrerequisites() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent(List.of("CSA201")));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CSA201", "CSA202"));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_PREREQUISITES", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    // ---------- Амжилттай бүртгэл ----------

    @Test
    void register_allPrerequisitesTaken_returnsOkSavesAndSendsEmail() {
        when(repository.findStudent(STUDENT_ID))
                .thenReturn(activeStudent(List.of("CSA201", "CSA202", "CSA105")));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of("CSA201", "CSA202"));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);

        verify(repository).findStudent(STUDENT_ID);
        verify(repository).courseExists(COURSE_ID);
        verify(repository).prerequisites(COURSE_ID);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verifyNoMoreInteractions(repository);

        ArgumentCaptor<String> to = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(to.capture(), subject.capture(), body.capture());
        assertEquals(EMAIL, to.getValue());
        assertEquals("Бүртгэл баталгаажлаа", subject.getValue());
        assertEquals(COURSE_ID + " хичээлд амжилттай бүртгэгдлээ.", body.getValue());
    }

    @Test
    void register_noPrerequisites_returnsOk() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent(List.of()));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of());

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verify(emailSender).send(eq(EMAIL), anyString(), anyString());
    }

    @Test
    void register_success_savesRegistrationBeforeSendingEmail() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent(List.of()));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of());

        service.register(STUDENT_ID, COURSE_ID);

        InOrder inOrder = inOrder(repository, emailSender);
        inOrder.verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        inOrder.verify(emailSender).send(anyString(), anyString(), anyString());
    }

    // ---------- И-мэйл илгээлт амжилтгүй ----------

    @Test
    void register_emailSendFails_stillReturnsOkAndRegistrationIsSaved() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(activeStudent(List.of()));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of());
        doThrow(new RuntimeException("SMTP unavailable"))
                .when(emailSender).send(anyString(), anyString(), anyString());

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verify(emailSender).send(eq(EMAIL), anyString(), anyString());
    }
}
```

# Лаб 8: Mock объектууд, Mockito

Д.Ууганбаяр, b232270004
F.CSA313 Программ хангамжийн чанарын баталгаа ба туршилт

## Орчин

```
$ java -version
openjdk version "17.0.16" 2025-07-15
OpenJDK Runtime Environment Homebrew (build 17.0.16+0)
OpenJDK 64-Bit Server VM Homebrew (build 17.0.16+0, mixed mode, sharing)
$ mvn -version
Apache Maven 3.9.14 (996c630dbc656c76214ce58821dcc58be960875b)
Maven home: /Users/uugnaa/.sdkman/candidates/maven/current
Java version: 17.0.16, vendor: Homebrew, runtime: /opt/homebrew/Cellar/openjdk@17/17.0.16/libexec/openjdk.jdk/Contents/Home
```

JUnit 5.10.2, Mockito 5.11.0 (starter-ийн pom.xml). Starter-ийг ажиллуулахад `No tests to run` ба `BUILD SUCCESS` гарсан.

## Хэсэг A: Тестүүд

Тестлэгдэх класс `CourseRegistrationService`, хоёр хамаарал `StudentRepository`, `EmailSender`-ийг `@Mock`-оор орлуулсан. Тестүүд [CourseRegistrationServiceTest.java](src/test/java/mn/edu/must/sqat/CourseRegistrationServiceTest.java).

| # | Тест | Хэрэгсэл |
|---|---|---|
| A1 | `register_activeStudentWithPrerequisites_returnsOkSavesAndSendsEmail` | when, verify |
| A2 | `register_unknownStudent_returnsErrorNoStudentAndSavesNothing` | verify(..., never()) |
| A3 | `register_inactiveStudent_returnsErrorInactiveAndNeverTouchesEmail` | verifyNoInteractions |
| A4 | `register_missingPrerequisite_returnsErrorPrerequisitesAndSavesNothing` | when, never |
| A5 | `register_emailSenderThrows_stillReturnsOkAndSaves` | doThrow(...).when(...) |
| A6 | `register_nullArguments_throwsIllegalArgumentAndTouchesNothing` | assertThrows, verifyNoInteractions |
| A7 | `register_success_emailBodyContainsCourseId` | ArgumentCaptor |
| A8 | `register_errorCases_returnsExpectedErrorAndSavesNothing` (3 мөр) | @ParameterizedTest, @CsvSource, lenient |

```bash
mvn test 2>&1 | tee results/mvn-test.txt
```

[results/mvn-test.txt](results/mvn-test.txt): `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`, `BUILD SUCCESS`. 7 энгийн тест, A8-ийн 3 мөр тус бүр тусдаа тест болж 10.

### lenient

A8-ийн мөр бүр `findStudent`, `courseExists` хоёуланг stub хийдэг ч ERROR_NO_STUDENT мөрөнд `register` эхний шалгалт дээр буцдаг тул `courseExists` stub ашиглагдахгүй үлдэнэ. MockitoExtension-ий strict stubs горим ашиглагдаагүй stub-ыг `UnnecessaryStubbingException` гэж унагадаг тул энэ хоёр stub-ыг `lenient().when(...)` хэлбэрээр бичсэн.

Matcher-ийн дүрэм: A7-д `verify(emailSender).send(eq(EMAIL), anyString(), body.capture())`, нэг аргументад matcher хэрэглэсэн тул бүгдэд нь matcher.

## Хэсэг B: Мутаци

Мутант бүрийг гараар хийж, `mvn test` ажиллуулж, кодыг сэргээсэн. Эцэст нь `results/mvn-test.txt`-ийг дахин ажиллуулахад 10 тест ногоон.

| Мутант | Эвдрэл | Унасан тест | Үр дүн | Файл |
|---|---|---|---|---|
| 1 | `if (!student.active()) ...` мөрийг комментолсон | A3 `register_inactiveStudent_...`, A8 мөр 2 (ERROR_INACTIVE_STUDENT) | Failures: 2, Errors: 0, BUILD FAILURE | [mvn-test-mutant-1.txt](results/mvn-test-mutant-1.txt) |
| 2 | `repository.saveRegistration(...)` мөрийг комментолсон | A1 `register_activeStudent...`, A5 `register_emailSenderThrows_...` | Failures: 2, Errors: 0, BUILD FAILURE | [mvn-test-mutant-2.txt](results/mvn-test-mutant-2.txt) |
| 3 | эхний if-ээс `\|\| courseId == null` устгасан | A6 `register_nullArguments_...` | Failures: 1, Errors: 0, BUILD FAILURE | [mvn-test-mutant-3.txt](results/mvn-test-mutant-3.txt) |

Мутант 1-д идэвхгүй оюутан цааш явж, A3-д `courseExists` mock анхдагчаараа false буцаасан тул ERROR_NO_COURSE, A8-ийн мөрөнд courseExists true байсан тул OK гарч хоёулаа унасан. Мутант 2-д `verify(repository).saveRegistration(...)` байгаа хоёр тест л унасан, A7 нь и-мэйл л шалгадаг тул давсан. Мутант 3-ыг A6 шууд барьсан, учир нь A6-г эхнээсээ хоёр аргументын null-ийг тус тусад нь `assertThrows`-оор шалгадаг бичсэн: `register(SID, null)` exception шидэхийн оронд `ERROR_NO_STUDENT` буцаасан тул `Expected IllegalArgumentException to be thrown, but nothing was thrown` гэж унасан. Хэрэв зөвхөн `register(null, null)` гэж шалгасан бол энэ мутант баригдахгүй байсан. Гурван мутантад UnnecessaryStubbingException гараагүй, учир нь унасан тестүүдийн stub-ууд эвдэрсэн кодоор ч ашиглагдсан хэвээр байсан.

Дүгнэлт: гурван мутантыг тус бүр өөр тест барьсан, тестийн цоорхой олдоогүй. Гэхдээ `courseExists` шалгалтыг устгах мутант хийвэл A8-ийн мөр 3 л барина, A1-A7-д тэр шалгалтыг зориуд унагаасан тест байхгүй.

## Дүгнэлт

Mock ба гараар бичсэн fake хоёр хоёулаа бодит хамаарлыг орлуулдаг ч fake нь өөрийн логиктой (жишээ нь HashMap дээр ажилладаг repository) тул өөрөө алдаатай байж болох ба дуудлагыг нь шалгахад хэцүү, харин mock нь логикгүй, зөвхөн заасан хариугаа буцаагаад ямар дуудалт ирснийг тэмдэглэдэг тул `verify`, `never`, `ArgumentCaptor`-оор харилцан үйлдлийг шууд шалгаж болдог. Энэ лабд SMTP сервер, өгөгдлийн сангүйгээр `register`-ийн бүх салаа, тэр дундаа и-мэйл унасан ч OK буцаах салааг `doThrow`-оор 0.5 секундэд тестэлсэн нь mock-ийн гол ашиг. Mock хийх ёсгүй зүйл бол тестлэгдэх класс өөрөө, `Student` шиг энгийн өгөгдлийн record, `List` шиг JDK-ийн төрлүүд, учир нь эдгээр нь гадаад хамаарал биш, mock хийвэл тест бодит кодыг биш өөрийнхөө stub-ыг шалгаж эхэлнэ. Verify-г хэтрүүлбэл тест хэрэгжүүлэлтийн дарааллыг давтсан хуулбар болж, жишээ нь `courseExists`-ийг яг нэг удаа, `prerequisites`-ээс өмнө дуудсан гэж шалгавал үр дүн өөрчлөгдөөгүй refactoring хийхэд ч тест унана. Тиймээс verify-г зөвхөн гаднаас ажиглагдах үр дагавартай дуудлагад (хадгалах, и-мэйл илгээх) тавьж, query дуудлагыг stub-аар л хангасан. Мутаци нь энэ сонголтыг баталсан: гурван эвдрэл бүгд өөр өөр тестээр баригдсан ч нэг ч тест stub-ын ашиглалтаас болж хоосон унаагүй. Strict stubs горим нь хэрэггүй stub-ыг алдаа гэж үздэг нь эхэндээ саад мэт санагдсан ч A8-д яг аль мөр аль stub-ыг ашигладгийг бодоход хүргэсэн тул ашигтай байсан.

## CI (нэмэлт, сонголт 1)

[![test](https://github.com/Uuganaaa12/lab08-mockito/actions/workflows/test.yml/badge.svg)](https://github.com/Uuganaaa12/lab08-mockito/actions/workflows/test.yml)

[.github/workflows/test.yml](.github/workflows/test.yml): push бүрд ubuntu дээр Temurin 17-оор `mvn -B test` ажиллана.

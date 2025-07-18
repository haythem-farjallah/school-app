package com.example.school_management.dev;

import com.example.school_management.feature.academic.entity.*;
import com.example.school_management.feature.academic.repository.*;
import com.example.school_management.feature.auth.entity.*;
import com.example.school_management.feature.auth.entity.Status;
import com.example.school_management.feature.auth.entity.UserRole;
import com.example.school_management.feature.auth.repository.*;
import com.github.javafaker.Faker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.CommandLineRunner;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Slf4j
@Component
@Profile("dev")                 // run only with:  spring.profiles.active=dev
@RequiredArgsConstructor
class DemoDataLoader implements CommandLineRunner {

    /* ── repositories ──────────────────────────────────────────────── */
    private final TeacherRepository            teacherRepo;
    private final StudentRepository            studentRepo;
    private final CourseRepository             courseRepo;
    private final ClassRepository              classRepo;
    private final TeachingAssignmentRepository assignmentRepo;
    private final PasswordEncoder              encoder;

    /* ── knobs you can tweak ───────────────────────────────────────── */
    private static final int TEACHERS          = 15;
    private static final int STUDENTS          = 120;
    private static final int LEVELS            = 4;
    private static final int CLASSES_PER_LEVEL = 3;
    private static final int COURSES_PER_TEACH = 2;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        if (teacherRepo.count() > 0 || studentRepo.count() > 0) {
            log.info("⚠️  Dev data already present – skipping fake data generation.");
            return;
        }

        Faker faker = new Faker(Locale.ENGLISH);
        AtomicInteger emailSeq = new AtomicInteger(1);

        /* ─────────────── TEACHERS ───────────────────────────── */
        List<Teacher> teachers = IntStream.rangeClosed(1, TEACHERS)
                .mapToObj(i -> {
                    Teacher t = new Teacher();
                    t.setFirstName(faker.name().firstName());
                    t.setLastName (faker.name().lastName());
                    t.setEmail("teacher"+emailSeq.getAndIncrement()+"@demo.school");
                    t.setPassword(encoder.encode("password"));
                    t.setRole(UserRole.TEACHER);
                    t.setStatus(Status.ACTIVE);
                    t.setQualifications(faker.educator().course());
                    t.setSubjectsTaught(faker.job().field());
                    t.setSchedulePreferences("Morning");
                    return t;
                }).map(teacherRepo::save).toList();
        log.info("👩‍🏫  Inserted {} teachers", teachers.size());

        /* ─────────────── STUDENTS ───────────────────────────── */
        List<Student> students = new ArrayList<>(IntStream.rangeClosed(1, STUDENTS)
                .mapToObj(i -> {
                    Student s = new Student();
                    s.setFirstName(faker.name().firstName());
                    s.setLastName(faker.name().lastName());
                    s.setEmail("student" + emailSeq.getAndIncrement() + "@demo.school");
                    s.setPassword(encoder.encode("password"));
                    s.setRole(UserRole.STUDENT);
                    s.setStatus(Status.ACTIVE);
                    return s;
                }).map(studentRepo::save).toList());
        log.info("🧑‍🎓  Inserted {} students", students.size());

        /* ─────────────── COURSES (+ teacher) ────────────────── */
        List<Course> courses = new ArrayList<>();
        teachers.forEach(t -> {
            for (int i = 0; i < COURSES_PER_TEACH; i++) {
                Course c = new Course();
                c.setName(faker.educator().course()+" "+(courses.size()+1));
                c.setColor("#"+faker.color().hex().substring(1,7));
                c.setCredit((float) faker.number().randomDouble(1,1,5));
                c.setWeeklyCapacity(faker.number().numberBetween(2, 6));
                c.setTeacher(t);
                courses.add(courseRepo.save(c));
            }
        });
        log.info("📚  Inserted {} courses", courses.size());

        /* ─────────────── LEVELS & CLASSES ───────────────────── */
        Random rnd = new Random();
        for (int lvIdx = 1; lvIdx <= LEVELS; lvIdx++) {


            for (int cIdx = 0; cIdx < CLASSES_PER_LEVEL; cIdx++) {
                ClassEntity clazz = new ClassEntity();
                clazz.setName(lvIdx+"-"+ (char)('A'+cIdx));


                /* pick 18-25 random students */
                Collections.shuffle(students);
                clazz.getStudents().addAll(
                        students.subList(0, rnd.nextInt(8)+18));

                /* pick 4-6 random courses */
                Collections.shuffle(courses);
                clazz.getCourses().addAll(
                        courses.subList(0, rnd.nextInt(3)+4));

                classRepo.save(clazz);

                /* build the teaching-assignment matrix  */
                clazz.getCourses().forEach(course -> {
                    TeachingAssignment ta = new TeachingAssignment();
                    ta.setClazz   (clazz);
                    ta.setCourse  (course);
                    ta.setTeacher (course.getTeacher());     // keep it consistent
                    ta.setWeeklyHours(rnd.nextInt(5)+2);
                    assignmentRepo.save(ta);
                });
            }
        }
        log.info("🏁  Dev fixtures loaded successfully.");
    }
}

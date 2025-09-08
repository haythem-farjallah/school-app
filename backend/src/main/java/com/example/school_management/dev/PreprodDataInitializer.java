package com.example.school_management.dev;

import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.entity.TeachingAssignment;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.academic.repository.TeachingAssignmentRepository;
import com.example.school_management.feature.auth.entity.*;
import com.example.school_management.feature.auth.repository.ParentRepository;
import com.example.school_management.feature.auth.repository.StudentRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import com.github.javafaker.Faker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Slf4j
@Component
@Profile("preprod")
@RequiredArgsConstructor
public class PreprodDataInitializer implements CommandLineRunner {

    private final TeacherRepository teacherRepo;
    private final StudentRepository studentRepo;
    private final ParentRepository parentRepo;
    private final CourseRepository courseRepo;
    private final ClassRepository classRepo;
    private final TeachingAssignmentRepository assignmentRepo;
    private final PasswordEncoder encoder;

    private static final int TEACHERS = 100;
    private static final int STUDENTS_PER_CLASS = 25;
    private static final String[] PRIMARY_LEVELS = {"1st Grade", "2nd Grade", "3rd Grade", "4th Grade", "5th Grade", "6th Grade"};
    private static final String[] SECONDARY_LEVELS = {"1ere Annee", "2eme Annee", "3eme Annee", "4eme Annee"};
    private static final int CLASSES_PER_LEVEL = 3;
    private static final int COURSES_PER_LEVEL = 5;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (teacherRepo.count() > 0 || studentRepo.count() > 0) {
            log.info("⚠️ Preprod data already present – skipping fake data generation.");
            return;
        }

        Faker faker = new Faker(new Locale("fr"));
        AtomicInteger emailSeq = new AtomicInteger(1);

        List<Teacher> teachers = createTeachers(faker, emailSeq);
        teacherRepo.saveAll(teachers);
        log.info("👩‍🏫 Inserted {} teachers", teachers.size());

        List<String> levels = new ArrayList<>();
        levels.addAll(Arrays.asList(PRIMARY_LEVELS));
        levels.addAll(Arrays.asList(SECONDARY_LEVELS));

        Map<String, List<Course>> coursesByLevel = createCourses(faker, teachers, levels);
        coursesByLevel.values().forEach(courseRepo::saveAll);
        log.info("📚 Inserted {} courses", coursesByLevel.values().stream().mapToLong(Collection::size).sum());


        for (String level : levels) {
            for (int i = 0; i < CLASSES_PER_LEVEL; i++) {
                ClassEntity clazz = new ClassEntity();
                clazz.setName(level + " - Section " + (char) ('A' + i));

                List<Student> students = createStudents(faker, emailSeq, STUDENTS_PER_CLASS);
                studentRepo.saveAll(students);
                createParentsForStudents(faker, emailSeq, students);
                clazz.getStudents().addAll(students);

                clazz.getCourses().addAll(coursesByLevel.get(level));
                classRepo.save(clazz);

                clazz.getCourses().forEach(course -> {
                    TeachingAssignment ta = new TeachingAssignment();
                    ta.setClazz(clazz);
                    ta.setCourse(course);
                    ta.setTeacher(course.getTeacher());
                    ta.setWeeklyHours(faker.number().numberBetween(2, 5));
                    assignmentRepo.save(ta);
                });
            }
        }

        log.info("🏁 Preprod fixtures loaded successfully.");
    }

    private List<Teacher> createTeachers(Faker faker, AtomicInteger emailSeq) {
        return IntStream.rangeClosed(1, TEACHERS)
                .mapToObj(i -> {
                    Teacher t = new Teacher();
                    t.setFirstName(faker.name().firstName());
                    t.setLastName(faker.name().lastName());
                    t.setEmail("teacher" + emailSeq.getAndIncrement() + "@preprod.school");
                    t.setPassword(encoder.encode("password"));
                    t.setRole(UserRole.TEACHER);
                    t.setStatus(Status.ACTIVE);
                    return t;
                }).toList();
    }

    private List<Student> createStudents(Faker faker, AtomicInteger emailSeq, int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> {
                    Student s = new Student();
                    s.setFirstName(faker.name().firstName());
                    s.setLastName(faker.name().lastName());
                    s.setEmail("student" + emailSeq.getAndIncrement() + "@preprod.school");
                    s.setPassword(encoder.encode("password"));
                    s.setRole(UserRole.STUDENT);
                    s.setStatus(Status.ACTIVE);
                    return s;
                }).toList();
    }

    private void createParentsForStudents(Faker faker, AtomicInteger emailSeq, List<Student> students) {
        List<Parent> parents = students.stream().map(student -> {
            Parent p = new Parent();
            p.setFirstName(faker.name().firstName());
            p.setLastName(student.getLastName()); // Same last name as student
            p.setEmail("parent" + emailSeq.getAndIncrement() + "@preprod.school");
            p.setPassword(encoder.encode("password"));
            p.setRole(UserRole.PARENT);
            p.setStatus(Status.ACTIVE);
            p.getChildren().add(student);
            return p;
        }).toList();
        parentRepo.saveAll(parents);
    }

    private Map<String, List<Course>> createCourses(Faker faker, List<Teacher> teachers, List<String> levels) {
        Map<String, List<Course>> coursesByLevel = new HashMap<>();
        Random rnd = new Random();
        for (String level : levels) {
            List<Course> courses = new ArrayList<>();
            for (int i = 0; i < COURSES_PER_LEVEL; i++) {
                Course c = new Course();
                c.setName(faker.educator().course() + " " + level);
                c.setColor("#" + faker.color().hex().substring(1, 7));
                c.setTeacher(teachers.get(rnd.nextInt(teachers.size())));
                courses.add(c);
            }
            coursesByLevel.put(level, courses);
        }
        return coursesByLevel;
    }
}

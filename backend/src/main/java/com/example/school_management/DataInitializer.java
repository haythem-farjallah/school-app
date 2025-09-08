package com.example.school_management;

import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.academic.entity.TeachingAssignment;
import com.example.school_management.feature.operational.entity.Room;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.academic.repository.TeachingAssignmentRepository;
import com.example.school_management.feature.operational.repository.RoomRepository;
import com.example.school_management.feature.auth.entity.*;
import com.example.school_management.feature.auth.repository.*;
import com.example.school_management.feature.operational.entity.Period;
import com.example.school_management.feature.operational.repository.PeriodRepository;
import com.example.school_management.feature.operational.repository.TimetableRepository;
import com.example.school_management.feature.operational.repository.TimetableSlotRepository;
import com.example.school_management.feature.operational.entity.TimetableSlot;
import com.example.school_management.feature.operational.entity.enums.DayOfWeek;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataInitializer.class);

    private final BaseUserRepository baseUserRepository;
    private final AdministrationRepository administrationRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final RoomRepository roomRepository;
    private final PeriodRepository periodRepository;
    private final TimetableRepository timetableRepository;
    private final TimetableSlotRepository timetableSlotRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (baseUserRepository.count() == 0) {
            log.info("Initializing college data...");
            initializeCollegeData();
        }
    }

    private void initializeCollegeData() {
        // Create admin if not exists
        Optional<BaseUser> existingAdmin = baseUserRepository.findByEmail("admin@college.edu");
        if (existingAdmin.isEmpty()) {
            Administration admin = new Administration();
            admin.setEmail("admin@college.edu");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            administrationRepository.save(admin);
            log.info("Admin user created");
        }

        // Clean existing data (except admin)
        cleanExistingData();

        // Initialize base data
        List<Room> rooms = createRooms();
        List<ClassEntity> classes = createClasses();
        List<Course> courses = createCourses();
        List<Teacher> teachers = createTeachers();
        List<Student> students = createStudents(classes);
        List<Period> periods = createPeriods();
        assignCoursesToClasses(courses, classes);
        createTeachingAssignments(teachers, classes, courses);
        createSampleTimetableSlots(teachers, classes, courses, periods, rooms);

        log.info("College data initialization completed");
    }

    private void cleanExistingData() {
        log.info("Cleaning existing data...");
        
        // Delete in correct order to avoid foreign key constraints
        timetableSlotRepository.deleteAll();
        timetableRepository.deleteAll();
        teachingAssignmentRepository.deleteAll();
        studentRepository.deleteAll();
        teacherRepository.deleteAll();
        courseRepository.deleteAll();
        classRepository.deleteAll();
        roomRepository.deleteAll();
        periodRepository.deleteAll();
        
        // Delete users except admin
        baseUserRepository.findAll().stream()
            .filter(user -> !((BaseUser)user).getEmail().equals("admin@college.edu"))
            .forEach(baseUserRepository::delete);
            
        log.info("Data cleanup completed");
    }

    private List<Room> createRooms() {
        List<Room> rooms = new ArrayList<>();
        
        // Create lecture halls
        for (int i = 1; i <= 3; i++) {
            Room room = new Room();
            room.setName("Lecture Hall " + i);
            room.setCapacity(60);
            rooms.add(roomRepository.save(room));
        }
        
        // Create regular classrooms
        for (int i = 1; i <= 6; i++) {
            Room room = new Room();
            room.setName("Room " + (100 + i));
            room.setCapacity(30);
            rooms.add(roomRepository.save(room));
        }
        
        // Create labs
        Room computerLab = new Room();
        computerLab.setName("Computer Lab");
        computerLab.setCapacity(25);
        rooms.add(roomRepository.save(computerLab));
        
        Room scienceLab = new Room();
        scienceLab.setName("Science Lab");
        scienceLab.setCapacity(20);
        rooms.add(roomRepository.save(scienceLab));
        
        log.info("Created {} rooms", rooms.size());
        return rooms;
    }

    private List<ClassEntity> createClasses() {
        List<ClassEntity> classes = new ArrayList<>();
        
        // Create classes for 3 grades, 2 sections each
        String[] grades = {"First Year", "Second Year", "Third Year"};
        String[] sections = {"A", "B"};
        
        for (String grade : grades) {
            for (String section : sections) {
                ClassEntity classEntity = new ClassEntity();
                classEntity.setName(grade + " - Section " + section);
                classEntity.setGradeLevel(grade);
                classEntity.setSection(section);
                classEntity.setCapacity(30);
                classEntity.setAcademicYear("2024-2025");
                
                // Set weekly hours based on grade
                int weeklyHours = grade.equals("First Year") ? 30 : 
                                 grade.equals("Second Year") ? 32 : 34;
                classEntity.setWeeklyHours(weeklyHours);
                
                classes.add(classRepository.save(classEntity));
            }
        }
        
        log.info("Created {} classes", classes.size());
        return classes;
    }

    private List<Course> createCourses() {
        List<Course> courses = new ArrayList<>();
        
        // Core courses with weekly hours and duration
        Object[][] courseData = {
            // {name, code, credits, weeklyFrequency, durationPeriods, canSplit, color}
            {"Mathematics", "MATH101", 4, 4, 1, true, "#FF6B6B"},
            {"Physics", "PHYS101", 3, 3, 1, true, "#4ECDC4"},
            {"Chemistry", "CHEM101", 3, 3, 2, false, "#45B7D1"},
            {"English", "ENG101", 3, 3, 1, true, "#96CEB4"},
            {"Computer Science", "CS101", 4, 3, 2, false, "#FFA07A"},
            {"Biology", "BIO101", 3, 2, 2, false, "#DDA0DD"},
            {"History", "HIST101", 2, 2, 1, true, "#F4A460"},
            {"Literature", "LIT101", 2, 2, 1, true, "#FFD700"},
            {"Physical Education", "PE101", 2, 2, 2, false, "#98D8C8"},
            {"Art", "ART101", 2, 1, 2, false, "#F06292"}
        };
        
        for (Object[] data : courseData) {
            Course course = new Course();
            course.setName((String) data[0]);
            course.setCode((String) data[1]);
            course.setCredits((Integer) data[2]);
            course.setDescription("Introduction to " + data[0]);
            course.setWeeklyFrequency((Integer) data[3]);
            course.setDurationPeriods((Integer) data[4]);
            course.setCanSplit((Boolean) data[5]);
            course.setColor((String) data[6]);
            courses.add(courseRepository.save(course));
        }
        
        log.info("Created {} courses", courses.size());
        return courses;
    }

    private List<Teacher> createTeachers() {
        List<Teacher> teachers = new ArrayList<>();
        
        // Create teachers with subject specializations
        Object[][] teacherData = {
            {"Ahmed", "Hassan", "ahmed.hassan@college.edu", "Mathematics", 20},
            {"Fatima", "Ali", "fatima.ali@college.edu", "Physics", 18},
            {"Mohamed", "Salem", "mohamed.salem@college.edu", "Chemistry", 16},
            {"Sarah", "Johnson", "sarah.johnson@college.edu", "English", 20},
            {"David", "Smith", "david.smith@college.edu", "Computer Science", 18},
            {"Layla", "Ibrahim", "layla.ibrahim@college.edu", "Biology", 16},
            {"Omar", "Khalil", "omar.khalil@college.edu", "History", 18},
            {"Nadia", "Saeed", "nadia.saeed@college.edu", "Literature", 16},
            {"Karim", "Abbas", "karim.abbas@college.edu", "Physical Education", 20},
            {"Hana", "Rashid", "hana.rashid@college.edu", "Art", 12}
        };
        
        for (Object[] data : teacherData) {
            Teacher teacher = new Teacher();
            teacher.setEmail((String) data[2]);
            teacher.setPassword(passwordEncoder.encode("password123"));
            teacher.setFirstName((String) data[0]);
            teacher.setLastName((String) data[1]);
            teacher.setSubjectsTaught((String) data[3]);
            teacher.setWeeklyCapacity((Integer) data[4]);
            teacher.setQualifications("Master's in " + data[3]);
            teacher.setSchedulePreferences("Morning preferred");
            
            teachers.add(teacherRepository.save(teacher));
        }
        
        log.info("Created {} teachers", teachers.size());
        return teachers;
    }

    private List<Student> createStudents(List<ClassEntity> classes) {
        List<Student> students = new ArrayList<>();
        Random random = new Random();
        
        String[] firstNames = {"Ali", "Fatima", "Omar", "Aisha", "Hassan", "Mariam", "Youssef", "Nour", "Ibrahim", "Salma"};
        String[] lastNames = {"Ahmed", "Mohamed", "Hassan", "Ali", "Ibrahim", "Salem", "Khalil", "Saeed", "Abbas", "Rashid"};
        
        int studentId = 1;
        for (ClassEntity classEntity : classes) {
            // Create 25 students per class
            for (int i = 0; i < 25; i++) {
                Student student = new Student();
                String firstName = firstNames[random.nextInt(firstNames.length)];
                String lastName = lastNames[random.nextInt(lastNames.length)];
                
                student.setEmail("student" + studentId + "@college.edu");
                student.setPassword(passwordEncoder.encode("password123"));
                student.setFirstName(firstName);
                student.setLastName(lastName);
                
                students.add(studentRepository.save(student));
                studentId++;
            }
        }
        
        log.info("Created {} students", students.size());
        return students;
    }

    private List<Period> createPeriods() {
        List<Period> periods = new ArrayList<>();
        
        // College schedule: 9:00 AM to 5:00 PM
        LocalTime startTime = LocalTime.of(9, 0);
        int periodDuration = 50; // 50 minutes per period
        int breakDuration = 10; // 10 minutes break
        
        for (int i = 1; i <= 8; i++) {
            Period period = new Period();
            period.setIndex(i);
            period.setStartTime(startTime);
            LocalTime endTime = startTime.plusMinutes(periodDuration);
            period.setEndTime(endTime);
            
            periods.add(periodRepository.save(period));
            
            // Add break time
            startTime = endTime.plusMinutes(breakDuration);
            
            // Lunch break after 4th period
            if (i == 4) {
                startTime = startTime.plusMinutes(30); // Extra 30 minutes for lunch
            }
        }
        
        log.info("Created {} periods", periods.size());
        return periods;
    }

    private void assignCoursesToClasses(List<Course> courses, List<ClassEntity> classes) {
        // Assign courses to classes
        for (ClassEntity classEntity : classes) {
            Set<Course> classCourses = new HashSet<>();
            
            for (Course course : courses) {
                // Skip some courses for higher grades
                if (classEntity.getGradeLevel().equals("Third Year") && 
                    (course.getCode().equals("PE101") || course.getCode().equals("ART101"))) {
                    continue;
                }
                
                classCourses.add(course);
            }
            
            classEntity.setCourses(classCourses);
            classRepository.save(classEntity);
        }
        
        log.info("Assigned courses to classes");
    }

    private void createTeachingAssignments(List<Teacher> teachers, List<ClassEntity> classes, List<Course> courses) {
        log.info("Creating teaching assignments...");
        
        List<TeachingAssignment> assignments = new ArrayList<>();
        Random random = new Random();
        
        // For each class, assign teachers to courses
        for (ClassEntity classEntity : classes) {
            Set<Course> classCourses = classEntity.getCourses();
            
            for (Course course : classCourses) {
                // Find a teacher who teaches this subject or assign randomly
                Teacher assignedTeacher = teachers.stream()
                    .filter(teacher -> teacher.getSubjectsTaught() != null && 
                            teacher.getSubjectsTaught().toLowerCase().contains(course.getName().toLowerCase()))
                    .findFirst()
                    .orElse(teachers.get(random.nextInt(teachers.size())));
                
                TeachingAssignment assignment = new TeachingAssignment();
                assignment.setClazz(classEntity);
                assignment.setCourse(course);
                assignment.setTeacher(assignedTeacher);
                assignment.setWeeklyHours(random.nextInt(4) + 2); // 2-5 hours per week
                
                assignments.add(assignment);
            }
        }
        
        teachingAssignmentRepository.saveAll(assignments);
        log.info("Created {} teaching assignments", assignments.size());
    }

    private void createSampleTimetableSlots(List<Teacher> teachers, List<ClassEntity> classes, 
                                          List<Course> courses, List<Period> periods, List<Room> rooms) {
        log.info("Creating sample timetable slots...");
        
        List<TimetableSlot> slots = new ArrayList<>();
        DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY};
        
        // Create a reasonable schedule for each teacher
        for (int teacherIndex = 0; teacherIndex < Math.min(teachers.size(), 5); teacherIndex++) {
            Teacher teacher = teachers.get(teacherIndex);
            
            // Each teacher gets 2-3 classes per day, 3 days a week
            for (int dayIndex = 0; dayIndex < 3; dayIndex++) {
                DayOfWeek day = days[dayIndex];
                
                // 2-3 periods per day for this teacher
                for (int periodIndex = 0; periodIndex < 3 && periodIndex < periods.size(); periodIndex++) {
                    Period period = periods.get(periodIndex);
                    
                    // Assign to different classes and courses
                    ClassEntity classEntity = classes.get((teacherIndex + dayIndex + periodIndex) % classes.size());
                    Course course = courses.get((teacherIndex + periodIndex) % courses.size());
                    Room room = rooms.get((teacherIndex + dayIndex) % rooms.size());
                    
                    TimetableSlot slot = new TimetableSlot();
                    slot.setDayOfWeek(day);
                    slot.setPeriod(period);
                    slot.setTeacher(teacher);
                    slot.setForClass(classEntity);
                    slot.setForCourse(course);
                    slot.setRoom(room);
                    slot.setDescription(String.format("%s - %s - %s", 
                        course.getName(), 
                        classEntity.getName(), 
                        teacher.getFirstName() + " " + teacher.getLastName()));
                    
                    slots.add(slot);
                }
            }
        }
        
        // Save all slots
        timetableSlotRepository.saveAll(slots);
        log.info("Created {} sample timetable slots for {} teachers", slots.size(), Math.min(teachers.size(), 5));
    }
}
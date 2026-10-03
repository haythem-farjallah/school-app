package com.example.school_management.feature.operational.service;

import com.example.school_management.commons.exceptions.ResourceNotFoundException;
import com.example.school_management.feature.academic.entity.ClassEntity;
import com.example.school_management.feature.academic.entity.Course;
import com.example.school_management.feature.auth.entity.Teacher;
import com.example.school_management.feature.operational.domain.TimetableLesson;
import com.example.school_management.feature.operational.domain.TimetableSolution;
import com.example.school_management.feature.operational.entity.Period;
import com.example.school_management.feature.operational.entity.Room;
import com.example.school_management.feature.operational.entity.Timetable;
import com.example.school_management.feature.operational.entity.TimetableSlot;
import com.example.school_management.feature.operational.entity.enums.DayOfWeek;
import com.example.school_management.feature.operational.repository.PeriodRepository;
import com.example.school_management.feature.operational.repository.RoomRepository;
import com.example.school_management.feature.school.service.CurrentSchoolResolver;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.example.school_management.feature.operational.repository.TimetableSlotRepository;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.solver.SolverJob;
import org.optaplanner.core.api.solver.SolverManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "optaplanner.enabled", havingValue = "true", matchIfMissing = false)
public class TimetableOptimizationService {

    private final SolverManager<TimetableSolution, UUID> solverManager;
    private final TimetableService timetableService;
    private final CurrentSchoolResolver currentSchoolResolver;
    private final TimetableSlotRepository timetableSlotRepository;
    private final ClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final RoomRepository roomRepository;
    private final PeriodRepository periodRepository;

    @Transactional
    public TimetableSolution optimizeTimetable(Long timetableId) {
        log.info("Starting timetable optimization for timetable ID: {}", timetableId);
        
        Timetable timetable = timetableService.requireSchoolTimetable(timetableId);

        // Create the problem
        TimetableSolution problem = createProblem(timetable);
        
        // Solve the problem
        UUID problemId = UUID.randomUUID();
        SolverJob<TimetableSolution, UUID> solverJob = solverManager.solve(problemId, problem);
        
        try {
            TimetableSolution solution = solverJob.getFinalBestSolution();
            log.info("Timetable optimization completed with score: {}", solution.getScore());
            
            // Save the optimized solution
            saveOptimizedSolution(timetable, solution);
            
            return solution;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error during timetable optimization", e);
            throw new RuntimeException("Timetable optimization failed", e);
        }
    }

    private TimetableSolution createProblem(Timetable timetable) {
        Long schoolId = currentSchoolResolver.resolve().getId();
        List<DayOfWeek> days = List.of(DayOfWeek.values());
        List<Period> periods = periodRepository.findBySchoolIdOrderByIndex(schoolId);
        List<Room> rooms = roomRepository.findBySchoolId(schoolId);
        List<Teacher> teachers = teacherRepository.findBySchoolId(schoolId);
        List<ClassEntity> classes = classRepository.findByAcademicYearSchoolId(schoolId);
        List<Course> courses = courseRepository.findBySchoolId(schoolId);
        classes.forEach(clazz -> validateClassContext(clazz, schoolId));
        courses.forEach(course -> validateCourseContext(course, schoolId));

        // Create lessons based on teaching assignments
        List<TimetableLesson> lessons = createLessons(timetable);

        return new TimetableSolution(days, periods, rooms, teachers, classes, courses, lessons);
    }

    private List<TimetableLesson> createLessons(Timetable timetable) {
        List<TimetableLesson> lessons = new ArrayList<>();
        long lessonId = 1;
        Long schoolId = currentSchoolResolver.resolve().getId();

        // Create lessons for each class-course combination
        for (ClassEntity classEntity : timetable.getClasses()) {
            for (Course course : classEntity.getCourses()) {
                if (!schoolId.equals(course.getSchool().getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Class has an incompatible course");
                }
                // Find the teacher for this course
                Teacher teacher = course.getTeacher();
                if (teacher == null) {
                    log.warn("No teacher assigned to course: {}", course.getName());
                    continue;
                }

                teacherRepository.findByIdAndSchoolId(teacher.getId(), schoolId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Course has an incompatible teacher"));
                // Create lessons based on weekly hours (assuming 1 hour per lesson)
                Integer weeklyHours = course.getWeeklyCapacity() != null ? course.getWeeklyCapacity() : 3;
                for (int i = 0; i < weeklyHours; i++) {
                    TimetableLesson lesson = new TimetableLesson(lessonId++, course.getName(), 1);
                    lessons.add(lesson);
                }
            }
        }

        log.info("Created {} lessons for timetable optimization", lessons.size());
        return lessons;
    }

    @Transactional
    public void saveOptimizedSolution(Timetable timetable, TimetableSolution solution) {
        Timetable ownedTimetable = timetableService.requireSchoolTimetable(timetable.getId());
        Long schoolId = currentSchoolResolver.resolve().getId();

        // Validate every optimized reference before replacing existing slots.
        List<TimetableSlot> slots = new ArrayList<>();
        
        for (TimetableLesson lesson : solution.getLessons()) {
            if (lesson.isAssigned()) {
                TimetableSlot slot = new TimetableSlot();
                slot.setTimetable(ownedTimetable);
                slot.setDayOfWeek(lesson.getDay());
                slot.setPeriod(periodRepository.findByIdAndSchoolId(lesson.getPeriod().getId(), schoolId)
                        .orElseThrow(() -> new ResourceNotFoundException("Period not found")));
                slot.setRoom(roomRepository.findByIdAndSchoolId(lesson.getRoom().getId(), schoolId)
                        .orElseThrow(() -> new ResourceNotFoundException("Room not found")));
                slot.setTeacher(teacherRepository.findByIdAndSchoolId(lesson.getTeacher().getId(), schoolId)
                        .orElseThrow(() -> new ResourceNotFoundException("Teacher not found")));
                slot.setForClass(classRepository.findByIdAndAcademicYearSchoolId(lesson.getClassEntity().getId(), schoolId)
                        .orElseThrow(() -> new ResourceNotFoundException("Class not found")));
                slot.setForCourse(courseRepository.findByIdAndSchoolId(lesson.getCourse().getId(), schoolId)
                        .orElseThrow(() -> new ResourceNotFoundException("Course not found")));
                validateClassContext(slot.getForClass(), schoolId);
                validateCourseContext(slot.getForCourse(), schoolId);
                slot.setDescription(lesson.getSubject());
                
                slots.add(slot);
            }
        }

        timetableSlotRepository.deleteByTimetableId(timetable.getId());
        timetableSlotRepository.saveAll(slots);
        log.info("Saved {} optimized timetable slots", slots.size());
    }

    private void validateClassContext(ClassEntity clazz, Long schoolId) {
        if (clazz.getAssignedRoom() != null && !schoolId.equals(clazz.getAssignedRoom().getSchool().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Class has an incompatible room");
        }
        clazz.getCourses().forEach(course -> validateCourseContext(course, schoolId));
    }

    private void validateCourseContext(Course course, Long schoolId) {
        if (!schoolId.equals(course.getSchool().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Class has an incompatible course");
        }
        if (course.getTeacher() != null && teacherRepository.findByIdAndSchoolId(course.getTeacher().getId(), schoolId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Course has an incompatible teacher");
        }
    }

    @Transactional(readOnly = true)
    public TimetableSolution getCurrentSolution(Long timetableId) {
        Timetable timetable = timetableService.requireSchoolTimetable(timetableId);

        return createProblem(timetable);
    }
} 
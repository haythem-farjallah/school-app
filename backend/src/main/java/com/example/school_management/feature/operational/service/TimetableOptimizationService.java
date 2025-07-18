package com.example.school_management.feature.operational.service;

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
import com.example.school_management.feature.operational.repository.TimetableRepository;
import com.example.school_management.feature.operational.repository.TimetableSlotRepository;
import com.example.school_management.feature.academic.repository.ClassRepository;
import com.example.school_management.feature.academic.repository.CourseRepository;
import com.example.school_management.feature.auth.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.solver.SolverJob;
import org.optaplanner.core.api.solver.SolverManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class TimetableOptimizationService {

    private final SolverManager<TimetableSolution, UUID> solverManager;
    private final TimetableRepository timetableRepository;
    private final TimetableSlotRepository timetableSlotRepository;
    private final ClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final RoomRepository roomRepository;
    private final PeriodRepository periodRepository;

    @Transactional
    public TimetableSolution optimizeTimetable(Long timetableId) {
        log.info("Starting timetable optimization for timetable ID: {}", timetableId);
        
        Timetable timetable = timetableRepository.findById(timetableId)
                .orElseThrow(() -> new RuntimeException("Timetable not found"));

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
        // Get all available resources
        List<DayOfWeek> days = List.of(DayOfWeek.values());
        List<Period> periods = periodRepository.findAll();
        List<Room> rooms = roomRepository.findAll();
        List<Teacher> teachers = teacherRepository.findAll();
        List<ClassEntity> classes = classRepository.findAll();
        List<Course> courses = courseRepository.findAll();

        // Create lessons based on teaching assignments
        List<TimetableLesson> lessons = createLessons(timetable);

        return new TimetableSolution(days, periods, rooms, teachers, classes, courses, lessons);
    }

    private List<TimetableLesson> createLessons(Timetable timetable) {
        List<TimetableLesson> lessons = new ArrayList<>();
        long lessonId = 1;

        // Create lessons for each class-course combination
        for (ClassEntity classEntity : timetable.getClasses()) {
            for (Course course : classEntity.getCourses()) {
                // Find the teacher for this course
                Teacher teacher = course.getTeacher();
                if (teacher == null) {
                    log.warn("No teacher assigned to course: {}", course.getName());
                    continue;
                }

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
        // Clear existing slots
        timetableSlotRepository.deleteByTimetableId(timetable.getId());

        // Create new slots from optimized solution
        List<TimetableSlot> slots = new ArrayList<>();
        
        for (TimetableLesson lesson : solution.getLessons()) {
            if (lesson.isAssigned()) {
                TimetableSlot slot = new TimetableSlot();
                slot.setTimetable(timetable);
                slot.setDayOfWeek(lesson.getDay());
                slot.setPeriod(lesson.getPeriod());
                slot.setRoom(lesson.getRoom());
                slot.setTeacher(lesson.getTeacher());
                slot.setForClass(lesson.getClassEntity());
                slot.setForCourse(lesson.getCourse());
                slot.setDescription(lesson.getSubject());
                
                slots.add(slot);
            }
        }

        timetableSlotRepository.saveAll(slots);
        log.info("Saved {} optimized timetable slots", slots.size());
    }

    public TimetableSolution getCurrentSolution(Long timetableId) {
        Timetable timetable = timetableRepository.findById(timetableId)
                .orElseThrow(() -> new RuntimeException("Timetable not found"));

        return createProblem(timetable);
    }
} 
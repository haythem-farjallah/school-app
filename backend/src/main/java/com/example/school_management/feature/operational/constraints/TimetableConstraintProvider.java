package com.example.school_management.feature.operational.constraints;

import com.example.school_management.feature.operational.domain.TimetableLesson;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import java.util.List;

public class TimetableConstraintProvider implements ConstraintProvider {

    private static final int MAX_PERIOD_INDEX = 7; // Adjust as needed for your school

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[]{
                // Hard constraints
                roomConflict(constraintFactory),
                teacherConflict(constraintFactory),
                classConflict(constraintFactory),
                teacherWeeklyCapacity(constraintFactory),
                classWeeklyHours(constraintFactory),
                maxLessonsPerDayPerClass(constraintFactory),
                
                // Soft constraints
                teacherPreferences(constraintFactory),
                roomCapacity(constraintFactory),
                consecutiveLessons(constraintFactory),
                avoidTeacherFirstOrLastPeriod(constraintFactory),
                minimizeTeacherGaps(constraintFactory),
                avoidDifficultSubjectsBackToBack(constraintFactory)
        };
    }

    // Hard Constraints

    /**
     * A room can only be used by one lesson at a time
     */
    private Constraint roomConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(TimetableLesson.class,
                        Joiners.equal(TimetableLesson::getDay),
                        Joiners.equal(TimetableLesson::getPeriod),
                        Joiners.equal(TimetableLesson::getRoom))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Room conflict");
    }

    /**
     * A teacher can only teach one lesson at a time
     */
    private Constraint teacherConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(TimetableLesson.class,
                        Joiners.equal(TimetableLesson::getDay),
                        Joiners.equal(TimetableLesson::getPeriod),
                        Joiners.equal(TimetableLesson::getTeacher))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Teacher conflict");
    }

    /**
     * A class can only have one lesson at a time
     */
    private Constraint classConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(TimetableLesson.class,
                        Joiners.equal(TimetableLesson::getDay),
                        Joiners.equal(TimetableLesson::getPeriod),
                        Joiners.equal(TimetableLesson::getClassEntity))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Class conflict");
    }

    /**
     * A teacher cannot exceed their weekly capacity
     */
    private Constraint teacherWeeklyCapacity(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(TimetableLesson.class)
                .filter(lesson -> lesson.getTeacher() != null && lesson.getTeacher().getWeeklyCapacity() != null)
                .filter(lesson -> lesson.getTeacher().getWeeklyCapacity() < 20) // Simplified check
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Teacher weekly capacity exceeded");
    }

    /**
     * A class must have the required weekly hours for each course
     */
    private Constraint classWeeklyHours(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(TimetableLesson.class)
                .filter(lesson -> lesson.getClassEntity() != null && lesson.getCourse() != null)
                .filter(lesson -> lesson.getCourse().getWeeklyCapacity() != null)
                .filter(lesson -> lesson.getCourse().getWeeklyCapacity() < 1) // Simplified check
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Class weekly hours not met");
    }

    /**
     * No more than N lessons per day for a class (hard constraint, N=6)
     */
    private Constraint maxLessonsPerDayPerClass(ConstraintFactory constraintFactory) {
        int MAX_LESSONS = 6;
        return constraintFactory.forEach(TimetableLesson.class)
            .groupBy(
                lesson -> lesson.getClassEntity(),
                lesson -> lesson.getDay(),
                ConstraintCollectors.count()
            )
            .filter((clazz, day, count) -> count > MAX_LESSONS)
            .penalize(HardSoftScore.ONE_HARD)
            .asConstraint("Max lessons per day per class");
    }

    // Soft Constraints

    /**
     * Prefer teachers to teach during their preferred times
     */
    private Constraint teacherPreferences(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(TimetableLesson.class)
                .filter(lesson -> lesson.getTeacher() != null && 
                        lesson.getTeacher().getSchedulePreferences() != null &&
                        !lesson.getTeacher().getSchedulePreferences().contains(lesson.getDay().name()))
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Teacher preferences");
    }

    /**
     * Prefer rooms with sufficient capacity for the class size
     */
    private Constraint roomCapacity(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(TimetableLesson.class)
                .filter(lesson -> lesson.getRoom() != null && lesson.getClassEntity() != null &&
                        lesson.getRoom().getCapacity() < lesson.getClassEntity().getMaxStudents())
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Room capacity insufficient");
    }

    /**
     * Prefer consecutive lessons for the same class
     */
    private Constraint consecutiveLessons(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(TimetableLesson.class,
                        Joiners.equal(TimetableLesson::getDay),
                        Joiners.equal(TimetableLesson::getClassEntity))
                .filter((lesson1, lesson2) -> 
                    lesson1.getPeriod() != null && lesson2.getPeriod() != null &&
                    Math.abs(lesson1.getPeriod().getIndex() - lesson2.getPeriod().getIndex()) == 1)
                .reward(HardSoftScore.ONE_SOFT)
                .asConstraint("Consecutive lessons");
    }

    /**
     * Avoid scheduling a teacher for the first or last period of the day (soft constraint)
     */
    private Constraint avoidTeacherFirstOrLastPeriod(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TimetableLesson.class)
                .filter(lesson -> lesson.getPeriod() != null && lesson.getTeacher() != null)
                .filter(lesson -> lesson.getPeriod().getIndex() == 1 || lesson.getPeriod().getIndex() == MAX_PERIOD_INDEX)
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Avoid teacher first/last period");
    }

    /**
     * Prefer to group a teacher's lessons together (minimize gaps, soft constraint)
     */
    private Constraint minimizeTeacherGaps(ConstraintFactory constraintFactory) {
        return constraintFactory.forEachUniquePair(TimetableLesson.class,
                Joiners.equal(TimetableLesson::getTeacher),
                Joiners.equal(TimetableLesson::getDay))
            .filter((l1, l2) -> l1.getPeriod() != null && l2.getPeriod() != null)
            .filter((l1, l2) -> Math.abs(l1.getPeriod().getIndex() - l2.getPeriod().getIndex()) > 1)
            .penalize(HardSoftScore.ONE_SOFT)
            .asConstraint("Minimize teacher gaps");
    }

    /**
     * Avoid scheduling difficult subjects back-to-back for a class (soft constraint)
     * Example: Math and Physics
     */
    private Constraint avoidDifficultSubjectsBackToBack(ConstraintFactory constraintFactory) {
        List<String> difficultSubjects = List.of("Math", "Physics");
        return constraintFactory.forEachUniquePair(TimetableLesson.class,
                Joiners.equal(TimetableLesson::getClassEntity),
                Joiners.equal(TimetableLesson::getDay))
            .filter((l1, l2) -> l1.getPeriod() != null && l2.getPeriod() != null)
            .filter((l1, l2) -> Math.abs(l1.getPeriod().getIndex() - l2.getPeriod().getIndex()) == 1)
            .filter((l1, l2) -> difficultSubjects.contains(l1.getSubject()) && difficultSubjects.contains(l2.getSubject()))
            .penalize(HardSoftScore.ONE_SOFT)
            .asConstraint("Avoid difficult subjects back-to-back");
    }
} 
package com.example.school_management.feature.operational.constraints;

import com.example.school_management.feature.operational.domain.TimetableLesson;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.util.List;

@ConditionalOnProperty(name = "optaplanner.enabled", havingValue = "true", matchIfMissing = false)
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
                avoidDifficultSubjectsBackToBack(constraintFactory),
                enforceRestTimeForStudents(constraintFactory),
                respectCourseDurationPeriods(constraintFactory),
                respectCourseWeeklyFrequency(constraintFactory)
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
                .groupBy(TimetableLesson::getClassEntity, TimetableLesson::getCourse, ConstraintCollectors.count())
                .filter((classEntity, course, count) -> {
                    Integer requiredHours = course.getWeeklyCapacity();
                    return requiredHours != null && count != requiredHours.longValue();
                })
                .penalize(HardSoftScore.ONE_HARD, (classEntity, course, count) -> {
                    Integer requiredHours = course.getWeeklyCapacity();
                    if (requiredHours == null) return 0;
                    return Math.abs(count.intValue() - requiredHours);
                })
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

    /**
     * Enforce rest time between classes for students (soft constraint)
     * Penalize having more than 4 consecutive classes without a break
     */
    private Constraint enforceRestTimeForStudents(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TimetableLesson.class)
            .groupBy(
                TimetableLesson::getClassEntity,
                TimetableLesson::getDay,
                ConstraintCollectors.toList()
            )
            .filter((classEntity, day, lessons) -> lessons.size() >= 4)
            .penalize(HardSoftScore.of(0, 1), (classEntity, day, lessons) -> {
                // Count consecutive sequences longer than 3
                lessons.sort((l1, l2) -> {
                    Integer i1 = l1.getPeriod() != null ? l1.getPeriod().getIndex() : 0;
                    Integer i2 = l2.getPeriod() != null ? l2.getPeriod().getIndex() : 0;
                    return i1.compareTo(i2);
                });
                
                int consecutiveCount = 1;
                int maxConsecutive = 1;
                
                for (int i = 1; i < lessons.size(); i++) {
                    Integer prevIndex = lessons.get(i-1).getPeriod() != null ? lessons.get(i-1).getPeriod().getIndex() : 0;
                    Integer currIndex = lessons.get(i).getPeriod() != null ? lessons.get(i).getPeriod().getIndex() : 0;
                    
                    if (currIndex == prevIndex + 1) {
                        consecutiveCount++;
                    } else {
                        maxConsecutive = Math.max(maxConsecutive, consecutiveCount);
                        consecutiveCount = 1;
                    }
                }
                maxConsecutive = Math.max(maxConsecutive, consecutiveCount);
                
                return Math.max(0, maxConsecutive - 3); // Penalty for more than 3 consecutive
            })
            .asConstraint("Enforce rest time for students");
    }

    /**
     * Respect course duration periods configuration (soft constraint)
     * Courses with durationPeriods > 1 should be scheduled in consecutive periods
     */
    private Constraint respectCourseDurationPeriods(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TimetableLesson.class)
            .filter(lesson -> lesson.getCourse() != null && lesson.getCourse().getDurationPeriods() != null)
            .filter(lesson -> lesson.getCourse().getDurationPeriods() > 1)
            .join(TimetableLesson.class,
                Joiners.equal(TimetableLesson::getCourse),
                Joiners.equal(TimetableLesson::getClassEntity),
                Joiners.equal(TimetableLesson::getDay),
                Joiners.filtering((l1, l2) -> l1.getPeriod() != null && l2.getPeriod() != null &&
                    Math.abs(l1.getPeriod().getIndex() - l2.getPeriod().getIndex()) == 1))
            .reward(HardSoftScore.ONE_SOFT)
            .asConstraint("Respect course duration periods");
    }

    /**
     * Respect course weekly frequency configuration (soft constraint)
     * Courses should be scheduled according to their weeklyFrequency setting
     */
    private Constraint respectCourseWeeklyFrequency(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(TimetableLesson.class)
            .filter(lesson -> lesson.getCourse() != null && lesson.getCourse().getWeeklyFrequency() != null)
            .groupBy(TimetableLesson::getCourse, TimetableLesson::getClassEntity, ConstraintCollectors.count())
            .filter((course, classEntity, count) -> {
                Integer targetFrequency = course.getWeeklyFrequency();
                return targetFrequency != null && count != targetFrequency.longValue();
            })
            .penalize(HardSoftScore.ONE_SOFT, (course, classEntity, count) -> {
                Integer targetFrequency = course.getWeeklyFrequency();
                if (targetFrequency == null) return 0;
                return Math.abs(count.intValue() - targetFrequency);
            })
            .asConstraint("Respect course weekly frequency");
    }
} 